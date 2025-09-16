package com.diamondq.cachly.impl;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.spi.KeyPlaceholderSPI;
import com.diamondq.cachly.spi.KeySPI;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;

public class StaticKeyPlaceholderWithDefault extends AbstractKey<String> implements KeyPlaceholderSPI<String> {

  private final KeySPI<String> mDefaultKey;

  public StaticKeyPlaceholderWithDefault(String pKey, Type pType, Key<String> pDefaultKey) {
    super("{" + pKey + "}", pType, true);
    if (pDefaultKey instanceof KeySPI) mDefaultKey = (KeySPI<String>) pDefaultKey;
    else throw new IllegalArgumentException("The default key must be a KeySPI");
  }

  @Override
  public KeySPI<String> resolveDefault(Cache pCache, AccessContext pAccessContext) {
    String cacheValue = pCache.get(pAccessContext, mDefaultKey);
    return new ResolvedKeyPlaceholder<>(this, cacheValue);
  }

  public KeySPI<String> getDefaultKey() {
    return mDefaultKey;
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
    if (pObj == null) return false;
    if (pObj == this) return true;
    if (!pObj.getClass().equals(StaticKeyPlaceholderWithDefault.class)) return false;
    StaticKeyPlaceholderWithDefault other = (StaticKeyPlaceholderWithDefault) pObj;
    return Objects.equals(mKey, other.mKey) && Objects.equals(mOutputType, other.mOutputType) && Objects.equals(
      mHasPlaceholders,
      other.mHasPlaceholders
    ) && Objects.equals(mDefaultKey, other.mDefaultKey);
  }
}