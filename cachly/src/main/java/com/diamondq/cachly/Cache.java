package com.diamondq.cachly;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface Cache {

  public static final String DEFAULT_SERIALIZER = "__DEFAULT__";

  /**
   * Retrieves a value from the cache
   *
   * @param <V> the type of the result
   * @param pKey the key
   * @return the result
   */
  public <V> V get(Key<V> pKey);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <V> the type of the result
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @return the result
   */
  public <K1, V> V get(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <V> the type of the result
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @return the result
   */
  public <K1, K2, V> V get(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1, KeyPlaceholder<K2> pHolder2,
    String pValue2);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <V> the type of the result
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @return the result
   */
  public <K1, K2, K3, V> V get(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1, KeyPlaceholder<K2> pHolder2,
    String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <K4> the type of the fourth placeholder
   * @param <V> the type of the result
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
  public <K1, K2, K3, K4, V> V get(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4);

  /**
   * Retrieves a value from the cache
   *
   * @param <V> the type of the result
   * @param pKey the key
   * @return the result
   */
  public <V> Optional<V> getIfPresent(Key<V> pKey);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <V> the type of the result
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @return the result
   */
  public <K1, V> Optional<V> getIfPresent(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <V> the type of the result
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @return the result
   */
  public <K1, K2, V> Optional<V> getIfPresent(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <V> the type of the result
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   * @return the result
   */
  public <K1, K2, K3, V> Optional<V> getIfPresent(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3);

  /**
   * Retrieves a value from the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <K4> the type of the fourth placeholder
   * @param <V> the type of the result
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
  public <K1, K2, K3, K4, V> Optional<V> getIfPresent(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4);

  /**
   * Invalidates an entry in the cache
   *
   * @param <V> the type of the result
   * @param pKey the key
   */
  public <V> void invalidate(Key<V> pKey);

  /**
   * Invalidates an entry in the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <V> the type of the result
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   */
  public <K1, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1);

  /**
   * Invalidates an entry in the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <V> the type of the result
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   */
  public <K1, K2, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2);

  /**
   * Invalidates an entry in the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <V> the type of the result
   * @param pKey the key
   * @param pHolder1 the first placeholder
   * @param pValue1 the value for the first placeholder
   * @param pHolder2 the second placeholder
   * @param pValue2 the value for the first placeholder
   * @param pHolder3 the third placeholder
   * @param pValue3 the value for the third placeholder
   */
  public <K1, K2, K3, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3);

  /**
   * Invalidates an entry in the cache
   *
   * @param <K1> the type of the first placeholder
   * @param <K2> the type of the second placeholder
   * @param <K3> the type of the third placeholder
   * @param <K4> the type of the fourth placeholder
   * @param <V> the type of the result
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
  public <K1, K2, K3, K4, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4);

  /**
   * Invalidate all keys
   */
  public void invalidateAll();

  /**
   * Returns all the existing CacheLoaderInfo associated to their path
   *
   * @return the Map of path to CacheLoaderInfo
   */
  public Map<String, CacheLoaderInfo<?>> getCacheLoadersByPath();

  /**
   * Returns a stream of Keys for everything stored in the cache
   *
   * @return the stream
   */
  public Stream<Key<?>> streamKeys();

}
