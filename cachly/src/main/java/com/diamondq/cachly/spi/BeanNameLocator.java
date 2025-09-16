package com.diamondq.cachly.spi;

import org.jspecify.annotations.Nullable;

/**
 * As there may be many ways to assign a name to a bean (@Named is only one), this provides a method to look it up
 */
public interface BeanNameLocator {

  /**
   * Returns the name of the given bean
   *
   * @param pBean the bean
   * @param <T> the type of the bean
   * @return the name of the bean or null if there is no name.
   */
  <T> @Nullable String getBeanName(T pBean);
}
