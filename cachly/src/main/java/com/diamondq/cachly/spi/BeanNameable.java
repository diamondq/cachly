package com.diamondq.cachly.spi;

/**
 * Defines an interface that can return a bean name
 */
@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface BeanNameable {
  /**
   * Returns the bean name
   *
   * @return the bean name
   */
  String getBeanName();
}
