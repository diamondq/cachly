package com.diamondq.cachly.impl;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.spi.KeyPlaceholderSPI;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;

public class StaticKeyPlaceholder<O> extends AbstractKey<O> implements KeyPlaceholderSPI<O>, KeySPI<O> {

  public StaticKeyPlaceholder(String pKey, TypeReference<O> pType) {
    super(pKey, pType, true);
  }

  @Override
  public KeySPI<O> resolveDefault(Cache pCache) {
    throw new IllegalStateException();
  }

}