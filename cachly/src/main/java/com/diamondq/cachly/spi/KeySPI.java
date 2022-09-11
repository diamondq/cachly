package com.diamondq.cachly.spi;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.impl.KeyDetails;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface KeySPI<O> extends Key<O> {

  @Nullable
  KeySPI<Object> getPreviousKey();

  CacheStorage getLastStorage();

  String getLastSerializerName();

  boolean supportsNull();

  CacheLoader<O> getLoader();

  void storeKeyDetails(KeyDetails<O> pDetails);

  boolean hasKeyDetails();

  @NonNull
  KeySPI<Object>[] getParts();

  @Override
  String getKey();

  String getBaseKey();

  /**
   * Returns true if there are any unresolved placeholders in the composite key
   *
   * @return true if there are placeholders or false if there are not
   */
  boolean hasPlaceholders();
}
