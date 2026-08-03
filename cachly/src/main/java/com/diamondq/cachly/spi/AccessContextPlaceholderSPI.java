package com.diamondq.cachly.spi;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.AccessContextPlaceholder;
import com.diamondq.cachly.Cache;
import org.jspecify.annotations.Nullable;

public interface AccessContextPlaceholderSPI<O extends @Nullable Object> extends AccessContextPlaceholder<O> {

  KeySPI<O> resolve(Cache pCache, AccessContext pAccessContext);

}
