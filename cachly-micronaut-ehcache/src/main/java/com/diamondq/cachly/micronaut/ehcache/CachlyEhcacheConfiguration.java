package com.diamondq.cachly.micronaut.ehcache;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.naming.Named;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@EachProperty("cachly.ehcache")
public class CachlyEhcacheConfiguration implements Named {
  private final String mName;

  private @Nullable Boolean mSerializer;

  private @Nullable CachlyDiskTieredCacheConfiguration mDisk;

  /**
   * @param name the cache name
   */
  public CachlyEhcacheConfiguration(@Parameter String name) {
    mName = name;
  }

  @Override
  public @NotNull String getName() {
    return mName;
  }

  public @Nullable Boolean getSerializer() {
    return mSerializer;
  }

  public void setSerializer(@Nullable Boolean pSerializer) {
    mSerializer = pSerializer;
  }

  /**
   * @return the disk tier configuration
   */
  public @Nullable CachlyDiskTieredCacheConfiguration getDisk() {
    return mDisk;
  }

  /**
   * @param disk the disk tier configuration
   */
  public void setDisk(@Nullable CachlyDiskTieredCacheConfiguration disk) {
    mDisk = disk;
  }

  /**
   * Disk tier configuration options.
   */
  @ConfigurationProperties("disk")
  public static class CachlyDiskTieredCacheConfiguration {

    private @Nullable Boolean mPersist;

    /**
     * @return is persisting
     */
    public @Nullable Boolean getPersist() {
      return mPersist;
    }

    /**
     * @param persist Whether to persist
     */
    public void setPersist(@Nullable Boolean persist) {
      mPersist = persist;
    }
  }
}
