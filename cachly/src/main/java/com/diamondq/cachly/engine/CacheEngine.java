package com.diamondq.cachly.engine;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.impl.AccessContextImpl;
import com.diamondq.cachly.impl.CompositeKey;
import com.diamondq.cachly.impl.KeyDetails;
import com.diamondq.cachly.impl.ResolvedKeyPlaceholder;
import com.diamondq.cachly.impl.StaticAccessContextPlaceholder;
import com.diamondq.cachly.impl.StaticCacheResult;
import com.diamondq.cachly.impl.StaticKey;
import com.diamondq.cachly.spi.AccessContextPlaceholderSPI;
import com.diamondq.cachly.spi.AccessContextSPI;
import com.diamondq.cachly.spi.BeanNameLocator;
import com.diamondq.cachly.spi.KeyPlaceholderSPI;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;
import com.diamondq.common.context.Context;
import com.diamondq.common.context.ContextFactory;
import com.diamondq.common.types.Types;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Singleton
public class CacheEngine implements Cache {

  private final ContextFactory                       mContextFactory;

  private final Map<String, CacheStorage>            mCacheStorageByPath;

  private final Map<String, CacheLoaderInfo<Object>> mLoadersByPath;

  private final Map<String, String>                  mSerializerNameByPath;

  private final ThreadLocal<Stack<Set<String>>>      mMonitored = ThreadLocal.withInitial(() -> new Stack<>());

  private final KeySPI<CacheInfo>                    mStorageKey;

  private final CacheInfo                            mCacheInfo;

  private final AccessContext                        mEmptyAccessContext;

  private final Map<Class<?>, AccessContextSPI<?>>   mAccessContextSPIMap;

  @Inject
  public CacheEngine(ContextFactory pContextFactory, List<CachlyPathConfiguration> pPaths,
    List<BeanNameLocator> pNameLocators, List<CacheStorage> pCacheStorages, List<CacheLoader<?>> pCacheLoaders,
    List<AccessContextSPI<?>> pAccessContextSPIs) {

    mContextFactory = pContextFactory;

    /* Build the map of storages by name */

    Map<String, CacheStorage> storagesByName = new HashMap<>();
    for (CacheStorage storage : pCacheStorages) {

      /* Query the bean name locators for the name */

      String name = null;
      for (BeanNameLocator bnl : pNameLocators) {
        name = bnl.getBeanName(storage);
        if (name != null)
          break;
      }
      if (name == null)
        throw new IllegalArgumentException(
          "The CacheStorage " + storage.getClass().getName() + " must have a name (such as @Named) associated with it");

      storagesByName.put(name, storage);
    }

    /* Build the storages by path */

    Map<String, CacheStorage> storagesByPath = new HashMap<>();
    Map<String, String> serializerNameByPath = new HashMap<>();
    for (CachlyPathConfiguration pathConfig : pPaths) {
      String storage = pathConfig.getStorage();
      String serializerName = pathConfig.getSerializer();
      String path = pathConfig.getName();
      CacheStorage cacheStorage = storagesByName.get(storage);
      if (cacheStorage == null)
        throw new IllegalArgumentException(
          "Configuration has a storage called " + storage + " at path " + path + " which cannot be located");
      storagesByPath.put(path, cacheStorage);
      if (serializerName == null)
        serializerName = DEFAULT_SERIALIZER;
      serializerNameByPath.put(path, serializerName);
    }

    if (storagesByPath.containsKey(CacheInfoLoader.CACHE_INFO_NAME) == false) {
      storagesByPath.put(CacheInfoLoader.CACHE_INFO_NAME, new MemoryCacheStorage());
    }
    if (serializerNameByPath.containsKey(CacheInfoLoader.CACHE_INFO_NAME) == false) {
      serializerNameByPath.put(CacheInfoLoader.CACHE_INFO_NAME, DEFAULT_SERIALIZER);
    }

    /* Build the map of loaders by path */

    Map<String, CacheLoaderInfo<Object>> loadersByPath = new HashMap<>();
    for (CacheLoader<?> loader : pCacheLoaders) {
      @SuppressWarnings("unchecked")
      CacheLoaderInfo<Object> details = (CacheLoaderInfo<Object>) loader.getInfo();
      String path = details.key.toString();
      loadersByPath.put(path, details);
    }

    /* Build the map of AccessContext SPIs */

    Map<Class<?>, AccessContextSPI<?>> accessContextsSPIMap = new HashMap<>();
    for (AccessContextSPI<?> spi : pAccessContextSPIs) {
      Class<?> clazz = spi.getAccessContextClass();
      accessContextsSPIMap.put(clazz, spi);
    }

    mCacheStorageByPath = storagesByPath;
    mLoadersByPath = loadersByPath;
    mSerializerNameByPath = serializerNameByPath;
    mAccessContextSPIMap = accessContextsSPIMap;
    mEmptyAccessContext = new AccessContextImpl(Collections.emptyMap());

    /* Setup the storage key and cache info */

    mStorageKey =
      (KeySPI<CacheInfo>) KeyBuilder.<CacheInfo> of(CacheInfoLoader.CACHE_INFO_NAME, new TypeReference<CacheInfo>() { // type
                                                                                                                      // reference
      });
    AccessContext ac = createAccessContext(null);
    setupKey(ac, mStorageKey);
    CacheResult<CacheInfo> cacheInfoResult = mStorageKey.getLastStorage().queryForKey(ac, mStorageKey);
    if (cacheInfoResult.entryFound() == false)
      mCacheInfo = new CacheInfo();
    else
      mCacheInfo = cacheInfoResult.getValue();
  }

