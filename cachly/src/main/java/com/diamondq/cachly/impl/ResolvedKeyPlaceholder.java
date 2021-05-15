package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.CacheStorage;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ResolvedKeyPlaceholder<I, O> implements Key<I, O>, KeyInternal<I, O> {

  private final KeyInternal<I, O>                      mPlaceholder;

  private final String                                 mKey;

  private final @NonNull KeyInternal<Object, Object>[] mParts;

  public ResolvedKeyPlaceholder(KeyInternal<I, O> pPlaceholder, String pKey) {
    mPlaceholder = pPlaceholder;
    mKey = pKey;
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

  @Override
  public String getBaseKey() {
    return mPlaceholder.getKey();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getLastStorage()
   */
  @Override
  public CacheStorage getLastStorage() {
    return mPlaceholder.getLastStorage();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#getLoader()
   */
  @Override
  public CacheLoader<I, O> getLoader() {
    return mPlaceholder.getLoader();
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
    return null;
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#supportsNull()
   */
  @Override
  public boolean supportsNull() {
    return mPlaceholder.supportsNull();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#hasKeyDetails()
   */
  @Override
  public boolean hasKeyDetails() {
    return mPlaceholder.hasKeyDetails();
  }

  /**
   * @see com.diamondq.cachly.impl.KeyInternal#storeKeyDetails(com.diamondq.cachly.impl.KeyDetails)
   */
  @Override
  public void storeKeyDetails(KeyDetails<I, O> pDetails) {
    mPlaceholder.storeKeyDetails(pDetails);
  }
}