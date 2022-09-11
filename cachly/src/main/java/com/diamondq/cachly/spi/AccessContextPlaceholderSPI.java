package com.diamondq.cachly.spi;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.AccessContextPlaceholder;
import com.diamondq.cachly.Cache;

public interface AccessContextPlaceholderSPI<O> extends AccessContextPlaceholder<O> {

  KeySPI<O> resolve(Cache pCache, AccessContext pAccessContext);

}
