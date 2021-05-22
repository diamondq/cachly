package com.diamondq.cachly.micronaut.ehcache;

import com.diamondq.cachly.micronaut.ValueName;
import com.diamondq.cachly.micronaut.ehcache.CachlyEhcacheConfiguration.CachlyDiskTieredCacheConfiguration;
import com.diamondq.common.Holder;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.inject.Named;
import javax.inject.Singleton;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.ResourcePool;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.ResourceType;
import org.ehcache.config.ResourceUnit;
import org.ehcache.config.SizedResourcePool;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.DefaultStatisticsService;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;
import org.ehcache.expiry.ExpiryPolicy;

import io.micronaut.cache.ehcache.EhcacheCacheFactory;
import io.micronaut.cache.ehcache.EhcacheSyncCache;
import io.micronaut.cache.ehcache.configuration.EhcacheCacheManagerConfiguration;
import io.micronaut.cache.ehcache.configuration.EhcacheConfiguration;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.scheduling.TaskExecutors;

@Factory
@Replaces(factory = EhcacheCacheFactory.class)
public class CachlyEhcacheCacheFactory {

  private final EhcacheCacheManagerConfiguration mConfiguration;

  /**
   * @param configuration the configuration
   */
  public CachlyEhcacheCacheFactory(EhcacheCacheManagerConfiguration configuration) {
    mConfiguration = configuration;
  }

  /**
   * @param statisticsService the Ehcache statistics service
   * @return The {@link CacheManager}
   */
  @Singleton
  @Bean(preDestroy = "close")
  CacheManager cacheManager(StatisticsService statisticsService) {
    CacheManagerBuilder<?> builder = mConfiguration.getBuilder();
    return builder.using(statisticsService).build(true);
  }

  /**
   * @return the Ehcache statistics service
   */
  @Singleton
  @Bean(preDestroy = "stop")
  StatisticsService statisticsService() {
    return new DefaultStatisticsService();
  }

  /**
   * Creates a cache instance based on configuration.
   *
   * @param pConfiguration The configuration
   * @param cacheManager The cache manager
   * @param conversionService The conversion service
   * @param executorService The executor
   * @param statisticsService The statistics service
   * @return The sync cache
   */
  @EachBean(EhcacheConfiguration.class)
  EhcacheSyncCache syncCache(@Parameter EhcacheConfiguration configuration, CacheManager cacheManager,
    ConversionService<?> conversionService, @Named(TaskExecutors.IO) ExecutorService executorService,
    StatisticsService statisticsService, CachlyEhcacheSerializer pSerializer, ApplicationContext pApplicationContext) {
    Optional<CachlyEhcacheConfiguration> cachlyConfigOpt =
      pApplicationContext.findBean(CachlyEhcacheConfiguration.class, Qualifiers.byName(configuration.getName()));
    @SuppressWarnings({"unchecked", "rawtypes"})
    Optional<ExpiryPolicy<Object, Object>> expiryPolicyOpt =
      (Optional) pApplicationContext.findBean(ExpiryPolicy.class);
    @SuppressWarnings("unchecked")
    CacheConfigurationBuilder<ValueName<?>, ValueName<?>> builder =
      (CacheConfigurationBuilder<ValueName<?>, ValueName<?>>) configuration.getBuilder();
    if (cachlyConfigOpt.isPresent()) {
      CachlyEhcacheConfiguration cachlyConfig = cachlyConfigOpt.get();
      Boolean enableSerializer = cachlyConfig.getSerializer();
      if ((enableSerializer != null) && (enableSerializer == true)) {
        builder = builder.withValueSerializer(pSerializer);
      }
      /* Get the list of resource pools */
      Holder<@Nullable ResourcePools> resourcePoolsHolder = new Holder<>(null);
      builder.updateResourcePools((rps) -> {
        resourcePoolsHolder.object = rps;
        return rps;
      });
      ResourcePools rps = resourcePoolsHolder.object;
      if (rps != null) {
        /* Are there any we want to override */

        ResourcePoolsBuilder rpBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder();
        boolean updated = false;
        for (ResourceType<?> rt : rps.getResourceTypeSet()) {
          CachlyDiskTieredCacheConfiguration cachlyDisk = cachlyConfig.getDisk();
          if ((cachlyDisk != null) && (ResourceType.Core.DISK.equals(rt))) {
            ResourcePool rp = rps.getPoolForResource(rt);
            if (rp instanceof SizedResourcePool) {
              SizedResourcePool srp = (SizedResourcePool) rp;
              long size = srp.getSize();
              ResourceUnit unit = srp.getUnit();
              if (unit instanceof MemoryUnit) {
                MemoryUnit memoryUnit = (MemoryUnit) unit;
                Boolean persist = cachlyDisk.getPersist();
                if (persist == null)
                  persist = false;
                rpBuilder = rpBuilder.disk(size, memoryUnit, persist);
                updated = true;
              }
            }
          }
        }
        if (updated == true)
          builder = builder.withResourcePools(rpBuilder);
      }
    }
    if (expiryPolicyOpt.isPresent()) {
      ExpiryPolicy<Object, Object> expiryPolicy = expiryPolicyOpt.get();
      builder = builder.withExpiry(expiryPolicy);
    }
    Cache<?, ?> nativeCache = cacheManager.createCache(configuration.getName(), builder);
    if (expiryPolicyOpt.isPresent()) {
      ExpiryPolicy<Object, Object> expiryPolicy = expiryPolicyOpt.get();
      if (expiryPolicy instanceof CacheEventListener) {
        @SuppressWarnings("unchecked")
        CacheEventListener<Object, Object> listener = (CacheEventListener<Object, Object>) expiryPolicy;
        nativeCache.getRuntimeConfiguration().registerCacheEventListener(listener, EventOrdering.ORDERED,
          EventFiring.SYNCHRONOUS, EnumSet.of(EventType.EVICTED, EventType.REMOVED, EventType.EXPIRED));
      }
    }
    return new EhcacheSyncCache(conversionService, configuration, nativeCache, executorService, statisticsService);
  }
}
