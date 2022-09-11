package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.engine.CacheStorage;

public class KeyDetails<O> {

  private final CacheStorage mLastStorage;

  private final String mLastSerializerName;

  private final boolean mSupportsNull;

  private final CacheLoader<O> mLoader;

  public KeyDetails(CacheStorage pLastStorage, String pLastSerializerName, boolean pSupportsNull,
    CacheLoader<O> pLoader) {
    mLastStorage = pLastStorage;
    mLastSerializerName = pLastSerializerName;
    mSupportsNull = pSupportsNull;
    mLoader = pLoader;
  }

  public CacheStorage getLastStorage() {
    return mLastStorage;
  }

  public String getLastSerializerName() {
    return mLastSerializerName;
  }

  public boolean supportsNull() {
    return mSupportsNull;
  }

  public CacheLoader<O> getLoader() {
    return mLoader;
  }

}
