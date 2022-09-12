package com.diamondq.cachly;

import com.diamondq.cachly.impl.StaticCacheResult;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;

/**
 * Describes a Cache Result (is mutable)
 *
 * @param <V> the value type
 */
@SuppressWarnings("UnusedReturnValue")
public interface CacheResult<V> {

  /**
   * Returns the expiry assigned to this result
   *
   * @return the duration or null if there is no expiry
   */
  @Nullable Duration getOverrideExpiry();

  /**
   * Sets the expiry
   *
   * @param pDuration the duration to set (or null if indicating no expiry)
   * @return the cache result (for fluent use)
   */
  CacheResult<V> setOverrideExpiry(@Nullable Duration pDuration);

  /**
   * Sets the value. If the value is null, then it's actually equivalent to setNotFound()
   *
   * @param pValue the optional value
   * @return the cache result (for fluent use)
   */
  CacheResult<V> setValue(@Nullable V pValue);

  /**
   * Sets the value. If the value is null, it's still marked as found
   *
   * @param pValue the nullable value
   * @return the cache result (for fluent use)
   */
  CacheResult<V> setNullableVaue(@Nullable V pValue);

  /**
   * Sets that the value was not found. This is separate from setting a null value, since a null value may be a found
   * result, just null.
   *
   * @return the cache result (for fluent use)
   */
  CacheResult<V> setNotFound();

  /**
   * Returns whether the entry was found
   *
   * @return true if the entry was found or false if it was not found
   */
  boolean entryFound();

  /**
   * Indicates whether the value is null
   *
   * @return true if the value is null or false if it is not null
   */
  boolean isNull();

  /**
   * Returns the value. If the value is null, then this will throw an error. If this is a possibility, always call
   * isNull() first to check.
   *
   * @return the value
   */
  @NonNull V getValue();

  /**
   * Returns not found result
   *
   * @param <A> the result type
   * @return the not found result
   */
  static <A> CacheResult<A> notFound() {
    @SuppressWarnings("unchecked") CacheResult<A> r = (CacheResult<A>) StaticCacheResult.sNOT_FOUND;
    return r;
  }

}