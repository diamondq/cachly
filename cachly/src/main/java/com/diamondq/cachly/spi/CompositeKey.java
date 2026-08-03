package com.diamondq.cachly.spi;

import com.diamondq.cachly.AccessContextPlaceholder;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyPlaceholder;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A composite key is made up of many smaller key parts
 *
 * @param <O> the data type
 */
public class CompositeKey<O extends @Nullable Object> implements KeySPI<O> {

  private final KeySPI<? extends @Nullable Object>[] mParts;

  private final KeySPI<O> mLast;

  private final int mPartsLen;

  private final boolean mHasPlaceholders;

  /**
   * A constructor that takes a string (that may have / separator characters)
   *
   * @param pKey the key string
   * @param pType the type
   */
  @SuppressWarnings("unchecked")
  public CompositeKey(String pKey, Type pType) {
    this(Arrays.stream(pKey.split("/")).map((partStr) -> new StaticKey<>(partStr, pType)).toArray(KeySPI[]::new));
  }

  /**
   * Constructor that takes 2 keys
   *
   * @param pKey1 the first key
   * @param pKey2 the second key
   */
  public CompositeKey(Key<? extends @Nullable Object> pKey1, Key<O> pKey2) {
    if (!(pKey1 instanceof KeySPI<? extends @Nullable Object> ki1))
      throw new IllegalStateException("Unsupported key type: " + pKey1.getClass().getName());
    KeySPI<? extends @Nullable Object>[] ki1Parts = ki1.getParts();
    if (!(pKey2 instanceof KeySPI<O> ki2))
      throw new IllegalStateException("Unsupported key type: " + pKey2.getClass().getName());
    KeySPI<? extends @Nullable Object>[] ki2Parts = ki2.getParts();
    @SuppressWarnings(
      "unchecked") KeySPI<? extends @Nullable Object>[] tempParts = Stream.concat(Arrays.stream(ki1Parts),
      Arrays.stream(ki2Parts)
    ).toArray(KeySPI[]::new);
    mParts = tempParts;
    mPartsLen = mParts.length;
    mLast = ki2;
    boolean hasPlaceHolders = false;
    for (KeySPI<? extends @Nullable Object> mPart : mParts) {
      if (mPart instanceof KeyPlaceholder) hasPlaceHolders = true;
      else if (mPart instanceof AccessContextPlaceholder) hasPlaceHolders = true;
    }
    mHasPlaceholders = hasPlaceHolders;
  }

  /**
   * Constructor that takes 3 keys
   *
   * @param pKey1 the first key
   * @param pKey2 the second key
   * @param pKey3 the third key
   */
  public CompositeKey(Key<? extends @Nullable Object> pKey1, Key<? extends @Nullable Object> pKey2, Key<O> pKey3) {
    if (!(pKey1 instanceof KeySPI<? extends @Nullable Object> ki1))
      throw new IllegalStateException("Unsupported key type: " + pKey1.getClass().getName());
    KeySPI<? extends @Nullable Object>[] ki1Parts = ki1.getParts();
    if (!(pKey2 instanceof KeySPI<? extends @Nullable Object> ki2))
      throw new IllegalStateException("Unsupported key type: " + pKey2.getClass().getName());
    KeySPI<? extends @Nullable Object>[] ki2Parts = ki2.getParts();
    if (!(pKey3 instanceof KeySPI<O> ki3))
      throw new IllegalStateException("Unsupported key type: " + pKey3.getClass().getName());
    KeySPI<? extends @Nullable Object>[] ki3Parts = ki3.getParts();
    @SuppressWarnings(
      "unchecked") KeySPI<? extends @Nullable Object>[] tempParts = Stream.concat(Stream.concat(Arrays.stream(ki1Parts),
        Arrays.stream(ki2Parts)
      ), Arrays.stream(ki3Parts)
    ).toArray(KeySPI[]::new);
    mParts = tempParts;
    mPartsLen = mParts.length;
    mLast = ki3;
    boolean hasPlaceHolders = false;
    for (KeySPI<? extends @Nullable Object> mPart : mParts) {
      if (mPart instanceof KeyPlaceholder) hasPlaceHolders = true;
      else if (mPart instanceof AccessContextPlaceholder) hasPlaceHolders = true;
    }
    mHasPlaceholders = hasPlaceHolders;
  }

