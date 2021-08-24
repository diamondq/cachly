package com.diamondq.cachly.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.common.types.Types;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
public class TestGetIfPresent {

  public static class Keys {

    private static class Strings {
      public static final String PARTIAL_PROCESS_DEFINITIONS = "ifpresent-process-definitions";

      public static final String PARTIAL_ID                  = "id";
    }

    public static final Key<Map<String, String>> PROCESS_DEFINITIONS =
      KeyBuilder.of(Strings.PARTIAL_PROCESS_DEFINITIONS, Types.MAP_OF_STRING_TO_STRING);

    public static final KeyPlaceholder<String>   PD_BY_ID_PLACE      =
      KeyBuilder.placeholder(Strings.PARTIAL_ID, Types.STRING);

    public static final Key<String>              PD_BY_ID            =
      KeyBuilder.from(PROCESS_DEFINITIONS, PD_BY_ID_PLACE);

  }

  @Singleton
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
    public void load(Cache pCache, Key<Map<String, String>> pKey, CacheResult<Map<String, String>> pResult) {
      pResult.setValue(sMap);
    }

  }

  @Singleton
  public static class PDIDLoader implements CacheLoader<String> {

    @Override
    public CacheLoaderInfo<String> getInfo() {
      return new CacheLoaderInfo<>(Keys.PD_BY_ID, false, "", this);
    }

    @Override
    public void load(Cache pCache, Key<String> pKey, CacheResult<String> pResult) {
      Map<String, String> map = pCache.get(Keys.PROCESS_DEFINITIONS);
      String r = map.get(pKey.getKey());
      pResult.setValue(r);
    }

  }

  @Inject
  Cache cache;

  @BeforeEach
  public void before() {
    cache.invalidateAll();
  }

  @Test
  void test() {
    String r = cache.getIfPresent(Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123").get();
    assertNotNull(r);
    String r2 = cache.getIfPresent(Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123").get();
    assertNotNull(r2);
    assertEquals(r, r2);
    cache.invalidate(Keys.PROCESS_DEFINITIONS);
    String r3 = cache.getIfPresent(Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123").get();
    assertNotNull(r3);
    assertEquals(r, r3);
  }

  @Test
  void keysTest() {
    String emptyKeys = cache.streamKeys().map((k) -> k.toString()).sorted().collect(Collectors.joining(","));
    assertEquals("", emptyKeys);

    /* Grab some entries which will populate the cache */

    cache.getIfPresent(Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123").get();

    String popKeys = cache.streamKeys().map((k) -> k.toString()).sorted().collect(Collectors.joining(","));
    assertEquals("__CacheEngine__,ifpresent-process-definitions,ifpresent-process-definitions/123", popKeys);

  }

  @Test
  void missingTest() {
    Optional<String> opt = cache.getIfPresent(Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "abc");
    assertEquals(false, opt.isPresent());
  }
}
