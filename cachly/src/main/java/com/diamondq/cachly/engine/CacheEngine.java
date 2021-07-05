package com.diamondq.cachly.engine;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.impl.CompositeKey;
import com.diamondq.cachly.impl.KeyDetails;
import com.diamondq.cachly.impl.ResolvedKeyPlaceholder;
import com.diamondq.cachly.impl.StaticKey;
import com.diamondq.cachly.spi.BeanNameLocator;
import com.diamondq.cachly.spi.KeyPlaceholderSPI;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;
import com.diamondq.common.types.Types;

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

@Singleton
public class CacheEngine implements Cache {

  private final Map<String, CacheStorage>            mCacheStorageByPath;

  private final Map<String, CacheLoaderInfo<Object>> mLoadersByPath;

  private final Map<String, String>                  mSerializerNameByPath;

  private final ThreadLocal<Stack<Set<String>>>      mMonitored = ThreadLocal.withInitial(() -> new Stack<>());

  private final KeySPI<CacheInfo>                    mStorageKey;

  private final CacheInfo                            mCacheInfo;

  @Inject
  public CacheEngine(List<CachlyPathConfiguration> pPaths, List<BeanNameLocator> pNameLocators,
    List<CacheStorage> pCacheStorages, List<CacheLoader<?>> pCacheLoaders) {

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

    mCacheStorageByPath = storagesByPath;
    mLoadersByPath = loadersByPath;
    mSerializerNameByPath = serializerNameByPath;

    /* Setup the storage key and cache info */

    mStorageKey =
      (KeySPI<CacheInfo>) KeyBuilder.<CacheInfo> of(CacheInfoLoader.CACHE_INFO_NAME, new TypeReference<CacheInfo>() { // type
                                                                                                                      // reference
      });
    setupKey(mStorageKey);
    CacheResult<CacheInfo> cacheInfoResult = mStorageKey.getLastStorage().queryForKey(mStorageKey);
    if (cacheInfoResult.entryFound() == false)
      mCacheInfo = new CacheInfo();
    else
      mCacheInfo = cacheInfoResult.getValue();
  }

  /**
   * This is the main resolution routine. It will take a key and resolve it to the cached value
   *
   * @param <O> the result type
   * @param pKey the key
   * @return the result
   */
  private <O> CacheResult<O> lookup(KeySPI<O> pKey, boolean pLoadIfMissing) {

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
            newParts[i] = sspi.resolveDefault(this);
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
      setupKey(pKey);
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

    CacheResult<O> queryResult = storage.queryForKey(pKey);

    if ((pLoadIfMissing == false) || (queryResult.entryFound() == true))
      return queryResult;

    /* Now attempt to lookup the data */

    CacheLoader<O> cacheLoader = pKey.getLoader();

    /* In order to track dependencies, create a new set to add to the current stack */

    dependencyStack.add(new HashSet<>());

    CacheResult<O> loadedResult;
    Set<String> dependencies;
    try {
      loadedResult = cacheLoader.load(this, pKey);
    }
    finally {

      /* Pull the dependency set off the stack */

      dependencies = dependencyStack.pop();
      if (placeholderDependencies != null)
        dependencies.addAll(placeholderDependencies);
    }

    /* Now store the result */

    if (loadedResult.entryFound() == true)
      storage.store(pKey, loadedResult);

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
      mStorageKey.getLastStorage().store(mStorageKey, new CacheResult<>(mCacheInfo, true));
    }

    /* Return */

