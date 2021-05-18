package com.diamondq.cachly;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface Cache {

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
  public <@NonNull K1, V> V get(Key<V> pKey, KeyPlaceholder<K1, ?> pHolder1, K1 pValue1);

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
  public <@NonNull K1, @NonNull K2, V> V get(Key<V> pKey, KeyPlaceholder<K1, ?> pHolder1, K1 pValue1,
    KeyPlaceholder<K2, ?> pHolder2, K2 pValue2);

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
  public <@NonNull K1, @NonNull K2, @NonNull K3, V> V get(Key<V> pKey, KeyPlaceholder<K1, ?> pHolder1, K1 pValue1,
    KeyPlaceholder<K2, ?> pHolder2, K2 pValue2, KeyPlaceholder<K3, ?> pHolder3, K3 pValue3);

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
  public <@NonNull K1, @NonNull K2, @NonNull K3, @NonNull K4, V> V get(Key<V> pKey, KeyPlaceholder<K1, ?> pHolder1,
    K1 pValue1, KeyPlaceholder<K2, ?> pHolder2, K2 pValue2, KeyPlaceholder<K3, ?> pHolder3, K3 pValue3,
    KeyPlaceholder<K4, ?> pHolder4, K4 pValue4);

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
  public <@NonNull K1, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1, ?> pHolder1, K1 pValue1);

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
  public <@NonNull K1, @NonNull K2, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1, ?> pHolder1, K1 pValue1,
    KeyPlaceholder<K2, ?> pHolder2, K2 pValue2);

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
  public <@NonNull K1, @NonNull K2, @NonNull K3, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1, ?> pHolder1,
    K1 pValue1, KeyPlaceholder<K2, ?> pHolder2, K2 pValue2, KeyPlaceholder<K3, ?> pHolder3, K3 pValue3);

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
  public <@NonNull K1, @NonNull K2, @NonNull K3, @NonNull K4, V> void invalidate(Key<V> pKey,
    KeyPlaceholder<K1, ?> pHolder1, K1 pValue1, KeyPlaceholder<K2, ?> pHolder2, K2 pValue2,
    KeyPlaceholder<K3, ?> pHolder3, K3 pValue3, KeyPlaceholder<K4, ?> pHolder4, K4 pValue4);

}
