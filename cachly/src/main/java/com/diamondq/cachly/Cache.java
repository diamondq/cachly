package com.diamondq.cachly;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Primary access to the Cache service. Get an instance of this via Dependency Injection
 */
@SuppressWarnings("unused")
public interface Cache {

  /**
   * The static string representing the default serializer
   */
  String DEFAULT_SERIALIZER = "__DEFAULT__";

  /**
   * Creates the AccessContext
   *
   * @param pExistingContext an optional existing AccessContext
   * @param pData data to add to this context
   * @return a new Access Context
   */
  AccessContext createAccessContext(@Nullable AccessContext pExistingContext, @Nullable Object @Nullable ... pData);

  /**
   * Retrieves a value from the cache
   *
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @return the result
   */
  <V> V get(AccessContext pAccessContext, Key<V> pKey);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @return the result
   */
  <K1, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @return the result
   */
  <K1, K2, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @return the result
   */
  <K1, K2, K3, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <K4> the type of the fourth placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param pHolder4 the fourth placeholder
   * @param pValue4 the value for the fourth placeholder
   * @return the result
   */
  <K1, K2, K3, K4, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4);

  /**
   * Retrieves a value from the cache
   *
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @return the optional result
   */
  <V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @return the optional result
   */
  <K1, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @return the optional result
   */
  <K1, K2, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @return the optional result
   */
  <K1, K2, K3, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <K4> the type of the fourth placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param pHolder4 the fourth placeholder
   * @param pValue4 the value for the fourth placeholder
   * @return the optional result
   */
  <K1, K2, K3, K4, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4);

  /**
   * Stores a value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pValue the value
   * @param <V> the value to store
   */
  <V> void set(AccessContext pAccessContext, Key<V> pKey, V pValue);

  /**
   * Stores a value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pValue the value
   * @param pExpiry the expiry duration
   * @param <V> the value to store
   */
  <V> void set(AccessContext pAccessContext, Key<V> pKey, V pValue, Duration pExpiry);

  /**
   * Stores a not found value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param <V> the value to store
   */
  <V> void setNotFound(AccessContext pAccessContext, Key<V> pKey);

  /**
   * Stores a not found value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pExpiry the expiry duration
   * @param <V> the value to store
   */
  <V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, Duration pExpiry);

  /**
   * Stores a value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pValue the value
   * @param <K1> the type of the first placeholder
   * @param <V> the value to store
   */
  <K1, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1, V pValue);

  /**
   * Stores a value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pValue the value
   * @param pExpiry the expiry duration
   * @param <K1> the type of the first placeholder
   * @param <V> the value to store
   */
  <K1, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1, V pValue,
    Duration pExpiry);

  /**
   * Stores a not found value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param <K1> the type of the first placeholder
   * @param <V> the value to store
   */
  <K1, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1);

  /**
   * Stores a not found value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pExpiry the expiry duration
   * @param <K1> the type of the first placeholder
   * @param <V> the value to store
   */
  <K1, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    Duration pExpiry);

  /**
   * Stores a value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pValue the value
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <V> the value to store
   */
  <K1, K2, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, V pValue);

  /**
   * Stores a value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pValue the value
   * @param pExpiry the expiry duration
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <V> the value to store
   */
  <K1, K2, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, V pValue, Duration pExpiry);

  /**
   * Stores a not found value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <V> the value to store
   */
  <K1, K2, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2);

  /**
   * Stores a not found value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pExpiry the expiry duration
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <V> the value to store
   */
  <K1, K2, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, Duration pExpiry);

  /**
   * Stores a value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param pValue the value
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <V> the value to store
   */
  <K1, K2, K3, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3, V pValue);

  /**
   * Stores a value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param pValue the value
   * @param pExpiry the expiry duration
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <V> the value to store
   */
  <K1, K2, K3, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3, V pValue,
    Duration pExpiry);

  /**
   * Stores a not found value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <V> the value to store
   */
  <K1, K2, K3, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3);

  /**
   * Stores a not found value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param pExpiry the expiry duration
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <V> the value to store
   */
  <K1, K2, K3, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    Duration pExpiry);

  /**
   * Stores a value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param pHolder4 the fourth placeholder
   * @param pValue4 the value for the fourth placeholder
   * @param pValue the value
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <K4> the type of the fourth placeholder
   * @param <V> the value to store
   */
  <K1, K2, K3, K4, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4, V pValue);

  /**
   * Stores a value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param pHolder4 the fourth placeholder
   * @param pValue4 the value for the fourth placeholder
   * @param pValue the value
   * @param pExpiry the expiry duration
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <K4> the type of the fourth placeholder
   * @param <V> the value to store
   */
  <K1, K2, K3, K4, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4, V pValue, Duration pExpiry);

  /**
   * Stores a not found value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param pHolder4 the fourth placeholder
   * @param pValue4 the value for the fourth placeholder
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <K4> the type of the fourth placeholder
   * @param <V> the value to store
   */
  <K1, K2, K3, K4, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4);

  /**
   * Stores a not found value into the cache (NOTE: This bypasses the loaders)
   *
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the second placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param pHolder4 the fourth placeholder
   * @param pValue4 the value for the fourth placeholder
   * @param pExpiry the expiry duration
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <K4> the type of the fourth placeholder
   * @param <V> the value to store
   */
  <K1, K2, K3, K4, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4, Duration pExpiry);

  /**
   * Invalidates an entry in the cache
   *
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   */
  <V> void invalidate(AccessContext pAccessContext, Key<V> pKey);

  /**
   * Invalidates an entry in the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   */
  <K1, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1);

  /**
   * Invalidates an entry in the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   */
  <K1, K2, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2);

  /**
   * Invalidates an entry in the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   */
  <K1, K2, K3, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3);

  /**
   * Invalidates an entry in the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <K4> the type of the fourth placeholder
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @param pHolder4 the fourth placeholder
   * @param pValue4 the value for the fourth placeholder
   */
  <K1, K2, K3, K4, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4);

  /**
   * Invalidate all keys
   *
   * @param pAccessContext the access context
   */
  void invalidateAll(AccessContext pAccessContext);

  /**
   * Returns all the existing CacheLoaderInfo associated to their path
   *
   * @return the Map of path to CacheLoaderInfo
   */
  Map<String, CacheLoaderInfo<?>> getCacheLoadersByPath();

  /**
   * Returns a stream of entries for everything stored in the cache
   *
   * @param pAccessContext the access context
   * @return the stream
   */
  Stream<Map.Entry<Key<?>, CacheResult<?>>> streamEntries(AccessContext pAccessContext);

  /**
   * Get the list of keys that are dependent on the given key
   *
   * @param pAccessContext the access context
   * @param pKeyStr the key
   * @return the collection of keys that are dependent
   */
  Collection<Key<?>> getDependentKeys(AccessContext pAccessContext, String pKeyStr);

  /**
   * Get the list of keys that the given key depends on
   *
   * @param pAccessContext the access context
   * @param pKeyStr the key
   * @return the collection of keys that are dependent
   */
  Collection<Key<?>> getDependentOnKeys(AccessContext pAccessContext, String pKeyStr);

  /**
   * Resolve a key and placeholder to a more resolved key
   *
   * @param pKey the key
   * @param pHolder the key placeholder
   * @param pValue the placeholder value
   * @param <K1> the placeholder type
   * @param <V> the key type
   * @return an updated key
   */
  <K1, V> Key<V> resolve(Key<V> pKey, KeyPlaceholder<K1> pHolder, String pValue);
}
