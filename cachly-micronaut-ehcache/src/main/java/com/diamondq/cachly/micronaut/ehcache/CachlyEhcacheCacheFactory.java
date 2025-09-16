package com.diamondq.cachly.micronaut.ehcache;

import com.diamondq.cachly.CacheKeyEvent;
import com.diamondq.cachly.engine.MemoryStorageData;
import com.diamondq.cachly.impl.CacheCallbackHandler;
import com.diamondq.cachly.micronaut.ehcache.CachlyEhcacheConfiguration.CachlyDiskTieredCacheConfiguration;
import com.diamondq.common.Holder;
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
import jakarta.inject.Named;
import jakarta.inject.Singleton;
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
import org.ehcache.core.internal.statistics.DefaultStatisticsService;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;
import org.ehcache.expiry.ExpiryPolicy;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Cache Factory for EHCaches
 */
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
  @SuppressWarnings("MethodMayBeStatic")
  @Singleton
  @Bean(preDestroy = "stop")
  StatisticsService statisticsService() {
    return new DefaultStatisticsService();
  }

  /**
   * Creates a cache instance based on configuration.
   *
   * @param configuration The configuration
   * @param cacheManager The cache manager
   * @param conversionService The conversion service
   * @param executorService The executor
   * @param statisticsService The statistics service
   * @return The sync cache
   */
  @SuppressWarnings("MethodMayBeStatic")
  @EachBean(EhcacheConfiguration.class)
  EhcacheSyncCache syncCache(@Parameter EhcacheConfiguration configuration, CacheManager cacheManager,
    ConversionService conversionService, @Named(TaskExecutors.IO) ExecutorService executorService,
    StatisticsService statisticsService, ApplicationContext pApplicationContext) {

    /* Look to see if there is a matching Cachly configuration by the same name */

    Optional<CachlyEhcacheConfiguration> cachlyConfigOpt = pApplicationContext.findBean(CachlyEhcacheConfiguration.class,
      Qualifiers.byName(configuration.getName())
    );

    /* Performing serialization? */

    boolean performSerialization = true;
    if (cachlyConfigOpt.isPresent()) {
      CachlyEhcacheConfiguration cachlyConfig = cachlyConfigOpt.get();

      Boolean configSerializer = cachlyConfig.getSerializer();
      if (configSerializer != null) performSerialization = configSerializer;
    }

    /* If the value type is still the default, then change it to match whether the code is serializing */

    Class<?> existingValueType = configuration.getValueType();
    if (existingValueType == EhcacheConfiguration.DEFAULT_VALUE_TYPE) {
      if (performSerialization) configuration.setValueType(byte[].class);
      else configuration.setValueType(MemoryStorageData.class);
    }

    /* Start the cache configuration builder */

    @SuppressWarnings(
      "unchecked") CacheConfigurationBuilder<Object, Object> builder = (CacheConfigurationBuilder<Object, Object>) configuration.getBuilder();

    /* If there is a Cachly config... */

    if (cachlyConfigOpt.isPresent()) {
      CachlyEhcacheConfiguration cachlyConfig = cachlyConfigOpt.get();

      /* Get the list of resource pools */

      Holder<@Nullable ResourcePools> resourcePoolsHolder = new Holder<>(null);
      builder.updateResourcePools((rps) -> {
        resourcePoolsHolder.object = rps;
        return rps;
      });
      ResourcePools rps = resourcePoolsHolder.object;
      if (rps != null) {

        /* Are there any to override? */

        ResourcePoolsBuilder rpBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder();
        boolean updated = false;
        for (ResourceType<?> rt : rps.getResourceTypeSet()) {

          /* Does the Cachly config have some disk information? */

          CachlyDiskTieredCacheConfiguration cachlyDisk = cachlyConfig.getDisk();
          if ((cachlyDisk != null) && (rt == ResourceType.Core.DISK)) {
            ResourcePool rp = rps.getPoolForResource(rt);
            if (rp instanceof SizedResourcePool srp) {
              long size = srp.getSize();
              ResourceUnit unit = srp.getUnit();
              if (unit instanceof MemoryUnit memoryUnit) {
                Boolean persist = cachlyDisk.getPersist();
                if (persist == null) persist = false;
                rpBuilder = rpBuilder.disk(size, memoryUnit, persist);
                updated = true;
              }
            }
          }
        }
        if (updated) builder = builder.withResourcePools(rpBuilder);
      }
    }

    /* Look for an ExpiryPolicy */

    @SuppressWarnings(
      { "unchecked", "rawtypes" }) Optional<ExpiryPolicy<Object, Object>> expiryPolicyOpt = (Optional) pApplicationContext.findBean(
      ExpiryPolicy.class);

    /* If there is an expiry policy, then add it to the config */

    if (expiryPolicyOpt.isPresent()) {
      ExpiryPolicy<Object, Object> expiryPolicy = expiryPolicyOpt.get();
      builder = builder.withExpiry(expiryPolicy);
    }

    /* Build the native ehcache */

    Cache<?, ?> nativeCache = cacheManager.createCache(configuration.getName(), builder);

    /* If there is an expiry policy, then register listeners to keep the metadata correct */

    if (expiryPolicyOpt.isPresent()) {
      ExpiryPolicy<Object, Object> expiryPolicy = expiryPolicyOpt.get();
      if (expiryPolicy instanceof CacheEventListener) {
        @SuppressWarnings(
          "unchecked") CacheEventListener<Object, Object> listener = (CacheEventListener<Object, Object>) expiryPolicy;
        nativeCache.getRuntimeConfiguration()
          .registerCacheEventListener(listener,
            EventOrdering.ORDERED,
            EventFiring.SYNCHRONOUS,
            EnumSet.of(EventType.EVICTED, EventType.REMOVED, EventType.EXPIRED)
          );
      }
    }

    /* Set up for notification for callbacks */

    var handlerOpt = pApplicationContext.findBean(CacheCallbackHandler.class);
    handlerOpt.ifPresent((handler) -> nativeCache.getRuntimeConfiguration().registerCacheEventListener((event) -> {
        var value = switch (event.getType()) {
          case EVICTED, EXPIRED, REMOVED -> event.getOldValue();
          case CREATED, UPDATED -> event.getNewValue();
        };
        var eventEnum = switch (event.getType()) {
          case EVICTED, EXPIRED, REMOVED -> CacheKeyEvent.REMOVED;
          case CREATED -> CacheKeyEvent.ADDED;
          case UPDATED -> CacheKeyEvent.MODIFIED;
        };
        handler.handleEvent(nativeCache, event.getKey(), eventEnum, value);
      }, EventOrdering.ORDERED, EventFiring.SYNCHRONOUS, EnumSet.allOf(EventType.class)
    ));

    /* Build the Cachly version of the SyncCache */

    return new CachlyEhcacheSyncCache(conversionService,
      configuration,
      nativeCache,
      executorService,
      statisticsService,
      performSerialization
    );
  }
}