  /**
   * @see com.diamondq.cachly.Cache#createAccessContext(com.diamondq.cachly.AccessContext, java.lang.Object[])
   */
  @Override
  public AccessContext createAccessContext(@Nullable AccessContext pExistingContext,
    @Nullable Object @Nullable... pData) {
    @Nullable
    Object[] localData = pData;
    if ((pExistingContext == null) && ((localData == null) || (localData.length == 0)))
      return mEmptyAccessContext;

    Map<Class<?>, Object> data;
    if (pExistingContext != null) {
      if (pExistingContext instanceof AccessContextImpl) {
        AccessContextImpl ac = (AccessContextImpl) pExistingContext;
        data = new HashMap<>(ac.getData());
      }
      else
        throw new IllegalArgumentException("The provided AccessContext (" + pExistingContext.getClass().getName()
          + ") was not defined by this library.");
    }
    else
      data = new HashMap<>();

    if (localData != null)
      for (@Nullable
      Object accessData : localData) {
        if (accessData != null)
          data.put(accessData.getClass(), accessData);
      }
    return new AccessContextImpl(data);
  }

  /**
   * This is the main resolution routine. It will take a key and resolve it to the cached value
   *
   * @param <O> the result type
   * @param pKey the key
   * @return the result
   */
  private <O> CacheResult<O> lookup(AccessContext pAccessContext, KeySPI<O> pKey, boolean pLoadIfMissing) {

    Stack<Set<String>> dependencyStack = mMonitored.get();

    /*
     * If there are still defaults, since they need to be resolved. This is done here since some of the defaults may
     * require lookups, and we want them included in the dependencies
     */

    Set<String> placeholderDependencies;
    if (pKey.hasPlaceholders() == true) {
      @NonNull
      KeySPI<Object>[] parts = pKey.getParts();
      int partsLen = parts.length;
      @SuppressWarnings({"null", "unchecked"})
      @NonNull
      KeySPI<Object>[] newParts = new KeySPI[partsLen];

      dependencyStack.add(new HashSet<>());
      try {
        for (int i = 0; i < partsLen; i++) {
          KeySPI<Object> part = parts[i];
          if (part instanceof KeyPlaceholderSPI) {
            @SuppressWarnings("unchecked")
            KeyPlaceholderSPI<Object> sspi = (KeyPlaceholderSPI<Object>) part;
            newParts[i] = sspi.resolveDefault(this, pAccessContext);
          }
          else if (part instanceof AccessContextPlaceholderSPI) {
            @SuppressWarnings("unchecked")
            AccessContextPlaceholderSPI<Object> sspi = (AccessContextPlaceholderSPI<Object>) part;
            newParts[i] = sspi.resolve(this, pAccessContext);
          }
          else
            newParts[i] = part;
        }
      }
      finally {

        /* Pull the dependency set off the stack */

        placeholderDependencies = dependencyStack.pop();
        if (placeholderDependencies.isEmpty() == true)
          placeholderDependencies = null;
      }

      pKey = new CompositeKey<O>(newParts);
      setupKey(pAccessContext, pKey);
    }
    else
      placeholderDependencies = null;

    String keyStr = pKey.toString();

    /* Are we monitoring? */

    if (dependencyStack.isEmpty() == false) {
      dependencyStack.peek().add(keyStr);
    }

    /* Find the last storage given the key */

    CacheStorage storage = pKey.getLastStorage();

    /* Query the storage for the full key */

    CacheResult<O> queryResult = storage.queryForKey(pAccessContext, pKey);

    if ((pLoadIfMissing == false) || (queryResult.entryFound() == true))
      return queryResult;

    /* Now attempt to lookup the data */

    CacheLoader<O> cacheLoader = pKey.getLoader();

    /* In order to track dependencies, create a new set to add to the current stack */

    dependencyStack.add(new HashSet<>());

    CacheResult<O> loadedResult = new StaticCacheResult<>();
    Set<String> dependencies;
    try {
      cacheLoader.load(this, pAccessContext, pKey, loadedResult);
    }
    finally {

      /* Pull the dependency set off the stack */

      dependencies = dependencyStack.pop();
      if (placeholderDependencies != null)
        dependencies.addAll(placeholderDependencies);
    }

    /* Now store the result */

    if (loadedResult.entryFound() == true)
      storage.store(pAccessContext, pKey, loadedResult);

    /* Store the dependencies for later tracking */

    if (dependencies.isEmpty() == false) {
      for (String dep : dependencies) {
        Set<KeySPI<?>> set = mCacheInfo.dependencyMap.get(dep);
        if (set == null) {
          set = new HashSet<>();
          mCacheInfo.dependencyMap.put(dep, set);
        }
        set.add(pKey);
      }
      mStorageKey.getLastStorage().store(pAccessContext, mStorageKey, new StaticCacheResult<>(mCacheInfo, true));
    }

    /* Return */

    return loadedResult;

  }

