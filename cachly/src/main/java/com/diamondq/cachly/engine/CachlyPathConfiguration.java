package com.diamondq.cachly.engine;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Introspected;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("unused")
@Introspected
@EachProperty(CachlyPathConfiguration.CACHLY_PATH_PREFIX)
public class CachlyPathConfiguration {

  public static final String CACHLY_PATH_PREFIX = "cachly.paths";

  private @Nullable String mStorage;

  private @Nullable String mSerializer;

  private final String mName;

  public CachlyPathConfiguration(@Parameter String name) {
    mName = name;
  }

  public String getName() {
    return mName;
  }

  public @Nullable String getStorage() {
    return mStorage;
  }

  public void setStorage(String pStorage) {
    mStorage = pStorage;
  }

  public @Nullable String getSerializer() {
    return mSerializer;
  }

  public void setSerializer(String pSerializer) {
    mSerializer = pSerializer;
  }

}