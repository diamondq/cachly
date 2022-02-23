package com.diamondq.cachly.engine;

import com.diamondq.common.converters.ConverterManager;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MemoryCacheStorage extends AbstractCacheStorage<String, String> {

  private static class DataRecord {
    public final Object data;
    public final @Nullable Long expiresAt;

    public DataRecord(Object pData, @Nullable Long pExpiresAt) {
      this.data = pData;
      this.expiresAt = pExpiresAt;
    }
  }
  private final ConcurrentMap<@NonNull String, @NonNull DataRecord> mData;

  public MemoryCacheStorage(ConverterManager pConverterManager) {
    super(pConverterManager, "", "", String.class, MemoryStorageData.class, false, null, null, null, null, null, null);
    mData = new ConcurrentHashMap<>();
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#writeToCache(com.diamondq.cachly.engine.CommonKeyValuePair)
   */
  @Override
  protected void writeToCache(CommonKeyValuePair<String, String> pEntry) {
    mData.put(pEntry.serKey, new DataRecord(Objects.requireNonNull(pEntry.serValue), pEntry.expiresIn != null ? Instant.now().plus(pEntry.expiresIn).toEpochMilli() : null));
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#readFromPrimaryCache(java.lang.Object)
   */
  @Override
  protected Optional<@NonNull ?> readFromPrimaryCache(String pKey) {
    DataRecord dataRecord = mData.get(pKey);
    if (dataRecord == null)
      return Optional.empty();
    if (dataRecord.expiresAt != null) {
      if (dataRecord.expiresAt < System.currentTimeMillis()) {
        mData.remove(pKey);
        return Optional.empty();
      }
    }
    return Optional.of(dataRecord.data);
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
  protected Stream<Map.Entry<String, @NonNull ?>> streamPrimary() {
    @SuppressWarnings({"unchecked", "rawtypes"})
    Stream<Map.Entry<String, @NonNull ?>> r = (Stream) mData.entrySet().stream().map((entry)-> new AbstractMap.SimpleEntry(entry.getKey(), entry.getValue().data));
    return r;
  }

  /**
   * @see com.diamondq.cachly.engine.AbstractCacheStorage#streamMetaEntries()
   */
  @Override
  protected Stream<Entry<String, @NonNull ?>> streamMetaEntries() {
    return streamPrimary();
  }

}