package com.diamondq.cachly.engine;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.impl.StaticCacheResult;
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
   * @see com.diamondq.cachly.engine.CacheStorage#queryForKey(com.diamondq.cachly.AccessContext,
   *      com.diamondq.cachly.spi.KeySPI)
   */
  @Override
  public <V> CacheResult<V> queryForKey(AccessContext pAccessContext, KeySPI<V> pKey) {
    Object obj = mData.get(pKey.toString());
    if (obj == null)
      return CacheResult.notFound();
    @SuppressWarnings("unchecked")
    V v = (V) obj;
    return new StaticCacheResult<V>(v, true);
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#store(com.diamondq.cachly.AccessContext,
   *      com.diamondq.cachly.spi.KeySPI, com.diamondq.cachly.CacheResult)
   */
  @Override
  public <V> void store(AccessContext pAccessContext, KeySPI<V> pKey, CacheResult<V> pLoadedResult) {
    if (pLoadedResult.entryFound() == true)
      mData.put(pKey.toString(), pLoadedResult.getValue());
    else
      mData.remove(pKey.toString());
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidate(com.diamondq.cachly.AccessContext,
   *      com.diamondq.cachly.spi.KeySPI)
   */
  @Override
  public <V> void invalidate(AccessContext pAccessContext, KeySPI<V> pKey) {
    mData.remove(pKey.toString());
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidateAll(com.diamondq.cachly.AccessContext)
   */
  @Override
  public void invalidateAll(AccessContext pAccessContext) {
    mData.clear();
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#streamKeys(com.diamondq.cachly.AccessContext)
   */
  @Override
  public Stream<String> streamKeys(AccessContext pAccessContext) {
    return mData.keySet().stream();
  }
}