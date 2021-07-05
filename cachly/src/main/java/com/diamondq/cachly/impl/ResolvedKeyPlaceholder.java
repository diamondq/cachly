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

public class ResolvedKeyPlaceholder<O> implements KeySPI<O> {

  private final KeySPI<O>                 mPlaceholder;

  private final String                    mKey;

  private final @NonNull KeySPI<Object>[] mParts;

  public ResolvedKeyPlaceholder(KeySPI<O> pPlaceholder, String pKey) {
    mPlaceholder = pPlaceholder;
    mKey = pKey;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeySPI<Object>[] tempParts = new KeySPI[] {this};
    mParts = tempParts;
  }

  @Override
  public boolean hasPlaceholders() {
    return false;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getKey()
   */
  @Override
  public String getKey() {
    return mKey;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getOutputType()
   */
  @Override
  public Type getOutputType() {
    return mPlaceholder.getOutputType();
  }

  /**
   * @see com.diamondq.cachly.Key#getOutputTypeReference()
   */
  @Override
  public TypeReference<O> getOutputTypeReference() {
    return mPlaceholder.getOutputTypeReference();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getBaseKey()
   */
  @Override
  public String getBaseKey() {
    return mPlaceholder.getKey();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getFullBaseKey()
   */
  @Override
  public String getFullBaseKey() {
    return mKey;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getLastStorage()
   */
  @Override
  public CacheStorage getLastStorage() {
    return mPlaceholder.getLastStorage();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getLastSerializerName()
   */
  @Override
  public String getLastSerializerName() {
    return mPlaceholder.getLastSerializerName();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getLoader()
   */
  @Override
  public CacheLoader<O> getLoader() {
    return mPlaceholder.getLoader();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getParts()
   */
  @Override
  public @NonNull KeySPI<Object>[] getParts() {
    return mParts;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#getPreviousKey()
   */
  @Override
  public @Nullable KeySPI<Object> getPreviousKey() {
    return null;
  }

  /**
   * @see com.diamondq.cachly.Key#getPreviousKey(com.diamondq.cachly.Key)
   */
  @Override
  public <P> @Nullable Key<P> getPreviousKey(Key<P> pTemplate) {
    return null;
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#supportsNull()
   */
  @Override
  public boolean supportsNull() {
    return mPlaceholder.supportsNull();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#hasKeyDetails()
   */
  @Override
  public boolean hasKeyDetails() {
    return mPlaceholder.hasKeyDetails();
  }

  /**
   * @see com.diamondq.cachly.spi.KeySPI#storeKeyDetails(com.diamondq.cachly.impl.KeyDetails)
   */
  @Override
  public void storeKeyDetails(KeyDetails<O> pDetails) {
    mPlaceholder.storeKeyDetails(pDetails);
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return mKey;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(mKey, mPlaceholder);
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(@Nullable Object pObj) {
    if (pObj == null)
      return false;
    if (pObj == this)
      return true;
    if (pObj.getClass().equals(CompositeKey.class) == false)
      return false;
    @SuppressWarnings("unchecked")
    ResolvedKeyPlaceholder<O> other = (ResolvedKeyPlaceholder<O>) pObj;
    if (Objects.equals(mKey, other.mKey) && Objects.equals(mPlaceholder, other.mPlaceholder))
      return true;
    return false;
  }
}