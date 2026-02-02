package com.diamondq.cachly.engine;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheKeyEvent;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.WriteBackCacheLoader;
import com.diamondq.cachly.impl.AccessContextImpl;
import com.diamondq.cachly.impl.CacheCallbackHandler;
import com.diamondq.cachly.impl.CompositeKey;
import com.diamondq.cachly.impl.KeyDetails;
import com.diamondq.cachly.impl.ResolvedKeyPlaceholder;
import com.diamondq.cachly.impl.StaticAccessContextPlaceholder;
import com.diamondq.cachly.impl.StaticCacheResult;
import com.diamondq.cachly.spi.AccessContextPlaceholderSPI;
import com.diamondq.cachly.spi.AccessContextSPI;
import com.diamondq.cachly.spi.BeanNameLocator;
import com.diamondq.cachly.spi.KeyPlaceholderSPI;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;
import com.diamondq.common.context.Context;
import com.diamondq.common.context.ContextFactory;
import com.diamondq.common.converters.ConverterManager;
import com.diamondq.common.lambda.interfaces.Consumer3;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * Implementation of the Cache
 */
@Singleton
@Component(service = Cache.class)
public class CacheEngine implements Cache {

  /**
   * The callback handler
   */
  @Reference protected CacheCallbackHandler mCallbackHandler;
  /**
   * The executor service
   */
  @Reference protected ExecutorService      mExecutorService;
  /**
   * The converter manager
   */
  @Reference protected ConverterManager     mConverterManager;
  /**
   * The context factory
   */
  @Reference protected ContextFactory       mContextFactory;

  /**
   * Defines the bean name locators that are available
   */
  protected final List<BeanNameLocator> mBeanNameLocators = new CopyOnWriteArrayList<>();

  private final Map<String, CacheStorage> mCacheStorageByPath = new ConcurrentHashMap<>();

  private final Map<String, CacheStorage> mCacheStorageByName = new ConcurrentHashMap<>();

  private final Map<String, CacheLoaderInfo<Object>> mLoadersByPath = new ConcurrentHashMap<>();

  private final Map<String, String> mSerializerNameByPath = new ConcurrentHashMap<>();

  private static final ThreadLocal<ArrayDeque<Set<String>>> sMonitored = ThreadLocal.withInitial(ArrayDeque::new);

  private final KeySPI<CacheInfo> mStorageKey = (KeySPI<CacheInfo>) KeyBuilder.of(CacheInfoLoader.CACHE_INFO_NAME,
    new TypeReference<CacheInfo>() { // type
      // reference
    }
  );

  private @Nullable CacheInfo mCacheInfo;

  private final AccessContext mEmptyAccessContext = new AccessContextImpl(Collections.emptyMap());

  private final Map<Class<?>, List<AccessContextSPI<?>>> mAccessContextSPIMap = new ConcurrentHashMap<>();

  private final Map<Class<?>, Class<?>> mAccessContextClassMap = new ConcurrentHashMap<>();

  /**
   * Constructor for OSGi-based solutions
   */
  public CacheEngine() {
  }

  /**
   * Constructor for basic use
   *
   * @param pCallbackHandler the Callback Handler
   * @param pExecutorService the Executor Service
   * @param pConverterManager the Converter Manager
   * @param pContextFactory the Context Factory
   */
  public CacheEngine(CacheCallbackHandler pCallbackHandler, ExecutorService pExecutorService,
    ConverterManager pConverterManager, ContextFactory pContextFactory) {
    mCallbackHandler = pCallbackHandler;
    mExecutorService = pExecutorService;
    mConverterManager = pConverterManager;
    mContextFactory = pContextFactory;
  }

  @Override
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void addBeanNameLocator(BeanNameLocator pBeanNameLocator) {
    mBeanNameLocators.add(pBeanNameLocator);
  }

  @Override
  public void removeBeanNameLocator(BeanNameLocator pBeanNameLocator) {
    mBeanNameLocators.remove(pBeanNameLocator);
  }

