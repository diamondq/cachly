package com.diamondq.cachly.micronaut.caffeine;

import com.diamondq.cachly.micronaut.KeyExtractor;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Performs the low-level key/value extraction from a Caffeine cache.
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
@Singleton
public class CaffeineKeyExtractor implements KeyExtractor {

  @Override
  public <K, V extends @Nullable Object> @Nullable Stream<Map.Entry<K, V>> getEntries(Object pNativeCache) {
    if (pNativeCache instanceof Cache<?, ?> cm) {
      @SuppressWarnings("unchecked") Cache<K, V> castedCM = (Cache<K, V>) cm;
      return castedCM.asMap().entrySet().stream();
    }
    return null;
  }
}
