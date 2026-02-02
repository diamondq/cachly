package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.spi.KeySPI;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Some basic methods for implementing keys
 *
 * @param <O> the key type
 */
public class AbstractKey<O extends @Nullable Object> implements KeySPI<O> {

  /**
   * The key string
   */
  protected final String mKey;

  /**
   * The key details
   */
  protected @Nullable KeyDetails<O> mKeyDetails;

  /**
   * The parts of the key
   */
  protected final KeySPI<@Nullable Object>[] mParts;

  /**
   * The output type
   */
  protected final Type mOutputType;

  /**
   * Whether there are placeholders within the parts
   */
  protected final boolean mHasPlaceholders;

  /**
   * Primary Constructor
   *
   * @param pKey the key
   * @param pOutputType the output type
   * @param pHasPlaceholders whether this has placeholders
   */
  public AbstractKey(String pKey, Type pOutputType, boolean pHasPlaceholders) {
    mKey = pKey;
    mOutputType = pOutputType;
    @SuppressWarnings({ "null", "unchecked" }) KeySPI<@Nullable Object>[] tempParts = new KeySPI[] { this };
    mParts = tempParts;
    mHasPlaceholders = pHasPlaceholders;
  }

  @Override
  public void clearKeyDetails() {
    mKeyDetails = null;
    for (KeySPI<@Nullable Object> part : mParts) if (part != this) part.clearKeyDetails();
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
  public KeySPI<@Nullable Object>[] getParts() {
    return mParts;
  }

  /**
   * @see com.diamondq.cachly.Key#getPreviousKey(com.diamondq.cachly.Key)
   */
  @Override
  public <P extends @Nullable Object> @Nullable Key<P> getPreviousKey(Key<P> pTemplate) {

    /* There are no previous keys in a SimpleKey */

    return null;
  }

  @Override
  public @Nullable KeySPI<@Nullable Object> getPreviousKey() {

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
    KeyDetails<O> keyDetails = mKeyDetails;
    if (keyDetails == null)
      throw new IllegalStateException("Unable to find a cache storage that will cover " + getFullBaseKey());
    return keyDetails.getLastStorage();
  }

  @Override
  public String getLastSerializerName() {
    KeyDetails<O> keyDetails = mKeyDetails;
    if (keyDetails == null)
      throw new IllegalStateException("Unable to find a serializer that will cover " + getFullBaseKey());
    return keyDetails.getLastSerializerName();
  }

  @Override
  public boolean supportsNull() {
    KeyDetails<O> keyDetails = mKeyDetails;
    if (keyDetails == null)
      throw new IllegalStateException("Unable to find a cache loader that will cover " + getFullBaseKey());
    return keyDetails.supportsNull();
  }

  @Override
  public CacheLoader<O> getLoader() {
    KeyDetails<O> keyDetails = mKeyDetails;
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