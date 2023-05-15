package com.diamondq.cachly.engine;

import com.diamondq.cachly.Key;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public class CommonKeyValuePair<CACHE, SER_KEY> {

  public final CACHE cache;

  public final @Nullable Duration expiresIn;

  public final @Nullable Key<?> key;

  public final SER_KEY serKey;

  public final @Nullable Object serValue;

  public CommonKeyValuePair(CACHE pCache, SER_KEY pSerKey, @Nullable Key<?> pKey, @Nullable Object pSerValue,
    @Nullable Duration pExpiresIn) {
    cache = pCache;
    serKey = pSerKey;
    key = pKey;
    serValue = pSerValue;
    expiresIn = pExpiresIn;
  }

}