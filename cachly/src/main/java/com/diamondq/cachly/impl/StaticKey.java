package com.diamondq.cachly.impl;

import com.diamondq.common.TypeReference;

public class StaticKey<O> extends AbstractKey<O> {

  public StaticKey(String pTextKey, TypeReference<O> pType) {
    super(pTextKey, pType);
  }

}