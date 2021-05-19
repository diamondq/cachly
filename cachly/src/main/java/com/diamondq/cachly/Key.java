package com.diamondq.cachly;

import com.diamondq.common.TypeReference;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This interface represents a key within the cache. It may also be a portion of a composite key
 *
 * @param <O> the type of data stored at this key
 */
public interface Key<O> {

  /**
   * Returns the previous key given a template key
   *
   * @param <P> the previous key type
   * @param pTemplate the template key
   * @return the Key or null if the template key is not present
   */
  public <P> @Nullable Key<P> getPreviousKey(Key<P> pTemplate);

  /**
   * Returns the string partial of the last piece of the key. This is usually the piece of data that CacheLoader's need
   * to retrieve their data.
   *
   * @return the key data
   */
  public String getKey();

  /**
   * Returns the TypeReference of the output type
   *
   * @return the output TypeReference
   */
  public TypeReference<O> getOutputTypeReference();
}
