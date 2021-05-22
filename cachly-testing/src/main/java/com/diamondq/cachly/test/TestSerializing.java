package com.diamondq.cachly.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.common.TypeReference;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
public class TestSerializing {

  public static class SerializeTest {
    public final String string;

    public final int    integer;

    public SerializeTest(String pString, int pInteger) {
      string = pString;
      integer = pInteger;
    }
  }

  public static class Keys {

    private static class LocalStrings {
      public static final String PARTIAL_MAP = "map";

      public static final String PARTIAL_ID  = "id";
    }

    private static class LocalTypes {
      public static final TypeReference<@Nullable SerializeTest>    NULLABLE_SERIALIZE_TEST         =
        new TypeReference<@Nullable SerializeTest>() {
                                                                                                      };

      public static final TypeReference<Map<String, SerializeTest>> MAP_OF_STRING_TO_SERIALIZE_TEST =
        new TypeReference<Map<String, SerializeTest>>() {
                                                                                                      };
    }

    public static final Key<Map<String, SerializeTest>>         MAP                   =
      KeyBuilder.of(LocalStrings.PARTIAL_MAP, LocalTypes.MAP_OF_STRING_TO_SERIALIZE_TEST);

    public static final KeyPlaceholder<@Nullable SerializeTest> MAP_BY_ID_PLACEHOLDER =
      KeyBuilder.placeholder(LocalStrings.PARTIAL_ID, LocalTypes.NULLABLE_SERIALIZE_TEST);

    public static final Key<@Nullable SerializeTest>            MAP_BY_ID             =
      KeyBuilder.from(MAP, MAP_BY_ID_PLACEHOLDER);
  }

  @Singleton
  public static class MapLoader implements CacheLoader<Map<String, SerializeTest>> {

    private static final Map<String, SerializeTest> sMap;
    static {
      sMap = new HashMap<>();
      sMap.put("abc", new SerializeTest("abc", 123));
    }

    @Override
    public CacheLoaderInfo<Map<String, SerializeTest>> getInfo() {
      return new CacheLoaderInfo<>(Keys.MAP, false, "", this);
    }

    @Override
    public CacheResult<Map<String, SerializeTest>> load(Cache pCache, Key<Map<String, SerializeTest>> pKey) {
      return new CacheResult<>(sMap, true);
    }
  }

  @Singleton
  public static class SerializeTestLoader implements CacheLoader<@Nullable SerializeTest> {
    @Override
    public CacheLoaderInfo<@Nullable SerializeTest> getInfo() {
      return new CacheLoaderInfo<>(Keys.MAP_BY_ID, false, "", this);
    }

    @Override
    public CacheResult<@Nullable SerializeTest> load(Cache pCache, Key<@Nullable SerializeTest> pKey) {
      Map<String, SerializeTest> map = pCache.get(Keys.MAP);
      SerializeTest serializeTest = map.get(pKey.getKey());
      return new CacheResult<>(serializeTest, serializeTest != null);
    }
  }

  @Inject
  Cache cache;

  @BeforeEach
  public void before() {
    cache.invalidateAll();
  }

  @Test
  void testCache() throws InterruptedException {
    SerializeTest test = cache.get(Keys.MAP_BY_ID, Keys.MAP_BY_ID_PLACEHOLDER, "abc");
    assertNotNull(test);
  }

}