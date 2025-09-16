package com.diamondq.cachly.spi;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Defines an SPI for code that can convert part of an AccessContext into a key
 *
 * @param <A> the AccessContext part class type
 */
public interface AccessContextSPI<A> {

  /**
   * Returns the class of the part of the AccessContext that this SPI supports
   *
   * @return the class
   */
  Class<A> getAccessContextClass();

  /**
   * Attempts to convert the given part of the AccessContext into the requested key
   *
   * @param pValue the part
   * @param pAccessKey the requested key
   * @return the resolved key or empty if this can't resolve
   */
  Optional<String> convertValue(@Nullable A pValue, String pAccessKey);
}
