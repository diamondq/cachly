package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.engine.AbstractSerializingCacheStorage;
import com.diamondq.common.converters.ConverterManager;

import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.EachBean;

@EachBean(SyncCache.class)
public class MicronautCacheStorage extends AbstractSerializingCacheStorage<SyncCache<?>, String, byte[]> {

  private final List<KeyExtractor>  mKeyExtractors;

  private final List<ExpiryHandler> mExpiryHandlers;

  @Inject
  public MicronautCacheStorage(ConverterManager pConverterManager, SyncCache<?> pCache,
    List<KeyExtractor> pKeyExtractors, List<ExpiryHandler> pExpiryHandlers) {
    super(pConverterManager, pCache, null, String.class, byte[].class, null, null, null, null, null, null);
    mKeyExtractors = pKeyExtractors;
    mExpiryHandlers = pExpiryHandlers;
    init();
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCommonCacheStorage#writeToCache(com.diamondq.cachly.engine.CommonKeyValuePair)
   */
  @Override
  protected void writeToCache(SerializedKeyValuePair<SyncCache<?>, String, byte[]> pEntry) {
    Duration expiresIn = pEntry.expiresIn;
    if (expiresIn != null)
      for (ExpiryHandler eh : mExpiryHandlers)
        eh.markForExpiry(pEntry.serKey, expiresIn);
    pEntry.cache.put(pEntry.serKey, Objects.requireNonNull(pEntry.serValue));
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCommonCacheStorage#readFromPrimaryCache(java.lang.Object)
   */
  @Override
  protected Optional<byte[]> readFromPrimaryCache(String pKey) {
    return mPrimaryCache.get(pKey, byte[].class);
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCommonCacheStorage#streamPrimary()
   */
  @Override
  protected Stream<Entry<String, byte[]>> streamPrimary() {
    Object nativeCache = mPrimaryCache.getNativeCache();
    for (KeyExtractor ke : mKeyExtractors) {
      Stream<Entry<String, Object>> entries = ke.getEntries(nativeCache);
      if (entries != null)
        return entries.map((entry) -> {
          Object value = entry.getValue();
          if (value instanceof byte[]) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Entry<String, byte[]> b = (Entry) entry;
            return b;
          }
          return new SimpleEntry<String, byte[]>(entry.getKey(), mConverterManager.convert(value, byte[].class));
        });
    }
    throw new IllegalStateException(
      "The cache " + nativeCache.getClass().getName() + " is not able to be key iterated");
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractSerializingCacheStorage#streamMetaEntries()
   */
  @Override
  protected Stream<Entry<String, byte[]>> streamMetaEntries() {
    return streamPrimary();
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractSerializingCacheStorage#invalidate(java.lang.Object, java.lang.Object)
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
