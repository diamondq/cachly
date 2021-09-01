package com.diamondq.cachly.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.common.types.Types;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
public class TestExpires {

  public static class Keys {

    private static class Strings {
      public static final String PARTIAL_LOAD_TIMESTAMP = "load-timestamp";
    }

    public static final Key<Long> LOAD_TIMESTAMP = KeyBuilder.of(Strings.PARTIAL_LOAD_TIMESTAMP, Types.LONG);

  }

  @Singleton
  public static class TimestampLoader implements CacheLoader<Long> {

    @Override
    public CacheLoaderInfo<Long> getInfo() {
      return new CacheLoaderInfo<>(Keys.LOAD_TIMESTAMP, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<Long> pKey, CacheResult<Long> pResult) {
      pResult.setValue(System.currentTimeMillis()).setOverrideExpiry(Duration.ofMillis(500));
    }

  }

  @Inject
  Cache cache;

  @BeforeEach
  public void before() {
    cache.invalidateAll(cache.createAccessContext(null));
  }

  @Test
  void testExpires() throws InterruptedException {
    AccessContext ac = cache.createAccessContext(null);
    Long firstResult = cache.get(ac, Keys.LOAD_TIMESTAMP);
    Long secondResult = cache.get(ac, Keys.LOAD_TIMESTAMP);
    assertEquals(firstResult, secondResult);
    Thread.sleep(2000L);
    Long thirdResult = cache.get(ac, Keys.LOAD_TIMESTAMP);
    assertNotEquals(firstResult, thirdResult);
  }

}
