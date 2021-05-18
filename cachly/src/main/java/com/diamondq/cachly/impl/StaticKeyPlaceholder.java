package com.diamondq.cachly.impl;

import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.spi.KeySPI;

import java.lang.reflect.Type;

import org.checkerframework.checker.nullness.qual.NonNull;

public class StaticKeyPlaceholder<@NonNull K, O> extends AbstractKey<O> implements KeyPlaceholder<K, O>, KeySPI<O> {

  public StaticKeyPlaceholder(String pKey, Type pType) {
    super(pKey, pType);
  }

}