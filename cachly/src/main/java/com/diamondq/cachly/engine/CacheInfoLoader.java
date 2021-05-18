package com.diamondq.cachly.engine;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderDetails;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@CacheLoaderDetails(path = CacheInfoLoader.CACHE_INFO_NAME)
public class CacheInfoLoader implements CacheLoader<CacheInfo> {
  public static final String CACHE_INFO_NAME = "__CacheEngine__";

  @Inject
  public CacheInfoLoader() {
  }

  /**
   * @see com.diamondq.cachly.CacheLoader#load(com.diamondq.cachly.Cache, com.diamondq.cachly.Key)
   */
  @Override
  public CacheResult<CacheInfo> load(Cache pCache, Key<CacheInfo> pKey) {
    return new CacheResult<>(new CacheInfo(), true);
  }

}
