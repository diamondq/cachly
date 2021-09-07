package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.engine.AbstractCacheStorage;
import com.diamondq.cachly.engine.CommonKeyValuePair;
import com.diamondq.cachly.engine.MemoryStorageData;
import com.diamondq.common.converters.ConverterManager;

import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.EachBean;

@EachBean(SyncCache.class)
public class MicronautCacheStorage extends AbstractCacheStorage<SyncCache<?>, String> {

  private final List<KeyExtractor>  mKeyExtractors;

  private final List<ExpiryHandler> mExpiryHandlers;

  @Inject
  public MicronautCacheStorage(ConverterManager pConverterManager, SyncCache<?> pCache,
    List<KeyExtractor> pKeyExtractors, List<ExpiryHandler> pExpiryHandlers) {
    super(pConverterManager,

      /* The cache object */

      pCache,

      /* There is no meta cache. Metadata is stored in the primary cache */

      null,

      /* The key type */

      String.class,

      /* The value type is either a byte[] if we're serializing or a MemoryStorageData if we're not */

      (pCache instanceof CachlySyncCache
        ? (((CachlySyncCache) pCache).getPerformSerialization() == true ? byte[].class : MemoryStorageData.class)
        : MemoryStorageData.class),

      /* Indicate whether we're serializing */

      (pCache instanceof CachlySyncCache ? ((CachlySyncCache) pCache).getPerformSerialization() : true),

      /* Default string, type, key, value prefixes */

      null, null, null, null,

      /* Default key serializers/deserializers, since the key is a String */

      null, null);
    mKeyExtractors = pKeyExtractors;
    mExpiryHandlers = pExpiryHandlers;
    init();
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#writeToCache(com.diamondq.cachly.engine.CommonKeyValuePair)
   */
  @Override
  protected void writeToCache(CommonKeyValuePair<SyncCache<?>, String> pEntry) {
    Duration expiresIn = pEntry.expiresIn;
    if (expiresIn != null)
      for (ExpiryHandler eh : mExpiryHandlers)
        eh.markForExpiry(pEntry.serKey, expiresIn);
    pEntry.cache.put(pEntry.serKey, Objects.requireNonNull(pEntry.serValue));
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#readFromPrimaryCache(java.lang.Object)
   */
  @Override
  protected Optional<@NonNull ?> readFromPrimaryCache(String pKey) {
    return mPrimaryCache.get(pKey, mSerValueClass);
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#streamPrimary()
   */
  @Override
  protected Stream<Entry<String, @NonNull ?>> streamPrimary() {
    Object nativeCache = mPrimaryCache.getNativeCache();
    for (KeyExtractor ke : mKeyExtractors) {
      Stream<Entry<String, Object>> entries = ke.getEntries(nativeCache);
      if (entries != null)
        return entries.map((entry) -> {
          Object value = entry.getValue();
          if (mSerializeValue) {

            /* Short cut if the data is a byte[] */

            if (value instanceof byte[]) {
              Entry<String, Object> b = entry;
              return b;
            }
            return new SimpleEntry<String, Object>(entry.getKey(), mConverterManager.convert(value, byte[].class));
          }
          /* Short cut if the data is a MemoryStorageData */

          if (value instanceof MemoryStorageData) {
            Entry<String, Object> b = entry;
            return b;
          }

          return new SimpleEntry<String, Object>(entry.getKey(),
            mConverterManager.convert(value, MemoryStorageData.class));
        });
    }
    throw new IllegalStateException(
      "The cache " + nativeCache.getClass().getName() + " is not able to be key iterated");
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#streamMetaEntries()
   */
  @Override
  protected Stream<Entry<String, @NonNull ?>> streamMetaEntries() {
    return streamPrimary();
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#invalidate(java.lang.Object, java.lang.Object)
   */
  @Override
  protected void invalidate(SyncCache<?> pCache, @Nullable String pKey) {
    if (pKey == null) {
      for (ExpiryHandler eh : mExpiryHandlers)
        eh.invalidateAll();
      pCache.invalidateAll();
    }
    else {
      for (ExpiryHandler eh : mExpiryHandlers)
        eh.invalidate(pKey);
      pCache.invalidate(pKey);
    }
  }

}
