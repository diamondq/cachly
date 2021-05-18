package com.diamondq.cachly;

import com.diamondq.cachly.impl.CompositeKey;
import com.diamondq.cachly.impl.StaticKey;
import com.diamondq.cachly.impl.StaticKeyPlaceholder;

import java.lang.reflect.Type;

import org.checkerframework.checker.nullness.qual.NonNull;

public class KeyBuilder {

  public static <O> Key<O> of(String pTextKey, Type pOutputType) {
    return new StaticKey<O>(pTextKey, pOutputType);
  }

  public static <O> Key<O> from(Key<O> pKey1) {
    return pKey1;
  }

  public static <O> Key<O> from(Key<?> pKey1, Key<O> pKey2) {
    return new CompositeKey<O>(pKey1, pKey2);
  }

  public static <O> Key<O> from(Key<?> pKey1, Key<?> pKey2, Key<O> pKey3) {
    return new CompositeKey<O>(pKey1, pKey2, pKey3);
  }

  public static <@NonNull K, O> KeyPlaceholder<K, O> placeholder(String pPlaceholderKey, Type pOutputType) {
    return new StaticKeyPlaceholder<K, O>(pPlaceholderKey, pOutputType);
  }

}
