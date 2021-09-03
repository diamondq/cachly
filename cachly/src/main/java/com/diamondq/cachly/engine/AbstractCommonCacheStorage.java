package com.diamondq.cachly.engine;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.spi.KeySPI;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractCommonCacheStorage<CACHE, SER_KEY, SER_VALUE, SER_KEY_VALUE_PAIR extends CommonKeyValuePair<CACHE, SER_KEY, SER_VALUE>>
  implements CacheStorage {

  /**
   * The primary cache
   */
  protected final CACHE                               mPrimaryCache;

  /**
   * The meta cache or null if there isn't one.
   */
  protected final @Nullable CACHE                     mMetaCache;

  /**
   * The prefix to put on all value keys
   */
  protected final @Nullable String                    mValuePrefix;

  protected final int                                 mValuePrefixLen;

  protected final @Nullable Function<String, SER_KEY> mKeySerializer;

  protected final @Nullable Function<SER_KEY, String> mKeyDeserializer;

  public AbstractCommonCacheStorage(CACHE pPrimaryCache, @Nullable CACHE pMetaCache, @Nullable String pValuePrefix,
    @Nullable Function<String, SER_KEY> pKeySerializer, @Nullable Function<SER_KEY, String> pKeyDeserializer) {
    mPrimaryCache = pPrimaryCache;
    mMetaCache = pMetaCache;
    mValuePrefix = pValuePrefix;
    mValuePrefixLen = pValuePrefix != null ? pValuePrefix.length() : 0;
    mKeySerializer = pKeySerializer;
    mKeyDeserializer = pKeyDeserializer;
  }

  /**
   * Writes to the cache as defined in the entry
   *
   * @param pEntry the entry of data to write
   */
  protected abstract void writeToCache(SER_KEY_VALUE_PAIR pEntry);

  /**
   * Reads from the primary cache
   *
   * @param pKey the key
   * @return the optional value
   */
  protected abstract Optional<SER_VALUE> readFromPrimaryCache(SER_KEY pKey);

  /**
   * Invalidate entries
   *
   * @param pCache the cache
   * @param pKey if provided, invalidate just this key, if null, then invalidate all keys
   */
  protected abstract void invalidate(CACHE pCache, @Nullable SER_KEY pKey);

  /**
   * Convert the given key/value into a list of CommonKeyValuePair's
   *
   * @param <V> the value type
   * @param pKey the key
   * @param pResult the cached value
   * @return the stream
   */
  protected abstract <V> List<SER_KEY_VALUE_PAIR> serializeEntry(KeySPI<V> pKey, CacheResult<V> pResult);

  /**
   * Returns back a stream of entries from the primary. NOTE: If meta is not a separate cache, then it is expected that
   * meta data may be present, so it's OK to return all keys
   *
   * @return the entries
   */
  protected abstract Stream<Map.Entry<SER_KEY, SER_VALUE>> streamPrimary();

  /**
   * Returns back a stream of entries from the meta. NOTE: If meta is not a separate cache, then it is expected that
   * regular data will be present, so it's OK to return all keys.
   *
   * @return the entries
   */
  protected abstract Stream<Map.Entry<SER_KEY, SER_VALUE>> streamMetaEntries();

  /**
   * Deserializes a SER_KEY and SER_VALUE into a Key<?> and CacheResult<?>
   *
   * @param pKey the key
   * @param pValue the value
   * @return the entry
   */
  protected abstract Map.Entry<Key<?>, CacheResult<?>> deserializeEntry(SER_KEY pKey, SER_VALUE pValue);

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#store(com.diamondq.cachly.AccessContext,
   *      com.diamondq.cachly.spi.KeySPI, com.diamondq.cachly.CacheResult)
   */
  @Override
  public <V> void store(AccessContext pAccessContext, KeySPI<V> pKey, CacheResult<V> pLoadedResult) {

    /* Convert the data into the things to actually write and write them to the cache */

    for (SER_KEY_VALUE_PAIR kvpair : serializeEntry(pKey, pLoadedResult))
      writeToCache(kvpair);
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidate(com.diamondq.cachly.AccessContext,
   *      com.diamondq.cachly.spi.KeySPI)
   */
  @Override
  public <V> void invalidate(AccessContext pAccessContext, KeySPI<V> pKey) {

    /* Get the key string */

    String keyStr = (mValuePrefix != null ? mValuePrefix + pKey.toString() : pKey.toString());

    /* Get the 'serialized version of the key */

    @SuppressWarnings("unchecked")
    SER_KEY serKey = (mKeySerializer != null ? mKeySerializer.apply(keyStr) : (SER_KEY) keyStr);

    invalidate(mPrimaryCache, serKey);
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidateAll(com.diamondq.cachly.AccessContext)
   */
  @Override
  public void invalidateAll(AccessContext pAccessContext) {
    invalidate(mPrimaryCache, null);

    /* Since we have removed everything, we can also remove all the metadata */

    CACHE metaCache = mMetaCache;
    if (metaCache != null)
      invalidate(metaCache, null);
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#streamEntries(com.diamondq.cachly.AccessContext)
   */
  @Override
  public Stream<Map.Entry<Key<?>, CacheResult<?>>> streamEntries(AccessContext pAccessContext) {

    /* Get the set of data */

    Stream<Map.Entry<SER_KEY, SER_VALUE>> rawStream = streamPrimary();

    /* If there is no separate meta cache, then the meta data may be present */

    if ((mMetaCache == null) && (mValuePrefix != null)) {
      if (mKeyDeserializer != null)
        rawStream = rawStream.filter((entry) -> mKeyDeserializer.apply(entry.getKey()).startsWith(mValuePrefix));
      else
        rawStream = rawStream.filter((entry) -> ((String) entry.getKey()).startsWith(mValuePrefix));
    }

    /* Convert the result back */

    return rawStream.map((entry) -> deserializeEntry(entry.getKey(), entry.getValue()));
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#queryForKey(com.diamondq.cachly.AccessContext,
   *      com.diamondq.cachly.spi.KeySPI)
   */
  @Override
  public <V> CacheResult<V> queryForKey(AccessContext pAccessContext, KeySPI<V> pKey) {

    /* Get the key string */

    String keyStr = (mValuePrefix != null ? mValuePrefix + pKey.toString() : pKey.toString());

    /* Get the 'serialized version of the key */

    @SuppressWarnings("unchecked")
    SER_KEY serKey = (mKeySerializer != null ? mKeySerializer.apply(keyStr) : (SER_KEY) keyStr);

    /* Query the underlying primary cache */

    Optional<SER_VALUE> valueOpt = readFromPrimaryCache(serKey);

    /* If it's not found, then we're done */

    if (valueOpt.isPresent() == false)
      return CacheResult.notFound();

    /* Deserialize the entry */

    Map.Entry<Key<?>, CacheResult<?>> result = deserializeEntry(serKey, valueOpt.get());

    /* Return the CacheResult */

    @SuppressWarnings("unchecked")
    CacheResult<V> cv = (CacheResult<V>) result.getValue();
    return cv;
  }
}
