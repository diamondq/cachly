package com.diamondq.cachly.test;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheInvalidator;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.common.types.Types;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Perform tests on expiring keys
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
@MicronautTest
public class TestExpires {

  private static final AtomicInteger sEXPIRED_COUNTER = new AtomicInteger(0);

  private static class Keys {

    private static class Strings {
      public static final String PARTIAL_LOAD_TIMESTAMP = "load-timestamp";
    }

    public static final Key<Long> LOAD_TIMESTAMP = KeyBuilder.of(Strings.PARTIAL_LOAD_TIMESTAMP, Types.LONG);

  }

  /**
   * Loader that will return the current timestamp
   */
  @Singleton
  public static class TimestampLoader implements CacheLoader<Long>, CacheInvalidator<Long> {

    @Override
    public CacheLoaderInfo<Long> getInfo() {
      return new CacheLoaderInfo<>(Keys.LOAD_TIMESTAMP, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<Long> pKey, CacheResult<Long> pResult) {
      //noinspection MagicNumber
      pResult.setValue(System.currentTimeMillis()).setOverrideExpiry(Duration.ofMillis(500));
    }

    @Override
    public void invalidate(Cache pCache, Key<Long> pKey) {
      sEXPIRED_COUNTER.incrementAndGet();
    }
  }

  /**
   * The Cache
   */
  @Inject public Cache cache;

  /**
   * Called before each test
   */
  @BeforeEach
  public void before() {
    cache.invalidateAll(cache.createAccessContext(null));
    sEXPIRED_COUNTER.set(0);
  }

  @Test
  void testExpires() throws InterruptedException {
    AccessContext ac = cache.createAccessContext(null);
    Long firstResult = cache.get(ac, Keys.LOAD_TIMESTAMP);
    Long secondResult = cache.get(ac, Keys.LOAD_TIMESTAMP);
    assertEquals(firstResult, secondResult);
    //noinspection MagicNumber
    Thread.sleep(2000L);
    Long thirdResult = cache.get(ac, Keys.LOAD_TIMESTAMP);
    assertNotEquals(firstResult, thirdResult);
    //noinspection MagicNumber
    Thread.sleep(2000L);
    assertEquals(sEXPIRED_COUNTER.get(), 1);
  }

}
