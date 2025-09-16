package com.diamondq.cachly.micronaut.ehcache;

import com.diamondq.cachly.micronaut.ExpiryHandler;
import jakarta.inject.Singleton;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.expiry.ExpiryPolicy;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Singleton
public class CachlyEhcacheExpiryPolicy<K, V extends @Nullable Object>
  implements ExpiryPolicy<K, V>, ExpiryHandler, CacheEventListener<K, V> {

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
  public Duration getExpiryForCreation(K pKey, V pValue) {
    Duration duration = mExpiries.get(pKey.toString());
    if (duration == null) return INFINITE;
    return duration;
  }

  @Override
  public @Nullable Duration getExpiryForAccess(K pKey, Supplier<? extends V> pValue) {
    return null;
  }

  @Override
  public @Nullable Duration getExpiryForUpdate(K pKey, Supplier<? extends V> pOldValue, V pNewValue) {
    return null;
  }

  @Override
  public void onEvent(CacheEvent<? extends K, ? extends V> pEvent) {
    K keyObj = pEvent.getKey();
    String key = keyObj.toString();
    mExpiries.remove(key);
  }
}