  @Override
  @Activate
  public void activate() {
    finishSetup();
  }

  /**
   * Injection Constructor for CDI-based solutions
   *
   * @param pContextFactory the Context Factory
   * @param pConverterManager the Converter Manager
   * @param pExecutorService the Executor Service
   * @param pCallbackHandler the Callback Handler
   * @param pPaths the list of Paths for storage
   * @param pNameLocators the name locator
   * @param pCacheStorages the cache storages
   * @param pCacheLoaders the cache loaders
   * @param pAccessContextSPIs the context SPIs
   */
  @Inject
  public CacheEngine(ContextFactory pContextFactory, ConverterManager pConverterManager,
    @Named("DiamondQ") ExecutorService pExecutorService, CacheCallbackHandler pCallbackHandler,
    List<CachlyPathConfiguration> pPaths, List<BeanNameLocator> pNameLocators, List<CacheStorage> pCacheStorages,
    List<CacheLoader<?>> pCacheLoaders, List<AccessContextSPI<?>> pAccessContextSPIs) {

    mContextFactory = pContextFactory;
    mConverterManager = pConverterManager;
    mExecutorService = pExecutorService;
    mCallbackHandler = pCallbackHandler;
    pNameLocators.forEach(this::addBeanNameLocator);

    /* Build the map of storages by name */

    pCacheStorages.forEach(this::addCacheStorage);

    /* Build the storages by path */

    pPaths.forEach(this::addPathConfiguration);

    /* Build the map of loaders by path */

    pCacheLoaders.forEach(this::addCacheLoader);

    /* Build the map of AccessContext SPIs */

    pAccessContextSPIs.forEach(this::addAccessContextSPI);

    finishSetup();
  }

  @Override
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void addCacheStorage(CacheStorage pStorage) {
    /* Query the bean name locators for the name */

    String name = mBeanNameLocators.stream()
      .map((bnl) -> bnl.getBeanName(pStorage))
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
    if (name == null) {
      throw new IllegalArgumentException(
        "The CacheStorage " + pStorage.getClass().getName() + " must have a name (such as @Named) associated with it");
    }

    pStorage.setCacheEngine(this);
    mCacheStorageByName.put(name, pStorage);
  }

  @Override
  public void removeCacheStorage(CacheStorage pStorage) {
    /* Query the bean name locators for the name */

    String name = mBeanNameLocators.stream()
      .map((bnl) -> bnl.getBeanName(pStorage))
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
    if (name == null) return;

    mCacheStorageByName.remove(name, pStorage);
  }

  @Override
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void addPathConfiguration(CachlyPathConfiguration pPathConfig) {
    String storage = pPathConfig.getStorage();
    String serializerName = pPathConfig.getSerializer();
    String path = pPathConfig.getName();
    CacheStorage cacheStorage = mCacheStorageByName.get(storage);
    if (cacheStorage == null) {
      throw new IllegalArgumentException(
        "Configuration has a storage called " + storage + " at path " + path + " which cannot be located");
    }
    mCacheStorageByPath.put(path, cacheStorage);
    if (serializerName == null) {
      serializerName = DEFAULT_SERIALIZER;
    }
    mSerializerNameByPath.put(path, serializerName);
  }

  @Override
  public void removePathConfiguration(CachlyPathConfiguration pPathConfig) {
    String storage = pPathConfig.getStorage();
    String path = pPathConfig.getName();
    var cacheStorage = mCacheStorageByName.get(storage);
    if (cacheStorage != null) mCacheStorageByPath.remove(path, cacheStorage);
    mSerializerNameByPath.remove(path, pPathConfig.getSerializer());
  }

