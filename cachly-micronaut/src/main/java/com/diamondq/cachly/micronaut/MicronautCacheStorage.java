package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.spi.KeySPI;

import java.util.Optional;

import javax.inject.Inject;

import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.type.DefaultArgument;

@EachBean(SyncCache.class)
public class MicronautCacheStorage implements CacheStorage {

  private final SyncCache<?> mCache;

  @Inject
  public MicronautCacheStorage(SyncCache<?> pCache) {
    mCache = pCache;
  }

  @Override
  public <V> CacheResult<V> queryForKey(KeySPI<V> pKey) {
    Optional<V> opt = mCache.get(pKey.toString(),
      new DefaultArgument<V>(pKey.getOutputType().getType(), null, AnnotationMetadata.EMPTY_METADATA));
    if (opt.isPresent() == false)
      return CacheResult.notFound();
    V result = opt.get();
    return new CacheResult<V>(result, true);
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#store(com.diamondq.cachly.spi.KeySPI, com.diamondq.cachly.CacheResult)
   */
  @Override
  public <V> void store(KeySPI<V> pKey, CacheResult<V> pLoadedResult) {
    mCache.put(pKey.toString(), pLoadedResult.getValue());
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidate(com.diamondq.cachly.spi.KeySPI)
   */
  @Override
  public <V> void invalidate(KeySPI<V> pKey) {
    mCache.invalidate(pKey.toString());
  }

}
