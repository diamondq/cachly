package com.diamondq.cachly.spi;

import com.diamondq.cachly.Key;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * A key-value pair container used internally by the cache engine to associate a cache entry with its metadata.
 * <p>
 * This record bundles together the cache reference, expiration duration, the original key, the serialized key, and the
 * serialized value for storage and retrieval operations.
 *
 * @param <CACHE> the type of the cache instance
 * @param <SER_KEY> the type of the serialized key
 * @param cache the cache instance this pair belongs to
 * @param expiresIn the duration after which this entry should expire, or null for no expiration
 * @param key the original Key object, or null if not available
 * @param serKey the serialized representation of the key
 * @param serValue the serialized representation of the value, or null if the value is null
 */
public record CommonKeyValuePair<CACHE, SER_KEY>(CACHE cache, @Nullable Duration expiresIn,
                                                 @Nullable Key<? extends @Nullable Object> key, SER_KEY serKey,
                                                 @Nullable Object serValue) {

  /**
   * Alternative constructor with a different parameter order for convenience.
   *
   * @param pCache the cache instance this pair belongs to
   * @param pSerKey the serialized representation of the key
   * @param pKey the original Key object, or null if not available
   * @param pSerValue the serialized representation of the value, or null if the value is null
   * @param pExpiresIn the duration after which this entry should expire, or null for no expiration
   */
  public CommonKeyValuePair(CACHE pCache, SER_KEY pSerKey, @Nullable Key<? extends @Nullable Object> pKey,
    @Nullable Object pSerValue, @Nullable Duration pExpiresIn) {
    this(pCache, pExpiresIn, pKey, pSerKey, pSerValue);
  }

}