package com.diamondq.cachly.spi;

import org.jspecify.annotations.Nullable;
import org.osgi.service.component.annotations.Component;

/**
 * A BeanNameLocator that returns the bean name if the object implements BeanNameable
 */
@Component(service = BeanNameLocator.class)
public class BeanNameableLocator implements BeanNameLocator {
  @Override
  public @Nullable <T> String getBeanName(T pBean) {
    if (pBean instanceof BeanNameable bn) return bn.getBeanName();
    return null;
  }
}
