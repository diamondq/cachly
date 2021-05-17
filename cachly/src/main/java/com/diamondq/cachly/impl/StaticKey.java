package com.diamondq.cachly.impl;

import com.diamondq.cachly.TypeReference;

public class StaticKey<I, O> extends AbstractKey<I, O> {

  public StaticKey(String pTextKey, TypeReference<O> pTypeReference) {
    super(pTextKey, pTypeReference);
  }

}