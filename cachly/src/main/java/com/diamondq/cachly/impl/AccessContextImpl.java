package com.diamondq.cachly.impl;

import com.diamondq.cachly.AccessContext;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Represents data used during resolution of the key and resolution of the value
 */
public final class AccessContextImpl implements AccessContext {

  private final Map<Class<?>, Object> mData;

  /**
   * Primary Constructor
   *
   * @param pData the map of data to store
   */
  public AccessContextImpl(Map<Class<?>, Object> pData) {
    mData = Map.copyOf(pData);
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
