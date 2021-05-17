package com.diamondq.cachly;

/**
 * This represents a cache loader that is capable of calculating or retrieving a given value
 *
 * @param <I> the parent's type
 * @param <O> the expected result type
 */
public interface CacheLoader<I, O> {

  /**
   * Called to load a specific key
   *
   * @param pCache the cache
   * @param pKey the key to load
   * @return the result
   */
  public CacheResult<O> load(Cache pCache, Key<I, O> pKey);

  public boolean supportsNull();

  public String getPath();

}
