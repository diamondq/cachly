package com.diamondq.cachly.impl;

import com.diamondq.cachly.AccessContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AccessContextImpl implements AccessContext {

  private final Map<Class<?>, Object> mData;

  public AccessContextImpl(Map<Class<?>, Object> pData) {
    mData = Collections.unmodifiableMap(new HashMap<>(pData));
  }

  @Override
  public Map<Class<?>, Object> getData() {
    return mData;
  }

  @Override
  public <X> Optional<X> get(Class<X> pClass) {
    @SuppressWarnings("unchecked") @Nullable X result = (X) mData.get(pClass);
    return Optional.ofNullable(result);
  }
}
