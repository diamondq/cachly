package com.diamondq.cachly.test;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderInfo;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.cachly.impl.CacheCallbackHandler;
import com.diamondq.cachly.impl.CacheEngineImpl;
import com.diamondq.cachly.impl.MemoryCacheStorage;
import com.diamondq.cachly.spi.BeanNameableLocator;
import com.diamondq.cachly.spi.CacheEngine;
import com.diamondq.cachly.spi.CachlyPathConfiguration;
import com.diamondq.common.context.impl.ContextFactorySetup;
import com.diamondq.common.converters.impl.ConverterManagerImpl;
import com.diamondq.common.types.Types;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that a value is not left stale in the cache when one of the keys that it was loaded from is invalidated while
 * the load is still running.
 * <p>
 * The dependencies of a key are only recorded once its load has finished, so an invalidation of a dependency that
 * arrives mid-load does not reach the key that is being loaded. The loader then finishes and stores a value that was
 * built from the old data.
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
public class TestInvalidateDuringLoad {

  private static final long TIMEOUT_SECONDS = 30L;

  /**
   * The keys
   */
  public static class Keys {

    private static class Strings {
      public static final String PARTIAL_SOURCE = "race-source";

      public static final String PARTIAL_ID = "id";
    }

    /**
     * The key for the underlying data
     */
    public static final Key<String> SOURCE = KeyBuilder.of(Strings.PARTIAL_SOURCE, Types.STRING);

    /**
     * The placeholder for the derived id
     */
    public static final KeyPlaceholder<@Nullable String> DERIVED_PLACE = KeyBuilder.placeholder(Strings.PARTIAL_ID,
      Types.STRING
    );

    /**
     * The key for the value that is derived from the underlying data
     */
    public static final Key<@Nullable String> DERIVED = KeyBuilder.from(SOURCE, DERIVED_PLACE);

    /**
     * The placeholder for the value that is built from the derived value
     */
    public static final KeyPlaceholder<@Nullable String> TOP_PLACE = KeyBuilder.placeholder("top-id", Types.STRING);

    /**
     * The key for the value that is built from the derived value
     */
    public static final Key<@Nullable String> TOP = KeyBuilder.from(DERIVED, TOP_PLACE);

  }

  /**
   * Loads a value that is built from the derived value. The data is only indirectly used by this loader.
   */
  public static class TopLoader implements CacheLoader<String> {

    @Override
    public CacheLoaderInfo<String> getInfo() {
      return new CacheLoaderInfo<>(Keys.TOP, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<String> pKey, CacheResult<String> pResult) {
      String derived = pCache.get(pAccessContext, Keys.DERIVED, Keys.DERIVED_PLACE, "x");
      pResult.setValue("top-of-" + derived);
    }
  }

  /**
   * Loads the current value of the underlying data
   */
  public static class SourceLoader implements CacheLoader<String> {

    private final AtomicReference<String> mSource;

    /**
     * Constructor
     *
     * @param pSource the underlying data
     */
    public SourceLoader(AtomicReference<String> pSource) {
      mSource = pSource;
    }

    @Override
    public CacheLoaderInfo<String> getInfo() {
      return new CacheLoaderInfo<>(Keys.SOURCE, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<String> pKey, CacheResult<String> pResult) {
      pResult.setValue(mSource.get());
    }
  }

  /**
   * Loads a value derived from the underlying data. After reading the data, it waits on a gate, which lets the test
   * invalidate the data in the middle of the load.
   */
  public static class DerivedLoader implements CacheLoader<String> {

    private final CountDownLatch mReadSource = new CountDownLatch(1);

    private final CountDownLatch mGate;

    /**
     * Constructor
     *
     * @param pGate the gate that the load waits on after reading the data
     */
    public DerivedLoader(CountDownLatch pGate) {
      mGate = pGate;
    }

    /**
     * Returns the latch that is released once the load has read the data
     *
     * @return the latch
     */
    public CountDownLatch getReadSource() {
      return mReadSource;
    }

    @Override
    public CacheLoaderInfo<String> getInfo() {
      return new CacheLoaderInfo<>(Keys.DERIVED, false, "", this);
    }

    @Override
    public void load(Cache pCache, AccessContext pAccessContext, Key<String> pKey, CacheResult<String> pResult) {
      String source = pCache.get(pAccessContext, Keys.SOURCE);
      mReadSource.countDown();
      try {
        if (!mGate.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) throw new IllegalStateException("Gate timed out");
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(ex);
      }
      pResult.setValue("derived-from-" + source);
    }
  }

  private final AtomicReference<String> mSource = new AtomicReference<>("old");

  private final CountDownLatch mGate = new CountDownLatch(1);

  private final DerivedLoader mDerivedLoader = new DerivedLoader(mGate);

  private volatile CacheEngine mCache;

  private volatile ExecutorService mLoadThread;

