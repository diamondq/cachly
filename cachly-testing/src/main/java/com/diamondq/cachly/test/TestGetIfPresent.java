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
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ClassNamePrefixedWithPackageName")
@MicronautTest
public class TestGetIfPresent {

  public static class Keys {

    private static class Strings {
      public static final String PARTIAL_PROCESS_DEFINITIONS = "ifpresent-process-definitions";

      public static final String PARTIAL_ID = "id";
    }

    public static final Key<Map<String, String>> PROCESS_DEFINITIONS = KeyBuilder.of(Strings.PARTIAL_PROCESS_DEFINITIONS,
      Types.MAP_OF_STRING_TO_STRING
    );

    public static final KeyPlaceholder<String> PD_BY_ID_PLACE = KeyBuilder.placeholder(Strings.PARTIAL_ID,
      Types.STRING
    );

    public static final Key<String> PD_BY_ID = KeyBuilder.from(PROCESS_DEFINITIONS, PD_BY_ID_PLACE);

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
    public void load(Cache pCache, AccessContext pAccessContext, Key<Map<String, String>> pKey,
      CacheResult<Map<String, String>> pResult) {
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
    public void load(Cache pCache, AccessContext pAccessContext, Key<String> pKey, CacheResult<String> pResult) {
      Map<String, String> map = pCache.get(pAccessContext, Keys.PROCESS_DEFINITIONS);
      String r = map.get(pKey.getKey());
      pResult.setValue(r);
    }

  }

  @Inject public Cache cache;

  @BeforeEach
  public void before() {
    cache.invalidateAll(cache.createAccessContext(null));
  }

  @Test
  void test() {
    AccessContext ac = cache.createAccessContext(null);
    @Nullable String r = cache.getIfPresent(ac, Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123").orElse(null);
    assertNotNull(r);
    @Nullable String r2 = cache.getIfPresent(ac, Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123").orElse(null);
    assertNotNull(r2);
    assertEquals(r, r2);
    cache.invalidate(ac, Keys.PROCESS_DEFINITIONS);
    @Nullable String r3 = cache.getIfPresent(ac, Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123").orElse(null);
    assertNotNull(r3);
    assertEquals(r, r3);
  }

  @Test
  void keysTest() {
    AccessContext ac = cache.createAccessContext(null);
    String emptyKeys = cache.streamEntries(ac)
      .map((entry) -> entry.getKey().toString())
      .sorted()
      .collect(Collectors.joining(","));
    assertEquals("", emptyKeys);

    /* Grab some entries which will populate the cache */

    cache.getIfPresent(ac, Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "123").orElseThrow(IllegalArgumentException::new);

    String popKeys = cache.streamEntries(ac)
      .map((entry) -> entry.getKey().toString())
      .sorted()
      .collect(Collectors.joining(","));
    //noinspection HardcodedFileSeparator
    assertEquals("__CacheEngine__,ifpresent-process-definitions,ifpresent-process-definitions/123", popKeys);

  }

  @Test
  void missingTest() {
    AccessContext ac = cache.createAccessContext(null);
    Optional<String> opt = cache.getIfPresent(ac, Keys.PD_BY_ID, Keys.PD_BY_ID_PLACE, "abc");
    assertFalse(opt.isPresent());
  }
}