    return loadedResult;

  }

  /**
   * @see com.diamondq.cachly.Cache#invalidateAll()
   */
  @Override
  public void invalidateAll() {
    mCacheStorageByPath.values().stream().distinct().forEach((cs) -> cs.invalidateAll());
  }

  private <O> void invalidate(KeySPI<O> pKey) {

    String keyStr = pKey.toString();

    /* Find the last storage given the key */

    CacheStorage storage = pKey.getLastStorage();

    storage.invalidate(pKey);

    /* Were there dependencies? */

    Set<KeySPI<?>> depSet = mCacheInfo.dependencyMap.remove(keyStr);
    if (depSet != null) {

      /* Save the updated CacheInfo */

      mStorageKey.getLastStorage().store(mStorageKey, new CacheResult<>(mCacheInfo, true));

      /* Invalidate all the subkeys */

      for (KeySPI<?> dep : depSet)
        invalidate(dep);

    }

  }

  private <O> void setupKey(KeySPI<O> pKey) {
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
      if (lastStorage == null)
        throw new IllegalStateException("Unable to find a cache storage that will cover " + currentPath);

      /* Lookup the serializer */

      String testSerializerName = mSerializerNameByPath.get(currentPath);
      if (testSerializerName != null)
        lastSerializerName = testSerializerName;
      if (lastSerializerName == null)
        throw new IllegalStateException("Unable to find a serializer that will cover " + currentPath);

      /* Now lookup the loader */

      CacheLoaderInfo<Object> loaderInfo = mLoadersByPath.get(currentPath);
      if (loaderInfo == null)
        throw new IllegalStateException("Unable to find a cache loader that will cover " + currentPath);

      KeyDetails<Object> keyDetails =
        new KeyDetails<>(lastStorage, lastSerializerName, loaderInfo.supportsNull, loaderInfo.loader);
      part.storeKeyDetails(keyDetails);

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
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.Key)
   */
  @Override
  public <V> V get(Key<V> pKey) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    if (ki.hasKeyDetails() == false)
      setupKey(ki);
    CacheResult<V> result = lookup(ki, true);
    if (result.entryFound() == true) {
      if (result.isNull() == true) {
        if (ki.supportsNull() == true) {
          V r = null;
          return r;
        }
        throw new NullPointerException();
      }
      return result.getValue();
    }
    if (ki.supportsNull() == true) {
      V r = null;
      return r;
    }
    throw new NoSuchElementException();
  }

  /**
   * @see com.diamondq.cachly.Cache#getIfPresent(com.diamondq.cachly.Key)
   */
  @Override
  public <V> Optional<V> getIfPresent(Key<V> pKey) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    if (ki.hasKeyDetails() == false)
      setupKey(ki);
    CacheResult<V> result = lookup(ki, false);
    if (result.entryFound())
      return Optional.ofNullable(result.getValue());
    return Optional.empty();
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, V> V get(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return get(resolve(ki, pHolder1, pValue1));
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder, java.lang.String,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, V> V get(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1, KeyPlaceholder<K2> pHolder2,
    String pValue2) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return get(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder, java.lang.String,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, K3, V> V get(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1, KeyPlaceholder<K2> pHolder2,
    String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return get(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder, java.lang.String,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, K3, K4, V> V get(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return get(resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4,
      pValue4));
  }

  /**
   * @see com.diamondq.cachly.Cache#getIfPresent(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String)
   */
  @Override
  public <K1, V> Optional<V> getIfPresent(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return getIfPresent(resolve(ki, pHolder1, pValue1));
  }

  /**
   * @see com.diamondq.cachly.Cache#getIfPresent(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, V> Optional<V> getIfPresent(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return getIfPresent(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  /**
   * @see com.diamondq.cachly.Cache#getIfPresent(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String)
   */
  @Override
  public <K1, K2, K3, V> Optional<V> getIfPresent(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return getIfPresent(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  /**
   * @see com.diamondq.cachly.Cache#getIfPresent(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, K3, K4, V> Optional<V> getIfPresent(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    return getIfPresent(resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3),
      pHolder4, pValue4));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.Key)
   */
  @Override
  public <V> void invalidate(Key<V> pKey) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    if (ki.hasKeyDetails() == false)
      setupKey(ki);
    invalidate(ki);
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String)
   */
  @Override
  public <K1, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    invalidate(resolve(ki, pHolder1, pValue1));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    invalidate(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String)
   */
  @Override
  public <K1, K2, K3, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    invalidate(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.String, com.diamondq.cachly.KeyPlaceholder, java.lang.String)
   */
  @Override
  public <K1, K2, K3, K4, V> void invalidate(Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
    KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3,
    KeyPlaceholder<K4> pHolder4, String pValue4) {
    if ((pKey instanceof KeySPI) == false)
      throw new IllegalStateException();
    KeySPI<V> ki = (KeySPI<V>) pKey;
    invalidate(resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4,
      pValue4));
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
   * @see com.diamondq.cachly.Cache#streamKeys()
   */
  @Override
  public Stream<Key<?>> streamKeys() {
    return //
    /* Get the distinct list of CacheStorages */
    mCacheStorageByPath.values().stream().distinct() //
      /* Expand each into a stream of string keys */
      .flatMap((cs) -> cs.streamKeys())
      /* Convert the strings to Keys */
      .map((strKey) -> from(strKey));
  }
}
