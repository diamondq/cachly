package com.diamondq.cachly.impl;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.spi.AccessContextPlaceholderSPI;
import com.diamondq.cachly.spi.AccessContextSPI;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;

import org.checkerframework.checker.nullness.qual.Nullable;

public class StaticAccessContextPlaceholder<A, O> extends AbstractKey<O> implements AccessContextPlaceholderSPI<O> {

  private final Class<A>                         mAccessContextValueClass;

  private volatile @Nullable AccessContextSPI<A> mAccessContextSPI;

  public StaticAccessContextPlaceholder(String pKey, Class<A> pAccessContextValueClass, TypeReference<O> pType) {
    super("{ac:" + pKey + "}", pType, true);
    mAccessContextValueClass = pAccessContextValueClass;
  }

  /**
   * @see com.diamondq.cachly.spi.AccessContextPlaceholderSPI#resolve(com.diamondq.cachly.Cache,
   *      com.diamondq.cachly.AccessContext)
   */
  @Override
  public KeySPI<O> resolve(Cache pCache, AccessContext pAccessContext) {
    AccessContextSPI<A> localSPI = mAccessContextSPI;
    if (localSPI == null)
      throw new IllegalStateException("There is no Access Context SPI for " + mAccessContextValueClass.getName());
    A value = pAccessContext.get(mAccessContextValueClass);
    String valueStr = localSPI.convertValue(value);
    return new ResolvedAccessContextPlaceholder<>(this, valueStr);
  }

  /**
   * Returns the access context value class
   *
   * @return the class
   */
  public Class<A> getAccessContextValueClass() {
    return mAccessContextValueClass;
  }

  @SuppressWarnings("unchecked")
  public void setAccessContextSPI(AccessContextSPI<?> pAcs) {
    mAccessContextSPI = (AccessContextSPI<A>) pAcs;
  }

}