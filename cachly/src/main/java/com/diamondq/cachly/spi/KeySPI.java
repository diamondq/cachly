package com.diamondq.cachly.spi;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.impl.KeyDetails;
import org.jspecify.annotations.Nullable;

/**
 * General definition of all key types
 *
 * @param <O> the key type
 */
public interface KeySPI<O extends @Nullable Object> extends Key<O> {

  @Nullable KeySPI<@Nullable Object> getPreviousKey();

  /**
   * Returns the storage used by the last part of the key
   *
   * @return the storage
   */
  CacheStorage getLastStorage();

  /**
   * Returns the serializer name used by the last part of the key
   *
   * @return the name
   */
  String getLastSerializerName();

  /**
   * Returns true if this key supports null values
   *
   * @return true or false
   */
  boolean supportsNull();

  /**
   * Returns the Cache Loader used for this key
   *
   * @return the cache loader
   */
  CacheLoader<O> getLoader();

  /**
   * Associate the key details with this key
   *
   * @param pDetails the key details
   */
  void storeKeyDetails(KeyDetails<O> pDetails);

  /**
   * Returns whether key details have been associated with this key
   *
   * @return true or false
   */
  boolean hasKeyDetails();

  /**
   * Returns the parts that this key is made up from
   *
   * @return the array of parts
   */
  KeySPI<@Nullable Object>[] getParts();

  /**
   * Returns the base key
   *
   * @return the base key
   */
  String getBaseKey();

  /**
   * Returns true if there are any unresolved placeholders in the composite key
   *
   * @return true if there are placeholders or false if there are not
   */
  boolean hasPlaceholders();

}
