package com.diamondq.cachly.spi;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.KeyPlaceholder;

public interface KeyPlaceholderSPI<O> extends KeyPlaceholder<O> {

  public KeySPI<O> resolveDefault(Cache pCache, AccessContext pAccessContext);
}
