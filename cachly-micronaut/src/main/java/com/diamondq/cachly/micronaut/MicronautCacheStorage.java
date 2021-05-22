package com.diamondq.cachly.micronaut;

import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.engine.CacheStorage;
import com.diamondq.cachly.spi.KeySPI;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.DefaultArgument;

@EachBean(SyncCache.class)
public class MicronautCacheStorage implements CacheStorage {

  private final SyncCache<?>           mCache;

  private final List<KeyExtractor>     mKeyExtractors;

  private final List<ExpiryHandler>    mExpiryHandlers;

  private final Argument<ValueName<?>> mValueNameArg;

  @Inject
  public MicronautCacheStorage(SyncCache<?> pCache, List<KeyExtractor> pKeyExtractors,
    List<ExpiryHandler> pExpiryHandlers) {
    mCache = pCache;
    mKeyExtractors = pKeyExtractors;
    mExpiryHandlers = pExpiryHandlers;
    mValueNameArg = new DefaultArgument<>(ValueName.class, null, AnnotationMetadata.EMPTY_METADATA);
  }

  @Override
  public <V> CacheResult<V> queryForKey(KeySPI<V> pKey) {
    Optional<ValueName<?>> opt = mCache.get(pKey.toString(), mValueNameArg);
    if (opt.isPresent() == false)
      return CacheResult.notFound();
    ValueName<?> valueName = opt.get();
    @SuppressWarnings("unchecked")
    V result = (V) valueName.value;
    return new CacheResult<V>(result, true);
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#store(com.diamondq.cachly.spi.KeySPI, com.diamondq.cachly.CacheResult)
   */
  @Override
  public <V> void store(KeySPI<V> pKey, CacheResult<V> pLoadedResult) {
    Duration overrideExpiry = pLoadedResult.getOverrideExpiry();
    String key = pKey.toString();
    if (overrideExpiry != null)
      for (ExpiryHandler eh : mExpiryHandlers)
        eh.markForExpiry(key, overrideExpiry);
    mCache.put(key, new ValueName<>(pLoadedResult.getValue(), pKey.getLastSerializerName()));
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidate(com.diamondq.cachly.spi.KeySPI)
   */
  @Override
  public <V> void invalidate(KeySPI<V> pKey) {
    String key = pKey.toString();
    for (ExpiryHandler eh : mExpiryHandlers)
      eh.invalidate(key);
    mCache.invalidate(key);
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#invalidateAll()
   */
  @Override
  public void invalidateAll() {
    for (ExpiryHandler eh : mExpiryHandlers)
      eh.invalidateAll();
    mCache.invalidateAll();
  }

  /**
   * @see com.diamondq.cachly.engine.CacheStorage#streamKeys()
   */
  @Override
  public Stream<String> streamKeys() {
    Object nativeCache = mCache.getNativeCache();
    for (KeyExtractor ke : mKeyExtractors) {
      Stream<String> keys = ke.getKeys(nativeCache);
      if (keys != null)
        return keys;
    }
    throw new IllegalStateException(
      "The cache " + nativeCache.getClass().getName() + " is not able to be key iterated");
  }

}
