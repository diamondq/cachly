package com.diamondq.cachly.engine;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.common.TypeReference;

import javax.inject.Singleton;

@Singleton
public class CacheInfoLoader implements CacheLoader<CacheInfo> {
  public static final String                   CACHE_INFO_NAME = "__CacheEngine__";

  public static final TypeReference<CacheInfo> TYPEREF         = new TypeReference<CacheInfo>() {
                                                                 // type reference
                                                               };

  /**
   * @see com.diamondq.cachly.CacheLoader#getInfo()
   */
  @Override
  public CacheLoaderInfo<CacheInfo> getInfo() {
    return new CacheLoaderInfo<>(KeyBuilder.of(CACHE_INFO_NAME, TYPEREF), false, "", this);
  }

  /**
   * @see com.diamondq.cachly.CacheLoader#load(com.diamondq.cachly.Cache, com.diamondq.cachly.Key)
   */
  @Override
  public CacheResult<CacheInfo> load(Cache pCache, Key<CacheInfo> pKey) {
    return new CacheResult<>(new CacheInfo(), true);
  }

}
