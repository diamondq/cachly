package com.diamondq.cachly.impl;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

public class StaticKey<O extends @Nullable Object> extends AbstractKey<O> {

  public StaticKey(String pTextKey, Type pType) {
    super(pTextKey, pType, false);
  }

}