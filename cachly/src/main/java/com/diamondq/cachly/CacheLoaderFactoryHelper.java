package com.diamondq.cachly;

import com.diamondq.common.lambda.interfaces.Function2;

import org.checkerframework.checker.nullness.qual.Nullable;

public class CacheLoaderFactoryHelper {

  private static class NullCacheLoader implements CacheLoader<@Nullable Void> {

    private final CacheLoaderInfo<@Nullable Void> mCacheLoaderInfo;

    public NullCacheLoader(Key<@Nullable Void> pKey, String pHelp) {
      mCacheLoaderInfo = new CacheLoaderInfo<>(pKey, true, pHelp, this);
    }

    @Override
    public CacheResult<@Nullable Void> load(Cache pCache, Key<@Nullable Void> pKey) {
      return new CacheResult<>(null, true);
    }

    @Override
    public CacheLoaderInfo<@Nullable Void> getInfo() {
      return mCacheLoaderInfo;
    }
  }

  public static CacheLoader<@Nullable Void> ofNull(Key<@Nullable Void> pKey, String pHelp) {
    return new NullCacheLoader(pKey, pHelp);
  }

  public static <V> CacheLoader<V> of(Key<V> pKey, boolean pSupportsNull, String pHelp,
    Function2<Cache, Key<V>, CacheResult<V>> pProvider) {
    return new CacheLoader<V>() {

      /**
       * @see com.diamondq.cachly.CacheLoader#getInfo()
       */
      @Override
      public CacheLoaderInfo<V> getInfo() {
        return new CacheLoaderInfo<>(pKey, pSupportsNull, pHelp, this);
      }

      /**
       * @see com.diamondq.cachly.CacheLoader#load(com.diamondq.cachly.Cache, com.diamondq.cachly.Key)
       */
      @Override
      public CacheResult<V> load(Cache pCache, Key<V> pLoadKey) {
        return pProvider.apply(pCache, pLoadKey);
      }
    };
  }
}
