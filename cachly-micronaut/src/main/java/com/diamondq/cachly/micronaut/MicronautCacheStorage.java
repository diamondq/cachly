package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.impl.KeyInternal;

import java.util.List;

public class MicronautCacheStorage implements CacheStorage {

  private final CachlyMicronautConfiguration mConfig;

  public MicronautCacheStorage(CachlyMicronautConfiguration pConfig) {
    mConfig = pConfig;
  }

  @Override
  public <V> CacheResult<V> queryForKey(KeyInternal<?, V> pKey) {
    return CacheResult.notFound();
  }

  @Override
  public <V> void store(KeyInternal<?, V> pKey, CacheResult<V> pLoadedResult) {
    // TODO Auto-generated method stub

  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#getBasePaths()
   */
  @Override
  public List<String> getBasePaths() {
    return mConfig.getPaths();
  }

}
