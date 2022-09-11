package com.diamondq.cachly;

import com.diamondq.cachly.impl.StaticCacheResult;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;

public interface CacheResult<V> {

  @Nullable
  Duration getOverrideExpiry();

  CacheResult<V> setOverrideExpiry(@Nullable Duration pDuration);

  /**
   * Sets the value. If the value is null, then it's actually equivalent to setNotFound()
   *
   * @param pValue the optional value
   * @return the cache result
   */
  CacheResult<V> setValue(@Nullable V pValue);

  /**
   * Sets the value. If the value is null, it's still marked as found
   *
   * @param pValue the nullable value
   * @return the cache result
   */
  CacheResult<V> setNullableVaue(@Nullable V pValue);

  CacheResult<V> setNotFound();

  boolean entryFound();

  boolean isNull();

  @NonNull
  V getValue();

  static <A> CacheResult<A> notFound() {
    @SuppressWarnings("unchecked") CacheResult<A> r = (CacheResult<A>) StaticCacheResult.sNOT_FOUND;
    return r;
  }

}