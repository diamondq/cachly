package com.diamondq.cachly.engine;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.spi.KeySPI;

public interface CacheStorage {

  <V> CacheResult<V> queryForKey(KeySPI<V> pKey);

  <V> void store(KeySPI<V> pKey, CacheResult<V> pLoadedResult);

  <V> void invalidate(KeySPI<V> pKey);

}
