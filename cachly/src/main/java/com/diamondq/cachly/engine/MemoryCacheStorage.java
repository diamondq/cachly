package com.diamondq.cachly.engine;

import com.diamondq.cachly.CacheKeyEvent;
import com.diamondq.cachly.impl.CacheCallbackHandler;
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
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * Cache Storage of entries into memory
 */
public class MemoryCacheStorage extends AbstractCacheStorage<String, String> {

  private final CacheCallbackHandler mHandler;

  private static final class DataRecord {
    public final           Object data;
    public final @Nullable Long   expiresAt;

    private DataRecord(Object pData, @Nullable Long pExpiresAt) {
      data = pData;
      expiresAt = pExpiresAt;
    }
  }

  private final ConcurrentMap<@NotNull String, @NotNull DataRecord> mData;

  /**
   * Primary constructor
   *
   * @param pConverterManager the Converter Manager
   * @param pExecutorService the Executor Service
   * @param pHandler the Handler
   */
  public MemoryCacheStorage(ConverterManager pConverterManager, ExecutorService pExecutorService,
    CacheCallbackHandler pHandler) {
    super(pConverterManager,
      pExecutorService,
      "",
      "",
      String.class,
      MemoryStorageData.class,
      false,
      null,
      null,
      null,
      null,
      null,
      null
    );
    mHandler = pHandler;
    mData = new ConcurrentHashMap<>();
    pHandler.registerCacheStorage(mData, this);
  }

  @Override
  protected void writeToCache(CommonKeyValuePair<String, String> pEntry) {
    var hasOld = mData.put(pEntry.serKey,
      new DataRecord(Objects.requireNonNull(pEntry.serValue),
        pEntry.expiresIn != null ? Instant.now().plus(pEntry.expiresIn).toEpochMilli() : null
      )
    ) != null;
    mHandler.handleEvent(mData, pEntry.serKey, hasOld ? CacheKeyEvent.MODIFIED : CacheKeyEvent.ADDED, pEntry.serValue);
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
    else {
      var origValue = mData.remove(pKey);
      if (origValue != null) mHandler.handleEvent(mData, pKey, CacheKeyEvent.REMOVED, origValue.data);
    }
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