package com.diamondq.cachly.engine;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.common.TypeReference;

import jakarta.inject.Singleton;

@Singleton
@javax.inject.Singleton
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
   * @see com.diamondq.cachly.CacheLoader#load(com.diamondq.cachly.Cache, com.diamondq.cachly.AccessContext,
   *      com.diamondq.cachly.Key, com.diamondq.cachly.CacheResult)
   */
  @Override
  public void load(Cache pCache, AccessContext pAccessContext, Key<CacheInfo> pKey, CacheResult<CacheInfo> pResult) {
    pResult.setValue(new CacheInfo());
  }

}
