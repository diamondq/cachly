package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.CacheStorage;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface KeyInternal<I, O> extends Key<I, O> {

  @Nullable
  public KeyInternal<Object, Object> getPreviousKey();

  public CacheStorage getLastStorage();

  public boolean supportsNull();

  public CacheLoader<I, O> getLoader();

  public void storeKeyDetails(KeyDetails<I, O> pDetails);

  public boolean hasKeyDetails();

  public @NonNull KeyInternal<Object, Object>[] getParts();

  @Override
  public String getKey();

  public String getBaseKey();
}
