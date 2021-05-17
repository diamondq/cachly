package com.diamondq.cachly.engine;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.impl.KeyInternal;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MemoryCacheStorage implements CacheStorage {

  private final ConcurrentMap<String, Object> mData;

  public MemoryCacheStorage() {
    mData = new ConcurrentHashMap<>();
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#queryForKey(com.diamondq.cachly.impl.KeyInternal)
   */
  @Override
  public <V> CacheResult<V> queryForKey(KeyInternal<?, V> pKey) {
    Object obj = mData.get(pKey.toString());
    if (obj == null)
      return CacheResult.notFound();
    @SuppressWarnings("unchecked")
    V v = (V) obj;
    return new CacheResult<V>(v, true);
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#store(com.diamondq.cachly.impl.KeyInternal,
   *      com.diamondq.cachly.CacheResult)
   */
  @Override
  public <V> void store(KeyInternal<?, V> pKey, CacheResult<V> pLoadedResult) {
    if (pLoadedResult.entryFound() == true)
      mData.put(pKey.toString(), pLoadedResult.getValue());
    else
      mData.remove(pKey.toString());
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidate(com.diamondq.cachly.impl.KeyInternal)
   */
  @Override
  public <V> void invalidate(KeyInternal<?, V> pKey) {
    mData.remove(pKey.toString());
  }

  @Override
  public List<String> getBasePaths() {
    throw new IllegalStateException();
  }

}