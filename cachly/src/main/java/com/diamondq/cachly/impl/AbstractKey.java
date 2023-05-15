package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.spi.KeySPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;

public class AbstractKey<O> implements KeySPI<O> {

  protected final String mKey;

  protected @Nullable KeyDetails<O> mKeyDetails;

  protected final @NotNull KeySPI<Object>[] mParts;

  protected final Type mOutputType;

  protected final boolean mHasPlaceholders;

  public AbstractKey(String pKey, Type pOutputType, boolean pHasPlaceholders) {
    mKey = pKey;
    mOutputType = pOutputType;
    @SuppressWarnings({ "null", "unchecked" }) @NotNull KeySPI<Object>[] tempParts = new KeySPI[] { this };
    mParts = tempParts;
    mHasPlaceholders = pHasPlaceholders;
  }

  @Override
  public boolean hasPlaceholders() {
    return mHasPlaceholders;
  }

  @Override
  public String getKey() {
    return mKey;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getOutputType()
   */
  @Override
  public Type getOutputType() {
    return mOutputType;
  }

  @Override
  public @NotNull KeySPI<Object>[] getParts() {
    return mParts;
  }

  /**
   * @see com.diamondq.cachly.Key#getPreviousKey(com.diamondq.cachly.Key)
   */
  @Override
  public <P> @Nullable Key<P> getPreviousKey(Key<P> pTemplate) {

    /* There are no previous keys in a SimpleKey */

    return null;
  }

  @Override
  public @Nullable KeySPI<Object> getPreviousKey() {

    /* There are no previous keys in a SimpleKey */

    return null;
  }

  @Override
  public void storeKeyDetails(KeyDetails<O> pDetails) {
    mKeyDetails = pDetails;
  }

  @Override
  public boolean hasKeyDetails() {
    return mKeyDetails != null;
  }

  @Override
  public CacheStorage getLastStorage() {
    @Nullable KeyDetails<O> keyDetails = mKeyDetails;
    if (keyDetails == null)
      throw new IllegalStateException("Unable to find a cache storage that will cover " + getFullBaseKey());
    return keyDetails.getLastStorage();
  }

  @Override
  public String getLastSerializerName() {
    @Nullable KeyDetails<O> keyDetails = mKeyDetails;
    if (keyDetails == null)
      throw new IllegalStateException("Unable to find a serializer that will cover " + getFullBaseKey());
    return keyDetails.getLastSerializerName();
  }

  @Override
  public boolean supportsNull() {
    @Nullable KeyDetails<O> keyDetails = mKeyDetails;
    if (keyDetails == null)
      throw new IllegalStateException("Unable to find a cache loader that will cover " + getFullBaseKey());
    return keyDetails.supportsNull();
  }

  @Override
  public CacheLoader<O> getLoader() {
    @Nullable KeyDetails<O> keyDetails = mKeyDetails;
    if (keyDetails == null)
      throw new IllegalStateException("Unable to find a cache loader that will cover " + getFullBaseKey());
    return keyDetails.getLoader();
  }

  @SuppressWarnings("SuspiciousGetterSetter")
  @Override
  public String getBaseKey() {
    return mKey;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getFullBaseKey()
   */
  @SuppressWarnings("SuspiciousGetterSetter")
  @Override
  public String getFullBaseKey() {
    return mKey;
  }

  @Override
  public String toString() {
    return mKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mKey, mOutputType, mHasPlaceholders);
  }

  @Override
  public boolean equals(@Nullable Object pObj) {
    if (pObj == null) return false;
    if (pObj == this) return true;
    if (!pObj.getClass().equals(getClass())) return false;
    @SuppressWarnings("unchecked") AbstractKey<O> other = (AbstractKey<O>) pObj;
    return Objects.equals(mKey, other.mKey) && Objects.equals(mOutputType, other.mOutputType) && Objects.equals(
      mHasPlaceholders,
      other.mHasPlaceholders
    );
  }
}