  private void finishSetup() {

    if (!mCacheStorageByPath.containsKey(CacheInfoLoader.CACHE_INFO_NAME)) {
      mCacheStorageByPath.put(CacheInfoLoader.CACHE_INFO_NAME,
        new MemoryCacheStorage(mConverterManager, mExecutorService, mCallbackHandler, CacheInfoLoader.CACHE_INFO_NAME)
      );
    }
    if (!mSerializerNameByPath.containsKey(CacheInfoLoader.CACHE_INFO_NAME)) {
      mSerializerNameByPath.put(CacheInfoLoader.CACHE_INFO_NAME, DEFAULT_SERIALIZER);
    }
    addCacheLoader(new CacheInfoLoader());

    /* Set up the storage key and cache information */

    AccessContext ac = createAccessContext(null);
    setupKey(mStorageKey);
    CacheResult<CacheInfo> cacheInfoResult = mStorageKey.getLastStorage().queryForKey(ac, mStorageKey);
    if (!cacheInfoResult.entryFound()) {
      mCacheInfo = new CacheInfo();
    } else {
      mCacheInfo = cacheInfoResult.getValue();
    }
  }

  @Override
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void addCacheLoader(CacheLoader<?> pCacheLoader) {
    @SuppressWarnings("unchecked") CacheLoaderInfo<Object> details = (CacheLoaderInfo<Object>) pCacheLoader.getInfo();
    String path = details.key.toString();
    mLoadersByPath.put(path, details);
  }

  @Override
  public void removeCacheLoader(CacheLoader<?> pCacheLoader) {
    @SuppressWarnings("unchecked") CacheLoaderInfo<Object> details = (CacheLoaderInfo<Object>) pCacheLoader.getInfo();
    String path = details.key.toString();
    mLoadersByPath.remove(path, details);
  }

  @Override
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void addAccessContextSPI(AccessContextSPI<?> pAccessContextSPI) {
    Class<?> clazz = pAccessContextSPI.getAccessContextClass();
    List<AccessContextSPI<?>> list = mAccessContextSPIMap.computeIfAbsent(clazz, (_) -> new CopyOnWriteArrayList<>());
    list.add(pAccessContextSPI);
  }

  @Override
  public void removeAccessContextSPI(AccessContextSPI<?> pAccessContextSPI) {
    Class<?> clazz = pAccessContextSPI.getAccessContextClass();
    List<AccessContextSPI<?>> list = mAccessContextSPIMap.get(clazz);
    if (list != null) list.remove(pAccessContextSPI);
  }

  @Override
  public AccessContext createAccessContext(@Nullable AccessContext pExistingContext,
    @Nullable Object @Nullable ... pData) {
    if ((pExistingContext == null) && ((pData == null) || (pData.length == 0))) {
      return mEmptyAccessContext;
    }

    Map<Class<?>, Object> data;
    if (pExistingContext != null) {
      if (pExistingContext instanceof AccessContextImpl ac) {
        data = new HashMap<>(ac.getData());
      } else {
        throw new IllegalArgumentException("The provided AccessContext (" + pExistingContext.getClass().getName()
          + ") was not defined by this library.");
      }
    } else {
      data = new HashMap<>();
    }

    if (pData != null) {
      for (Object accessData : pData) {
        if (accessData != null) {
          Class<?> accessDataClass = accessData.getClass();
          Class<?> acClass = mAccessContextClassMap.get(accessDataClass);
          if (acClass == null) {

            /* Pull apart the class and try to find a matching AccessContextSPI */

            for (Class<?> testClass : getAllClasses(accessDataClass)) {
              List<AccessContextSPI<?>> spiList = mAccessContextSPIMap.get(testClass);
              //noinspection VariableNotUsedInsideIf
              if (spiList != null) {
                acClass = testClass;
                break;
              }
            }

            if (acClass != null) mAccessContextClassMap.put(accessDataClass, acClass);
          }
          if (acClass != null) data.put(acClass, accessData);
        }
      }
    }
    return new AccessContextImpl(data);
  }

