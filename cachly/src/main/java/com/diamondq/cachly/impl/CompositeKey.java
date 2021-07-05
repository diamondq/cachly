package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;

import java.lang.reflect.Type;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CompositeKey<O> implements KeySPI<O> {

  private final @NonNull KeySPI<Object>[] mParts;

  private final KeySPI<O>                 mLast;

  private final int                       mPartsLen;

  private final boolean                   mHasPlaceholders;

  public CompositeKey(Key<?> pKey1, Key<O> pKey2) {
    if (pKey1 instanceof KeySPI == false)
      throw new IllegalStateException();
    KeySPI<?> ki1 = (KeySPI<?>) pKey1;
    @NonNull
    KeySPI<Object>[] ki1Parts = ki1.getParts();
    if (pKey2 instanceof KeySPI == false)
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
    }
    mHasPlaceholders = hasPlaceHolders;
  }

  public CompositeKey(Key<?> pKey1, Key<?> pKey2, Key<O> pKey3) {
    if (pKey1 instanceof KeySPI == false)
      throw new IllegalStateException();
    KeySPI<?> ki1 = (KeySPI<?>) pKey1;
    @NonNull
    KeySPI<Object>[] ki1Parts = ki1.getParts();
    if (pKey2 instanceof KeySPI == false)
      throw new IllegalStateException();
    KeySPI<?> ki2 = (KeySPI<?>) pKey2;
    @NonNull
    KeySPI<Object>[] ki2Parts = ki2.getParts();
    if (pKey3 instanceof KeySPI == false)
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
    }
    mHasPlaceholders = hasPlaceHolders;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#hasPlaceholders()
   */
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

  /**
   * @see com.diamondq.cachly.Key#getOutputTypeReference()
   */
  @Override
  public TypeReference<O> getOutputTypeReference() {
    return mLast.getOutputTypeReference();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getParts()
   */
  @Override
  public @NonNull KeySPI<Object>[] getParts() {
    return mParts;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getKey()
   */
  @Override
  public String getKey() {
    return mLast.getKey();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getBaseKey()
   */
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

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getLastStorage()
   */
  @Override
  public CacheStorage getLastStorage() {
    return mLast.getLastStorage();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getLastSerializerName()
   */
  @Override
  public String getLastSerializerName() {
    return mLast.getLastSerializerName();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getLoader()
   */
  @Override
  public CacheLoader<O> getLoader() {
    return mLast.getLoader();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getPreviousKey()
   */
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
    KeySPI<Object> testKey = getPreviousKey();
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

  /**
   * @see com.diamondq.cachly.spi.KeySPI#hasKeyDetails()
   */
  @Override
  public boolean hasKeyDetails() {
    for (KeySPI<?> part : mParts)
      if (part.hasKeyDetails() == false)
        return false;
    return true;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#storeKeyDetails(com.diamondq.cachly.impl.KeyDetails)
   */
  @Override
  public void storeKeyDetails(KeyDetails<O> pDetails) {
    throw new IllegalStateException();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#supportsNull()
   */
  @Override
  public boolean supportsNull() {
    return mLast.supportsNull();
  }

  /**
   * @see java.lang.Object#toString()
   */
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
}