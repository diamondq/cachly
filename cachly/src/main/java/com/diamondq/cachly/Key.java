package com.diamondq.cachly;

/**
 * This interface represents a key within the cache. It may also be a portion of a composite key
 *
 * @param <O> the type of data stored at this key
 */
public interface Key<O> {

  public String getKey();

}
