package com.diamondq.cachly.engine;

import com.diamondq.cachly.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MemoryStorageData {
  public final Key<?> key;

  public final @Nullable Object value;

  public MemoryStorageData(Key<?> pKey, @Nullable Object pValue) {
    key = pKey;
    value = pValue;
  }

}