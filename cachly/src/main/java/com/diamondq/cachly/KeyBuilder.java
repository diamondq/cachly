package com.diamondq.cachly;

import com.diamondq.cachly.impl.CompositeKey;
import com.diamondq.cachly.impl.StaticKey;
import com.diamondq.cachly.impl.StaticKeyPlaceholder;

import org.checkerframework.checker.nullness.qual.NonNull;

public class KeyBuilder {

  public static <I, O> Key<I, O> of(String pTextKey, TypeReference<O> pOutputType) {
    return new StaticKey<I, O>(pTextKey, pOutputType);
  }

  public static <O> Key<ROOT, O> from(Key<ROOT, O> pKey1) {
    return pKey1;
  }

  public static <M1, O> Key<M1, O> from(Key<ROOT, M1> pKey1, Key<M1, O> pKey2) {
    return new CompositeKey<M1, O>(pKey1, pKey2);
  }

  public static <M1, M2, O> Key<M2, O> from(Key<ROOT, M1> pKey1, Key<M1, M2> pKey2, Key<M2, O> pKey3) {
    return new CompositeKey<M2, O>(pKey1, pKey2, pKey3);
  }

  public static <I, @NonNull K, O> KeyPlaceholder<I, K, O> placeholder(String pPlaceholderKey,
    TypeReference<O> pOutputType) {
    return new StaticKeyPlaceholder<I, K, O>(pPlaceholderKey, pOutputType);
  }

}
