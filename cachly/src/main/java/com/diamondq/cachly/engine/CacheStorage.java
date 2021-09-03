package com.diamondq.cachly.engine;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.spi.KeySPI;

import java.util.Map;
import java.util.stream.Stream;

public interface CacheStorage {

  public <V> CacheResult<V> queryForKey(AccessContext pAccessContext, KeySPI<V> pKey);

  public <V> void store(AccessContext pAccessContext, KeySPI<V> pKey, CacheResult<V> pLoadedResult);

  public <V> void invalidate(AccessContext pAccessContext, KeySPI<V> pKey);

  public Stream<Map.Entry<Key<?>, CacheResult<?>>> streamEntries(AccessContext pAccessContext);

  public void invalidateAll(AccessContext pAccessContext);

}
