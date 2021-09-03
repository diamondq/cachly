package com.diamondq.cachly.engine;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.AbstractNonSerializingCacheStorage.NonSerializedKeyValuePair;
import com.diamondq.cachly.spi.KeySPI;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This base class provides functionality for storing the richer data set that Cachely has
 *
 * @param <CACHE> the underlying cache type
 * @param <SER_VALUE>
 */
public abstract class AbstractNonSerializingCacheStorage<@NonNull CACHE, SER_VALUE>
  extends AbstractCommonCacheStorage<CACHE, String, SER_VALUE, NonSerializedKeyValuePair<CACHE, SER_VALUE>> {

  public static class NonSerializedKeyValuePair<PAIR_CACHE, PAIR_SER_VALUE>
    extends CommonKeyValuePair<PAIR_CACHE, String, PAIR_SER_VALUE> {

    public NonSerializedKeyValuePair(PAIR_CACHE pCache, String pSerKey, Key<?> pKey, @Nullable PAIR_SER_VALUE pSerValue,
      @Nullable Duration pExpiresIn) {
      super(pCache, pSerKey, pKey, pSerValue, pExpiresIn);
    }

  }

  public AbstractNonSerializingCacheStorage(CACHE pPrimaryCache, @Nullable String pValuePrefix) {
    super(pPrimaryCache, null, pValuePrefix, null, null);
  }

  /**
   * Serialize the value into the data to store
   *
   * @param pKey the key
   * @param pValue the value
   * @return the serialized value
   */
  protected abstract SER_VALUE serializeValue(Key<?> pKey, @Nullable Object pValue);

  /**
   * Deserialize the value back into Key and CacheResult
   *
   * @param pValue the value
   * @return the entry
   */
  protected abstract Map.Entry<Key<?>, CacheResult<?>> deserializeValue(SER_VALUE pValue);

  /**
   * @see com.diamondq.cachly.engine.AbstractCommonCacheStorage#serializeEntry(com.diamondq.cachly.spi.KeySPI,
   *      com.diamondq.cachly.CacheResult)
   */
  @Override
  protected <V> List<NonSerializedKeyValuePair<CACHE, SER_VALUE>> serializeEntry(KeySPI<V> pKey,
    CacheResult<V> pResult) {

    Duration overrideExpiry = pResult.getOverrideExpiry();

    SER_VALUE serValue = serializeValue(pKey, pResult.getValue());

    return Collections.singletonList(
      new NonSerializedKeyValuePair<CACHE, SER_VALUE>(mPrimaryCache, pKey.toString(), pKey, serValue, overrideExpiry));
  }

  /**
   * Convert serialized back to a Key, CacheResult
   *
   * @param pKey the serialized key
   * @param pValue the serialized value
   * @return the Entry
   */
  @Override
  protected Map.Entry<Key<?>, CacheResult<?>> deserializeEntry(String pKey, SER_VALUE pValue) {
    return deserializeValue(pValue);
  }

}
