package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.spi.BeanNameLocator;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.inject.BeanIdentifier;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("ClassNamePrefixedWithPackageName")
@Singleton
public class MicronautBeanNameLocator implements BeanNameLocator {

  private final ApplicationContext mAppContext;

  @Inject
  public MicronautBeanNameLocator(ApplicationContext pAppContext) {
    mAppContext = pAppContext;
  }

  @Override
  public <T> @Nullable String getBeanName(@NotNull T pBean) {
    Optional<BeanRegistration<T>> regOpt = mAppContext.findBeanRegistration(pBean);
    if (!regOpt.isPresent()) return null;
    BeanRegistration<T> beanReg = regOpt.get();
    BeanIdentifier identifier = beanReg.getIdentifier();
    return identifier.getName();
  }
}
