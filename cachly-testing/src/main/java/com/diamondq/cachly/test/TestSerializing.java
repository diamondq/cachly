package com.diamondq.cachly.test;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.common.TypeReference;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("ClassNamePrefixedWithPackageName")
@MicronautTest
public class TestSerializing {

  public static class SerializeTest {
    public final String string;

    public final int integer;

    public SerializeTest(String pString, int pInteger) {
      string = pString;
      integer = pInteger;
    }
  }

  public static class Keys {

    private static class LocalStrings {
      public static final String PARTIAL_MAP = "map";

      public static final String PARTIAL_ID = "id";
    }

    private static class LocalTypes {
      public static final TypeReference<@Nullable SerializeTest> NULLABLE_SERIALIZE_TEST = new TypeReference<@Nullable SerializeTest>() {                                                                                                                                                                                                              // type
        // reference
      };

      public static final TypeReference<Map<String, SerializeTest>> MAP_OF_STRING_TO_SERIALIZE_TEST = new TypeReference<Map<String, SerializeTest>>() {                                                                                                                                                                                                           // type
        // reference
      };
    }

    public static final Key<Map<String, SerializeTest>> MAP = KeyBuilder.of(LocalStrings.PARTIAL_MAP,
      LocalTypes.MAP_OF_STRING_TO_SERIALIZE_TEST
    );

    public static final KeyPlaceholder<@Nullable SerializeTest> MAP_BY_ID_PLACEHOLDER = KeyBuilder.placeholder(
      LocalStrings.PARTIAL_ID,
      LocalTypes.NULLABLE_SERIALIZE_TEST
    );

    public static final Key<@Nullable SerializeTest> MAP_BY_ID = KeyBuilder.from(MAP, MAP_BY_ID_PLACEHOLDER);
  }

  @Singleton
  public static class MapLoader implements CacheLoader<Map<String, SerializeTest>> {

    private static final Map<String, SerializeTest> sMap;

    static {
      sMap = new HashMap<>();
      //noinspection MagicNumber
      sMap.put("abc", new SerializeTest("abc", 123));
    }

    @Override
    public CacheLoaderInfo<Map<String, SerializeTest>> getInfo() {
      return new CacheLoaderInfo<>(Keys.MAP, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<Map<String, SerializeTest>> pKey,
      CacheResult<Map<String, SerializeTest>> pResult) {
      pResult.setValue(sMap);
    }
  }

  @Singleton
  public static class SerializeTestLoader implements CacheLoader<@Nullable SerializeTest> {
    @Override
    public CacheLoaderInfo<@Nullable SerializeTest> getInfo() {
      return new CacheLoaderInfo<>(Keys.MAP_BY_ID, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<@Nullable SerializeTest> pKey,
      CacheResult<@Nullable SerializeTest> pResult) {
      Map<String, SerializeTest> map = pCache.get(pAccessContext, Keys.MAP);
      SerializeTest serializeTest = map.get(pKey.getKey());
      pResult.setValue(serializeTest);
    }
  }

  @Inject public Cache cache;

  @BeforeEach
  public void before() {
    cache.invalidateAll(cache.createAccessContext(null));
  }

  @Test
  void testCache() {
    AccessContext ac = cache.createAccessContext(null);
    SerializeTest test = cache.get(ac, Keys.MAP_BY_ID, Keys.MAP_BY_ID_PLACEHOLDER, "abc");
    assertNotNull(test);
  }

}
