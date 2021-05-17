package com.diamondq.cachly.engine;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.TypeReference;
import com.diamondq.cachly.impl.CompositeKey;
import com.diamondq.cachly.impl.KeyDetails;
import com.diamondq.cachly.impl.KeyInternal;
import com.diamondq.cachly.impl.ResolvedKeyPlaceholder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.checkerframework.checker.nullness.qual.NonNull;

@Singleton
public class CacheEngine implements Cache {

  private final Map<String, CacheStorage>                mCacheStorage;

  private final Map<String, CacheLoader<Object, Object>> mLoader;

  private final ThreadLocal<Stack<Set<String>>>          mMonitored = ThreadLocal.withInitial(() -> new Stack<>());

  private final KeyInternal<?, CacheInfo>                mStorageKey;

  private final CacheInfo                                mCacheInfo;

  @Inject
  public CacheEngine(List<CacheStorage> pCacheStorages, List<CacheLoader<?, ?>> pCacheLoaders) {
    Map<String, CacheStorage> storages = new HashMap<>();
    for (CacheStorage storage : pCacheStorages) {
      List<String> basePaths = storage.getBasePaths();
      for (String basePath : basePaths)
        storages.put(basePath, storage);
    }
    Map<String, CacheLoader<Object, Object>> loaders = new HashMap<>();
    for (CacheLoader<?, ?> loader : pCacheLoaders) {
      String path = loader.getPath();
      @SuppressWarnings("unchecked")
      CacheLoader<Object, Object> obj = (CacheLoader<Object, Object>) loader;
      loaders.put(path, obj);
    }

    /* If there is no CacheStorage for __CacheEngine__, then create a simple memory one */

    if (storages.containsKey(CacheInfoLoader.CACHE_INFO_NAME) == false)
      storages.put(CacheInfoLoader.CACHE_INFO_NAME, new MemoryCacheStorage());

    mCacheStorage = storages;
    mLoader = loaders;

    /* Setup the storage key and cache info */

    mStorageKey =
      (KeyInternal<?, CacheInfo>) KeyBuilder.of(CacheInfoLoader.CACHE_INFO_NAME, new TypeReference<CacheInfo>() {
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
  private <O> CacheResult<O> lookup(KeyInternal<Object, O> pKey) {

    String keyStr = pKey.toString();

    /* Are we monitoring? */

    Stack<Set<String>> dependencyStack = mMonitored.get();
    if (dependencyStack.isEmpty() == false) {
      dependencyStack.peek().add(keyStr);
    }

    /* Find the last storage given the key */

    CacheStorage storage = pKey.getLastStorage();

    /* Query the storage for the full key */

    CacheResult<O> queryResult = storage.queryForKey(pKey);

    if (queryResult.entryFound() == true)
      return queryResult;

    /* Now attempt to lookup the data */

    CacheLoader<Object, O> cacheLoader = pKey.getLoader();

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
    }

    /* Now store the result */

    if (loadedResult.entryFound() == true)
      storage.store(pKey, loadedResult);

    /* Store the dependencies for later tracking */

    if (dependencies.isEmpty() == false) {
      for (String dep : dependencies) {
        Set<KeyInternal<?, ?>> set = mCacheInfo.dependencyMap.get(dep);
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

  private <O> void invalidate(KeyInternal<?, O> pKey) {

    String keyStr = pKey.toString();

    /* Find the last storage given the key */

    CacheStorage storage = pKey.getLastStorage();

    storage.invalidate(pKey);

    /* Were there dependencies? */

    Set<KeyInternal<?, ?>> depSet = mCacheInfo.dependencyMap.remove(keyStr);
    if (depSet != null) {

      /* Save the updated CacheInfo */

      mStorageKey.getLastStorage().store(mStorageKey, new CacheResult<>(mCacheInfo, true));

      /* Invalidate all the subkeys */

      for (KeyInternal<?, ?> dep : depSet)
        invalidate(dep);

    }

  }

  private <O> void setupKey(KeyInternal<?, O> pKey) {
    KeyInternal<Object, Object>[] parts = pKey.getParts();
    StringBuilder sb = new StringBuilder();
    CacheStorage lastStorage = null;
    for (KeyInternal<Object, Object> part : parts) {
      sb.append(part.getBaseKey());

      /* Lookup the storage */

      String currentPath = sb.toString();
      CacheStorage testCacheStorage = mCacheStorage.get(currentPath);
      if (testCacheStorage != null)
        lastStorage = testCacheStorage;

      if (lastStorage == null)
        throw new IllegalStateException("Unable to find a cache storage that will cover " + currentPath);

      /* Now lookup the loader */

      CacheLoader<Object, Object> loader = mLoader.get(currentPath);
      if (loader == null)
        throw new IllegalStateException("Unable to find a cache loader that will cover " + currentPath);

      KeyDetails<Object, Object> keyDetails = new KeyDetails<>(lastStorage, loader.supportsNull(), loader);
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
  private <@NonNull K, V> KeyInternal<Object, V> resolve(KeyInternal<?, V> pKey, KeyPlaceholder<?, K, ?> pHolder,
    K pValue) {
    if (pHolder instanceof KeyInternal == false)
      throw new IllegalStateException();
    @SuppressWarnings("unchecked")
    KeyInternal<Object, Object> hi = (KeyInternal<Object, Object>) pHolder;
    @NonNull
    KeyInternal<Object, Object>[] parts = pKey.getParts();
    int partsLen = parts.length;
    @SuppressWarnings({"null", "unchecked"})
    @NonNull
    KeyInternal<Object, Object>[] newParts = new KeyInternal[partsLen];

    for (int i = 0; i < partsLen; i++) {
      KeyInternal<Object, Object> part = parts[i];
      if (part == pHolder) {
        newParts[i] = new ResolvedKeyPlaceholder<Object, Object>(hi, pValue.toString());
      }
      else
        newParts[i] = part;
    }
    KeyInternal<Object, V> r = new CompositeKey<Object, V>(newParts);
    return r;
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.Key)
   */
  @Override
  public <V> V get(Key<?, V> pKey) {
    if ((pKey instanceof KeyInternal) == false)
      throw new IllegalStateException();
    @SuppressWarnings("unchecked")
    KeyInternal<Object, V> ki = (KeyInternal<Object, V>) pKey;
    if (ki.hasKeyDetails() == false)
      setupKey(ki);
    CacheResult<V> result = lookup(ki);
    if (result.entryFound() == true) {
      if (result.isNull() == true) {
        if (ki.supportsNull() == true) {
          @SuppressWarnings("null")
          V r = null;
          return r;
        }
        throw new NullPointerException();
      }
      return result.getValue();
    }
    if (ki.supportsNull() == true) {
      @SuppressWarnings("null")
      V r = null;
      return r;
    }
    throw new NoSuchElementException();
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder, java.lang.Object)
   */
  @Override
  public <@NonNull K1, V> V get(Key<?, V> pKey, KeyPlaceholder<?, K1, ?> pHolder1, K1 pValue1) {
    if ((pKey instanceof KeyInternal) == false)
      throw new IllegalStateException();
    KeyInternal<?, V> ki = (KeyInternal<?, V>) pKey;
    return get(resolve(ki, pHolder1, pValue1));
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder, java.lang.Object,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.Object)
   */
  @Override
  public <@NonNull K1, @NonNull K2, V> V get(Key<?, V> pKey, KeyPlaceholder<?, K1, ?> pHolder1, K1 pValue1,
    KeyPlaceholder<?, K2, ?> pHolder2, K2 pValue2) {
    if ((pKey instanceof KeyInternal) == false)
      throw new IllegalStateException();
    KeyInternal<?, V> ki = (KeyInternal<?, V>) pKey;
    return get(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder, java.lang.Object,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.Object, com.diamondq.cachly.KeyPlaceholder, java.lang.Object)
   */
  @Override
  public <@NonNull K1, @NonNull K2, @NonNull K3, V> V get(Key<?, V> pKey, KeyPlaceholder<?, K1, ?> pHolder1, K1 pValue1,
    KeyPlaceholder<?, K2, ?> pHolder2, K2 pValue2, KeyPlaceholder<?, K3, ?> pHolder3, K3 pValue3) {
    if ((pKey instanceof KeyInternal) == false)
      throw new IllegalStateException();
    KeyInternal<?, V> ki = (KeyInternal<?, V>) pKey;
    return get(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  /**
   * @see com.diamondq.cachly.Cache#get(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder, java.lang.Object,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.Object, com.diamondq.cachly.KeyPlaceholder, java.lang.Object,
   *      com.diamondq.cachly.KeyPlaceholder, java.lang.Object)
   */
  @Override
  public <@NonNull K1, @NonNull K2, @NonNull K3, @NonNull K4, V> V get(Key<?, V> pKey,
    KeyPlaceholder<?, K1, ?> pHolder1, K1 pValue1, KeyPlaceholder<?, K2, ?> pHolder2, K2 pValue2,
    KeyPlaceholder<?, K3, ?> pHolder3, K3 pValue3, KeyPlaceholder<?, K4, ?> pHolder4, K4 pValue4) {
    if ((pKey instanceof KeyInternal) == false)
      throw new IllegalStateException();
    KeyInternal<?, V> ki = (KeyInternal<?, V>) pKey;
    return get(resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4,
      pValue4));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.Key)
   */
  @Override
  public <V> void invalidate(Key<?, V> pKey) {
    if ((pKey instanceof KeyInternal) == false)
      throw new IllegalStateException();
    @SuppressWarnings("unchecked")
    KeyInternal<Object, V> ki = (KeyInternal<Object, V>) pKey;
    if (ki.hasKeyDetails() == false)
      setupKey(ki);
    invalidate(ki);
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.Object)
   */
  @Override
  public <@NonNull K1, V> void invalidate(Key<?, V> pKey, KeyPlaceholder<?, K1, ?> pHolder1, K1 pValue1) {
    if ((pKey instanceof KeyInternal) == false)
      throw new IllegalStateException();
    KeyInternal<?, V> ki = (KeyInternal<?, V>) pKey;
    invalidate(resolve(ki, pHolder1, pValue1));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.Object, com.diamondq.cachly.KeyPlaceholder, java.lang.Object)
   */
  @Override
  public <@NonNull K1, @NonNull K2, V> void invalidate(Key<?, V> pKey, KeyPlaceholder<?, K1, ?> pHolder1, K1 pValue1,
    KeyPlaceholder<?, K2, ?> pHolder2, K2 pValue2) {
    if ((pKey instanceof KeyInternal) == false)
      throw new IllegalStateException();
    KeyInternal<?, V> ki = (KeyInternal<?, V>) pKey;
    invalidate(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.Object, com.diamondq.cachly.KeyPlaceholder, java.lang.Object, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.Object)
   */
  @Override
  public <@NonNull K1, @NonNull K2, @NonNull K3, V> void invalidate(Key<?, V> pKey, KeyPlaceholder<?, K1, ?> pHolder1,
    K1 pValue1, KeyPlaceholder<?, K2, ?> pHolder2, K2 pValue2, KeyPlaceholder<?, K3, ?> pHolder3, K3 pValue3) {
    if ((pKey instanceof KeyInternal) == false)
      throw new IllegalStateException();
    KeyInternal<?, V> ki = (KeyInternal<?, V>) pKey;
    invalidate(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
  }

  /**
   * @see com.diamondq.cachly.Cache#invalidate(com.diamondq.cachly.Key, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.Object, com.diamondq.cachly.KeyPlaceholder, java.lang.Object, com.diamondq.cachly.KeyPlaceholder,
   *      java.lang.Object, com.diamondq.cachly.KeyPlaceholder, java.lang.Object)
   */
  @Override
  public <@NonNull K1, @NonNull K2, @NonNull K3, @NonNull K4, V> void invalidate(Key<?, V> pKey,
    KeyPlaceholder<?, K1, ?> pHolder1, K1 pValue1, KeyPlaceholder<?, K2, ?> pHolder2, K2 pValue2,
    KeyPlaceholder<?, K3, ?> pHolder3, K3 pValue3, KeyPlaceholder<?, K4, ?> pHolder4, K4 pValue4) {
    if ((pKey instanceof KeyInternal) == false)
      throw new IllegalStateException();
    KeyInternal<?, V> ki = (KeyInternal<?, V>) pKey;
    invalidate(resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4,
      pValue4));
  }
}
