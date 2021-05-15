package com.diamondq.cachly.micronaut;

import java.util.ArrayList;
import java.util.List;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.util.CollectionUtils;

@EachProperty(value = "cachly.micronaut")
public class CachlyMicronautConfiguration {
  private final String mName;

  private List<String> mPaths = new ArrayList<>();

  public CachlyMicronautConfiguration(@Parameter String name) {
    mName = name;
  }

  public String getName() {
    return mName;
  }

  public List<String> getPaths() {
    return mPaths;
  }

  public void setPaths(List<String> pPaths) {
    if (CollectionUtils.isNotEmpty(pPaths)) {
      mPaths = pPaths;
    }
  }

}
