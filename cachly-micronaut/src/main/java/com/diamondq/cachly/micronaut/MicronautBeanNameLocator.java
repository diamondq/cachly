package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.spi.BeanNameLocator;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.inject.BeanIdentifier;

@Singleton
public class MicronautBeanNameLocator implements BeanNameLocator {

  private final ApplicationContext mAppContext;

  @Inject
  public MicronautBeanNameLocator(ApplicationContext pAppContext) {
    mAppContext = pAppContext;
  }

  /**
   * @see com.diamondq.cachly.spi.BeanNameLocator#getBeanName(java.lang.Object)
   */
  @Override
  public <T> @Nullable String getBeanName(@NonNull T pBean) {
    Optional<BeanRegistration<T>> regOpt = mAppContext.findBeanRegistration(pBean);
    if (regOpt.isPresent() == false)
      return null;
    BeanRegistration<T> beanReg = regOpt.get();
    BeanIdentifier identifier = beanReg.getIdentifier();
    String name = identifier.getName();
    return name;
  }
}
