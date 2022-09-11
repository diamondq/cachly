package com.diamondq.cachly;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Type;

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
  <P> @Nullable Key<P> getPreviousKey(Key<P> pTemplate);

  /**
   * Returns the string partial of the last piece of the key. This is usually the piece of data that CacheLoader's need
   * to retrieve their data.
   *
   * @return the key data
   */
  String getKey();

  /**
   * Returns the base keys of all the parts
   *
   * @return the full base key
   */
  String getFullBaseKey();

  /**
   * Returns the Type of the output type
   *
   * @return the output Type
   */
  Type getOutputType();
}
