package com.diamondq.cachly.micronaut;

import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface KeyExtractor {

  /**
   * Attempts to extract the list of keys from the native cache
   *
   * @param pNativeCache the native cache (type is unknown at this point)
   * @return the list of keys or null if keys cannot be extracted
   */
  public @Nullable Stream<String> getKeys(Object pNativeCache);

}
