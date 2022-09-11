package com.diamondq.cachly.impl;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.spi.KeySPI;

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

  @Override
  public CacheStorage getLastStorage() {
    return mPlaceholder.getLastStorage();
  }

  @Override
  public String getLastSerializerName() {
    return mPlaceholder.getLastSerializerName();
  }

  @Override
  public CacheLoader<O> getLoader() {
    return mPlaceholder.getLoader();
  }

  @Override
  public @NonNull KeySPI<Object>[] getParts() {
    return mParts;
  }

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

  @Override
  public boolean supportsNull() {
    return mPlaceholder.supportsNull();
  }

  @Override
  public boolean hasKeyDetails() {
    return mPlaceholder.hasKeyDetails();
  }

  @Override
  public void storeKeyDetails(KeyDetails<O> pDetails) {
    mPlaceholder.storeKeyDetails(pDetails);
  }

  @Override
  public String toString() {
    return mKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mKey, mPlaceholder);
  }

  public KeySPI<O> getPlaceholder() {
    return mPlaceholder;
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
    ResolvedKeyPlaceholder<O> other = (ResolvedKeyPlaceholder<O>) pObj;
    return Objects.equals(mKey, other.mKey) && Objects.equals(mPlaceholder, other.mPlaceholder);
  }
}