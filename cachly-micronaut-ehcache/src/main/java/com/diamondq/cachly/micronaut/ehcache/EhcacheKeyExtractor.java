package com.diamondq.cachly.micronaut.ehcache;

import com.diamondq.cachly.micronaut.KeyExtractor;
import jakarta.inject.Singleton;
import org.ehcache.Cache;
import org.jspecify.annotations.Nullable;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("ClassNamePrefixedWithPackageName")
@Singleton
public class EhcacheKeyExtractor implements KeyExtractor {

  @Override
  public <K, V extends @Nullable Object> @Nullable Stream<Map.Entry<K, V>> getEntries(Object pNativeCache) {
    if (pNativeCache instanceof Cache) {
      @SuppressWarnings("unchecked") Cache<K, V> cache = (Cache<K, V>) pNativeCache;
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cache.iterator(), Spliterator.NONNULL), false) //
        .map((entry) -> new SimpleEntry<>(entry.getKey(), entry.getValue()));
    }
    return null;
  }
}