  /**
   * Constructor that takes an array of keys
   *
   * @param pNewParts the array
   */
  public CompositeKey(KeySPI<? extends @Nullable Object>[] pNewParts) {
    mParts = pNewParts;
    mPartsLen = mParts.length;
    if (mPartsLen == 0) throw new IllegalStateException("CompositeKey cannot be created with no parts");
    @SuppressWarnings("unchecked") KeySPI<O> temp = (KeySPI<O>) mParts[mPartsLen - 1];
    mLast = temp;
    boolean hasPlaceHolders = false;
    for (KeySPI<? extends @Nullable Object> mPart : mParts) {
      if (mPart instanceof KeyPlaceholder) hasPlaceHolders = true;
      else if (mPart instanceof AccessContextPlaceholder) hasPlaceHolders = true;
    }
    mHasPlaceholders = hasPlaceHolders;
  }

  @Override
  public void clearKeyDetails() {
    if (!mLast.equals(this)) mLast.clearKeyDetails();
    for (KeySPI<? extends @Nullable Object> part : mParts) if (!part.equals(this)) part.clearKeyDetails();
  }

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

  @Override
  public KeySPI<? extends @Nullable Object>[] getParts() {
    return mParts;
  }

  @Override
  public String getKey() {
    return mLast.getKey();
  }

  @Override
  public String getBaseKey() {
    throw new IllegalStateException("Base key is not available in a CompositeKey");
  }

  @Override
  public String getFullBaseKey() {
    StringBuilder sb = new StringBuilder();
    for (KeySPI<?> part : mParts) {
      sb.append(part.getBaseKey());
      //noinspection HardcodedFileSeparator
      sb.append("/");
    }
    var len = sb.length();
    if (len == 0) throw new IllegalStateException("CompositeKey cannot be created with no parts");
    sb.setLength(len - 1);
    return sb.toString();
  }

  @Override
  public CacheStorage getLastStorage() {
    return mLast.getLastStorage();
  }

  @Override
  public String getLastSerializerName() {
    return mLast.getLastSerializerName();
  }

  @Override
  public CacheLoader<O> getLoader() {
    return mLast.getLoader();
  }

  @Override
  public @Nullable KeySPI<@Nullable Object> getPreviousKey() {
    if (mPartsLen == 1) return null;
    if (mPartsLen <= 0) throw new IllegalStateException("CompositeKey cannot be created with no parts");
    @SuppressWarnings("nullness") KeySPI<? extends @Nullable Object>[] parentParts = Arrays.copyOfRange(mParts,
      0,
      mParts.length - 1
    );
    return new CompositeKey<>(parentParts);
  }

  /**
   * @see com.diamondq.cachly.Key#getPreviousKey(com.diamondq.cachly.Key)
   */
  @Override
  public <P extends @Nullable Object> @Nullable Key<P> getPreviousKey(Key<P> pTemplate) {
    @SuppressWarnings("unchecked") KeySPI<@Nullable Object> testKey = (KeySPI<@Nullable Object>) this;
    String testKeyStr = pTemplate.toString();
    while (testKey != null) {
      if (testKey.getFullBaseKey().equals(testKeyStr)) {
        @SuppressWarnings("unchecked") Key<P> result = (Key<P>) testKey;
        return result;
      }
      testKey = testKey.getPreviousKey();
    }
    return null;
  }

  @Override
  public boolean hasKeyDetails() {
    for (KeySPI<? extends @Nullable Object> part : mParts)
      if (!part.hasKeyDetails()) return false;
    return true;
  }

  @Override
  public void storeKeyDetails(KeyDetails<O> pDetails) {
    throw new IllegalStateException("storeKeyDetails() not supported in a CompositeKey");
  }

  @Override
  public boolean supportsNull() {
    return mLast.supportsNull();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (KeySPI<? extends @Nullable Object> part : mParts) {
      sb.append(part.getKey());
      //noinspection HardcodedFileSeparator
      sb.append("/");
    }
    var len = sb.length();
    if (len == 0) throw new IllegalStateException("CompositeKey cannot be created with no parts");
    sb.setLength(len - 1);
    return sb.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(mParts), mHasPlaceholders);
  }

  @Override
  public boolean equals(@Nullable Object pObj) {
    if (pObj == this) return true;
    if (pObj == null) return false;
    if (pObj.getClass() != CompositeKey.class) return false;
    @SuppressWarnings("unchecked") CompositeKey<O> other = (CompositeKey<O>) pObj;
    return Arrays.equals(mParts, other.mParts) && Objects.equals(mHasPlaceholders, other.mHasPlaceholders);
  }
}