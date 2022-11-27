package com.diamondq.cachly.engine;

import com.diamondq.cachly.spi.KeySPI;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CacheInfo {

  public final Map<String, Set<KeySPI<?>>> dependencyMap;
  public final Map<String, Set<String>>    reverseDependencyMap;

  public CacheInfo() {
    dependencyMap = new HashMap<>();
    reverseDependencyMap = new HashMap<>();
  }
}