  /**
   * Creates the cache engine for testing
   */
  @BeforeEach
  public void before() {
    var contextFactory = ContextFactorySetup.setup();
    var converterManager = new ConverterManagerImpl(List.of());
    var executorService = Executors.newSingleThreadExecutor();
    var callbackHandler = new CacheCallbackHandler();
    Keys.SOURCE.clearKeyDetails();
    Keys.DERIVED_PLACE.clearKeyDetails();
    Keys.DERIVED.clearKeyDetails();
    Keys.TOP_PLACE.clearKeyDetails();
    Keys.TOP.clearKeyDetails();
    mCache =new CacheEngineImpl(callbackHandler, executorService, converterManager, contextFactory);
    mCache.addBeanNameLocator(new BeanNameableLocator());
    mCache.addCacheStorage(new MemoryCacheStorage(contextFactory, converterManager, executorService, callbackHandler, "my-cache"));
    var pathConfig = new CachlyPathConfiguration(Keys.Strings.PARTIAL_SOURCE);
    pathConfig.setStorage("my-cache");
    pathConfig.setSerializer("kryo");
    mCache.addPathConfiguration(pathConfig);
    mCache.addCacheLoader(new SourceLoader(mSource));
    mCache.addCacheLoader(mDerivedLoader);
    mCache.addCacheLoader(new TopLoader());
    mCache.activate();
    mLoadThread = Executors.newSingleThreadExecutor();
  }

  /**
   * Releases the gate and stops the loading thread
   */
  @AfterEach
  public void after() {
    mGate.countDown();
    mLoadThread.shutdownNow();
  }

  /**
   * Control: if the data is invalidated after the derived value has been stored, the derived value is invalidated
   * with it. This passes today, and shows that the race test below is not failing because of the setup.
   */
  @Test
  void invalidateAfterLoadInvalidatesDerivedValue() {
    AccessContext ac = mCache.createAccessContext(null);
    mGate.countDown();

    assertEquals("derived-from-old", mCache.get(ac, Keys.DERIVED, Keys.DERIVED_PLACE, "x"));

    mSource.set("new");
    mCache.invalidate(ac, Keys.SOURCE);

    assertEquals("derived-from-new", mCache.get(ac, Keys.DERIVED, Keys.DERIVED_PLACE, "x"));
  }

  /**
   * If the data is invalidated while the derived value is being loaded, the derived value that is left in the cache
   * must not be the one that was built from the old data.
   */
  @Test
  void invalidateDuringLoadDoesNotLeaveStaleValue() throws Exception {
    AccessContext ac = mCache.createAccessContext(null);

    /* Start the load on another thread. It reads the (old) data and then waits at the gate */

    Future<String> loading = mLoadThread.submit(() -> mCache.<@Nullable String, @Nullable String>get(ac,
      Keys.DERIVED,
      Keys.DERIVED_PLACE,
      "x"
    ));
    assertTrue(mDerivedLoader.getReadSource().await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "The load never started");

    /* Change the data and invalidate it, while the load is still in progress */

    mSource.set("new");
    mCache.invalidate(ac, Keys.SOURCE);

    /* Let the load finish. The caller that was already waiting may see a value built from the old data */

    mGate.countDown();
    assertEquals("derived-from-old", loading.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

    /* But the cache must not keep it. A new lookup must see the new data */

    assertEquals("derived-from-new",
      mCache.get(ac, Keys.DERIVED, Keys.DERIVED_PLACE, "x"),
      "The cache kept a value that was built from data that was invalidated during the load"
    );
  }

  /**
   * The same as above, but the value that was invalidated during the load is read by a second load, which itself must
   * not keep a value that was built from it.
   */
  @Test
  void invalidateDuringNestedLoadDoesNotLeaveStaleValue() throws Exception {
    AccessContext ac = mCache.createAccessContext(null);

    Future<String> loading = mLoadThread.submit(() -> mCache.<@Nullable String, @Nullable String, @Nullable String>get(
      ac,
      Keys.TOP,
      Keys.DERIVED_PLACE,
      "x",
      Keys.TOP_PLACE,
      "y"
    ));
    assertTrue(mDerivedLoader.getReadSource().await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "The load never started");

    mSource.set("new");
    mCache.invalidate(ac, Keys.SOURCE);

    mGate.countDown();
    assertEquals("top-of-derived-from-old", loading.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

    assertEquals("top-of-derived-from-new",
      mCache.get(ac, Keys.TOP, Keys.DERIVED_PLACE, "x", Keys.TOP_PLACE, "y"),
      "The cache kept a value that was built from data that was invalidated during the load"
    );
  }

  /**
   * Invalidating everything while a load is running must also stop that load from leaving a stale value
   */
  @Test
  void invalidateAllDuringLoadDoesNotLeaveStaleValue() throws Exception {
    AccessContext ac = mCache.createAccessContext(null);

    Future<String> loading = mLoadThread.submit(() -> mCache.<@Nullable String, @Nullable String>get(ac,
      Keys.DERIVED,
      Keys.DERIVED_PLACE,
      "x"
    ));
    assertTrue(mDerivedLoader.getReadSource().await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "The load never started");

    mSource.set("new");
    mCache.invalidateAll(ac);

    mGate.countDown();
    assertEquals("derived-from-old", loading.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

    assertEquals("derived-from-new",
      mCache.get(ac, Keys.DERIVED, Keys.DERIVED_PLACE, "x"),
      "The cache kept a value that was built before everything was invalidated"
    );
  }

}
