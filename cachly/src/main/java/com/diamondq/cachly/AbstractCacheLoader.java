package com.diamondq.cachly;

public abstract class AbstractCacheLoader<I, O> implements CacheLoader<I, O> {

  protected final boolean mSupportsNull;

  protected final String  mPath;

  public AbstractCacheLoader(boolean pSupportsNull, String pPath) {
    mSupportsNull = pSupportsNull;
    mPath = pPath;
  }

  public AbstractCacheLoader(boolean pSupportsNull, Key<I, O> pKey) {
    mSupportsNull = pSupportsNull;
    mPath = pKey.toString();
  }

  /**
   * @see com.diamondq.cachly.CacheLoader#getPath()
   */
  @Override
  public String getPath() {
    return mPath;
  }

  /**
   * @see com.diamondq.cachly.CacheLoader#supportsNull()
   */
  @Override
  public boolean supportsNull() {
    return mSupportsNull;
  }

}
