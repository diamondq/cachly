package com.diamondq.cachly.test;

import static com.tc.util.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.AccessContextPlaceholder;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.spi.AccessContextSPI;
import com.diamondq.common.TypeReference;
import com.diamondq.common.types.Types;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import java.util.Date;

@MicronautTest
public class TestAccessContext {

  public static class Keys {

    private static class Strings {
      public static final String PARTIAL_USERS_NAME = "username";
      public static final String PARTIAL_USERS_DATE = "userdate";

      public static final String PARTIAL_USERS      = "users";
      public static final String PARTIAL_ROOT      = "root";
    }

    private static class LocalTypes {
      public static final TypeReference<Date> DATE = new TypeReference<Date>(){};
    }
    public static final Key<@Nullable Void>              ROOT          =
      KeyBuilder.of(Strings.PARTIAL_ROOT, Types.VOID);

    public static final AccessContextPlaceholder<@Nullable Void> USERS_BY_ID_AC =
      KeyBuilder.accessContext(Strings.PARTIAL_USERS_NAME, Integer.class, Types.VOID);

    public static final Key<@Nullable Void>              USERS          =
            KeyBuilder.from(ROOT, USERS_BY_ID_AC);

    public static final Key<String>                      USER_BY_NAME   = KeyBuilder.from(USERS, KeyBuilder.of(Strings.PARTIAL_USERS_NAME, Types.STRING));

    public static final Key<Date>                      USER_BY_DATE   = KeyBuilder.from(USERS, KeyBuilder.of(Strings.PARTIAL_USERS_DATE, LocalTypes.DATE));

  }

  @Singleton
  public static class IntegerAccessContext implements AccessContextSPI<Integer> {
    @Override
    public Class<Integer> getAccessContextClass() {
      return Integer.class;
    }

    @Override
    public String convertValue(@Nullable Integer pValue, String pAccessKey) {
      if (pValue == null)
        return "(NULL)";
      return String.valueOf(pValue);
    }
  }

  @Singleton
  public static class UserIDLoader implements CacheLoader<String> {

    @Override
    public CacheLoaderInfo<String> getInfo() {
      return new CacheLoaderInfo<>(Keys.USER_BY_NAME, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<String> pKey, CacheResult<String> pResult) {
      Key<@Nullable Void> prevKey = pKey.getPreviousKey(Keys.USERS);
      if (prevKey == null)
        throw new IllegalStateException();
      pResult.setValue(prevKey.getKey());
    }

  }


  @Singleton
  public static class UserDateLoader implements CacheLoader<Date> {

    @Override
    public CacheLoaderInfo<Date> getInfo() {
      return new CacheLoaderInfo<>(Keys.USER_BY_DATE, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<Date> pKey, CacheResult<Date> pResult) {
      pResult.setValue(new Date());
    }
  }

  @Inject
  Cache cache;

  @BeforeEach
  public void before() {
    cache.invalidateAll(cache.createAccessContext(null));
  }

  @Test
  void test() {
    Integer i7 = 7;
    AccessContext ac7 = cache.createAccessContext(null, i7);
    String r = cache.get(ac7, Keys.USER_BY_NAME);
    assertNotNull(r);
    Integer i8 = 8;
    AccessContext ac8 = cache.createAccessContext(null, i8);
    String r2 = cache.get(ac8, Keys.USER_BY_NAME);
    assertNotNull(r2);
    assertNotEquals(r, r2);
  }


  @Test
  void testInvalidation() {
    Integer i7 = 7;
    Integer i8 = 8;
    AccessContext ac7 = cache.createAccessContext(null, i7);
    AccessContext ac8 = cache.createAccessContext(null, i8);
    Date d71 = cache.get(ac7, Keys.USER_BY_DATE);
    assertNotNull(d71);
    Date d81 = cache.get(ac8, Keys.USER_BY_DATE);
    assertNotNull(d81);
    assertNotEquals(d71, d81);

    /* Now get them again, and verify they are same/different */

    Date d72 = cache.get(ac7, Keys.USER_BY_DATE);
    assertNotNull(d72);
    Date d82 = cache.get(ac8, Keys.USER_BY_DATE);
    assertNotNull(d82);
    assertNotEquals(d72, d82);
    assertEquals(d71, d72);
    assertEquals(d81, d82);

    /* Now invalidate and check again */

    cache.invalidate(ac7, Keys.USER_BY_DATE);
    Date d73 = cache.get(ac7, Keys.USER_BY_DATE);
    assertNotNull(d72);
    assertNotEquals(d73, d72);
  }
}
