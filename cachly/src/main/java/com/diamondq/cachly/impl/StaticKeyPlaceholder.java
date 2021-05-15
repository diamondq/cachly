package com.diamondq.cachly.impl;

import com.diamondq.cachly.KeyPlaceholder;

import org.checkerframework.checker.nullness.qual.NonNull;

public class StaticKeyPlaceholder<I, @NonNull K, O> extends AbstractKey<I, O>
  implements KeyPlaceholder<I, K, O>, KeyInternal<I, O> {

  public StaticKeyPlaceholder(String pType) {
    super(pType);
  }

}