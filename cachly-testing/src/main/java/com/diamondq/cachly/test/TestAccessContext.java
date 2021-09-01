package com.diamondq.cachly.test;

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
import com.diamondq.common.types.Types;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
public class TestAccessContext {

  public static class Keys {

    private static class Strings {
      public static final String PARTIAL_USERS_NAME = "username";

      public static final String PARTIAL_USERS      = "users";
    }

    public static final Key<@Nullable Void>              USERS          =
      KeyBuilder.of(Strings.PARTIAL_USERS, Types.VOID);

    public static final AccessContextPlaceholder<String> USERS_BY_ID_AC =
      KeyBuilder.accessContext(Strings.PARTIAL_USERS_NAME, Integer.class, Types.STRING);

    public static final Key<String>                      USER_BY_NAME   = KeyBuilder.from(USERS, USERS_BY_ID_AC);

  }

  @Singleton
  public static class IntegerAccessContext implements AccessContextSPI<Integer> {
    @Override
    public Class<Integer> getAccessContextClass() {
      return Integer.class;
    }

    @Override
    public String convertValue(@Nullable Integer pValue) {
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
      Key<String> prevKey = pKey.getPreviousKey(Keys.USER_BY_NAME);
      if (prevKey == null)
        throw new IllegalStateException();
      pResult.setValue(prevKey.getKey());
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

}