  /**
   * Build a list of classes that make up this class. They must be sorted starting with the most specific and ending
   * with the most generic
   *
   * @param pClass the starting class
   * @return the list of classes
   */
  private static List<Class<?>> getAllClasses(Class<?> pClass) {
    Class<?> currentClass = pClass;
    Set<Class<?>> seen = new HashSet<>();
    List<Class<?>> result = new ArrayList<>();
    while (currentClass != null) {
      seen.add(currentClass);
      result.add(currentClass);

      /* Now look at the interfaces */

      Class<?>[] interfaceClasses = currentClass.getInterfaces();
      for (Class<?> ic : interfaceClasses) {
        List<Class<?>> icChildren = getAllClasses(ic);
        for (Class<?> icChild : icChildren) {
          if (seen.add(icChild)) {
            result.add(icChild);
          }
        }
      }

      currentClass = currentClass.getSuperclass();
    }
    return result;
  }

  private record PlaceHolderResult<O>(KeySPI<O> key, @Nullable Set<String> placeholderDependencies) {
  }

  private <O> PlaceHolderResult<O> resolvePlaceholders(AccessContext pAccessContext, KeySPI<O> pKey,
    ArrayDeque<Set<String>> dependencyStack) {

    /*
     * If there are still defaults, since they need to be resolved. This is done here since some defaults may
     * require lookups and want them included in the dependencies
     */

    Set<String> placeholderDependencies;
    if (pKey.hasPlaceholders()) {
      KeySPI<Object>[] parts = pKey.getParts();
      int partsLen = parts.length;
      @SuppressWarnings({ "null", "unchecked" }) KeySPI<Object>[] newParts = new KeySPI[partsLen];

      dependencyStack.push(new HashSet<>());
      try {
        for (int i = 0; i < partsLen; i++) {
          KeySPI<Object> part = parts[i];
          if (part instanceof KeyPlaceholderSPI) {
            @SuppressWarnings("unchecked") KeyPlaceholderSPI<Object> sspi = (KeyPlaceholderSPI<Object>) part;
            newParts[i] = sspi.resolveDefault(this, pAccessContext);
          } else if (part instanceof AccessContextPlaceholderSPI) {
            @SuppressWarnings(
              "unchecked") AccessContextPlaceholderSPI<Object> sspi = (AccessContextPlaceholderSPI<Object>) part;
            newParts[i] = sspi.resolve(this, pAccessContext);
          } else {
            newParts[i] = part;
          }
        }
      }
      finally {

        /* Pull the dependency set off the stack */

        placeholderDependencies = dependencyStack.pop();
        if (placeholderDependencies.isEmpty()) {
          placeholderDependencies = null;
        }
      }

      pKey = new CompositeKey<>(newParts);
      setupKey(pKey);
    } else {
      placeholderDependencies = null;
    }

    return new PlaceHolderResult<>(pKey, placeholderDependencies);
  }

