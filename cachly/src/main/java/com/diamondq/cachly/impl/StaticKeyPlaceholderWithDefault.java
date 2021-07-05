package com.diamondq.cachly.impl;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.spi.KeyPlaceholderSPI;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;

public class StaticKeyPlaceholderWithDefault extends AbstractKey<String> implements KeyPlaceholderSPI<String> {

  private final Key<String> mDefaultKey;

  public StaticKeyPlaceholderWithDefault(String pKey, TypeReference<String> pType, Key<String> pDefaultKey) {
    super(pKey, pType, true);
    mDefaultKey = pDefaultKey;
  }

  @Override
  public KeySPI<String> resolveDefault(Cache pCache) {
    String cacheValue = pCache.get(mDefaultKey);
    return new ResolvedKeyPlaceholder<>(this, cacheValue);
  }

}