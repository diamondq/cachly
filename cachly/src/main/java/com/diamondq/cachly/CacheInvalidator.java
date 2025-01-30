package com.diamondq.cachly;

/**
 * This represents a cache invalidator which is called whenever a key is invalidated from the cache. Used to do extra
 * cleanup.
 *
 * @param <O> the expected result type
 */
@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface CacheInvalidator<O> {

  /**
   * Called to invalidate a specific key
   *
   * @param pCache the cache
   * @param pKey the key to load
   */
  void invalidate(Cache pCache, Key<O> pKey);

}
