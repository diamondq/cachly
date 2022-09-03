package com.diamondq.cachly.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
import com.diamondq.cachly.spi.AccessContextPlaceholderSPI;
import com.diamondq.cachly.spi.AccessContextSPI;
import com.diamondq.cachly.spi.BeanNameLocator;
import com.diamondq.cachly.spi.KeyPlaceholderSPI;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.TypeReference;
import com.diamondq.common.context.Context;
import com.diamondq.common.context.ContextFactory;
import com.diamondq.common.converters.ConverterManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@javax.inject.Singleton
public class CacheEngine implements Cache
{

	private final ContextFactory mContextFactory;

	private final Map<String, CacheStorage> mCacheStorageByPath;

	private final Map<String, CacheLoaderInfo<Object>> mLoadersByPath;

	private final Map<String, String> mSerializerNameByPath;

	private final ThreadLocal<Stack<Set<String>>> mMonitored = ThreadLocal.withInitial(Stack::new);

	private final KeySPI<CacheInfo> mStorageKey;

	private final CacheInfo mCacheInfo;

	private final AccessContext mEmptyAccessContext;

	private final Map<Class<?>, List<AccessContextSPI<?>>> mAccessContextSPIMap;

	private final Map<Class<?>, Class<?>> mAccessContextClassMap;

	@Inject
	@javax.inject.Inject
	public CacheEngine(ContextFactory pContextFactory, ConverterManager pConverterManager, List<CachlyPathConfiguration> pPaths,
			List<BeanNameLocator> pNameLocators, List<CacheStorage> pCacheStorages, List<CacheLoader<?>> pCacheLoaders,
			List<AccessContextSPI<?>> pAccessContextSPIs)
	{

		mContextFactory = pContextFactory;

		/* Build the map of storages by name */

		Map<String, CacheStorage> storagesByName = new HashMap<>();
		for (CacheStorage storage : pCacheStorages)
		{

			/* Query the bean name locators for the name */

			@Nullable String name = null;
			for (BeanNameLocator bnl : pNameLocators)
			{
				name = bnl.getBeanName(storage);
				if (name != null)
				{
					break;
				}
			}
			if (name == null)
			{
				throw new IllegalArgumentException(
						"The CacheStorage " + storage.getClass().getName() + " must have a name (such as @Named) associated with it");
			}

			storagesByName.put(name, storage);
		}

		/* Build the storages by path */

		Map<String, CacheStorage> storagesByPath = new HashMap<>();
		Map<String, String> serializerNameByPath = new HashMap<>();
		for (CachlyPathConfiguration pathConfig : pPaths)
		{
			@Nullable String storage = pathConfig.getStorage();
			@Nullable String serializerName = pathConfig.getSerializer();
			String path = pathConfig.getName();
			CacheStorage cacheStorage = storagesByName.get(storage);
			if (cacheStorage == null)
			{
				throw new IllegalArgumentException(
						"Configuration has a storage called " + storage + " at path " + path + " which cannot be located");
			}
			storagesByPath.put(path, cacheStorage);
			if (serializerName == null)
			{
				serializerName = DEFAULT_SERIALIZER;
			}
			serializerNameByPath.put(path, serializerName);
		}

		if (!storagesByPath.containsKey(CacheInfoLoader.CACHE_INFO_NAME))
		{
			storagesByPath.put(CacheInfoLoader.CACHE_INFO_NAME, new MemoryCacheStorage(pConverterManager));
		}
		if (!serializerNameByPath.containsKey(CacheInfoLoader.CACHE_INFO_NAME))
		{
			serializerNameByPath.put(CacheInfoLoader.CACHE_INFO_NAME, DEFAULT_SERIALIZER);
		}

		/* Build the map of loaders by path */

		Map<String, CacheLoaderInfo<Object>> loadersByPath = new HashMap<>();
		for (CacheLoader<?> loader : pCacheLoaders)
		{
			@SuppressWarnings("unchecked")
			CacheLoaderInfo<Object> details = (CacheLoaderInfo<Object>) loader.getInfo();
			String path = details.key.toString();
			loadersByPath.put(path, details);
		}

		/* Build the map of AccessContext SPIs */

		Map<Class<?>, List<AccessContextSPI<?>>> accessContextsSPIMap = new HashMap<>();
		for (AccessContextSPI<?> spi : pAccessContextSPIs)
		{
			Class<?> clazz = spi.getAccessContextClass();
			List<AccessContextSPI<?>> list = accessContextsSPIMap.computeIfAbsent(clazz, (key) -> new ArrayList<>());
			list.add(spi);
		}

		mCacheStorageByPath = storagesByPath;
		mLoadersByPath = loadersByPath;
		mSerializerNameByPath = serializerNameByPath;
		mAccessContextSPIMap = accessContextsSPIMap;
		mEmptyAccessContext = new AccessContextImpl(Collections.emptyMap());
		mAccessContextClassMap = new ConcurrentHashMap<>();

		/* Set up the storage key and cache info */

		mStorageKey = (KeySPI<CacheInfo>) KeyBuilder.of(CacheInfoLoader.CACHE_INFO_NAME, new TypeReference<CacheInfo>()
		{ // type
			// reference
		});
		AccessContext ac = createAccessContext(null);
		setupKey(ac, mStorageKey);
		CacheResult<CacheInfo> cacheInfoResult = mStorageKey.getLastStorage().queryForKey(ac, mStorageKey);
		if (!cacheInfoResult.entryFound())
		{
			mCacheInfo = new CacheInfo();
		}
		else
		{
			mCacheInfo = cacheInfoResult.getValue();
		}
	}

