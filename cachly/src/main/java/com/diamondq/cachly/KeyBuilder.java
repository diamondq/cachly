package com.diamondq.cachly;

import com.diamondq.cachly.impl.CompositeKey;
import com.diamondq.cachly.impl.StaticAccessContextPlaceholder;
import com.diamondq.cachly.impl.StaticKey;
import com.diamondq.cachly.impl.StaticKeyPlaceholder;
import com.diamondq.cachly.impl.StaticKeyPlaceholderWithDefault;
import com.diamondq.common.TypeReference;

public class KeyBuilder {

  public static <O> Key<O> of(String pTextKey, TypeReference<O> pOutputType) {
    return new StaticKey<>(pTextKey, pOutputType.getType());
  }

  public static <O> Key<O> from(Key<O> pKey1) {
    return pKey1;
  }

  public static <O> Key<O> from(Key<?> pKey1, Key<O> pKey2) {
    return new CompositeKey<>(pKey1, pKey2);
  }

  public static <O> Key<O> from(Key<?> pKey1, Key<?> pKey2, Key<O> pKey3) {
    return new CompositeKey<>(pKey1, pKey2, pKey3);
  }

  public static <O> AccessContextPlaceholder<O> accessContext(String pAccessContextKey, TypeReference<O> pOutputType) {
    return new StaticAccessContextPlaceholder<>(pAccessContextKey, pOutputType.getType());
  }

  public static <O> KeyPlaceholder<O> placeholder(String pPlaceholderKey, TypeReference<O> pOutputType) {
    return new StaticKeyPlaceholder<>(pPlaceholderKey, pOutputType.getType());
  }

  public static KeyPlaceholder<String> placeholder(String pPlaceholderKey, TypeReference<String> pOutputType,
    Key<String> pDefaultKey) {
    return new StaticKeyPlaceholderWithDefault(pPlaceholderKey, pOutputType.getType(), pDefaultKey);
  }

}
