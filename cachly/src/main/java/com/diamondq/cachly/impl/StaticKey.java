package com.diamondq.cachly.impl;

import java.lang.reflect.Type;

public class StaticKey<O> extends AbstractKey<O> {

  public StaticKey(String pTextKey, Type pType) {
    super(pTextKey, pType);
  }

}