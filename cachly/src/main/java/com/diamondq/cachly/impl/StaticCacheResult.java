package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheResult;

import java.time.Duration;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StaticCacheResult<V> implements CacheResult<V> {

  public static final CacheResult<Object> sNOT_FOUND = new StaticCacheResult<>();

  private @Nullable Duration              mDuration;

  private V                               mValue;

  private boolean                         mFound     = false;

  public StaticCacheResult() {
    mValue = null;
    mFound = false;
  }

  public StaticCacheResult(V pValue, boolean pFound) {
    mValue = pValue;
    mFound = pFound;
  }

  /**
   * @see com.diamondq.cachly.CacheResult#getOverrideExpiry()
   */
  @Override
  public @Nullable Duration getOverrideExpiry() {
    return mDuration;
  }

  /**
   * @see com.diamondq.cachly.CacheResult#setOverrideExpiry(java.time.Duration)
   */
  @Override
  public CacheResult<V> setOverrideExpiry(@Nullable Duration pDuration) {
    mDuration = pDuration;
    return this;
  }

  /**
   * @see com.diamondq.cachly.CacheResult#getValue()
   */
  @Override
  public @NonNull V getValue() {
    return Objects.requireNonNull(mValue);
  }

  /**
   * @see com.diamondq.cachly.CacheResult#setValue(java.lang.Object)
   */
  @Override
  public CacheResult<V> setValue(V pValue) {
    mValue = pValue;
    mFound = true;
    return this;
  }

  /**
   * @see com.diamondq.cachly.CacheResult#entryFound()
   */
  @Override
  public boolean entryFound() {
    return mFound;
  }

  /**
   * @see com.diamondq.cachly.CacheResult#setNotFound()
   */
  @Override
  public void setNotFound() {
    mFound = false;
    mValue = null;
  }

  /**
   * @see com.diamondq.cachly.CacheResult#isNull()
   */
  @Override
  public boolean isNull() {
    return mValue == null;
  }
}
