package com.diamondq.cachly.micronaut.ehcache;

import com.diamondq.cachly.micronaut.CachlySyncCache;
import io.micronaut.cache.ehcache.EhcacheSyncCache;
import io.micronaut.cache.ehcache.configuration.EhcacheConfiguration;
import io.micronaut.core.convert.ConversionService;
import org.ehcache.Cache;
import org.ehcache.core.spi.service.StatisticsService;

import java.util.concurrent.ExecutorService;

public class CachlyEhcacheSyncCache extends EhcacheSyncCache implements CachlySyncCache {

  private final boolean mPerformSerialization;

  public CachlyEhcacheSyncCache(ConversionService pConversionService, EhcacheConfiguration pConfiguration,
    Cache<?, ?> pNativeCache, ExecutorService pExecutorService, StatisticsService pStatisticsService,
    boolean pPerformSerialization) {
    super(pConversionService, pConfiguration, pNativeCache, pExecutorService, pStatisticsService);
    mPerformSerialization = pPerformSerialization;
  }

  @Override
  public boolean getPerformSerialization() {
    return mPerformSerialization;
  }
}
