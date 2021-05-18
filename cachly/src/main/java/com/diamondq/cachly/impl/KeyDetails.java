package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.engine.CacheStorage;

public class KeyDetails<O> {

  private final CacheStorage   mLastStorage;

  private final boolean        mSupportsNull;

  private final CacheLoader<O> mLoader;

  public KeyDetails(CacheStorage pLastStorage, boolean pSupportsNull, CacheLoader<O> pLoader) {
    mLastStorage = pLastStorage;
    mSupportsNull = pSupportsNull;
    mLoader = pLoader;
  }

  public CacheStorage getLastStorage() {
    return mLastStorage;
  }

  public boolean supportsNull() {
    return mSupportsNull;
  }

  public CacheLoader<O> getLoader() {
    return mLoader;
  }

}
