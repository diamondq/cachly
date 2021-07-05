package com.diamondq.cachly;

import com.diamondq.cachly.impl.StaticCacheResult;

import java.time.Duration;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface CacheResult<V> {

  public @Nullable Duration getOverrideExpiry();

  public CacheResult<V> setOverrideExpiry(@Nullable Duration pDuration);

  public CacheResult<V> setValue(V pValue);

  public CacheResult<V> setNotFound();

  public boolean entryFound();

  public boolean isNull();

  public @NonNull V getValue();

  public static <A> CacheResult<A> notFound() {
    @SuppressWarnings("unchecked")
    CacheResult<A> r = (CacheResult<A>) StaticCacheResult.sNOT_FOUND;
    return r;
  }

}