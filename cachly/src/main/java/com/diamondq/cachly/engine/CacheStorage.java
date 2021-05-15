package com.diamondq.cachly.engine;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.impl.KeyInternal;

import java.util.List;

public interface CacheStorage {

  <V> CacheResult<V> queryForKey(KeyInternal<?, V> pKey);

  <V> void store(KeyInternal<?, V> pKey, CacheResult<V> pLoadedResult);

  List<String> getBasePaths();

}
