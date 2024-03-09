package com.diamondq.cachly.spi;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.KeyPlaceholder;

/**
 * An SPI that represents a placeholder
 *
 * @param <O>
 */
public interface KeyPlaceholderSPI<O> extends KeyPlaceholder<O> {

  /**
   * Resolves the placeholder to a key
   *
   * @param pCache the cache engine
   * @param pAccessContext the access context
   * @return the resolved key
   */
  KeySPI<O> resolveDefault(Cache pCache, AccessContext pAccessContext);
}
