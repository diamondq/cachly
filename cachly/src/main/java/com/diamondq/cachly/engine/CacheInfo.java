package com.diamondq.cachly.engine;

import com.diamondq.cachly.impl.KeyInternal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CacheInfo {

  public Map<String, Set<KeyInternal<?, ?>>> dependencyMap;

  public CacheInfo() {
    dependencyMap = new HashMap<>();
  }
}
