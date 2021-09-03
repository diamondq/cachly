package com.diamondq.cachly.engine;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.impl.StaticCacheResult;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

public class MemoryCacheStorage extends AbstractNonSerializingCacheStorage<String, MemoryStorageData> {

  private final ConcurrentMap<String, MemoryStorageData> mData;

  public MemoryCacheStorage() {
    super("", null);
    mData = new ConcurrentHashMap<>();
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractNonSerializingCacheStorage#serializeValue(com.diamondq.cachly.Key,
   *      java.lang.Object)
   */
  @Override
  protected MemoryStorageData serializeValue(Key<?> pKey, @Nullable Object pValue) {
    return new MemoryStorageData(pKey, pValue);
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractNonSerializingCacheStorage#deserializeValue(java.lang.Object)
   */
  @Override
  protected Map.Entry<Key<?>, CacheResult<?>> deserializeValue(MemoryStorageData pValue) {
    return new SimpleEntry<Key<?>, CacheResult<?>>(pValue.key,
      new StaticCacheResult<@Nullable Object>(pValue.value, true));
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCommonCacheStorage#writeToCache(com.diamondq.cachly.engine.CommonKeyValuePair)
   */
  @Override
  protected void writeToCache(NonSerializedKeyValuePair<String, MemoryStorageData> pEntry) {
    mData.put(pEntry.serKey, Objects.requireNonNull(pEntry.serValue));
    if (pEntry.expiresIn != null)
      throw new UnsupportedOperationException("Expiry is not yet supported for MemoryStorage");
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCommonCacheStorage#readFromPrimaryCache(java.lang.Object)
   */
  @Override
  protected Optional<MemoryStorageData> readFromPrimaryCache(String pKey) {
    return Optional.ofNullable(mData.get(pKey));
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCommonCacheStorage#invalidate(java.lang.Object, java.lang.Object)
   */
  @Override
  protected void invalidate(String pCache, @Nullable String pKey) {
    if (pKey == null)
      mData.clear();
    else
      mData.remove(pKey);
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCommonCacheStorage#streamPrimary()
   */
  @Override
  protected Stream<Map.Entry<String, MemoryStorageData>> streamPrimary() {
    return mData.entrySet().stream();
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCommonCacheStorage#streamMetaEntries()
   */
  @Override
  protected Stream<Entry<String, MemoryStorageData>> streamMetaEntries() {
    return streamPrimary();
  }

}