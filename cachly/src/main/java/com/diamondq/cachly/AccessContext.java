package com.diamondq.cachly;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An AccessContext provides additional information to refine queries and lookups, such as authentication
 */
public interface AccessContext {

  /**
   * Returns a piece of data from the AccessContext
   *
   * @param <X> the data type
   * @param pClass the data type class
   * @return the data or null
   */
  public <X> @Nullable X get(Class<X> pClass);
}
