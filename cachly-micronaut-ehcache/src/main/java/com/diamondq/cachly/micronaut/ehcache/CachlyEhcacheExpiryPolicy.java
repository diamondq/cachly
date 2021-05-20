package com.diamondq.cachly.micronaut.ehcache;

import com.diamondq.cachly.micronaut.ExpiryHandler;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import javax.inject.Singleton;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.expiry.ExpiryPolicy;

@Singleton
public class CachlyEhcacheExpiryPolicy<K, V> implements ExpiryPolicy<K, V>, ExpiryHandler, CacheEventListener<K, V> {

  private final ConcurrentMap<String, Duration> mExpiries;

  public CachlyEhcacheExpiryPolicy() {
    mExpiries = new ConcurrentHashMap<>();
  }

  /**
   * @see com.diamondq.cachly.micronaut.ExpiryHandler#invalidate(java.lang.String)
   */
  @Override
  public void invalidate(String pKey) {
    mExpiries.remove(pKey);
  }

  /**
   * @see com.diamondq.cachly.micronaut.ExpiryHandler#invalidateAll()
   */
  @Override
  public void invalidateAll() {
    mExpiries.clear();
  }

  /**
   * @see com.diamondq.cachly.micronaut.ExpiryHandler#markForExpiry(java.lang.String, java.time.Duration)
   */
  @Override
  public void markForExpiry(String pKey, Duration pOverrideExpiry) {
    mExpiries.put(pKey, pOverrideExpiry);
  }

  /**
   * @see org.ehcache.expiry.ExpiryPolicy#getExpiryForCreation(java.lang.Object, java.lang.Object)
   */
  @Override
  public Duration getExpiryForCreation(@NonNull K pKey, V pValue) {
    Duration duration = mExpiries.get(pKey.toString());
    if (duration == null)
      return ExpiryPolicy.INFINITE;
    return duration;
  }

  /**
   * @see org.ehcache.expiry.ExpiryPolicy#getExpiryForAccess(java.lang.Object, java.util.function.Supplier)
   */
  @Override
  public @Nullable Duration getExpiryForAccess(@NonNull K pKey, Supplier<? extends V> pValue) {
    return null;
  }

  /**
   * @see org.ehcache.expiry.ExpiryPolicy#getExpiryForUpdate(java.lang.Object, java.util.function.Supplier,
   *      java.lang.Object)
   */
  @Override
  public @Nullable Duration getExpiryForUpdate(@NonNull K pKey, Supplier<? extends V> pOldValue, V pNewValue) {
    return null;
  }

  /**
   * @see org.ehcache.event.CacheEventListener#onEvent(org.ehcache.event.CacheEvent)
   */
  @Override
  public void onEvent(CacheEvent<? extends K, ? extends V> pEvent) {
    K keyObj = pEvent.getKey();
    String key = keyObj.toString();
    mExpiries.remove(key);
  }
}
