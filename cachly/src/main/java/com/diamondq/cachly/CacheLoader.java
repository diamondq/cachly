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
  public CacheLoaderInfo<O> getInfo();

  /**
   * Called to load a specific key
   *
   * @param pCache the cache
   * @param pKey the key to load
   * @param pResult the place to store the result
   */
  public void load(Cache pCache, Key<O> pKey, CacheResult<O> pResult);

}
