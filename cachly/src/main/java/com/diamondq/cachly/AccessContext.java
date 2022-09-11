package com.diamondq.cachly;

import java.util.Map;
import java.util.Optional;

/**
 * An AccessContext provides additional information to refine queries and lookups, such as authentication
 */
public interface AccessContext {

  /**
   * Returns the data from the AccessContext
   *
   * @return the data map
   */
  Map<Class<?>, Object> getData();

  /**
   * Returns a piece of data from the AccessContext
   *
   * @param <X> the data type
   * @param pClass the data type class
   * @return the optional data
   */
  <X> Optional<X> get(Class<X> pClass);

}
