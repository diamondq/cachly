package com.diamondq.cachly.spi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * As there may be many ways to assign a name to a bean (@Named is only one) this provides a method to look it up
 */
public interface BeanNameLocator {

  <T> @Nullable String getBeanName(@NotNull T pBean);
}
