package com.diamondq.cachly.spi;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This is called when the engine is not managed by OSGi, but the parts (like CacheLoaders) are.
 */
@SuppressWarnings("unused")
public final class OSGiEngineSetup {

  private OSGiEngineSetup() {}

  /**
   * Sets up an existing CacheEngine to be populated from services in OSGi
   *
   * @param pContext the Bundle Context
   * @param pEngine the CacheEngine
   * @param pCacheReg the cache registration
   * @param pCacheEngineReg the cache engine registration
   * @return a Closeable that closes the trackers
   */
  public static Closeable setup(BundleContext pContext, CacheEngine pEngine, ServiceRegistration<Cache> pCacheReg,
    ServiceRegistration<CacheEngine> pCacheEngineReg) {

    ServiceTracker<CachlyPathConfiguration, CachlyPathConfiguration> pathTracker = new ServiceTracker<>(pContext,
      CachlyPathConfiguration.class,
      new ServiceTrackerCustomizer<>() {

        @Override
        public CachlyPathConfiguration addingService(ServiceReference<CachlyPathConfiguration> reference) {
          var service = pContext.getService(reference);
          if (service != null) pEngine.addPathConfiguration(service);
          return service;
        }

        @Override
        public void modifiedService(ServiceReference<CachlyPathConfiguration> reference,
          CachlyPathConfiguration service) {
          // Nothing to do
        }

        @Override
        public void removedService(ServiceReference<CachlyPathConfiguration> reference,
          CachlyPathConfiguration service) {
          pEngine.removePathConfiguration(service);
        }
      }
    );
    pathTracker.open();
    ServiceTracker<CacheLoader<?>, CacheLoader<?>> loaderTracker = new ServiceTracker<>(pContext,
      CacheLoader.class.getName(),
      new ServiceTrackerCustomizer<CacheLoader<?>, CacheLoader<?>>() {
        @Override
        public CacheLoader<?> addingService(ServiceReference<CacheLoader<?>> reference) {
          var service = pContext.getService(reference);
          if (service != null) pEngine.addCacheLoader(service);
          return service;
        }

        @Override
        public void modifiedService(ServiceReference<CacheLoader<?>> reference, CacheLoader<?> service) {
          // Nothing to do
        }

        @Override
        public void removedService(ServiceReference<CacheLoader<?>> reference, CacheLoader<?> service) {
          pEngine.removeCacheLoader(service);
        }
      }
    );
    loaderTracker.open();
    ServiceTracker<CacheStorage, CacheStorage> storageTracker = new ServiceTracker<>(pContext,
      CacheStorage.class,
      new ServiceTrackerCustomizer<>() {
        @Override
        public CacheStorage addingService(ServiceReference<CacheStorage> reference) {
          var service = pContext.getService(reference);
          if (service != null) pEngine.addCacheStorage(service);
          return service;
        }

        @Override
        public void modifiedService(ServiceReference<CacheStorage> reference, CacheStorage service) {
          // Nothing to do
        }

        @Override
        public void removedService(ServiceReference<CacheStorage> reference, CacheStorage service) {
          pEngine.removeCacheStorage(service);
        }
      }
    );
    storageTracker.open();
    ServiceTracker<BeanNameLocator, BeanNameLocator> nameTracker = new ServiceTracker<>(pContext,
      BeanNameLocator.class,
      new ServiceTrackerCustomizer<>() {

        @Override
        public BeanNameLocator addingService(ServiceReference<BeanNameLocator> reference) {
          var service = pContext.getService(reference);
          if (service != null) pEngine.addBeanNameLocator(service);
          return service;
        }

        @Override
        public void modifiedService(ServiceReference<BeanNameLocator> reference, BeanNameLocator service) {
          // Nothing to do
        }

        @Override
        public void removedService(ServiceReference<BeanNameLocator> reference, BeanNameLocator service) {
          pEngine.removeBeanNameLocator(service);
        }
      }
    );
    nameTracker.open();
    ServiceTracker<AccessContextSPI<?>, AccessContextSPI<?>> accessTracker = new ServiceTracker<>(pContext,
      AccessContextSPI.class.getName(),
      new ServiceTrackerCustomizer<AccessContextSPI<?>, AccessContextSPI<?>>() {
        @Override
        public AccessContextSPI<?> addingService(ServiceReference<AccessContextSPI<?>> reference) {
          var service = pContext.getService(reference);
          if (service != null) pEngine.addAccessContextSPI(service);
          return service;
        }

        @Override
        public void modifiedService(ServiceReference<AccessContextSPI<?>> reference, AccessContextSPI<?> service) {
          // Nothing to do
        }

        @Override
        public void removedService(ServiceReference<AccessContextSPI<?>> reference, AccessContextSPI<?> service) {
          pEngine.removeAccessContextSPI(service);
        }
      }
    );
    accessTracker.open();
    pEngine.setRegistrationChangeListener((pChangeList) -> {

      Map<String, List<String>> building = new HashMap<>();
      for (var pair : pChangeList) {
        var type = pair.getLeft();
        var list = building.computeIfAbsent("active_" + type.name().toLowerCase(Locale.ROOT), (_) -> new ArrayList<>());
        list.add(pair.getRight());
      }
      Map<String, String[]> buildingArray = new HashMap<>();
      for (var entry : building.entrySet()) {
        buildingArray.put(entry.getKey(), entry.getValue().toArray(new String[0]));
      }

      /* Rebuild the properties */

      var cacheEngineProps = pCacheEngineReg.getReference().getProperties();
      buildingArray.forEach(cacheEngineProps::put);
      pCacheEngineReg.setProperties(cacheEngineProps);

      var cacheProps = pCacheReg.getReference().getProperties();
      buildingArray.forEach(cacheProps::put);
      pCacheReg.setProperties(cacheProps);
    });
    return () -> {
      pathTracker.close();
      loaderTracker.close();
      storageTracker.close();
      nameTracker.close();
      accessTracker.close();
    };
  }
}