  /**
   * This is the main resolution routine. It will take a key and resolve it to the cached value
   *
   * @param <O> the result type
   * @param pKey the key
   * @return the result
   */
  private <O> CacheResult<O> lookup(AccessContext pAccessContext, KeySPI<O> pKey,
    @SuppressWarnings("SameParameterValue") boolean pLoadIfMissing) {

    ArrayDeque<Set<String>> dependencyStack = sMonitored.get();

    /*
     * If there are still defaults, since they need to be resolved. This is done here since some defaults may
     * require lookups and want them included in the dependencies
     */

    var resolveResult = resolvePlaceholders(pAccessContext, pKey, dependencyStack);
    pKey = resolveResult.key();
    Set<String> placeholderDependencies = resolveResult.placeholderDependencies();

    String keyStr = pKey.toString();

    /* Is monitoring enabled? */

    if (!dependencyStack.isEmpty()) {
      dependencyStack.peek().add(keyStr);
    }

    /* Find the last storage given the key */

    CacheStorage storage = pKey.getLastStorage();

    /* Query the storage for the full key */

    CacheResult<O> queryResult = storage.queryForKey(pAccessContext, pKey);

    if ((!pLoadIfMissing) || (queryResult.entryFound())) {
      return queryResult;
    }

    /* Synchronize so that if there are two threads requesting the same key at the same time,
      one loads the result and the second waits and then sees the result. Otherwise, both threads could look up
      the object (only one is permanently kept), and for certain types of objects (like Class's) this could cause
      multiple instances of the object to be inuse when it was meant as a singleton.

      NOTE: At the moment, this is synchronizing against an intern version of the key string. This will cause a 'memory-leak' for each unique string.
      However, in most cases, this should only represent thousands of strings, not millions.
     */

    synchronized (pKey.toString().intern()) {
      queryResult = storage.queryForKey(pAccessContext, pKey);

      if (queryResult.entryFound()) return queryResult;

      /* Now attempt to look up the data */

      CacheLoader<O> cacheLoader = pKey.getLoader();

      /* To track dependencies, create a new set to add to the current stack */

      dependencyStack.push(new HashSet<>());

      CacheResult<O> loadedResult = new StaticCacheResult<>();
      Set<String> dependencies;
      try {
        cacheLoader.load(this, pAccessContext, pKey, loadedResult);
      }
      finally {

        /* Pull the dependency set off the stack */

        dependencies = dependencyStack.pop();
        if (placeholderDependencies != null) {
          dependencies.addAll(placeholderDependencies);
        }
      }

      /* Now store the result */

      if (loadedResult.entryFound()) {
        storage.store(pAccessContext, pKey, loadedResult);
      }

      /* Store the dependencies for later tracking */

      if (!dependencies.isEmpty()) {
        for (String dep : dependencies) {
          Set<KeySPI<?>> set = Objects.requireNonNull(mCacheInfo).dependencyMap.computeIfAbsent(dep,
            (_) -> new HashSet<>()
          );
          set.add(pKey);
        }
        Set<String> set = mCacheInfo.reverseDependencyMap.computeIfAbsent(pKey.toString(), (_) -> new HashSet<>());
        set.addAll(dependencies);
        mStorageKey.getLastStorage().store(pAccessContext, mStorageKey, new StaticCacheResult<>(mCacheInfo, true));
      }

      /* Return */

      return loadedResult;
    }

  }

  /**
   * This is the main set routine
   *
   * @param <O> the result type
   * @param pKey the key
   * @param pCacheResult the cache result to store
   */
  private <O> void setInternal(AccessContext pAccessContext, KeySPI<O> pKey, CacheResult<O> pCacheResult) {
    try (Context ignored = mContextFactory.newContext(CacheEngine.class, this, pKey, pCacheResult)) {

      pKey = resolvePlaceholders(pAccessContext, pKey, new ArrayDeque<>()).key();

      /* Find the last storage given the key */

      CacheStorage storage = pKey.getLastStorage();

      storage.store(pAccessContext, pKey, pCacheResult);

      /* Now attempt to look up the data */

      CacheLoader<O> cacheLoader = pKey.getLoader();
      if (cacheLoader instanceof WriteBackCacheLoader<O> writeBackCacheLoader) {
        writeBackCacheLoader.store(this, pAccessContext, pKey, pCacheResult);
      }

    }
  }

  @Override
  public void invalidateAll(AccessContext pAccessContext) {
    mCacheStorageByPath.values().stream().distinct().forEach((cs) -> cs.invalidateAll(pAccessContext));
  }

  private <O> void invalidateInternal(AccessContext pAccessContext, KeySPI<O> pKey) {
    try (Context ignored = mContextFactory.newContext(CacheEngine.class, this, pKey)) {

      pKey = resolvePlaceholders(pAccessContext, pKey, new ArrayDeque<>()).key();

      String keyStr = pKey.toString();

      /* Find the last storage given the key */

      CacheStorage storage = pKey.getLastStorage();

      Objects.requireNonNull(mCacheInfo).reverseDependencyMap.remove(keyStr);

      /* Were there dependencies? */

      Set<KeySPI<?>> depSet = mCacheInfo.dependencyMap.remove(keyStr);

      /* Call the invalidation routine on the storage. NOTE: This may cause the data to load back depending on callbacks */

      storage.invalidate(pAccessContext, pKey);

      if (depSet != null) {

        /* Save the updated CacheInfo */

        mStorageKey.getLastStorage().store(pAccessContext, mStorageKey, new StaticCacheResult<>(mCacheInfo, true));

        /* Invalidate all the keys */

        for (KeySPI<?> dep : depSet) {
          invalidate(pAccessContext, dep);
        }
      }

    }
  }

