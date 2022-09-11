package com.diamondq.cachly;

/**
 * This represents a cache loader that is capable of calculating or retrieving a given value
 *
 * @param <O> the expected result type
 */
public interface CacheLoader<O> {

  /**
   * Returns information about this CacheLoader
   *
   * @return the info
   */
  CacheLoaderInfo<O> getInfo();

  /**
   * Called to load a specific key
   *
   * @param pCache the cache
   * @param pAccessContext the access context
   * @param pKey the key to load
   * @param pResult the place to store the result
   */
  void load(Cache pCache, AccessContext pAccessContext, Key<O> pKey, CacheResult<O> pResult);

}
