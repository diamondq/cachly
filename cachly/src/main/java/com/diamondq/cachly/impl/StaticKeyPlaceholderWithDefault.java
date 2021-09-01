package com.diamondq.cachly.impl;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.spi.KeyPlaceholderSPI;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

public class StaticKeyPlaceholderWithDefault extends AbstractKey<String> implements KeyPlaceholderSPI<String> {

  private final Key<String> mDefaultKey;

  public StaticKeyPlaceholderWithDefault(String pKey, TypeReference<String> pType, Key<String> pDefaultKey) {
    super(pKey, pType, true);
    mDefaultKey = pDefaultKey;
  }

  @Override
  public KeySPI<String> resolveDefault(Cache pCache, AccessContext pAccessContext) {
    String cacheValue = pCache.get(pAccessContext, mDefaultKey);
    return new ResolvedKeyPlaceholder<>(this, cacheValue);
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(mKey, mOutputType, mHasPlaceholders, mDefaultKey);
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(@Nullable Object pObj) {
    if (pObj == null)
      return false;
    if (pObj == this)
      return true;
    if (pObj.getClass().equals(StaticKeyPlaceholderWithDefault.class) == false)
      return false;
    StaticKeyPlaceholderWithDefault other = (StaticKeyPlaceholderWithDefault) pObj;
    if (Objects.equals(mKey, other.mKey) && Objects.equals(mOutputType, other.mOutputType)
      && Objects.equals(mHasPlaceholders, other.mHasPlaceholders) && Objects.equals(mDefaultKey, other.mDefaultKey))
      return true;
    return false;
  }
}