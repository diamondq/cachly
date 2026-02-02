package com.diamondq.cachly.test;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.engine.CacheEngine;
import com.diamondq.cachly.engine.CachlyPathConfiguration;
import com.diamondq.cachly.engine.MemoryCacheStorage;
import com.diamondq.cachly.impl.CacheCallbackHandler;
import com.diamondq.cachly.spi.BeanNameableLocator;
import com.diamondq.common.context.impl.ContextFactorySetup;
import com.diamondq.common.converters.impl.ConverterManagerImpl;
import com.diamondq.common.types.Types;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the get method without CDI
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
public class TestGetNoCDI {

  /**
   * The keys
   */
  public static class Keys {

    private static class Strings {
      public static final String PARTIAL_PROCESS_DEFINITIONS = "process-definitions";

      public static final String PARTIAL_ID = "id";
    }

    /**
     * The process definitions
     */
    public static final Key<Map<String, String>> PROCESS_DEFINITIONS = KeyBuilder.of(Strings.PARTIAL_PROCESS_DEFINITIONS,
      Types.MAP_OF_STRING_TO_STRING
    );

    /**
     * The placeholder for the id
     */
    public static final KeyPlaceholder<@Nullable String> PD_BY_ID_PLACE = KeyBuilder.placeholder(Strings.PARTIAL_ID,
      Types.STRING
    );

    /**
     * The key for the id
     */
    public static final Key<@Nullable String> PD_BY_ID = KeyBuilder.from(PROCESS_DEFINITIONS, PD_BY_ID_PLACE);

  }

  /**
   * A cache loader
   */
  public static class PDLoader implements CacheLoader<Map<String, String>> {

    private static final Map<String, String> sMap;

    static {
      sMap = new HashMap<>();
      sMap.put("123", "ProcessDef123");
    }

    @Override
    public CacheLoaderInfo<Map<String, String>> getInfo() {
      return new CacheLoaderInfo<>(Keys.PROCESS_DEFINITIONS, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<Map<String, String>> pKey,
      CacheResult<Map<String, String>> pResult) {
      pResult.setValue(sMap);
    }

  }

  /**
   * A cache loader
   */
  public static class PDIDLoader implements CacheLoader<String> {

    @Override
    public CacheLoaderInfo<String> getInfo() {
      return new CacheLoaderInfo<>(Keys.PD_BY_ID, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<String> pKey, CacheResult<String> pResult) {
      @SuppressWarnings("unused") Map<String, String> map = pCache.get(pAccessContext, Keys.PROCESS_DEFINITIONS);
      // String r = map.get(pKey.getKey());
      pResult.setValue(String.valueOf(System.currentTimeMillis()));
    }

  }

  private volatile Cache cache;

  /**
   * Creates the cache engine for testing
   */
  @BeforeEach
  public void before() {
    var contextFactory = ContextFactorySetup.setup();
    var converterManager = new ConverterManagerImpl(List.of());
    var executorService = Executors.newSingleThreadExecutor();
    var callbackHandler = new CacheCallbackHandler();
    Keys.PROCESS_DEFINITIONS.clearKeyDetails();
    Keys.PD_BY_ID_PLACE.clearKeyDetails();
    Keys.PD_BY_ID.clearKeyDetails();
    cache = new CacheEngine(callbackHandler, executorService, converterManager, contextFactory);
    cache.addBeanNameLocator(new BeanNameableLocator());
    cache.addCacheStorage(new MemoryCacheStorage(converterManager, executorService, callbackHandler, "my-cache"));
    var pathConfig = new CachlyPathConfiguration(Keys.Strings.PARTIAL_PROCESS_DEFINITIONS);
    pathConfig.setStorage("my-cache");
    pathConfig.setSerializer("kryo");
    cache.addPathConfiguration(pathConfig);
    cache.addCacheLoader(new PDLoader());
    cache.addCacheLoader(new PDIDLoader());
    cache.activate();
  }

  @Test
  void test() {
    AccessContext ac = cache.createAccessContext(null);
    String r = cache.<@Nullable String, @Nullable String>get(ac, Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123");
    assertNotNull(r);
    final Collection<String> deps = cache.getDependentOnKeys(ac,
      cache.resolve(Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123").toString()
    );
    assertNotNull(deps);
    String r2 = cache.get(ac, Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123");
    assertNotNull(r2);
    assertEquals(r, r2);
    cache.invalidate(ac, Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123");
    synchronized (this) {
      try {
        //noinspection MagicNumber
        wait(3000L);
      }
      catch (InterruptedException ignored) {
      }
    }
    String r3 = cache.get(ac, Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123");
    assertNotNull(r3);
    assertNotEquals(r, r3);
  }

  @Test
  void keysTest() {
    AccessContext ac = cache.createAccessContext(null);
    String emptyKeys = cache.streamEntries(ac)
      .map((entry) -> entry.getKey().toString())
      .sorted()
      .collect(Collectors.joining(","));
    assertEquals("", emptyKeys);

    /* Grab some entries that will populate the cache */

    var result = cache.get(ac, Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123");
    assertNotNull(result);

    String popKeys = cache.streamEntries(ac)
      .map((entry) -> entry.getKey().toString())
      .sorted()
      .collect(Collectors.joining(","));
    //noinspection HardcodedFileSeparator
    assertEquals("__CacheEngine__,process-definitions,process-definitions/123", popKeys);

  }

}
