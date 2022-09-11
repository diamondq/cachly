package com.diamondq.cachly.micronaut.ehcache;

import com.diamondq.cachly.micronaut.ExpiryHandler;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.expiry.ExpiryPolicy;

@Singleton
@javax.inject.Singleton
public class CachlyEhcacheExpiryPolicy<K, V> implements ExpiryPolicy<K, V>, ExpiryHandler, CacheEventListener<K, V> {

  private final ConcurrentMap<String, Duration> mExpiries;

  public CachlyEhcacheExpiryPolicy() {
    mExpiries = new ConcurrentHashMap<>();
  }

  @Override
  public void invalidate(String pKey) {
    mExpiries.remove(pKey);
  }

  @Override
  public void invalidateAll() {
    mExpiries.clear();
  }

  @Override
  public void markForExpiry(String pKey, Duration pOverrideExpiry) {
    mExpiries.put(pKey, pOverrideExpiry);
  }

  @Override
  public Duration getExpiryForCreation(@NonNull K pKey, V pValue) {
    Duration duration = mExpiries.get(pKey.toString());
    if (duration == null)
      return ExpiryPolicy.INFINITE;
    return duration;
  }

  @Override
  public @Nullable Duration getExpiryForAccess(@NonNull K pKey, Supplier<? extends V> pValue) {
    return null;
  }

  @Override
  public @Nullable Duration getExpiryForUpdate(@NonNull K pKey, Supplier<? extends V> pOldValue, V pNewValue) {
    return null;
  }

  @Override
  public void onEvent(CacheEvent<? extends K, ? extends V> pEvent) {
    K keyObj = pEvent.getKey();
    String key = keyObj.toString();
    mExpiries.remove(key);
  }
}
