package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.engine.CacheStorage;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;

@Factory
public class CachlyMicronautStorageFactory {

  @EachBean(CachlyMicronautConfiguration.class)
  public CacheStorage createCacheStorage(CachlyMicronautConfiguration pConfig) {
    return new MicronautCacheStorage(pConfig);
  }

}
