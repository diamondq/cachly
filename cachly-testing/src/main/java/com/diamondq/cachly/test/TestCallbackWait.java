package com.diamondq.cachly.test;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.impl.CacheCallbackHandler;
import com.diamondq.cachly.impl.CacheEngineImpl;
import com.diamondq.cachly.impl.MemoryCacheStorage;
import com.diamondq.cachly.spi.BeanNameableLocator;
import com.diamondq.cachly.spi.CacheEngine;
import com.diamondq.cachly.spi.CachlyPathConfiguration;
import com.diamondq.common.context.impl.ContextFactorySetup;
import com.diamondq.common.converters.impl.ConverterManagerImpl;
import com.diamondq.common.types.Types;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that a writer (set/invalidate) of a key that has callbacks registered is not left waiting forever for the
 * callbacks to run.
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
public class TestCallbackWait {

  /**
   * Fails the test if the code takes longer than this. It's much shorter than the default callback timeout, so a writer
   * that is left waiting is noticed.
   */
  private static final Duration LIMIT = Duration.ofSeconds(10);

  /**
   * The keys
   */
  public static class Keys {

    /**
     * The key that has a value
     */
    public static final Key<String> PRESENT = KeyBuilder.of("cbwait-present", Types.STRING);

    /**
     * The key that never has a value
     */
    public static final Key<String> ABSENT = KeyBuilder.of("cbwait-absent", Types.STRING);
  }

  /**
   * Loads a value
   */
  public static class PresentLoader implements CacheLoader<String> {

    @Override
    public CacheLoaderInfo<String> getInfo() {
      return new CacheLoaderInfo<>(Keys.PRESENT, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<String> pKey, CacheResult<String> pResult) {
      pResult.setValue("loaded");
    }
  }

  /**
   * Never finds a value
   */
  public static class AbsentLoader implements CacheLoader<String> {

    @Override
    public CacheLoaderInfo<String> getInfo() {
      return new CacheLoaderInfo<>(Keys.ABSENT, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<String> pKey, CacheResult<String> pResult) {
      // leave the result as not found
    }
  }

  private volatile CacheEngine mCache;

  private volatile MemoryCacheStorage mStorage;

  private volatile ExecutorService mExecutor;

  /**
   * Creates the cache engine for testing
   */
  @BeforeEach
  public void before() {
    var contextFactory = ContextFactorySetup.setup();
    var converterManager = new ConverterManagerImpl(List.of());
    mExecutor = Executors.newFixedThreadPool(4);
    var callbackHandler = new CacheCallbackHandler();
    Keys.PRESENT.clearKeyDetails();
    Keys.ABSENT.clearKeyDetails();
    mCache = new CacheEngineImpl(callbackHandler, mExecutor, converterManager, contextFactory);
    mCache.addBeanNameLocator(new BeanNameableLocator());
    mStorage = new MemoryCacheStorage(contextFactory, converterManager, mExecutor, callbackHandler, "my-cache");
    mCache.addCacheStorage(mStorage);
    for (String path : List.of("cbwait-present", "cbwait-absent")) {
      var pathConfig = new CachlyPathConfiguration(path);
      pathConfig.setStorage("my-cache");
      pathConfig.setSerializer("kryo");
      mCache.addPathConfiguration(pathConfig);
    }
    mCache.addCacheLoader(new PresentLoader());
    mCache.addCacheLoader(new AbsentLoader());
    mCache.activate();
  }

  /**
   * Stops the executor
   */
  @AfterEach
  public void after() {
    mExecutor.shutdownNow();
  }

  /**
   * Invalidating a key that isn't in the cache produces no event, so the writer must not wait for one
   */
  @Test
  void invalidateAbsentKeyWithCallbackReturns() {
    AccessContext ac = mCache.createAccessContext(null);
    mCache.registerOnChange(ac, Keys.ABSENT, (key, event, value) -> {
    });

    assertTimeoutPreemptively(LIMIT, () -> mCache.invalidate(ac, Keys.ABSENT));
  }

  /**
   * A callback that throws must not leave the writer waiting, and must not stop the other callbacks from running
   */
  @Test
  void throwingCallbackDoesNotBlockWriter() {
    AccessContext ac = mCache.createAccessContext(null);
    AtomicBoolean armed = new AtomicBoolean();
    AtomicInteger secondCalls = new AtomicInteger();
    mCache.registerOnChange(ac, Keys.PRESENT, (key, event, value) -> {
      if (armed.get()) throw new IllegalStateException("Expected failure from the test");
    });
    mCache.registerOnChange(ac, Keys.PRESENT, (key, event, value) -> secondCalls.incrementAndGet());
    int before = secondCalls.get();
    armed.set(true);

    assertTimeoutPreemptively(LIMIT, () -> mCache.set(ac, Keys.PRESENT, "changed"));

    assertEquals(before + 1, secondCalls.get(), "The callback after the failing one was not called");
  }

  /**
   * Registering another callback while the callbacks of a key are running must not break the run or the writer
   */
  @Test
  void callbackRegisteredDuringRunDoesNotBreakWriter() {
    AccessContext ac = mCache.createAccessContext(null);
    AtomicBoolean armed = new AtomicBoolean();
    AtomicInteger addedCalls = new AtomicInteger();
    mCache.registerOnChange(ac, Keys.PRESENT, (key, event, value) -> {
      if (armed.compareAndSet(true, false)) {
        mCache.registerOnChange(ac, Keys.PRESENT, (key2, event2, value2) -> addedCalls.incrementAndGet());
      }
    });
    armed.set(true);

    assertTimeoutPreemptively(LIMIT, () -> mCache.set(ac, Keys.PRESENT, "changed"));

    /* The callback that was added is part of the next run */

    assertTimeoutPreemptively(LIMIT, () -> mCache.set(ac, Keys.PRESENT, "changed again"));
    assertTrue(addedCalls.get() >= 1, "The callback registered during the run was never called");
  }

  /**
   * If the callbacks don't finish, the writer gives up after the timeout instead of waiting forever
   */
  @Test
  void writerGivesUpAfterTimeout() throws Exception {
    AccessContext ac = mCache.createAccessContext(null);
    AtomicBoolean armed = new AtomicBoolean();
    CountDownLatch release = new CountDownLatch(1);
    mCache.registerOnChange(ac, Keys.PRESENT, (key, event, value) -> {
      if (armed.get()) {
        try {
          release.await(LIMIT.toSeconds(), TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    });
    mStorage.setCallbackTimeout(Duration.ofMillis(500));
    armed.set(true);

    long start = System.nanoTime();
    try {
      assertTimeoutPreemptively(LIMIT, () -> mCache.set(ac, Keys.PRESENT, "changed"));
    }
    finally {
      release.countDown();
    }
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    assertTrue(elapsedMillis >= 400, "The writer did not wait for the callbacks: " + elapsedMillis + "ms");
  }

}
