package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.ROOT;
import com.diamondq.cachly.TypeReference;
import com.diamondq.cachly.engine.CacheStorage;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CompositeKey<I, O> implements Key<I, O>, KeyInternal<I, O> {

  private final @NonNull KeyInternal<Object, Object>[] mParts;

  private final KeyInternal<I, O>                      mLast;

  private final int                                    mPartsLen;

  public CompositeKey(Key<ROOT, I> pKey1, Key<I, O> pKey2) {
    if (pKey1 instanceof KeyInternal == false)
      throw new IllegalStateException();
    KeyInternal<ROOT, I> ki1 = (KeyInternal<ROOT, I>) pKey1;
    if (pKey2 instanceof KeyInternal == false)
      throw new IllegalStateException();
    KeyInternal<I, O> ki2 = (KeyInternal<I, O>) pKey2;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeyInternal<Object, Object>[] tempParts = new KeyInternal[] {ki1, ki2};
    mParts = tempParts;
    mPartsLen = mParts.length;
    mLast = ki2;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getOutputType()
   */
  @Override
  public TypeReference<O> getOutputType() {
    return mLast.getOutputType();
  }

  public <M1> CompositeKey(Key<ROOT, M1> pKey1, Key<M1, I> pKey2, Key<I, O> pKey3) {
    if (pKey1 instanceof KeyInternal == false)
      throw new IllegalStateException();
    KeyInternal<ROOT, M1> ki1 = (KeyInternal<ROOT, M1>) pKey1;
    if (pKey2 instanceof KeyInternal == false)
      throw new IllegalStateException();
    KeyInternal<M1, I> ki2 = (KeyInternal<M1, I>) pKey2;
    if (pKey3 instanceof KeyInternal == false)
      throw new IllegalStateException();
    KeyInternal<I, O> ki3 = (KeyInternal<I, O>) pKey3;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeyInternal<Object, Object>[] tempParts = new KeyInternal[] {ki1, ki2, ki3};
    mParts = tempParts;
    mPartsLen = mParts.length;
    mLast = ki3;
  }

  public CompositeKey(@NonNull KeyInternal<Object, Object>[] pNewParts) {
    mParts = pNewParts;
    mPartsLen = mParts.length;
    @SuppressWarnings("unchecked")
    KeyInternal<I, O> temp = (KeyInternal<I, O>) mParts[mPartsLen - 1];
    mLast = temp;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getParts()
   */
  @Override
  public @NonNull KeyInternal<Object, Object>[] getParts() {
    return mParts;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getKey()
   */
  @Override
  public String getKey() {
    return mLast.getKey();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getBaseKey()
   */
  @Override
  public String getBaseKey() {
    throw new IllegalStateException();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getLastStorage()
   */
  @Override
  public CacheStorage getLastStorage() {
    return mLast.getLastStorage();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getLoader()
   */
  @Override
  public CacheLoader<I, O> getLoader() {
    return mLast.getLoader();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getPreviousKey()
   */
  @Override
  public @Nullable KeyInternal<Object, Object> getPreviousKey() {
    if (mPartsLen == 1)
      return null;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeyInternal<Object, Object>[] parentParts = new KeyInternal[mPartsLen - 1];
    System.arraycopy(mParts, 0, parentParts, 0, mPartsLen - 1);
    return new CompositeKey<>(parentParts);
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#hasKeyDetails()
   */
  @Override
  public boolean hasKeyDetails() {
    for (KeyInternal<?, ?> part : mParts)
      if (part.hasKeyDetails() == false)
        return false;
    return true;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#storeKeyDetails(com.diamondq.cachly.impl.KeyDetails)
   */
  @Override
  public void storeKeyDetails(KeyDetails<I, O> pDetails) {
    throw new IllegalStateException();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#supportsNull()
   */
  @Override
  public boolean supportsNull() {
    throw new IllegalStateException();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (KeyInternal<?, ?> part : mParts) {
      sb.append(part.toString());
      sb.append("/");
    }
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }
}