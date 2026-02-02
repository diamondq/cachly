package com.diamondq.cachly.engine;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Introspected;
import org.jspecify.annotations.Nullable;

/**
 * Configuration class (normally reading from the Micronaut application.yml), but can be manually created as well
 */
@SuppressWarnings("unused")
@Introspected
@EachProperty(CachlyPathConfiguration.CACHLY_PATH_PREFIX)
public class CachlyPathConfiguration {

  /**
   * The prefix in the Micronaut configuration for these configuration entries
   */
  public static final String CACHLY_PATH_PREFIX = "cachly.paths";

  private @Nullable String mStorage;

  private @Nullable String mSerializer;

  private final String mName;

  /**
   * Constructor
   *
   * @param name the name
   */
  public CachlyPathConfiguration(@Parameter String name) {
    mName = name;
  }

  /**
   * Gets the name
   *
   * @return the name
   */
  public String getName() {
    return mName;
  }

  /**
   * Gets the storage name
   *
   * @return the storage name
   */
  public @Nullable String getStorage() {
    return mStorage;
  }

  /**
   * Sets the storage name
   *
   * @param pStorage the storage name
   */
  public void setStorage(String pStorage) {
    mStorage = pStorage;
  }

  /**
   * Gets the serializer name
   *
   * @return the serializer name
   */
  public @Nullable String getSerializer() {
    return mSerializer;
  }

  /**
   * Sets the serializer name
   *
   * @param pSerializer the serializer name
   */
  public void setSerializer(String pSerializer) {
    mSerializer = pSerializer;
  }

}