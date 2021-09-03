package com.diamondq.cachly.engine;

import com.diamondq.cachly.Key;

import java.time.Duration;

import org.checkerframework.checker.nullness.qual.Nullable;

public class CommonKeyValuePair<CACHE, SER_KEY, SER_VALUE> {

  public final CACHE               cache;

  public final @Nullable Duration  expiresIn;

  public final @Nullable Key<?>    key;

  public final SER_KEY             serKey;

  public final @Nullable SER_VALUE serValue;

  public CommonKeyValuePair(CACHE pCache, SER_KEY pSerKey, @Nullable Key<?> pKey, @Nullable SER_VALUE pSerValue,
    @Nullable Duration pExpiresIn) {
    cache = pCache;
    serKey = pSerKey;
    key = pKey;
    serValue = pSerValue;
    expiresIn = pExpiresIn;
  }

}