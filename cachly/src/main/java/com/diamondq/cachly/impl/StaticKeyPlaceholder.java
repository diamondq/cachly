package com.diamondq.cachly.impl;

import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;

public class StaticKeyPlaceholder<O> extends AbstractKey<O> implements KeyPlaceholder<O>, KeySPI<O> {

  public StaticKeyPlaceholder(String pKey, TypeReference<O> pType) {
    super(pKey, pType);
  }

}