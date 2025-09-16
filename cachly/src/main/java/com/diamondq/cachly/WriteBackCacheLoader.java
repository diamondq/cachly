package com.diamondq.cachly;

import org.jspecify.annotations.Nullable;

/**
 * Allows for direct writes to the cache to also be written back to the original source
 *
 * @param <O> the value type
 */
@SuppressWarnings("unused")
public interface WriteBackCacheLoader<O extends @Nullable Object> extends CacheLoader<O> {

  /**
   * Called to store a specific key and value
   *
   * @param pCache the cache
   * @param pAccessContext the access context
   * @param pKey the key to load
   * @param pResult the value to store
   */
  void store(Cache pCache, AccessContext pAccessContext, Key<O> pKey, CacheResult<O> pResult);
}
