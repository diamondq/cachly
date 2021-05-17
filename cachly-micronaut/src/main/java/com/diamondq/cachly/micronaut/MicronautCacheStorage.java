package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.impl.KeyInternal;

import java.util.List;
import java.util.Optional;

import io.micronaut.cache.CacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.type.DefaultArgument;

public class MicronautCacheStorage implements CacheStorage {

  private final CachlyMicronautConfiguration mConfig;

  private final SyncCache<?>                 mCache;

  public MicronautCacheStorage(CachlyMicronautConfiguration pConfig, CacheManager<?> pCacheManager) {
    mConfig = pConfig;
    mCache = pCacheManager.getCache(pConfig.getName());
  }

  @Override
  public <V> CacheResult<V> queryForKey(KeyInternal<?, V> pKey) {
    Optional<V> opt = mCache.get(pKey.toString(),
      new DefaultArgument<V>(pKey.getOutputType().getType(), null, AnnotationMetadata.EMPTY_METADATA));
    if (opt.isPresent() == false)
      return CacheResult.notFound();
    V result = opt.get();
    return new CacheResult<V>(result, true);
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#store(com.diamondq.cachly.impl.KeyInternal,
   *      com.diamondq.cachly.CacheResult)
   */
  @Override
  public <V> void store(KeyInternal<?, V> pKey, CacheResult<V> pLoadedResult) {
    mCache.put(pKey.toString(), pLoadedResult.getValue());
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidate(com.diamondq.cachly.impl.KeyInternal)
   */
  @Override
  public <V> void invalidate(KeyInternal<?, V> pKey) {
    mCache.invalidate(pKey.toString());
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#getBasePaths()
   */
  @Override
  public List<String> getBasePaths() {
    return mConfig.getPaths();
  }

}
