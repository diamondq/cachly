package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheResult;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

public class StaticCacheResult<V extends @Nullable Object> implements CacheResult<V> {

  public static final CacheResult<Object> sNOT_FOUND = new StaticCacheResult<>();

  private @Nullable Duration mDuration;

  private @Nullable V mValue;

  private boolean mFound;

  public StaticCacheResult() {
    mValue = null;
    mFound = false;
  }

  public StaticCacheResult(@Nullable V pValue, boolean pFound) {
    mValue = pValue;
    mFound = pFound;
  }

  @SuppressWarnings("SuspiciousGetterSetter")
  @Override
  public @Nullable Duration getOverrideExpiry() {
    return mDuration;
  }

  @Override
  public CacheResult<V> setOverrideExpiry(@Nullable Duration pDuration) {
    mDuration = pDuration;
    return this;
  }

  @Override
  public V getValue() {
    return Objects.requireNonNull(mValue);
  }

  @Override
  public CacheResult<V> setValue(@Nullable V pValue) {
    mValue = pValue;
    mFound = (pValue != null);
    return this;
  }

  @Override
  public CacheResult<V> setNullableVaue(@Nullable V pValue) {
    mValue = pValue;
    mFound = true;
    return this;
  }

  @Override
  public boolean entryFound() {
    return mFound;
  }

  @Override
  public CacheResult<V> setNotFound() {
    mFound = false;
    mValue = null;
    return this;
  }

  @Override
  public boolean isNull() {
    return mValue == null;
  }
}
