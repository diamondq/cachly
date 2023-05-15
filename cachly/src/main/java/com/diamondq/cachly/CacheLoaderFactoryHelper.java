package com.diamondq.cachly;

import com.diamondq.common.lambda.interfaces.Consumer3;
import org.jetbrains.annotations.Nullable;

public class CacheLoaderFactoryHelper {

  private static final class NullCacheLoader implements CacheLoader<@Nullable Void> {

    private final CacheLoaderInfo<@Nullable Void> mCacheLoaderInfo;

    private NullCacheLoader(Key<@Nullable Void> pKey, String pHelp) {
      mCacheLoaderInfo = new CacheLoaderInfo<>(pKey, true, pHelp, this);
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public CacheLoaderInfo<@Nullable Void> getInfo() {
      return mCacheLoaderInfo;
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<@Nullable Void> pKey,
      CacheResult<@Nullable Void> pResult) {
      pResult.setNotFound();
    }

  }

  public static CacheLoader<@Nullable Void> ofNull(Key<@Nullable Void> pKey, String pHelp) {
    return new NullCacheLoader(pKey, pHelp);
  }

  public static <V> CacheLoader<V> of(Key<V> pKey, boolean pSupportsNull, String pHelp,
    Consumer3<Cache, Key<V>, CacheResult<V>> pProvider) {
    return new CacheLoader<V>() {

      @Override
      public CacheLoaderInfo<V> getInfo() {
        return new CacheLoaderInfo<>(pKey, pSupportsNull, pHelp, this);
      }

      @Override
      public void load(Cache pCache, AccessContext pAccessContext, Key<V> pLoadKey, CacheResult<V> pResult) {
        pProvider.accept(pCache, pLoadKey, pResult);
      }
    };
  }
}
