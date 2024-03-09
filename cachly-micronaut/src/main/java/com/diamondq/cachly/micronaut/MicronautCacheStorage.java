package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.engine.AbstractCacheStorage;
import com.diamondq.cachly.engine.CommonKeyValuePair;
import com.diamondq.cachly.engine.MemoryStorageData;
import com.diamondq.cachly.impl.CacheCallbackHandler;
import com.diamondq.common.converters.ConverterManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * Implements the Cache Storage for interacting with Micronaut Caches
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
@EachBean(SyncCache.class)
public class MicronautCacheStorage extends AbstractCacheStorage<SyncCache<?>, String> {

  private final List<KeyExtractor> mKeyExtractors;

  private final List<ExpiryHandler> mExpiryHandlers;

  /**
   * Injection Constructor
   *
   * @param pConverterManager the Converter Manager
   * @param pExecutorService the Executor Service
   * @param pHandler the Callback Handler bridge
   * @param pPrimaryCache the Micronaut cache for storage
   * @param pKeyExtractors the list of key extractors to get the contents from the native cache
   * @param pExpiryHandlers the list of handlers for expiry
   */
  @Inject
  public MicronautCacheStorage(ConverterManager pConverterManager,
    @Named(TaskExecutors.IO) ExecutorService pExecutorService, CacheCallbackHandler pHandler,
    SyncCache<?> pPrimaryCache, List<KeyExtractor> pKeyExtractors, List<ExpiryHandler> pExpiryHandlers) {
    super(pConverterManager, pExecutorService,

      /* The cache object */

      pPrimaryCache,

      /* There is no meta cache. Metadata is stored in the primary cache */

      null,

      /* The key type */

      String.class,

      /* The value type is either a byte[] if we're serializing or a MemoryStorageData if we're not */

      (pPrimaryCache instanceof CachlySyncCache ? (((CachlySyncCache) pPrimaryCache).getPerformSerialization() ? byte[].class : MemoryStorageData.class) : MemoryStorageData.class),

      /* Indicate whether we're serializing */

      (!(pPrimaryCache instanceof CachlySyncCache) || ((CachlySyncCache) pPrimaryCache).getPerformSerialization()),

      /* Default string, type, key, value prefixes */

      null, null, null, null,

      /* Default key serializers/deserializers, since the key is a String */

      null, null
    );
    mKeyExtractors = pKeyExtractors;
    mExpiryHandlers = pExpiryHandlers;
    pHandler.registerCacheStorage(mPrimaryCache.getNativeCache(), this);
    init();
  }

  @Override
  protected void writeToCache(CommonKeyValuePair<SyncCache<?>, String> pEntry) {
    @Nullable Duration expiresIn = pEntry.expiresIn;
    if (expiresIn != null) for (ExpiryHandler eh : mExpiryHandlers)
      eh.markForExpiry(pEntry.serKey, expiresIn);
    pEntry.cache.put(pEntry.serKey, Objects.requireNonNull(pEntry.serValue));
  }

  @Override
  protected Optional<?> readFromPrimaryCache(String pKey) {
    return mPrimaryCache.get(pKey, mSerValueClass);
  }

  @Override
  protected Stream<Entry<String, ?>> streamPrimary() {
    Object nativeCache = mPrimaryCache.getNativeCache();
    for (KeyExtractor ke : mKeyExtractors) {
      @Nullable Stream<Entry<String, Object>> entries = ke.getEntries(nativeCache);
      if (entries != null) return entries.map((entry) -> {
        Object value = entry.getValue();
        if (mSerializeValue) {

          /* Shortcut if the data is a byte[] */

          if (value instanceof byte[]) {
            return entry;
          }
          return new SimpleEntry<String, Object>(entry.getKey(), mConverterManager.convert(value, byte[].class));
        }
        /* Shortcut if the data is a MemoryStorageData */

        if (value instanceof MemoryStorageData) {
          return entry;
        }

        return new SimpleEntry<String, Object>(entry.getKey(),
          mConverterManager.convert(value, MemoryStorageData.class)
        );
      });
    }
    throw new IllegalStateException(
      "The cache " + nativeCache.getClass().getName() + " is not able to be key iterated");
  }

  @Override
  protected Stream<Entry<String, ?>> streamMetaEntries() {
    return streamPrimary();
  }

  @Override
  protected void invalidate(SyncCache<?> pCache, @Nullable String pKey) {
    if (pKey == null) {
      for (ExpiryHandler eh : mExpiryHandlers)
        eh.invalidateAll();
      pCache.invalidateAll();
    } else {
      for (ExpiryHandler eh : mExpiryHandlers)
        eh.invalidate(pKey);
      pCache.invalidate(pKey);
    }
  }

}
