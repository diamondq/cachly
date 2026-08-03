package com.diamondq.cachly;

import com.diamondq.common.lambda.interfaces.Consumer3;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.jspecify.annotations.Nullable;

/**
 * A Helper class that provides some pre-built Cache Loaders
 */
public final class CacheLoaderFactoryHelper {

  private CacheLoaderFactoryHelper() {}

  private static final class NullCacheLoader implements CacheLoader<Void> {

    private final @NotOnlyInitialized CacheLoaderInfo<Void> mCacheLoaderInfo;

    private NullCacheLoader(Key<Void> pKey, String pHelp) {
      mCacheLoaderInfo = new CacheLoaderInfo<>(pKey, true, pHelp, this);
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public CacheLoaderInfo<Void> getInfo() {
      return mCacheLoaderInfo;
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<Void> pKey, CacheResult<Void> pResult) {
      pResult.setNotFound();
    }

  }

  /**
   * Returns a Cache Loader that always returns not found
   *
   * @param pKey the key
   * @param pHelp the help string
   * @return the cache loader
   */
  public static CacheLoader<Void> ofNull(Key<Void> pKey, String pHelp) {
    return new NullCacheLoader(pKey, pHelp);
  }

  /**
   * Returns a Cache Loader that works by calling the Consumer
   *
   * @param pKey the key
   * @param pSupportsNull whether the loader supports null
   * @param pHelp the help string
   * @param pProvider the Consumer that will be called to load the cache value
   * @param <V> the key type
   * @return the Cache Loader
   */
  public static <V extends @Nullable Object> CacheLoader<V> of(Key<V> pKey, boolean pSupportsNull, String pHelp,
    Consumer3<Cache, Key<V>, CacheResult<V>> pProvider) {
    //noinspection Convert2Diamond
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
