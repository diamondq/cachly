package com.diamondq.cachly.base;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.Key;

/**
 * This abstract base class simplifies the implementation of a CacheLoader
 *
 * @param <VALUE> the type of the data returned
 */
public abstract class AbstractCacheLoader<VALUE> implements CacheLoader<VALUE> {

  protected final CacheLoaderInfo<VALUE> mCacheLoaderInfo;

  /**
   * Constructor
   *
   * @param pKey the key that is returned
   * @param pSupportsNull whether null is a valid value
   * @param pHelp help describing this loader
   */
  public AbstractCacheLoader(Key<VALUE> pKey, boolean pSupportsNull, String pHelp) {
    mCacheLoaderInfo = new CacheLoaderInfo<>(pKey, pSupportsNull, pHelp, this);
  }

  /**
   * @see com.diamondq.cachly.CacheLoader#getInfo()
   */
  @Override
  public CacheLoaderInfo<VALUE> getInfo() {
    return mCacheLoaderInfo;
  }
}
