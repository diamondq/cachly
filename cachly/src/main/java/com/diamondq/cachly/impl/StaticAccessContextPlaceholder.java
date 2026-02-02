package com.diamondq.cachly.impl;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.spi.AccessContextPlaceholderSPI;
import com.diamondq.cachly.spi.AccessContextSPI;
import com.diamondq.cachly.spi.KeySPI;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StaticAccessContextPlaceholder<O extends @Nullable Object> extends AbstractKey<O>
  implements AccessContextPlaceholderSPI<O> {

  private final String mAccessKey;

  private volatile @Nullable Map<Class<?>, List<AccessContextSPI<Object>>> mAccessContextSPI;

  public StaticAccessContextPlaceholder(String pKey, Type pType) {
    super("{ac:" + pKey + "}", pType, true);
    mAccessKey = pKey;
  }

  @Override
  public KeySPI<O> resolve(Cache pCache, AccessContext pAccessContext) {
    Map<Class<?>, List<AccessContextSPI<Object>>> localSPIMap = mAccessContextSPI;
    if (localSPIMap == null) throw new IllegalStateException("There is no Access Context SPI");
    final Map<Class<?>, Object> data = pAccessContext.getData();
    for (Map.Entry<Class<?>, Object> pair : data.entrySet()) {
      final Class<?> key = pair.getKey();
      List<AccessContextSPI<Object>> accessContextSPIS = localSPIMap.get(key);
      if (accessContextSPIS == null) continue;
      for (AccessContextSPI<Object> ac : accessContextSPIS) {
        Optional<String> optValue = ac.convertValue(pair.getValue(), mAccessKey);
        if (optValue.isPresent()) return new ResolvedAccessContextPlaceholder<>(this, optValue.get());
      }
    }
    throw new IllegalStateException("Unable to find an Access Context entry that supports " + mAccessKey);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void setAccessContextSPI(Map<Class<?>, List<AccessContextSPI<?>>> pAcs) {
    mAccessContextSPI = (Map) pAcs;
  }

  @Override
  public void clearKeyDetails() {
    super.clearKeyDetails();
    mAccessContextSPI = null;
  }
}