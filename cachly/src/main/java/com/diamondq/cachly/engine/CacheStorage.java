package com.diamondq.cachly.engine;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.spi.KeySPI;

import java.util.stream.Stream;

public interface CacheStorage {

  public <V> CacheResult<V> queryForKey(KeySPI<V> pKey);

  public <V> void store(KeySPI<V> pKey, CacheResult<V> pLoadedResult);

  public <V> void invalidate(KeySPI<V> pKey);

  public Stream<String> streamKeys();

  public void invalidateAll();
}
