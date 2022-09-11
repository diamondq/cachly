package com.diamondq.cachly.engine;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.spi.KeySPI;

import java.util.Map;
import java.util.stream.Stream;

public interface CacheStorage {

  <V> CacheResult<V> queryForKey(AccessContext pAccessContext, KeySPI<V> pKey);

  <V> void store(AccessContext pAccessContext, KeySPI<V> pKey, CacheResult<V> pLoadedResult);

  <V> void invalidate(AccessContext pAccessContext, KeySPI<V> pKey);

  Stream<Map.Entry<Key<?>, CacheResult<?>>> streamEntries(AccessContext pAccessContext);

  void invalidateAll(AccessContext pAccessContext);

}
