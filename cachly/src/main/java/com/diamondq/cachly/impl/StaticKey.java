package com.diamondq.cachly.impl;

import com.diamondq.cachly.TypeReference;

public class StaticKey<O> extends AbstractKey<O> {

  public StaticKey(String pTextKey, TypeReference<O> pTypeReference) {
    super(pTextKey, pTypeReference);
  }

}