package com.diamondq.cachly;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface Cache {

  public static final String DEFAULT_SERIALIZER = "__DEFAULT__";

  /**
   * Creates the AccessContext
   *
   * @param pExistingContext an optional existing AccessContext
   * @param pData data to add to this context
   * @return a new Access Context
   */
  public AccessContext createAccessContext(@Nullable AccessContext pExistingContext,
    @Nullable Object @Nullable... pData);

  /**
   * Retrieves a value from the cache
   *
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @return the result
   */
  public <V> V get(AccessContext pAccessContext, Key<V> pKey);

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
  public <K1, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1);

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
  public <K1, K2, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
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
  public <K1, K2, K3, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
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
  public <K1, K2, K3, K4, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4);

  /**
   * Retrieves a value from the cache
   *
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   * @return the optional result
   */
  public <V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey);

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
  public <K1, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
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
  public <K1, K2, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
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
  public <K1, K2, K3, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey,
    KeyPlaceholder<K1> pHolder1, String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2,
    KeyPlaceholder<K3> pHolder3, String pValue3);

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
  public <K1, K2, K3, K4, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey,
    KeyPlaceholder<K1> pHolder1, String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2,
    KeyPlaceholder<K3> pHolder3, String pValue3, KeyPlaceholder<K4> pHolder4, String pValue4);

  /**
   * Invalidates an entry in the cache
   *
   * @param <V> the type of the result
   * @param pAccessContext the access context
   * @param pKey the key
   */
  public <V> void invalidate(AccessContext pAccessContext, Key<V> pKey);

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
  public <K1, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1);

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
  public <K1, K2, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2);

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
  public <K1, K2, K3, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
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
  public <K1, K2, K3, K4, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4);

  /**
   * Invalidate all keys
   *
   * @param pAccessContext the access context
   */
  public void invalidateAll(AccessContext pAccessContext);

  /**
   * Returns all the existing CacheLoaderInfo associated to their path
   *
   * @return the Map of path to CacheLoaderInfo
   */
  public Map<String, CacheLoaderInfo<?>> getCacheLoadersByPath();

  /**
   * Returns a stream of entries for everything stored in the cache
   *
   * @param pAccessContext the access context
   * @return the stream
   */
  public Stream<Map.Entry<Key<?>, CacheResult<?>>> streamEntries(AccessContext pAccessContext);

   Collection<Key<?>> dependencies(AccessContext pAccessContext, String pKeyStr);
}
