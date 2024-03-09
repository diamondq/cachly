package com.diamondq.cachly.engine;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.lambda.interfaces.Consumer2;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents the underlying Cache storage system
 */
public interface CacheStorage {

  /**
   * Tries to look up a given key within the storage
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param <V> the key type
   * @return the result
   */
  <V> CacheResult<V> queryForKey(AccessContext pAccessContext, KeySPI<V> pKey);

  /**
   * Stores a new value into the Cache Storage
   *
   * @param pAccessContext the Access Context
   * @param pKey the key
   * @param pLoadedResult the data to store
   * @param <V> the key type
   */
  <V> void store(AccessContext pAccessContext, KeySPI<V> pKey, CacheResult<V> pLoadedResult);

  /**
   * Invalidates (i.e. removes) a given key and its value from the Cache Storage
   *
   * @param pAccessContext the Access Storage
   * @param pKey the key
   * @param <V> the key type
   */
  <V> void invalidate(AccessContext pAccessContext, KeySPI<V> pKey);

  /**
   * Returns a stream of all stored key and values
   *
   * @param pAccessContext the access context
   * @return the stream of Key and Values
   */
  Stream<Map.Entry<Key<?>, CacheResult<?>>> streamEntries(AccessContext pAccessContext);

  /**
   * Invalidates all the keys and values
   *
   * @param pAccessContext the access context
   */
  void invalidateAll(AccessContext pAccessContext);

  /**
   * Register a callback for create/update/delete callbacks
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pCallback the callback
   * @param <V> the key type
   */
  <V> void registerOnChange(AccessContext pAccessContext, KeySPI<V> pKey, Consumer2<Key<V>, Optional<V>> pCallback);

  /**
   * Called by the low-level cache informing that a key has changed
   *
   * @param pKey the key (from the low level cache)
   * @param pValue the value (from the low level cache; may not be the latest)
   */
  void handleEvent(Object pKey, @Nullable Object pValue);

  /**
   * This is called during construction. It's necessary because otherwise you'd have a parent &lt;--> child problem
   *
   * @param pCacheEngine the cache engine
   */
  void setCacheEngine(CacheEngine pCacheEngine);
}
