package com.diamondq.cachly;

import com.diamondq.cachly.impl.StaticCacheResult;

import java.time.Duration;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface CacheResult<V> {

  public @Nullable Duration getOverrideExpiry();

  public CacheResult<V> setOverrideExpiry(@Nullable Duration pDuration);

  /**
   * Sets the value. If the value is null, then it's actually equivalent to setNotFound()
   *
   * @param pValue the optional value
   * @return the cache result
   */
  public CacheResult<V> setValue(@Nullable V pValue);

  /**
   * Sets the value. If the value is null, it's still marked as found
   *
   * @param pValue the nullable value
   * @return the cache result
   */
  public CacheResult<V> setNullableVaue(@Nullable V pValue);

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