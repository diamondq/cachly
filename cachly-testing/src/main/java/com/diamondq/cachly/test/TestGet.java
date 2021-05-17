package com.diamondq.cachly.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.diamondq.cachly.AbstractCacheLoader;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.CommonTypeReferences;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.ROOT;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
public class TestGet {

  public static class KEYS {

    public static final Key<ROOT, Map<String, String>>                      PROCESS_DEFINITIONS =
      KeyBuilder.of("processDefinitions", CommonTypeReferences.MAP_STRING_TO_STRING);

    public static final KeyPlaceholder<Map<String, String>, String, String> PD_BY_ID_PLACE      =
      KeyBuilder.placeholder("id", CommonTypeReferences.STRING);

    public static final Key<Map<String, String>, String>                    PD_BY_ID            =
      KeyBuilder.from(PROCESS_DEFINITIONS, PD_BY_ID_PLACE);

  }

  @Singleton
  public static class PDLoader extends AbstractCacheLoader<ROOT, Map<String, String>> {

    private static final Map<String, String> sMap;

    static {
      sMap = new HashMap<>();
      sMap.put("123", "ProcessDef123");
    }

    @Inject
    public PDLoader() {
      super(false, KEYS.PROCESS_DEFINITIONS);
    }

    @Override
    public CacheResult<Map<String, String>> load(Cache pCache, Key<ROOT, Map<String, String>> pKey) {
      return new CacheResult<>(sMap, true);
    }

  }

  @Singleton
  public static class PDIDLoader extends AbstractCacheLoader<Map<String, String>, String> {

    @Inject
    public PDIDLoader() {
      super(false, KEYS.PD_BY_ID);
    }

    @Override
    public CacheResult<String> load(Cache pCache, Key<Map<String, String>, String> pKey) {
      @SuppressWarnings("unused")
      Map<String, String> map = pCache.get(KEYS.PROCESS_DEFINITIONS);
      // String r = map.get(pKey.getKey());
      return new CacheResult<>(String.valueOf(System.currentTimeMillis()), true);
    }

  }

  @Inject
  Cache cache;

  @Test
  void test() {
    String r = cache.get(KEYS.PD_BY_ID, KEYS.PD_BY_ID_PLACE, "123");
    assertNotNull(r);
    String r2 = cache.get(KEYS.PD_BY_ID, KEYS.PD_BY_ID_PLACE, "123");
    assertNotNull(r2);
    assertEquals(r, r2);
    cache.invalidate(KEYS.PROCESS_DEFINITIONS);
    String r3 = cache.get(KEYS.PD_BY_ID, KEYS.PD_BY_ID_PLACE, "123");
    assertNotNull(r3);
    assertNotEquals(r, r3);
  }

}
