package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.TypeReference;
import com.diamondq.cachly.engine.CacheStorage;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AbstractKey<I, O> implements Key<I, O>, KeyInternal<I, O> {

  private final String                                 mKey;

  private @Nullable KeyDetails<I, O>                   mKeyDetails;

  private final @NonNull KeyInternal<Object, Object>[] mParts;

  private final TypeReference<O>                       mOutputType;

  public AbstractKey(String pKey, TypeReference<O> pOutputType) {
    mKey = pKey;
    mOutputType = pOutputType;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeyInternal<Object, Object>[] tempParts = new KeyInternal[] {this};
    mParts = tempParts;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getKey()
   */
  @Override
  public String getKey() {
    return mKey;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getOutputType()
   */
  @Override
  public TypeReference<O> getOutputType() {
    return mOutputType;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getParts()
   */
  @Override
  public @NonNull KeyInternal<Object, Object>[] getParts() {
    return mParts;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getPreviousKey()
   */
  @Override
  public @Nullable KeyInternal<Object, Object> getPreviousKey() {

    /* There are no previous keys in a SimpleKey */

    return null;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#storeKeyDetails(com.diamondq.cachly.impl.KeyDetails)
   */
  @Override
  public void storeKeyDetails(KeyDetails<I, O> pDetails) {
    mKeyDetails = pDetails;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#hasKeyDetails()
   */
  @Override
  public boolean hasKeyDetails() {
    return mKeyDetails != null;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getLastStorage()
   */
  @Override
  public CacheStorage getLastStorage() {
    return Objects.requireNonNull(mKeyDetails).getLastStorage();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#supportsNull()
   */
  @Override
  public boolean supportsNull() {
    return Objects.requireNonNull(mKeyDetails).supportsNull();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getLoader()
   */
  @Override
  public CacheLoader<I, O> getLoader() {
    return Objects.requireNonNull(mKeyDetails).getLoader();
  }

  @Override
  public String getBaseKey() {
    return mKey;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return mKey;
  }
}