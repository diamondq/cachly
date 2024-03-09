package com.diamondq.cachly.engine;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.spi.KeySPI;

import java.util.Map;
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

}
