package com.diamondq.cachly.spi;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;

import java.util.Map;

/**
 * SPI level methods on the cache engine
 */
public interface CacheEngine extends Cache {

  /**
   * Performs the key setup
   *
   * @param pKey the key
   * @param <O> the key type
   */
  <O> void setupKey(KeySPI<O> pKey);

  /**
   * Returns all the existing CacheLoaderInfo associated with their path
   *
   * @return the Map of a path to CacheLoaderInfo
   */
  Map<String, CacheLoaderInfo<?>> getCacheLoadersByPath();

  /**
   * Adds a new path configuration dynamically (i.e., one that wasn't automatically set up via injection).
   *
   * @param pPathConfig the path config
   */
  void addPathConfiguration(CachlyPathConfiguration pPathConfig);

  /**
   * Removes a path configuration.
   *
   * @param pPathConfig the path config
   */
  void removePathConfiguration(CachlyPathConfiguration pPathConfig);

  /**
   * Adds a new cache loader dynamically (i.e., one that wasn't automatically set up via injection).
   *
   * @param pCacheLoader the cache loader
   */
  void addCacheLoader(CacheLoader<?> pCacheLoader);

  /**
   * Removes a Cache Loader
   *
   * @param pCacheLoader the Cache Loader
   */
  void removeCacheLoader(CacheLoader<?> pCacheLoader);

  /**
   * Adds a new Cache Storage
   *
   * @param pStorage the storage
   */
  void addCacheStorage(CacheStorage pStorage);

  /**
   * Removes a Cache Storage
   *
   * @param pStorage the storage
   */
  void removeCacheStorage(CacheStorage pStorage);

  /**
   * Adds a new bean name locator
   *
   * @param pBeanNameLocator the new bean name locator
   */
  void addBeanNameLocator(BeanNameLocator pBeanNameLocator);

  /**
   * Removes a bean name locator
   *
   * @param pBeanNameLocator the bean name locator to remove
   */
  void removeBeanNameLocator(BeanNameLocator pBeanNameLocator);

  /**
   * Adds a new access context SPI
   *
   * @param pAccessContextSPI the access context SPI
   */
  void addAccessContextSPI(AccessContextSPI<?> pAccessContextSPI);

  /**
   * Removes an access context SPI
   *
   * @param pAccessContextSPI the access context SPI
   */
  void removeAccessContextSPI(AccessContextSPI<?> pAccessContextSPI);

  /**
   * If it's not created via CDI or OSGi, this method must be called to finish the internal setup
   */
  void activate();

}