  /**
   * Performs the key setup
   *
   * @param pKey the key
   * @param <O> the key type
   */
  public <O> void setupKey(KeySPI<O> pKey) {
    KeySPI<Object>[] parts = pKey.getParts();
    StringBuilder sb = new StringBuilder();
    CacheStorage lastStorage = null;
    String lastSerializerName = null;
    for (KeySPI<Object> part : parts) {
      sb.append(part.getBaseKey());

      String currentPath = sb.toString();

      /* Look up the storage */

      CacheStorage testCacheStorage = mCacheStorageByPath.get(currentPath);
      if (testCacheStorage != null) {
        lastStorage = testCacheStorage;
      }

      /* Look up the serializer */

      String testSerializerName = mSerializerNameByPath.get(currentPath);
      if (testSerializerName != null) {
        lastSerializerName = testSerializerName;
      }

      /* Now look up the loader */

      CacheLoaderInfo<Object> loaderInfo = mLoadersByPath.get(currentPath);

      if ((lastStorage != null) && (lastSerializerName != null) && (loaderInfo != null)) {
        KeyDetails<Object> keyDetails = new KeyDetails<>(lastStorage,
          lastSerializerName,
          loaderInfo.supportsNull,
          loaderInfo.loader
        );
        part.storeKeyDetails(keyDetails);
      }

      if (part instanceof StaticAccessContextPlaceholder<?> sacp) {
        sacp.setAccessContextSPI(mAccessContextSPIMap);
      }
      //noinspection HardcodedFileSeparator
      sb.append("/");
    }
  }

  /**
   * This internal method resolves a placeholder out of a CompositeKey
   *
   * @param <K> the key value type
   * @param <V> the result value type
   * @param pKey the composite key
   * @param pHolder the key placeholder
   * @param pValue the value
   * @return the new composite key with the placeholder removed
   */
  private static <K extends @Nullable Object, V extends @Nullable Object> KeySPI<V> resolve(KeySPI<V> pKey,
    KeyPlaceholder<K> pHolder, String pValue) {
    if (!(pHolder instanceof KeySPI)) {
      throw new IllegalStateException();
    }
    @SuppressWarnings("unchecked") KeySPI<Object> hi = (KeySPI<Object>) pHolder;
    KeySPI<Object>[] parts = pKey.getParts();
    int partsLen = parts.length;
    @SuppressWarnings({ "null", "unchecked" }) KeySPI<Object>[] newParts = new KeySPI[partsLen];

    for (int i = 0; i < partsLen; i++) {
      KeySPI<Object> part = parts[i];
      if (part == pHolder) {
        newParts[i] = new ResolvedKeyPlaceholder<>(hi, pValue);
      } else {
        newParts[i] = part;
      }
    }
    return new CompositeKey<>(newParts);
  }

  @Override
  public <V extends @Nullable Object> V get(AccessContext pAccessContext, Key<V> pKey) {
    try (Context ctx = mContextFactory.newContext(CacheEngine.class, this, pKey)) {
      if (!(pKey instanceof KeySPI<V> ki)) {
        throw ctx.reportThrowable(new IllegalStateException());
      }
      if (!ki.hasKeyDetails()) {
        setupKey(ki);
      }
      CacheResult<V> result = lookup(pAccessContext, ki, true);
      if (result.entryFound()) {
        if (result.isNull()) {
          if (ki.supportsNull()) {
            return ctx.exit(null);
          }
          throw ctx.reportThrowable(new NullPointerException(ki.toString()));
        }
        return ctx.exit(result.getValue());
      }
      if (ki.supportsNull()) {
        return ctx.exit(null);
      }
      throw ctx.reportThrowable(new NoSuchElementException(ki.toString()));
    }
  }

