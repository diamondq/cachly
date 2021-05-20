package com.diamondq.cachly.micronaut.ehcache;

import com.diamondq.cachly.micronaut.KeyExtractor;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Singleton;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.ehcache.Cache;

@Singleton
public class EhcacheKeyExtractor implements KeyExtractor {

  @Override
  public @Nullable Stream<String> getKeys(Object pNativeCache) {
    if (pNativeCache instanceof Cache) {
      Cache<@NonNull ?, ?> cache = (Cache<@NonNull ?, ?>) pNativeCache;
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cache.iterator(), Spliterator.NONNULL), false) //
        .map((entry) -> entry.getKey().toString());
    }
    return null;
  }
}
