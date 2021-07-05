package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;

import java.lang.reflect.Type;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AbstractKey<O> implements KeySPI<O> {

  private final String                    mKey;

  private @Nullable KeyDetails<O>         mKeyDetails;

  private final @NonNull KeySPI<Object>[] mParts;

  private final TypeReference<O>          mOutputTypeRef;

  private final Type                      mOutputType;

  private final boolean                   mHasPlaceholders;

  public AbstractKey(String pKey, TypeReference<O> pOutputType, boolean pHasPlaceholders) {
    mKey = pKey;
    mOutputTypeRef = pOutputType;
    mOutputType = pOutputType.getType();
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeySPI<Object>[] tempParts = new KeySPI[] {this};
    mParts = tempParts;
    mHasPlaceholders = pHasPlaceholders;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#hasPlaceholders()
   */
  @Override
  public boolean hasPlaceholders() {
    return mHasPlaceholders;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getKey()
   */
  @Override
  public String getKey() {
    return mKey;
  }

  /**
   * @see com.diamondq.cachly.Key#getOutputTypeReference()
   */
  @Override
  public TypeReference<O> getOutputTypeReference() {
    return mOutputTypeRef;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getOutputType()
   */
  @Override
  public Type getOutputType() {
    return mOutputType;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getParts()
   */
  @Override
  public @NonNull KeySPI<Object>[] getParts() {
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

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getPreviousKey()
   */
  @Override
  public @Nullable KeySPI<Object> getPreviousKey() {

    /* There are no previous keys in a SimpleKey */

    return null;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#storeKeyDetails(com.diamondq.cachly.impl.KeyDetails)
   */
  @Override
  public void storeKeyDetails(KeyDetails<O> pDetails) {
    mKeyDetails = pDetails;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#hasKeyDetails()
   */
  @Override
  public boolean hasKeyDetails() {
    return mKeyDetails != null;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getLastStorage()
   */
  @Override
  public CacheStorage getLastStorage() {
    return Objects.requireNonNull(mKeyDetails).getLastStorage();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getLastSerializerName()
   */
  @Override
  public String getLastSerializerName() {
    return Objects.requireNonNull(mKeyDetails).getLastSerializerName();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#supportsNull()
   */
  @Override
  public boolean supportsNull() {
    return Objects.requireNonNull(mKeyDetails).supportsNull();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getLoader()
   */
  @Override
  public CacheLoader<O> getLoader() {
    return Objects.requireNonNull(mKeyDetails).getLoader();
  }

  @Override
  public String getBaseKey() {
    return mKey;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getFullBaseKey()
   */
  @Override
  public String getFullBaseKey() {
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