	@Override
	public AccessContext createAccessContext(@Nullable AccessContext pExistingContext, @Nullable Object @Nullable ... pData)
	{
		@Nullable Object @Nullable [] localData = pData;
		if ((pExistingContext == null) && ((localData == null) || (localData.length == 0)))
		{
			return mEmptyAccessContext;
		}

		Map<Class<?>, Object> data;
		if (pExistingContext != null)
		{
			if (pExistingContext instanceof AccessContextImpl)
			{
				AccessContextImpl ac = (AccessContextImpl) pExistingContext;
				data = new HashMap<>(ac.getData());
			}
			else
			{
				throw new IllegalArgumentException(
						"The provided AccessContext (" + pExistingContext.getClass().getName() + ") was not defined by this library.");
			}
		}
		else
		{
			data = new HashMap<>();
		}

		if (localData != null)
		{
			for (@Nullable Object accessData : localData)
			{
				if (accessData != null)
				{
					Class<?> accessDataClass = accessData.getClass();
					@Nullable Class<?> acClass = mAccessContextClassMap.get(accessDataClass);
					if (acClass == null)
					{

						/* Pull apart the class and try to find a matching AccessContextSPI */

						for (Class<?> testClass : getAllClasses(accessDataClass))
						{
							List<AccessContextSPI<?>> spiList = mAccessContextSPIMap.get(testClass);
							if (spiList != null)
							{
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
	 * Build a list of classes that make up this class. They must be sorted starting with the most specific and ending with the most generic
	 *
	 * @param pClass the starting class
	 * @return the list of classes
	 */
	private List<Class<?>> getAllClasses(Class<?> pClass)
	{
		Class<?> currentClass = pClass;
		Set<Class<?>> seen = new HashSet<>();
		List<Class<?>> result = new ArrayList<>();
		while (currentClass != null)
		{
			seen.add(currentClass);
			result.add(currentClass);

			/* Now look at the interfaces */

			@NonNull Class<?>[] interfaceClasses = currentClass.getInterfaces();
			for (Class<?> ic : interfaceClasses)
			{
				List<Class<?>> icChildren = getAllClasses(ic);
				for (Class<?> icChild : icChildren)
				{
					if (seen.add(icChild))
					{
						result.add(icChild);
					}
				}
			}

			currentClass = currentClass.getSuperclass();
		}
		return result;
	}

	/**
	 * This is the main resolution routine. It will take a key and resolve it to the cached value
	 *
	 * @param <O> the result type
	 * @param pKey the key
	 * @return the result
	 */
	private <O> CacheResult<O> lookup(AccessContext pAccessContext, KeySPI<O> pKey, boolean pLoadIfMissing)
	{

		Stack<Set<String>> dependencyStack = mMonitored.get();

		/*
		 * If there are still defaults, since they need to be resolved. This is done here since some defaults may
		 * require lookups, and we want them included in the dependencies
		 */

		@Nullable Set<String> placeholderDependencies;
		if (pKey.hasPlaceholders())
		{
			@NonNull KeySPI<Object>[] parts = pKey.getParts();
			int partsLen = parts.length;
			@SuppressWarnings({ "null", "unchecked" })
			@NonNull KeySPI<Object>[] newParts = new KeySPI[partsLen];

			dependencyStack.add(new HashSet<>());
			try
			{
				for (int i = 0; i < partsLen; i++)
				{
					KeySPI<Object> part = parts[i];
					if (part instanceof KeyPlaceholderSPI)
					{
						@SuppressWarnings("unchecked")
						KeyPlaceholderSPI<Object> sspi = (KeyPlaceholderSPI<Object>) part;
						newParts[i] = sspi.resolveDefault(this, pAccessContext);
					}
					else if (part instanceof AccessContextPlaceholderSPI)
					{
						@SuppressWarnings("unchecked")
						AccessContextPlaceholderSPI<Object> sspi = (AccessContextPlaceholderSPI<Object>) part;
						newParts[i] = sspi.resolve(this, pAccessContext);
					}
					else
					{
						newParts[i] = part;
					}
				}
			}
			finally
			{

				/* Pull the dependency set off the stack */

				placeholderDependencies = dependencyStack.pop();
				if (placeholderDependencies.isEmpty())
				{
					placeholderDependencies = null;
				}
			}

			pKey = new CompositeKey<>(newParts);
			setupKey(pAccessContext, pKey);
		}
		else
		{
			placeholderDependencies = null;
		}

		String keyStr = pKey.toString();

		/* Are we monitoring? */

		if (!dependencyStack.isEmpty())
		{
			dependencyStack.peek().add(keyStr);
		}

		/* Find the last storage given the key */

		CacheStorage storage = pKey.getLastStorage();

		/* Query the storage for the full key */

		CacheResult<O> queryResult = storage.queryForKey(pAccessContext, pKey);

		if ((!pLoadIfMissing) || (queryResult.entryFound()))
		{
			return queryResult;
		}

		/* Now attempt to look up the data */

		CacheLoader<O> cacheLoader = pKey.getLoader();

		/* In order to track dependencies, create a new set to add to the current stack */

		dependencyStack.add(new HashSet<>());

		CacheResult<O> loadedResult = new StaticCacheResult<>();
		Set<String> dependencies;
		try
		{
			cacheLoader.load(this, pAccessContext, pKey, loadedResult);
		}
		finally
		{

			/* Pull the dependency set off the stack */

			dependencies = dependencyStack.pop();
			if (placeholderDependencies != null)
			{
				dependencies.addAll(placeholderDependencies);
			}
		}

		/* Now store the result */

		if (loadedResult.entryFound())
		{
			storage.store(pAccessContext, pKey, loadedResult);
		}

		/* Store the dependencies for later tracking */

		if (!dependencies.isEmpty())
		{
			for (String dep : dependencies)
			{
				Set<KeySPI<?>> set = mCacheInfo.dependencyMap.computeIfAbsent(dep, k -> new HashSet<>());
				set.add(pKey);
			}
			mStorageKey.getLastStorage().store(pAccessContext, mStorageKey, new StaticCacheResult<>(mCacheInfo, true));
		}

		/* Return */

		return loadedResult;

	}

	@Override
	public void invalidateAll(AccessContext pAccessContext)
	{
		mCacheStorageByPath.values().stream().distinct().forEach((cs) -> cs.invalidateAll(pAccessContext));
	}

	private <O> void invalidateInternal(AccessContext pAccessContext, KeySPI<O> pKey)
	{
		try (Context ignored = mContextFactory.newContext(CacheEngine.class, this, pKey))
		{

			/*
			 * If there are still defaults, since they need to be resolved. This is done here since some defaults may
			 * require lookups, and we want them included in the dependencies
			 */

			if (pKey.hasPlaceholders())
			{
				@NonNull KeySPI<Object>[] parts = pKey.getParts();
				int partsLen = parts.length;
				@SuppressWarnings({ "null", "unchecked" })
				@NonNull KeySPI<Object>[] newParts = new KeySPI[partsLen];

				for (int i = 0; i < partsLen; i++)
				{
					KeySPI<Object> part = parts[i];
					if (part instanceof KeyPlaceholderSPI)
					{
						@SuppressWarnings("unchecked")
						KeyPlaceholderSPI<Object> sspi = (KeyPlaceholderSPI<Object>) part;
						newParts[i] = sspi.resolveDefault(this, pAccessContext);
					}
					else if (part instanceof AccessContextPlaceholderSPI)
					{
						@SuppressWarnings("unchecked")
						AccessContextPlaceholderSPI<Object> sspi = (AccessContextPlaceholderSPI<Object>) part;
						newParts[i] = sspi.resolve(this, pAccessContext);
					}
					else
					{
						newParts[i] = part;
					}
				}

				pKey = new CompositeKey<>(newParts);
				setupKey(pAccessContext, pKey);
			}

			String keyStr = pKey.toString();

			/* Find the last storage given the key */

			CacheStorage storage = pKey.getLastStorage();

			storage.invalidate(pAccessContext, pKey);

			/* Were there dependencies? */

			Set<KeySPI<?>> depSet = mCacheInfo.dependencyMap.remove(keyStr);
			if (depSet != null)
			{

				/* Save the updated CacheInfo */

				mStorageKey.getLastStorage().store(pAccessContext, mStorageKey, new StaticCacheResult<>(mCacheInfo, true));

				/* Invalidate all the sub keys */

				for (KeySPI<?> dep : depSet)
				{
					invalidate(pAccessContext, dep);
				}
			}
		}
	}

	private <O> void setupKey(@SuppressWarnings("unused") AccessContext pAccessContext, KeySPI<O> pKey)
	{
		KeySPI<Object>[] parts = pKey.getParts();
		StringBuilder sb = new StringBuilder();
		@Nullable CacheStorage lastStorage = null;
		@Nullable String lastSerializerName = null;
		for (KeySPI<Object> part : parts)
		{
			sb.append(part.getBaseKey());

			String currentPath = sb.toString();

			/* Lookup the storage */

			CacheStorage testCacheStorage = mCacheStorageByPath.get(currentPath);
			if (testCacheStorage != null)
			{
				lastStorage = testCacheStorage;
			}

			/* Lookup the serializer */

			String testSerializerName = mSerializerNameByPath.get(currentPath);
			if (testSerializerName != null)
			{
				lastSerializerName = testSerializerName;
			}

			/* Now lookup the loader */

			CacheLoaderInfo<Object> loaderInfo = mLoadersByPath.get(currentPath);

			if ((lastStorage != null) && (lastSerializerName != null) && (loaderInfo != null))
			{
				KeyDetails<Object> keyDetails = new KeyDetails<>(lastStorage, lastSerializerName, loaderInfo.supportsNull, loaderInfo.loader);
				part.storeKeyDetails(keyDetails);
			}

			if (part instanceof StaticAccessContextPlaceholder)
			{
				StaticAccessContextPlaceholder<?> sacp = (StaticAccessContextPlaceholder<?>) part;
				sacp.setAccessContextSPI(mAccessContextSPIMap);
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
	private <K, V> KeySPI<V> resolve(KeySPI<V> pKey, KeyPlaceholder<K> pHolder, String pValue)
	{
		if (!(pHolder instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		@SuppressWarnings("unchecked")
		KeySPI<Object> hi = (KeySPI<Object>) pHolder;
		@NonNull KeySPI<Object>[] parts = pKey.getParts();
		int partsLen = parts.length;
		@SuppressWarnings({ "null", "unchecked" })
		@NonNull KeySPI<Object>[] newParts = new KeySPI[partsLen];

		for (int i = 0; i < partsLen; i++)
		{
			KeySPI<Object> part = parts[i];
			if (part == pHolder)
			{
				newParts[i] = new ResolvedKeyPlaceholder<>(hi, pValue);
			}
			else
			{
				newParts[i] = part;
			}
		}
		return new CompositeKey<>(newParts);
	}

	@Override
	public <V> V get(AccessContext pAccessContext, Key<V> pKey)
	{
		try (Context ctx = mContextFactory.newContext(CacheEngine.class, this, pKey))
		{
			if (!(pKey instanceof KeySPI))
			{
				throw ctx.reportThrowable(new IllegalStateException());
			}
			KeySPI<V> ki = (KeySPI<V>) pKey;
			if (!ki.hasKeyDetails())
			{
				setupKey(pAccessContext, ki);
			}
			CacheResult<V> result = lookup(pAccessContext, ki, true);
			if (result.entryFound())
			{
				if (result.isNull())
				{
					if (ki.supportsNull())
					{
						return ctx.exit(null);
					}
					throw ctx.reportThrowable(new NullPointerException());
				}
				return ctx.exit(result.getValue());
			}
			if (ki.supportsNull())
			{
				return ctx.exit(null);
			}
			throw ctx.reportThrowable(new NoSuchElementException());
		}
	}

	@Override
	public <V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey)
	{
		try (Context ctx = mContextFactory.newContext(CacheEngine.class, this, pKey))
		{
			if (!(pKey instanceof KeySPI))
			{
				throw ctx.reportThrowable(new IllegalStateException());
			}
			KeySPI<V> ki = (KeySPI<V>) pKey;
			if (!ki.hasKeyDetails())
			{
				setupKey(pAccessContext, ki);
			}
			CacheResult<V> result = lookup(pAccessContext, ki, true);
			if (result.entryFound())
			{
				//noinspection ConstantConditions
				return ctx.exit(Optional.ofNullable(result.getValue()));
			}
			return ctx.exit(Optional.empty());
		}
	}

	@Override
	public <K1, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		return get(pAccessContext, resolve(ki, pHolder1, pValue1));
	}

	@Override
	public <K1, K2, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
			KeyPlaceholder<K2> pHolder2, String pValue2)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		return get(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
	}

	@Override
	public <K1, K2, K3, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
			KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		return get(pAccessContext, resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
	}

	@Override
	public <K1, K2, K3, K4, V> V get(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
			KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3, KeyPlaceholder<K4> pHolder4, String pValue4)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		return get(pAccessContext,
				resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4, pValue4));
	}

	@Override
	public <K1, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		return getIfPresent(pAccessContext, resolve(ki, pHolder1, pValue1));
	}

	@Override
	public <K1, K2, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
			KeyPlaceholder<K2> pHolder2, String pValue2)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		return getIfPresent(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
	}

	@Override
	public <K1, K2, K3, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
			KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		return getIfPresent(pAccessContext, resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
	}

	@Override
	public <K1, K2, K3, K4, V> Optional<V> getIfPresent(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1,
			String pValue1, KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3, KeyPlaceholder<K4> pHolder4,
			String pValue4)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		return getIfPresent(pAccessContext,
				resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4, pValue4));
	}

	@Override
	public <V> void invalidate(AccessContext pAccessContext, Key<V> pKey)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		if (!ki.hasKeyDetails())
		{
			setupKey(pAccessContext, ki);
		}
		invalidateInternal(pAccessContext, ki);
	}

	@Override
	public <K1, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		invalidate(pAccessContext, resolve(ki, pHolder1, pValue1));
	}

	@Override
	public <K1, K2, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
			KeyPlaceholder<K2> pHolder2, String pValue2)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		invalidate(pAccessContext, resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2));
	}

	@Override
	public <K1, K2, K3, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
			KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		invalidate(pAccessContext, resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3));
	}

	@Override
	public <K1, K2, K3, K4, V> void invalidate(AccessContext pAccessContext, Key<V> pKey, KeyPlaceholder<K1> pHolder1, String pValue1,
			KeyPlaceholder<K2> pHolder2, String pValue2, KeyPlaceholder<K3> pHolder3, String pValue3, KeyPlaceholder<K4> pHolder4, String pValue4)
	{
		if (!(pKey instanceof KeySPI))
		{
			throw new IllegalStateException();
		}
		KeySPI<V> ki = (KeySPI<V>) pKey;
		invalidate(pAccessContext,
				resolve(resolve(resolve(resolve(ki, pHolder1, pValue1), pHolder2, pValue2), pHolder3, pValue3), pHolder4, pValue4));
	}

	@Override
	public Map<String, CacheLoaderInfo<?>> getCacheLoadersByPath()
	{
		Map<String, CacheLoaderInfo<?>> result = new HashMap<>(mLoadersByPath);
		result.remove(CacheInfoLoader.CACHE_INFO_NAME);
		return result;
	}

	@Override
	public Stream<Map.Entry<Key<?>, CacheResult<?>>> streamEntries(AccessContext pAccessContext)
	{
		return //
				/* Get the distinct list of CacheStorages */
				mCacheStorageByPath.values().stream().distinct() //
						/* Expand each into a stream of string keys */.flatMap((cs) -> cs.streamEntries(pAccessContext));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Collection<Key<?>> dependencies(AccessContext pAccessContext, String pKeyStr)
	{
		return (Collection<Key<?>>) (Collection) mCacheInfo.dependencyMap.get(pKeyStr);
	}
}
