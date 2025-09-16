package com.diamondq.cachly.impl;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.spi.KeyPlaceholderSPI;
import com.diamondq.cachly.spi.KeySPI;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

public class StaticKeyPlaceholder<O extends @Nullable Object> extends AbstractKey<O> implements KeyPlaceholderSPI<O> {

  public StaticKeyPlaceholder(String pKey, Type pType) {
    super("{" + pKey + "}", pType, true);
  }

  @Override
  public KeySPI<O> resolveDefault(Cache pCache, AccessContext pAccessContext) {
    throw new IllegalStateException();
  }

}