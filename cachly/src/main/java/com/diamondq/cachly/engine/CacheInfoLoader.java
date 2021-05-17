package com.diamondq.cachly.engine;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.ROOT;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CacheInfoLoader implements CacheLoader<ROOT, CacheInfo> {
  public static final String CACHE_INFO_NAME = "__CacheEngine__";

  @Inject
  public CacheInfoLoader() {
  }

  /**
   * @see com.diamondq.cachly.CacheLoader#load(com.diamondq.cachly.Cache, com.diamondq.cachly.Key)
   */
  @Override
  public CacheResult<CacheInfo> load(Cache pCache, Key<ROOT, CacheInfo> pKey) {
    return new CacheResult<>(new CacheInfo(), true);
  }

  @Override
  public boolean supportsNull() {
    return false;
  }

  /**
   * @see com.diamondq.cachly.CacheLoader#getPath()
   */
  @Override
  public String getPath() {
    return CACHE_INFO_NAME;
  }

}
