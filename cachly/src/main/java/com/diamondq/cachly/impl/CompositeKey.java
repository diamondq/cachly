package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.TypeReference;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.spi.KeySPI;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CompositeKey<O> implements Key<O>, KeySPI<O> {

  private final @NonNull KeySPI<Object>[] mParts;

  private final KeySPI<O>                 mLast;

  private final int                            mPartsLen;

  public CompositeKey(Key<?> pKey1, Key<O> pKey2) {
    if (pKey1 instanceof KeySPI == false)
      throw new IllegalStateException();
    KeySPI<?> ki1 = (KeySPI<?>) pKey1;
    if (pKey2 instanceof KeySPI == false)
      throw new IllegalStateException();
    KeySPI<O> ki2 = (KeySPI<O>) pKey2;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeySPI<Object>[] tempParts = new KeySPI[] {ki1, ki2};
    mParts = tempParts;
    mPartsLen = mParts.length;
    mLast = ki2;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getOutputType()
   */
  @Override
  public TypeReference<O> getOutputType() {
    return mLast.getOutputType();
  }

  public CompositeKey(Key<?> pKey1, Key<?> pKey2, Key<O> pKey3) {
    if (pKey1 instanceof KeySPI == false)
      throw new IllegalStateException();
    KeySPI<?> ki1 = (KeySPI<?>) pKey1;
    if (pKey2 instanceof KeySPI == false)
      throw new IllegalStateException();
    KeySPI<?> ki2 = (KeySPI<?>) pKey2;
    if (pKey3 instanceof KeySPI == false)
      throw new IllegalStateException();
    KeySPI<O> ki3 = (KeySPI<O>) pKey3;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeySPI<Object>[] tempParts = new KeySPI[] {ki1, ki2, ki3};
    mParts = tempParts;
    mPartsLen = mParts.length;
    mLast = ki3;
  }

  public CompositeKey(@NonNull KeySPI<Object>[] pNewParts) {
    mParts = pNewParts;
    mPartsLen = mParts.length;
    @SuppressWarnings("unchecked")
    KeySPI<O> temp = (KeySPI<O>) mParts[mPartsLen - 1];
    mLast = temp;
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

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getLastStorage()
   */
  @Override
  public CacheStorage getLastStorage() {
    return mLast.getLastStorage();
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
    throw new IllegalStateException();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (KeySPI<?> part : mParts) {
      sb.append(part.toString());
      sb.append("/");
    }
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }
}