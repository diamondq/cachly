package com.diamondq.cachly.impl;

import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.TypeReference;

import org.checkerframework.checker.nullness.qual.NonNull;

public class StaticKeyPlaceholder<I, @NonNull K, O> extends AbstractKey<I, O>
  implements KeyPlaceholder<I, K, O>, KeyInternal<I, O> {

  public StaticKeyPlaceholder(String pType, TypeReference<O> pTypeReference) {
    super(pType, pTypeReference);
  }

}