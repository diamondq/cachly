package com.diamondq.cachly.micronaut;

import java.util.Map;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface KeyExtractor {

  /**
   * Attempts to extract the list of keys from the native cache
   *
   * @param pNativeCache the native cache (type is unknown at this point)
   * @return the list of keys or null if keys cannot be extracted
   */
  <K, V> @Nullable Stream<Map.Entry<K, V>> getEntries(Object pNativeCache);

}
