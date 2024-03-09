package com.diamondq.cachly;

import com.diamondq.cachly.impl.CompositeKey;
import com.diamondq.cachly.impl.StaticAccessContextPlaceholder;
import com.diamondq.cachly.impl.StaticKey;
import com.diamondq.cachly.impl.StaticKeyPlaceholder;
import com.diamondq.cachly.impl.StaticKeyPlaceholderWithDefault;
import com.diamondq.common.TypeReference;

/**
 * The primary helper for producing Keys
 */
public class KeyBuilder {

  /**
   * Generates a root key from a string and a type
   *
   * @param pTextKey the root string
   * @param pOutputType the key type reference
   * @param <O> the key type
   * @return the key
   */
  public static <O> Key<O> of(String pTextKey, TypeReference<O> pOutputType) {
    return new StaticKey<>(pTextKey, pOutputType.getType());
  }

  /**
   * Generates a new key from an existing key
   *
   * @param pKey1 the starting key
   * @param <O> the key type
   * @return the new key
   */
  public static <O> Key<O> from(Key<O> pKey1) {
    return pKey1;
  }

  /**
   * Generates a new key by combining the first key with the second key
   *
   * @param pKey1 the first key
   * @param pKey2 the second key
   * @param <O> the second key type
   * @return the new key
   */
  public static <O> Key<O> from(Key<?> pKey1, Key<O> pKey2) {
    return new CompositeKey<>(pKey1, pKey2);
  }

  /**
   * Generates a new key by combining the first key with the second and third keys
   *
   * @param pKey1 the first key
   * @param pKey2 the second key
   * @param pKey3 the third key
   * @param <O> the third key type
   * @return the new key
   */
  public static <O> Key<O> from(Key<?> pKey1, Key<?> pKey2, Key<O> pKey3) {
    return new CompositeKey<>(pKey1, pKey2, pKey3);
  }

  /**
   * Generates a placeholder based on an Access Context lookup
   *
   * @param pAccessContextKey the access context key
   * @param pOutputType the type of data to get from the Access Context
   * @param <O> the data type
   * @return the placeholder
   */
  public static <O> AccessContextPlaceholder<O> accessContext(String pAccessContextKey, TypeReference<O> pOutputType) {
    return new StaticAccessContextPlaceholder<>(pAccessContextKey, pOutputType.getType());
  }

  /**
   * Generates a placeholder that will be resolved at run time.
   *
   * @param pPlaceholderKey the name of the placeholder
   * @param pOutputType the type of data to expect during resolution
   * @param <O> the data type
   * @return the placeholder
   */
  public static <O> KeyPlaceholder<O> placeholder(String pPlaceholderKey, TypeReference<O> pOutputType) {
    return new StaticKeyPlaceholder<>(pPlaceholderKey, pOutputType.getType());
  }

  /**
   * Generates a placeholder that will be resolved at run time (and if it's not present at run time, then uses a default
   * value)
   *
   * @param pPlaceholderKey the name of the placeholder
   * @param pOutputType the type of data to expect during resolution
   * @param pDefaultKey the key to use for handling the default case
   * @return the placeholder
   */
  public static KeyPlaceholder<String> placeholder(String pPlaceholderKey, TypeReference<String> pOutputType,
    Key<String> pDefaultKey) {
    return new StaticKeyPlaceholderWithDefault(pPlaceholderKey, pOutputType.getType(), pDefaultKey);
  }

}
