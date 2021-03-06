package com.diamondq.cachly.spi;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.impl.KeyDetails;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface KeySPI<O> extends Key<O> {

  @Nullable
  public KeySPI<Object> getPreviousKey();

  public CacheStorage getLastStorage();

  public String getLastSerializerName();

  public boolean supportsNull();

  public CacheLoader<O> getLoader();

  public void storeKeyDetails(KeyDetails<O> pDetails);

  public boolean hasKeyDetails();

  public @NonNull KeySPI<Object>[] getParts();

  @Override
  public String getKey();

  public String getBaseKey();

  /**
   * Returns true if there are any unresolved placeholders in the composite key
   *
   * @return true if there are placeholders or false if there are not
   */
  public boolean hasPlaceholders();
}
