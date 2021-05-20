package com.diamondq.cachly;

import java.time.Duration;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CacheResult<V> {

  private final @Nullable V        mValue;

  private final boolean            mFound;

  private final @Nullable Duration mOverrideExpiry;

  public CacheResult(@Nullable V pValue, boolean pFound) {
    super();
    mValue = pValue;
    mFound = pFound;
    mOverrideExpiry = null;
  }

  public CacheResult(@Nullable V pValue, boolean pFound, @Nullable Duration pOverrideExpiry) {
    super();
    mValue = pValue;
    mFound = pFound;
    mOverrideExpiry = pOverrideExpiry;
  }

  public @Nullable Duration getOverrideExpiry() {
    return mOverrideExpiry;
  }

  public boolean entryFound() {
    return mFound;
  }

  public boolean isNull() {
    return mValue == null;
  }

  public @NonNull V getValue() {
    return Objects.requireNonNull(mValue);
  }

  private static final CacheResult<Object> sNOT_FOUND = new CacheResult<>(null, false);

  public static <A> CacheResult<A> notFound() {
    @SuppressWarnings("unchecked")
    CacheResult<A> r = (CacheResult<A>) sNOT_FOUND;
    return r;
  }
}