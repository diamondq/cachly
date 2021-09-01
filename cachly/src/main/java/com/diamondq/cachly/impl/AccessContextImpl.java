package com.diamondq.cachly.impl;

import com.diamondq.cachly.AccessContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class AccessContextImpl implements AccessContext {

  private final Map<Class<?>, Object> mData;

  public AccessContextImpl(Map<Class<?>, Object> pData) {
    mData = Collections.unmodifiableMap(new HashMap<>(pData));
  }

  /**
   * @see com.diamondq.cachly.AccessContext#get(java.lang.Class)
   */
  @Override
  public <X> @Nullable X get(Class<X> pClass) {
    @SuppressWarnings("unchecked")
    X x = (X) mData.get(pClass);
    return x;
  }

  public Map<Class<?>, Object> getData() {
    return mData;
  }
}
