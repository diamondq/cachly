package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.spi.BeanNameLocator;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.inject.BeanIdentifier;

@Singleton
@javax.inject.Singleton
public class MicronautBeanNameLocator implements BeanNameLocator {

  private final ApplicationContext mAppContext;

  @Inject
  @javax.inject.Inject
  public MicronautBeanNameLocator(ApplicationContext pAppContext) {
    mAppContext = pAppContext;
  }

  @Override
  public <T> @Nullable String getBeanName(@NonNull T pBean) {
    Optional<BeanRegistration<T>> regOpt = mAppContext.findBeanRegistration(pBean);
    if (!regOpt.isPresent())
      return null;
    BeanRegistration<T> beanReg = regOpt.get();
    BeanIdentifier identifier = beanReg.getIdentifier();
    String name = identifier.getName();
    return name;
  }
}
