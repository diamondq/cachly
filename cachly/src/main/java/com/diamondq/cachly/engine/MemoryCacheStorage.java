package com.diamondq.cachly.engine;

import com.diamondq.common.converters.ConverterManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class MemoryCacheStorage extends AbstractCacheStorage<String, String> {

  private static final class DataRecord {
    public final           Object data;
    public final @Nullable Long   expiresAt;

    private DataRecord(Object pData, @Nullable Long pExpiresAt) {
      data = pData;
      expiresAt = pExpiresAt;
    }
  }

  private final ConcurrentMap<@NotNull String, @NotNull DataRecord> mData;

  public MemoryCacheStorage(ConverterManager pConverterManager) {
    super(pConverterManager, "", "", String.class, MemoryStorageData.class, false, null, null, null, null, null, null);
    mData = new ConcurrentHashMap<>();
  }

  @Override
  protected void writeToCache(CommonKeyValuePair<String, String> pEntry) {
    mData.put(pEntry.serKey,
      new DataRecord(Objects.requireNonNull(pEntry.serValue),
        pEntry.expiresIn != null ? Instant.now().plus(pEntry.expiresIn).toEpochMilli() : null
      )
    );
  }

  @Override
  protected Optional<?> readFromPrimaryCache(String pKey) {
    DataRecord dataRecord = mData.get(pKey);
    if (dataRecord == null) return Optional.empty();
    if (dataRecord.expiresAt != null) {
      if (dataRecord.expiresAt < System.currentTimeMillis()) {
        mData.remove(pKey);
        return Optional.empty();
      }
    }
    return Optional.of(dataRecord.data);
  }

  @Override
  protected void invalidate(String pCache, @Nullable String pKey) {
    if (pKey == null) mData.clear();
    else mData.remove(pKey);
  }

  @Override
  protected Stream<Map.Entry<String, ?>> streamPrimary() {
    @SuppressWarnings({ "unchecked", "rawtypes" }) Stream<Map.Entry<String, ?>> r = (Stream) mData.entrySet()
      .stream()
      .map((entry) -> new AbstractMap.SimpleEntry(entry.getKey(), entry.getValue().data));
    return r;
  }

  @Override
  protected Stream<Entry<String, ?>> streamMetaEntries() {
    return streamPrimary();
  }

}