  /**
   * @see com.diamondq.cachly.Cache#invalidateAll(com.diamondq.cachly.AccessContext)
   */
  @Override
  public void invalidateAll(AccessContext pAccessContext) {
    mCacheStorageByPath.values().stream().distinct().forEach((cs) -> cs.invalidateAll(pAccessContext));
  }

  private <O> void invalidateInternal(AccessContext pAccessContext, KeySPI<O> pKey) {
    try (Context ctx = mContextFactory.newContext(CacheEngine.class, this, pKey)) {
      String keyStr = pKey.toString();

      /* Find the last storage given the key */

      CacheStorage storage = pKey.getLastStorage();

      storage.invalidate(pAccessContext, pKey);

      /* Were there dependencies? */

      Set<KeySPI<?>> depSet = mCacheInfo.dependencyMap.remove(keyStr);
      if (depSet != null) {

        /* Save the updated CacheInfo */

        mStorageKey.getLastStorage().store(pAccessContext, mStorageKey, new StaticCacheResult<>(mCacheInfo, true));

        /* Invalidate all the subkeys */

        for (KeySPI<?> dep : depSet)
          invalidate(pAccessContext, dep);
      }
    }
  }

  private <O> void setupKey(@SuppressWarnings("unused") AccessContext pAccessContext, KeySPI<O> pKey) {
    KeySPI<Object>[] parts = pKey.getParts();
    StringBuilder sb = new StringBuilder();
    CacheStorage lastStorage = null;
    String lastSerializerName = null;
    for (KeySPI<Object> part : parts) {
      sb.append(part.getBaseKey());

      String currentPath = sb.toString();

      /* Lookup the storage */

      CacheStorage testCacheStorage = mCacheStorageByPath.get(currentPath);
      if (testCacheStorage != null)
        lastStorage = testCacheStorage;

      /* Lookup the serializer */

      String testSerializerName = mSerializerNameByPath.get(currentPath);
      if (testSerializerName != null)
        lastSerializerName = testSerializerName;

      /* Now lookup the loader */

      CacheLoaderInfo<Object> loaderInfo = mLoadersByPath.get(currentPath);

      if ((lastStorage != null) && (lastSerializerName != null) && (loaderInfo != null)) {
        KeyDetails<Object> keyDetails =
          new KeyDetails<>(lastStorage, lastSerializerName, loaderInfo.supportsNull, loaderInfo.loader);
        part.storeKeyDetails(keyDetails);
      }

      if (part instanceof StaticAccessContextPlaceholder) {
        StaticAccessContextPlaceholder<?, ?> sacp = (StaticAccessContextPlaceholder<?, ?>) part;
        /* Lookup the access function */
        AccessContextSPI<?> acs = mAccessContextSPIMap.get(sacp.getAccessContextValueClass());
        if (acs == null)
          throw new IllegalStateException(
            "Unable to find an AccessContext SPI for " + sacp.getAccessContextValueClass().getName());
        sacp.setAccessContextSPI(acs);
      }
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
  private <K, V> KeySPI<V> resolve(KeySPI<V> pKey, KeyPlaceholder<K> pHolder, String pValue) {
    if (pHolder instanceof KeySPI == false)
      throw new IllegalStateException();
    @SuppressWarnings("unchecked")
    KeySPI<Object> hi = (KeySPI<Object>) pHolder;
    @NonNull
    KeySPI<Object>[] parts = pKey.getParts();
    int partsLen = parts.length;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeySPI<Object>[] newParts = new KeySPI[partsLen];

    for (int i = 0; i < partsLen; i++) {
      KeySPI<Object> part = parts[i];
      if (part == pHolder) {
        newParts[i] = new ResolvedKeyPlaceholder<Object>(hi, pValue.toString());
      }
      else
        newParts[i] = part;
    }
    KeySPI<V> r = new CompositeKey<V>(newParts);
    return r;
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key)
   */
  @Override
  public <V> V get(AccessContext pAccessContext, Key<V> pKey) {
    try (Context ctx = mContextFactory.newContext(CacheEngine.class, this, pKey)) {
      if ((pKey instanceof KeySPI) == false)
        throw ctx.reportThrowable(new IllegalStateException());
      KeySPI<V> ki = (KeySPI<V>) pKey;
      if (ki.hasKeyDetails() == false)
        setupKey(pAccessContext, ki);
      CacheResult<V> result = lookup(pAccessContext, ki, true);
      if (result.entryFound() == true) {
        if (result.isNull() == true) {
          if (ki.supportsNull() == true) {
            V r = null;
            return ctx.exit(r);
          }
          throw ctx.reportThrowable(new NullPointerException());
        }
        return ctx.exit(result.getValue());
      }
      if (ki.supportsNull() == true) {
        V r = null;
        return ctx.exit(r);
      }
      throw ctx.reportThrowable(new NoSuchElementException());
    }
  }

  /**
   * @see com.diamondq.cachly.Cache#getIfPresent(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key)
   */
  @Override
  public <V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey) {
    try (Context ctx = mContextFactory.newContext(CacheEngine.class, this, pKey)) {
      if ((pKey instanceof KeySPI) == false)
        throw ctx.reportThrowable(new IllegalStateException());
      KeySPI<V> ki = (KeySPI<V>) pKey;
      if (ki.hasKeyDetails() == false)
        setupKey(pAccessContext, ki);
      CacheResult<V> result = lookup(pAccessContext, ki, true);
      if (result.entryFound())
        return ctx.exit(Optional.ofNullable(result.getValue()));
      return ctx.exit(Optional.empty());
    }
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return get(pAccessContext, resolve(ki, pHolder1, pValue1));
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return get(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, K3, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return get(pAccessContext, resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, K3, K4, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return get(pAccessContext, resolve(
      resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4, pValue4));
  }

  /**
   * @see com.diamondq.cachly.Cache#getIfPresent(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return getIfPresent(pAccessContext, resolve(ki, pHolder1, pValue1));
  }

  /**
   * @see com.diamondq.cachly.Cache#getIfPresent(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return getIfPresent(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  /**
   * @see com.diamondq.cachly.Cache#getIfPresent(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, K3, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey,
    KeyPlaceholder<K1> pHolder1, String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2,
    KeyPlaceholder<K3> pHolder3, String pValue3) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return getIfPresent(pAccessContext,
      resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  /**
   * @see com.diamondq.cachly.Cache#getIfPresent(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, K3, K4, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey,
    KeyPlaceholder<K1> pHolder1, String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2,
    KeyPlaceholder<K3> pHolder3, String pValue3, KeyPlaceholder<K4> pHolder4, String pValue4) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return getIfPresent(pAccessContext, resolve(
      resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4, pValue4));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key)
   */
  @Override
  public <V> void invalidate(AccessContext pAccessContext, Key<V> pKey) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    if (ki.hasKeyDetails() == false)
      setupKey(pAccessContext, ki);
    invalidateInternal(pAccessContext, ki);
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    invalidate(pAccessContext, resolve(ki, pHolder1, pValue1));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    invalidate(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, K3, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    invalidate(pAccessContext, resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.AccessContext, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, K3, K4, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
    String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    invalidate(pAccessContext, resolve(
      resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4, pValue4));
  }

  /**
   * @see com.diamondq.cachly.Cache#getCacheLoadersByPath()
   */
  @Override
  public Map<String, CacheLoaderInfo<?>> getCacheLoadersByPath() {
    Map<String, CacheLoaderInfo<?>> result = new HashMap<>(mLoadersByPath);
    result.remove(CacheInfoLoader.CACHE_INFO_NAME);
    return result;
  }

  private Key<?> from(String pFullKey) {
    @NonNull
    String[] partStrs = pFullKey.split("/");
    int partsLen = partStrs.length;
    @SuppressWarnings({"unchecked", "null"})
    @NonNull
    KeySPI<Object>[] parts = new KeySPI[partsLen];
    for (int i = 0; i < partsLen; i++)
      parts[i] = new StaticKey<Object>(partStrs[i], Types.OBJECT);
    return new CompositeKey<Object>(parts);
  }

  /**
   * @see com.diamondq.cachly.Cache#streamKeys(com.diamondq.cachly.AccessContext)
   */
  @Override
  public Stream<Key<?>> streamKeys(AccessContext pAccessContext) {
    return //
    /* Get the distinct list of CacheStorages */
    mCacheStorageByPath.values().stream().distinct() //
      /* Expand each into a stream of string keys */
      .flatMap((cs) -> cs.streamKeys(pAccessContext))
      /* Convert the strings to Keys */
      .map((strKey) -> from(strKey));
  }
}
