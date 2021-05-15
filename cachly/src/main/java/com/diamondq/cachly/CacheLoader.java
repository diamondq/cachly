package com.diamondq.cachly;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface CacheLoader<I, O> {

  public CacheResult<O> load(Key<I, O> pKey, @Nullable I pObject);

  public boolean supportsNull();

  public String getPath();

}
