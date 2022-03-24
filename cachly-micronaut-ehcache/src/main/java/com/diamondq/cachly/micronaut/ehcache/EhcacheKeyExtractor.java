package com.diamondq.cachly.micronaut.ehcache;

import com.diamondq.cachly.micronaut.KeyExtractor;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.inject.Singleton;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.ehcache.Cache;

@Singleton
@javax.inject.Singleton
public class EhcacheKeyExtractor implements KeyExtractor {

  @Override
  public <K, V> @Nullable Stream<Map.Entry<K, V>> getEntries(Object pNativeCache) {
    if (pNativeCache instanceof Cache) {
      @SuppressWarnings("unchecked")
      Cache<@NonNull K, V> cache = (Cache<@NonNull K, V>) pNativeCache;
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cache.iterator(), Spliterator.NONNULL), false) //
        .map((entry) -> new SimpleEntry<K, V>(entry.getKey(), entry.getValue()));
    }
    return null;
  }
}
