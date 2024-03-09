package com.diamondq.cachly;

/**
 * Each Cache Loader must return a Cache Loader Info that defines information about this loader
 *
 * @param <O> the type of the loader
 */
public class CacheLoaderInfo<O> {

  /**
   * The key that this loader is supporting
   */
  public final Key<O> key;

  /**
   * Whether the cache loader supports null values
   */
  public final boolean supportsNull;

  /**
   * The help string
   */
  public final String help;

  /**
   * The Cache Loader
   */
  public final CacheLoader<O> loader;

  /**
   * Primary Constructor
   *
   * @param pKey the key
   * @param pSupportsNull whether this loader supports null
   * @param pHelp the help string
   * @param pLoader the loader
   */
  public CacheLoaderInfo(Key<O> pKey, boolean pSupportsNull, String pHelp, CacheLoader<O> pLoader) {
    key = pKey;
    supportsNull = pSupportsNull;
    help = pHelp;
    loader = pLoader;
  }

}
