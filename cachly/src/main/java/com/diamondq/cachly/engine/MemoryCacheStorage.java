package com.diamondq.cachly.engine;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

public class MemoryCacheStorage extends AbstractCacheStorage<String, String, MemoryStorageData> {

  private final ConcurrentMap<String, MemoryStorageData> mData;

  public MemoryCacheStorage() {
    super(null, "", "", String.class, MemoryStorageData.class, false, null, null, null, null, null, null);
    mData = new ConcurrentHashMap<>();
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#writeToCache(com.diamondq.cachly.engine.CommonKeyValuePair)
   */
  @Override
  protected void writeToCache(CommonKeyValuePair<String, String, MemoryStorageData> pEntry) {
    mData.put(pEntry.serKey, Objects.requireNonNull(pEntry.serValue));
    if (pEntry.expiresIn != null)
      throw new UnsupportedOperationException("Expiry is not yet supported for MemoryStorage");
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#readFromPrimaryCache(java.lang.Object)
   */
  @Override
  protected Optional<MemoryStorageData> readFromPrimaryCache(String pKey) {
    return Optional.ofNullable(mData.get(pKey));
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#invalidate(java.lang.Object, java.lang.Object)
   */
  @Override
  protected void invalidate(String pCache, @Nullable String pKey) {
    if (pKey == null)
      mData.clear();
    else
      mData.remove(pKey);
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#streamPrimary()
   */
  @Override
  protected Stream<Map.Entry<String, MemoryStorageData>> streamPrimary() {
    return mData.entrySet().stream();
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#streamMetaEntries()
   */
  @Override
  protected Stream<Entry<String, MemoryStorageData>> streamMetaEntries() {
    return streamPrimary();
  }

}