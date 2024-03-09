package com.diamondq.cachly.engine;

import com.diamondq.cachly.spi.KeySPI;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents data that Cachly uses to work but is stored within the underlying Cache engine for persistence
 */
public class CacheInfo {

  /**
   * The dependency map from a given key to a set of keys
   */
  public final Map<String, Set<KeySPI<?>>> dependencyMap;

  /**
   * The reverse dependency map from a key to a parent key
   */
  public final Map<String, Set<String>> reverseDependencyMap;

  /**
   * Primary constructor
   */
  public CacheInfo() {
    dependencyMap = new ConcurrentHashMap<>();
    reverseDependencyMap = new ConcurrentHashMap<>();
  }
}