  @Override
  public <V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey) {
    try (Context ctx = mContextFactory.newContext(CacheEngine.class, this, pKey)) {
      if (!(pKey instanceof KeySPI<V> ki)) {
        throw ctx.reportThrowable(new IllegalStateException());
      }
      if (!ki.hasKeyDetails()) {
        setupKey(ki);
      }
      CacheResult<V> result = lookup(pAccessContext, ki, true);
      //noinspection ConstantConditions
      if (result.entryFound()) return ctx.exit(Optional.ofNullable(result.isNull() ? null : result.getValue()));
      return ctx.exit(Optional.empty());
    }
  }

  @Override
  public <K1 extends @Nullable Object, V extends @Nullable Object> V get(AccessContext pAccessContext, Key<V> pKey,
    KeyPlaceholder<K1> pHolder1, String pValue1) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    return get(pAccessContext, resolve(ki, pHolder1, pValue1));
  }

  @Override
  public <K1, K2, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    return get(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  @Override
  public <K1, K2, K3, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    return get(pAccessContext, resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  @Override
  public <K1, K2, K3, K4, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    return get(pAccessContext,
      resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4, pValue4)
    );
  }

  @Override
  public <K1, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    return getIfPresent(pAccessContext, resolve(ki, pHolder1, pValue1));
  }

  @Override
  public <K1, K2, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    return getIfPresent(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  @Override
  public <K1, K2, K3, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey,
    KeyPlaceholder<K1> pHolder1, String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2,
    KeyPlaceholder<K3> pHolder3, String pValue3) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    return getIfPresent(pAccessContext,
      resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3)
    );
  }

  @Override
  public <K1, K2, K3, K4, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey,
    KeyPlaceholder<K1> pHolder1, String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2,
    KeyPlaceholder<K3> pHolder3, String pValue3, KeyPlaceholder<K4> pHolder4, String pValue4) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    return getIfPresent(pAccessContext,
      resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4, pValue4)
    );
  }

  @Override
  public <V> void set(AccessContext pAccessContext, Key<V> pKey, V pValue) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setInternal(pAccessContext, ki, new StaticCacheResult<>(pValue, true));
  }

  @Override
  public <V> void set(AccessContext pAccessContext, Key<V> pKey, V pValue, Duration pExpiry) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setInternal(pAccessContext, ki, new StaticCacheResult<>(pValue, true).setOverrideExpiry(pExpiry));
  }

  @Override
  public <V> void setNotFound(AccessContext pAccessContext, Key<V> pKey) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setInternal(pAccessContext, ki, new StaticCacheResult<>());
  }

  @Override
  public <V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, Duration pExpiry) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setInternal(pAccessContext, ki, new StaticCacheResult<V>().setOverrideExpiry(pExpiry));
  }

  @Override
  public <K1, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    V pValue) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    set(pAccessContext, resolve(ki, pHolder1, pValue1), pValue);
  }

  @Override
  public <K1, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    V pValue, Duration pExpiry) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    set(pAccessContext, resolve(ki, pHolder1, pValue1), pValue, pExpiry);
  }

  @Override
  public <K1, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setNotFound(pAccessContext, resolve(ki, pHolder1, pValue1));
  }

  @Override
  public <K1, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, Duration pExpiry) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setNotFound(pAccessContext, resolve(ki, pHolder1, pValue1), pExpiry);
  }

  @Override
  public <K1, K2, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, V pValue) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    set(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pValue);
  }

  @Override
  public <K1, K2, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, V pValue, Duration pExpiry) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    set(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pValue, pExpiry);
  }

  @Override
  public <K1, K2, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setNotFound(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  @Override
  public <K1, K2, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, Duration pExpiry) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setNotFound(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pExpiry);
  }

  @Override
  public <K1, K2, K3, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    V pValue) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    set(pAccessContext, resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pValue);
  }

  @Override
  public <K1, K2, K3, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3, V pValue,
    Duration pExpiry) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    set(pAccessContext,
      resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3),
      pValue,
      pExpiry
    );
  }

  @Override
  public <K1, K2, K3, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setNotFound(pAccessContext, resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  @Override
  public <K1, K2, K3, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    Duration pExpiry) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setNotFound(pAccessContext,
      resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3),
      pExpiry
    );
  }

  @Override
  public <K1, K2, K3, K4, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4, V pValue) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    set(pAccessContext,
      resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3),
        pHolder4,
        pValue4
      ),
      pValue
    );
  }

  @Override
  public <K1, K2, K3, K4, V> void set(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4, V pValue, Duration pExpiry) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    set(pAccessContext,
      resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3),
        pHolder4,
        pValue4
      ),
      pValue,
      pExpiry
    );
  }

  @Override
  public <K1, K2, K3, K4, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setNotFound(pAccessContext,
      resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4, pValue4)
    );
  }

  @Override
  public <K1, K2, K3, K4, V> void setNotFound(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4, Duration pExpiry) {
    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    setNotFound(pAccessContext,
      resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3),
        pHolder4,
        pValue4
      ),
      pExpiry
    );
  }

  @Override
  public <V> void invalidate(AccessContext pAccessContext, Key<V> pKey) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }
    invalidateInternal(pAccessContext, ki);
  }

  @Override
  public <K1, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    invalidate(pAccessContext, resolve(ki, pHolder1, pValue1));
  }

  @Override
  public <K1, K2, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    invalidate(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  @Override
  public <K1, K2, K3, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    invalidate(pAccessContext, resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  @Override
  public <K1, K2, K3, K4, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4) {
    if (!(pKey instanceof KeySPI<V> ki)) {
      throw new IllegalStateException();
    }
    invalidate(pAccessContext,
      resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4, pValue4)
    );
  }

  @Override
  public Map<String, CacheLoaderInfo<?>> getCacheLoadersByPath() {
    Map<String, CacheLoaderInfo<?>> result = new HashMap<>(mLoadersByPath);
    result.remove(CacheInfoLoader.CACHE_INFO_NAME);
    return result;
  }

  @Override
  public Stream<Map.Entry<Key<?>, CacheResult<?>>> streamEntries(AccessContext pAccessContext) {
    return //
      /* Get the distinct list of CacheStorages */
      mCacheStorageByPath.values().stream().distinct() //
        /* Expand each into a stream of string keys */.flatMap((cs) -> cs.streamEntries(pAccessContext));
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Collection<Key<?>> getDependentKeys(AccessContext pAccessContext, String pKeyStr) {
    final Collection<Key<?>> result = (Collection) Objects.requireNonNull(mCacheInfo).dependencyMap.get(pKeyStr);
    if (result == null) return Collections.emptyList();
    return result;
  }

  @Override
  public Collection<String> getDependentOnKeys(AccessContext pAccessContext, String pKeyStr) {
    final Collection<String> result = Objects.requireNonNull(mCacheInfo).reverseDependencyMap.get(pKeyStr);
    if (result == null) return Collections.emptyList();
    return result;
  }

  @Override
  public <K1, V> Key<V> resolve(Key<V> pKey, KeyPlaceholder<K1> pHolder, String pValue) {
    return resolve((KeySPI<V>) pKey, pHolder, pValue);
  }

  @Override
  public <V> void registerOnChange(AccessContext pAccessContext, Key<V> pKey,
    Consumer3<Key<V>, CacheKeyEvent, Optional<V>> pCallback) {

    if (!(pKey instanceof KeySPI<V> ki)) throw new IllegalStateException();

    if (!ki.hasKeyDetails()) {
      setupKey(ki);
    }

    var resolvedKey = resolvePlaceholders(pAccessContext, ki, new ArrayDeque<>()).key();

    /* Find the last storage given the key */

    CacheStorage storage = resolvedKey.getLastStorage();

    /* Register the callback with the actual cache */

    storage.registerOnChange(pAccessContext, resolvedKey, pCallback);

    getIfPresent(pAccessContext, resolvedKey);
  }
}
