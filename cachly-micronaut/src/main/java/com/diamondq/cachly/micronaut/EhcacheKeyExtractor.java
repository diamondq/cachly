package com.diamondq.cachly.micronaut;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.ehcache.Cache;

public class EhcacheKeyExtractor {

  public static Stream<String> getKeys(Object pNativeCache) {
    Cache<@NonNull ?, ?> cache = (Cache<@NonNull ?, ?>) pNativeCache;
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cache.iterator(), Spliterator.NONNULL), false) //
      .map((entry) -> entry.getKey().toString());
  }
}
