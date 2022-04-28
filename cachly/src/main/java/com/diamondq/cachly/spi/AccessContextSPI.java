package com.diamondq.cachly.spi;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface AccessContextSPI<A> {

  public Class<A> getAccessContextClass();

  public String convertValue(@Nullable A pValue, String pAccessKey);
}
