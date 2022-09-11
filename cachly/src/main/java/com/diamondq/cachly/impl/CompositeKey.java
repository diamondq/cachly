package com.diamondq.cachly.impl;

import com.diamondq.cachly.AccessContextPlaceholder;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.spi.KeySPI;

import java.lang.reflect.Type;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CompositeKey<O> implements KeySPI<O> {

  private final @NonNull KeySPI<Object>[] mParts;

  private final KeySPI<O>                 mLast;

  private final int                       mPartsLen;

  private final boolean                   mHasPlaceholders;

  public CompositeKey(Key<?> pKey1, Key<O> pKey2) {
    if (!(pKey1 instanceof KeySPI))
      throw new IllegalStateException();
    KeySPI<?> ki1 = (KeySPI<?>) pKey1;
    @NonNull
    KeySPI<Object>[] ki1Parts = ki1.getParts();
    if (!(pKey2 instanceof KeySPI))
      throw new IllegalStateException();
    KeySPI<O> ki2 = (KeySPI<O>) pKey2;
    @NonNull
    KeySPI<Object>[] ki2Parts = ki2.getParts();
    int partsLen = ki1Parts.length + ki2Parts.length;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeySPI<Object>[] tempParts = new KeySPI[partsLen];
    System.arraycopy(ki1Parts, 0, tempParts, 0, ki1Parts.length);
    System.arraycopy(ki2Parts, 0, tempParts, ki1Parts.length, ki2Parts.length);
    mParts = tempParts;
    mPartsLen = mParts.length;
    mLast = ki2;
    boolean hasPlaceHolders = false;
    for (int i = 0; i < mParts.length; i++) {
      if (mParts[i] instanceof KeyPlaceholder)
        hasPlaceHolders = true;
      else if (mParts[i] instanceof AccessContextPlaceholder)
        hasPlaceHolders = true;
    }
    mHasPlaceholders = hasPlaceHolders;
  }

  public CompositeKey(Key<?> pKey1, Key<?> pKey2, Key<O> pKey3) {
    if (!(pKey1 instanceof KeySPI))
      throw new IllegalStateException();
    KeySPI<?> ki1 = (KeySPI<?>) pKey1;
    @NonNull
    KeySPI<Object>[] ki1Parts = ki1.getParts();
    if (!(pKey2 instanceof KeySPI))
      throw new IllegalStateException();
    KeySPI<?> ki2 = (KeySPI<?>) pKey2;
    @NonNull
    KeySPI<Object>[] ki2Parts = ki2.getParts();
    if (!(pKey3 instanceof KeySPI))
      throw new IllegalStateException();
    KeySPI<O> ki3 = (KeySPI<O>) pKey3;
    @NonNull
    KeySPI<Object>[] ki3Parts = ki3.getParts();
    int partsLen = ki1Parts.length + ki2Parts.length + ki3Parts.length;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeySPI<Object>[] tempParts = new KeySPI[partsLen];
    System.arraycopy(ki1Parts, 0, tempParts, 0, ki1Parts.length);
    System.arraycopy(ki2Parts, 0, tempParts, ki1Parts.length, ki2Parts.length);
    System.arraycopy(ki3Parts, 0, tempParts, ki1Parts.length + ki2Parts.length, ki3Parts.length);
    mParts = tempParts;
    mPartsLen = mParts.length;
    mLast = ki3;
    boolean hasPlaceHolders = false;
    for (int i = 0; i < mParts.length; i++) {
      if (mParts[i] instanceof KeyPlaceholder)
        hasPlaceHolders = true;
      else if (mParts[i] instanceof AccessContextPlaceholder)
        hasPlaceHolders = true;
    }
    mHasPlaceholders = hasPlaceHolders;
  }

  public CompositeKey(@NonNull KeySPI<Object>[] pNewParts) {
    mParts = pNewParts;
    mPartsLen = mParts.length;
    @SuppressWarnings("unchecked")
    KeySPI<O> temp = (KeySPI<O>) mParts[mPartsLen - 1];
    mLast = temp;
    boolean hasPlaceHolders = false;
    for (int i = 0; i < mParts.length; i++) {
      if (mParts[i] instanceof KeyPlaceholder)
        hasPlaceHolders = true;
      else if (mParts[i] instanceof AccessContextPlaceholder)
        hasPlaceHolders = true;
    }
    mHasPlaceholders = hasPlaceHolders;
  }

  @Override
  public boolean hasPlaceholders() {
    return mHasPlaceholders;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getOutputType()
   */
  @Override
  public Type getOutputType() {
    return mLast.getOutputType();
  }

  @Override
  public @NonNull KeySPI<Object>[] getParts() {
    return mParts;
  }

  @Override
  public String getKey() {
    return mLast.getKey();
  }

  @Override
  public String getBaseKey() {
    throw new IllegalStateException();
  }

  @Override
  public String getFullBaseKey() {
    StringBuilder sb = new StringBuilder();
    for (KeySPI<?> part : mParts) {
      sb.append(part.getBaseKey());
      sb.append("/");
    }
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }

  @Override
  public CacheStorage getLastStorage() {
    return mLast.getLastStorage();
  }

  @Override
  public String getLastSerializerName() {
    return mLast.getLastSerializerName();
  }

  @Override
  public CacheLoader<O> getLoader() {
    return mLast.getLoader();
  }

  @Override
  public @Nullable KeySPI<Object> getPreviousKey() {
    if (mPartsLen == 1)
      return null;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeySPI<Object>[] parentParts = new KeySPI[mPartsLen - 1];
    System.arraycopy(mParts, 0, parentParts, 0, mPartsLen - 1);
    return new CompositeKey<>(parentParts);
  }

  /**
   * @see com.diamondq.cachly.Key#getPreviousKey(com.diamondq.cachly.Key)
   */
  @Override
  public <P> @Nullable Key<P> getPreviousKey(Key<P> pTemplate) {
    @SuppressWarnings("unchecked")
    KeySPI<Object> testKey = (KeySPI<Object>) this;
    String testKeyStr = pTemplate.toString();
    while (testKey != null) {
      if (testKey.getFullBaseKey().equals(testKeyStr)) {
        @SuppressWarnings("unchecked")
        Key<P> result = (Key<P>) testKey;
        return result;
      }
      testKey = testKey.getPreviousKey();
    }
    return null;
  }

  @Override
  public boolean hasKeyDetails() {
    for (KeySPI<?> part : mParts)
      if (!part.hasKeyDetails())
        return false;
    return true;
  }

  @Override
  public void storeKeyDetails(KeyDetails<O> pDetails) {
    throw new IllegalStateException();
  }

  @Override
  public boolean supportsNull() {
    return mLast.supportsNull();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (KeySPI<?> part : mParts) {
      sb.append(part.getKey());
      sb.append("/");
    }
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(mParts, mHasPlaceholders);
  }

  @Override
  public boolean equals(@Nullable Object pObj) {
    if (pObj == null)
      return false;
    if (pObj == this)
      return true;
    if (!pObj.getClass().equals(CompositeKey.class))
      return false;
    @SuppressWarnings("unchecked")
    CompositeKey<O> other = (CompositeKey<O>) pObj;
    return Objects.equals(mParts, other.mParts) && Objects.equals(mHasPlaceholders, other.mHasPlaceholders);
  }
}