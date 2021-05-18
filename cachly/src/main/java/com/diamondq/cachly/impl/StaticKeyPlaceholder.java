package com.diamondq.cachly.impl;

import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.TypeReference;
import com.diamondq.cachly.spi.KeySPI;

import org.checkerframework.checker.nullness.qual.NonNull;

public class StaticKeyPlaceholder<@NonNull K, O> extends AbstractKey<O>
  implements KeyPlaceholder<K, O>, KeySPI<O> {

  public StaticKeyPlaceholder(String pType, TypeReference<O> pTypeReference) {
    super(pType, pTypeReference);
  }

}