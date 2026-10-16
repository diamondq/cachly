package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheKeyEvent;
import com.diamondq.cachly.spi.CacheStorage;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is an internal bridge between the native cache and the Cache Engine / Cache Storage. This is necessary due to
 * constraints during loading order
 */
@Singleton
public class CacheCallbackHandler {

  /* The native caches are compared by identity, not hashcode, since they may be collections (like a Map), whose hash code changes
    as they are modified. */

  private final Map<Object, CacheStorage> mNativeCacheToCacheStorageMap;

  private final Set<Object> mEventSources;

  /**
   * Injection Constructor
   */
  @Inject
  public CacheCallbackHandler() {
    mNativeCacheToCacheStorageMap = Collections.synchronizedMap(new IdentityHashMap<>());
    mEventSources = Collections.newSetFromMap(Collections.synchronizedMap(new IdentityHashMap<>()));
  }

  /**
   * Registers a new binding between a native cache and the cache storage
   *
   * @param pNativeCache the native cache
   * @param pCacheStorage the cache storage
   */
  public void registerCacheStorage(Object pNativeCache, CacheStorage pCacheStorage) {
    mNativeCacheToCacheStorageMap.put(pNativeCache, pCacheStorage);
  }

  /**
   * Records that a native cache reports its changes to {@link #handleEvent}. Writers only wait for callbacks on caches
   * that do.
   *
   * @param pNativeCache the native cache
   */
  public void registerEventSource(Object pNativeCache) {
    mEventSources.add(pNativeCache);
  }

  /**
   * Returns whether the native cache reports its changes to {@link #handleEvent}
   *
   * @param pNativeCache the native cache
   * @return true if it does or false otherwise
   */
  public boolean isEventSource(Object pNativeCache) {
    return mEventSources.contains(pNativeCache);
  }

  /**
   * Called by the low-level cache when a key changes
   *
   * @param pNativeCache the native cache
   * @param pKey the key
   * @param pEvent the event
   * @param pValue the value (NOTE: May not be the latest)
   */
  public void handleEvent(Object pNativeCache, Object pKey, CacheKeyEvent pEvent, @Nullable Object pValue) {
    var cacheStorage = mNativeCacheToCacheStorageMap.get(pNativeCache);
    if (cacheStorage == null) return;

    cacheStorage.handleEvent(pKey, pEvent, pValue);
  }
}
