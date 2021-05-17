package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.engine.CacheStorage;

import io.micronaut.cache.CacheManager;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;

@Factory
public class CachlyMicronautStorageFactory {

  private final CacheManager<?> mCacheManager;

  public CachlyMicronautStorageFactory(CacheManager<?> pCacheManager) {
    mCacheManager = pCacheManager;
  }

  @EachBean(CachlyMicronautConfiguration.class)
  public CacheStorage createCacheStorage(CachlyMicronautConfiguration pConfig) {
    return new MicronautCacheStorage(pConfig, mCacheManager);
  }

}
