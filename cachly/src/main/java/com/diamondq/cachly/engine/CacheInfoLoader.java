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

/**
 * Cache Loader representing the internal CacheInfo data
 */
@Singleton
public class CacheInfoLoader implements CacheLoader<CacheInfo> {
  /**
   * The key name used for the Cache Information storage
   */
  public static final String CACHE_INFO_NAME = "__CacheEngine__";

  /**
   * The TypeReference for the CacheInfo
   */
  public static final TypeReference<CacheInfo> TYPEREF = new TypeReference<>() {
  };

  @Override
  public CacheLoaderInfo<CacheInfo> getInfo() {
    return new CacheLoaderInfo<>(KeyBuilder.of(CACHE_INFO_NAME, TYPEREF), false, "", this);
  }

  @Override
  public void load(Cache pCache, AccessContext pAccessContext, Key<CacheInfo> pKey, CacheResult<CacheInfo> pResult) {
    pResult.setValue(new CacheInfo());
  }

}
