package com.diamondq.cachly.test;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.common.types.Types;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the Callback functionality
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
@MicronautTest
public class TestCallbacks {

  /**
   * Cache Keys
   */
  public static class Keys {

    private static class Strings {
      public static final String PARTIAL_ROOT = "callbacks";

      public static final String PARTIAL_ID = "id";
    }

    /**
     * Key that represents the Map of strings
     */
    public static final Key<Map<String, String>> STRING_MAP = KeyBuilder.of(Strings.PARTIAL_ROOT,
      Types.MAP_OF_STRING_TO_STRING
    );

    /**
     * Placeholder for the specific key to retrieve from the map
     */
    public static final KeyPlaceholder<String> KEY_PLACEHOLDER = KeyBuilder.placeholder(Strings.PARTIAL_ID,
      Types.STRING
    );

    /**
     * Key that represents a single string from the map
     */
    public static final Key<String> STRING_FROM_MAP = KeyBuilder.from(STRING_MAP, KEY_PLACEHOLDER);

  }

  /**
   * Cache loader to retrieve the Map of strings
   */
  @Singleton
  public static class MapStringLoader implements CacheLoader<Map<String, String>> {

    private static final Map<String, String> sMap;

    static {
      sMap = new HashMap<>();
      sMap.put("123", "ProcessDef123");
    }

    @Override
    public CacheLoaderInfo<Map<String, String>> getInfo() {
      return new CacheLoaderInfo<>(Keys.STRING_MAP, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<Map<String, String>> pKey,
      CacheResult<Map<String, String>> pResult) {
      pResult.setValue(sMap);
    }

  }

  /**
   * Cache loader that retrieves a single key from the map
   */
  @Singleton
  public static class KeyFromMapLoader implements CacheLoader<String> {

    @Override
    public CacheLoaderInfo<String> getInfo() {
      return new CacheLoaderInfo<>(Keys.STRING_FROM_MAP, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<String> pKey, CacheResult<String> pResult) {
      Map<String, String> map = pCache.get(pAccessContext, Keys.STRING_MAP);
      String r = map.get(pKey.getKey());
      pResult.setValue(r);
    }
  }

  /**
   * Cachly Cache
   */
  @Inject public Cache cache;

  /**
   * Runs before each test
   */
  @BeforeEach
  public void before() {
    cache.invalidateAll(cache.createAccessContext(null));
  }

  @Test
  void test() {
    AccessContext ac = cache.createAccessContext(null);
    AtomicReference<StringBuilder> builder = new AtomicReference<>(new StringBuilder());
    cache.registerOnChange(ac,
      Keys.STRING_MAP,
      (key, event, value) -> builder.get()
        .append("|")
        .append(key.toString())
        .append("|")
        .append(value.map(Object::toString).orElse("(null)"))
        .append("|\n")
    );
    var resolvedKey = cache.resolve(Keys.STRING_FROM_MAP, Keys.KEY_PLACEHOLDER, "123");
    cache.registerOnChange(ac,
      resolvedKey,
      (key, event, value) -> builder.get()
        .append("|")
        .append(key.toString())
        .append("|")
        .append(value.map(Object::toString).orElse("(null)"))
        .append("|\n")
    );

    /* Verify that we got calling during registration */

    assertEquals("|callbacks|{123=ProcessDef123}|\n|callbacks/123|ProcessDef123|\n",
      builder.get().toString(),
      "Initialization failed"
    );
    builder.set(new StringBuilder());

    /* Now get a value */

    cache.get(ac, Keys.STRING_FROM_MAP, Keys.KEY_PLACEHOLDER, "123");
    assertEquals("", builder.get().toString(), "Retrieval failed");
    builder.set(new StringBuilder());

    /* Get a value a second time */

    cache.get(ac, Keys.STRING_FROM_MAP, Keys.KEY_PLACEHOLDER, "123");
    assertEquals("", builder.get().toString(), "Second Retrieval failed");
    builder.set(new StringBuilder());

    /* Now set a value */

    cache.set(ac, Keys.STRING_FROM_MAP, Keys.KEY_PLACEHOLDER, "123", "NewValue");
    assertEquals("|callbacks/123|NewValue|\n", builder.get().toString(), "Setting failed");
    builder.set(new StringBuilder());

    /* Now set the same value */

    cache.set(ac, Keys.STRING_FROM_MAP, Keys.KEY_PLACEHOLDER, "123", "NewValue");
    assertEquals("", builder.get().toString(), "Same Setting failed");
    builder.set(new StringBuilder());

    /* Now invalidate */

    cache.invalidate(ac, Keys.STRING_MAP);
    assertEquals("|callbacks/123|ProcessDef123|\n", builder.get().toString(), "Invalidation failed");
    builder.set(new StringBuilder());
  }

}
