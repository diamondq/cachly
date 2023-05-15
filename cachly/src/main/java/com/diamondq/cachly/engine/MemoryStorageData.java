package com.diamondq.cachly.engine;

import com.diamondq.cachly.Key;
import org.jetbrains.annotations.Nullable;

public class MemoryStorageData {
  public final Key<?> key;

  public final @Nullable Object value;

  public MemoryStorageData(Key<?> pKey, @Nullable Object pValue) {
    key = pKey;
    value = pValue;
  }

}