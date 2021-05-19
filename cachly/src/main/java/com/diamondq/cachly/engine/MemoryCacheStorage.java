package com.diamondq.cachly.engine;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.spi.KeySPI;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class MemoryCacheStorage implements CacheStorage {

  private final ConcurrentMap<String, Object> mData;

  public MemoryCacheStorage() {
    mData = new ConcurrentHashMap<>();
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#queryForKey(com.diamondq.cachly.spi.KeySPI)
   */
  @Override
  public <V> CacheResult<V> queryForKey(KeySPI<V> pKey) {
    Object obj = mData.get(pKey.toString());
    if (obj == null)
      return CacheResult.notFound();
    @SuppressWarnings("unchecked")
    V v = (V) obj;
    return new CacheResult<V>(v, true);
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#store(com.diamondq.cachly.spi.KeySPI, com.diamondq.cachly.CacheResult)
   */
  @Override
  public <V> void store(KeySPI<V> pKey, CacheResult<V> pLoadedResult) {
    if (pLoadedResult.entryFound() == true)
      mData.put(pKey.toString(), pLoadedResult.getValue());
    else
      mData.remove(pKey.toString());
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidate(com.diamondq.cachly.spi.KeySPI)
   */
  @Override
  public <V> void invalidate(KeySPI<V> pKey) {
    mData.remove(pKey.toString());
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidateAll()
   */
  @Override
  public void invalidateAll() {
    mData.clear();
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#streamKeys()
   */
  @Override
  public Stream<String> streamKeys() {
    return mData.keySet().stream();
  }
}