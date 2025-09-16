package com.diamondq.cachly.engine;

import com.diamondq.cachly.Key;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("ClassCanBeRecord")
public class MemoryStorageData {
  public final Key<? extends @Nullable Object> key;

  public final @Nullable Object value;

  public MemoryStorageData(Key<? extends @Nullable Object> pKey, @Nullable Object pValue) {
    key = pKey;
    value = pValue;
  }

}