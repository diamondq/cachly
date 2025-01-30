package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheKeyEvent;
import com.diamondq.cachly.engine.CacheStorage;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is an internal bridge between the native cache and the Cache Engine / Cache Storage. This is necessary due to
 * constraints during loading order
 */
@Singleton
public class CacheCallbackHandler {

  private final Map<Object, CacheStorage> mNativeCacheToCacheStorageMap;

  /**
   * Injection Constructor
   */
  @Inject
  public CacheCallbackHandler() {
    mNativeCacheToCacheStorageMap = new ConcurrentHashMap<>();
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
