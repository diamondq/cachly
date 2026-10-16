# TODO

Mark an item `[x]` when done and fill in a `Resolution` date (YYYY-MM-DD) with details of the resolution.
All details sit under each item.

Line numbers refer to the code at that date the TODO item was created. A new item takes the next free number and names
its group in the Category field,
so nothing is ever renumbered.

- [x] **1. Writers can hang forever on the callback semaphore** (`AbstractCacheStorage` `store`/`invalidate`)
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why: `store` and `invalidate` call `prepareSemphore`, write, then `acquire()` with no timeout. The wait only ends
      if
      a matching event arrives and `callCallbacks` reaches its final `release()`. Paths that break this:
        - Invalidating an absent key: `MemoryCacheStorage.invalidate` (and Ehcache) fires no event, so with a callback
          registered on the key `invalidate` blocks permanently.
        - Caffeine never calls `CacheCallbackHandler`, so any `store`/`invalidate` on a key with a registered `onChange`
          callback hangs.
        - `callCallbacks` has no `try/finally`: if a user callback or its `getIfPresent` throws, the permit is never
          released, and the exception is lost in the discarded `Future` from `submit`.
        - Self-deadlock in the `LoadTracker` fix: the second `isAffected` check calls `invalidateInternal(...)` while
          still
          inside `synchronized(key)`. `invalidate` waits for the callback task, which calls `getIfPresent` (a `lookup`
          with
          `loadIfMissing=true`) and blocks on the monitor the waiting thread holds.
        - Pool starvation: with a bounded executor, writers waiting on callback tasks can exhaust the pool that has to
          run
          those tasks.
        - Permit leaks: the per-key semaphore is shared and never reset. Any event with no waiter (expiry, eviction,
          another writer) adds a permit, so later waiters return before their own callback has run.
    - Fix: per-operation latch/token, released in `finally`, waited on with a timeout. Better, drop the wait and make
      delivery ordered per key (see item 13).
    - Resolution (2026-10-15): each `store`/`invalidate` now waits on its own latch, queued per key and released by the
      next callback run for that key in a `finally`. The wait times out (default 30s, `setCallbackTimeout`) with a
      `ContextFactory` warning. A throwing callback is reported and no longer blocks the writer or the other callbacks.
      `invalidate` skips the wait for an absent key, and `supportsEvents()` skips it for storages whose native cache
      never reports events (Caffeine, see item 20). The `lookup` self-deadlock is fixed by running the final
      stale-check invalidate after the key's monitor is released. Also fixed `CacheCallbackHandler` dropping every
      `MemoryCacheStorage` event (it keyed on the data map, whose hash code changes), now identity-based. Not done:
      pool starvation is only bounded by the timeout (needs item 13). Tests: `TestCallbackWait`.

- [ ] **2. `CacheInfo` uses plain `HashSet`s under a `ConcurrentHashMap`** (`CacheEngineImpl:634-638`)
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why: different keys load under different locks and mutate the same `dependencyMap[dep]` set at the same time,
      which is unsafe for `HashSet`. `invalidateInternal` iterates `depSet` after removing it while a loader may still
      hold a reference and add to it (`ConcurrentModificationException`). `getDependentKeys` and `getDependentOnKeys`
      also hand out the live sets.
    - Fix: `ConcurrentHashMap.newKeySet()`, atomic `compute`-style updates, and return copies from the getters.

- [ ] **3. The dependency graph only grows, and `invalidateAll` doesn't clear it**
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why: `invalidateInternal` removes `reverseDependencyMap[key]` but never removes `key` from
      `dependencyMap[otherDep]`. Expiry and eviction never touch the graph. A reload with different dependencies only
      adds edges (`addAll`). With per-user placeholder keys and a TTL, `dependencyMap[D]` accumulates every key ever
      loaded (memory leak, over-invalidation, ever-larger blob persisted on each load). `invalidateAll` clears the
      storages but leaves the in-memory `mCacheInfo` maps untouched.
    - Fix: use `reverseDependencyMap` to unlink the key from its old dependencies on every reload and invalidation.
      Clear both maps in `invalidateAll`.

- [ ] **4. `invalidateAll` corrupts the metadata of serializing storages**
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why: `invalidate(cache, null)` deletes the persisted `s/`, `t/` and `k/` id entries, but the in-memory
      `mStringToShort`, `mTypeToShort`, `mKeyToShort` and the counters stay. Later writes reuse the old ids and never
      re-persist them, so after a restart `init()` / `decompressString` fails with "id not recognized". With no meta
      cache (Micronaut) the primary invalidate removes the metadata too, so the same thing happens.
    - Fix: reset the in-memory maps and counters together with the persisted ones.

- [ ] **5. Persisted metadata can end up unwritten** (`serializeEntry`)
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why: `compressString` / `compressType` / `compressKey` publish the new id to the in-memory maps immediately, but
      the meta entry only goes into `listOfEntries`. If anything throws before the write, the id exists in memory but
      was never persisted. The value conversion (line 584) runs after the compress calls and can throw for a
      non-serializable value. After a restart the data entries are undecodable. Also: another thread's fast path can
      write its data before the first thread's meta write, and `(short) incrementAndGet()` has no overflow guard.
    - Fix: write the meta entry before publishing the id, or convert the value first.

- [ ] **6. `init()` skips `mTypeToShort` for plain classes** (`AbstractCacheStorage:362`)
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why: the `TYPE_CLASS` branch fills `mShortToType` and the counter but not `mTypeToShort`. On a persistent cache
      every restart re-registers each class under a fresh id and writes a duplicate meta entry, and the ids creep toward
      the `short` limit of 32767. Related: line 297 sets `mKeyPrefixLen = mTypePrefix.length()`, harmless with the
      defaults (`t/`, `k/`) but wrong for custom prefixes.

- [ ] **7. A bad cached entry should be a miss, not an exception**
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why: `deserializeEntry` throws on an unknown serialization version, unknown class (renamed in a release) or
      converter failure, and that propagates out of `get`. `init()` runs in the `MicronautCacheStorage` constructor, so
      one stale entry in a persistent disk cache can stop the application from starting.
    - Fix: treat deserialization failures as a miss, remove the entry and log it. `init()` should fall back to clearing
      the cache on failure.

- [ ] **8. Invalidation can lose the dependency edges it needs** (`invalidateInternal:729`)
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why: it removes the dependency set first, then calls `storage.invalidate` and cascades. If the storage throws, or
      one dependent's invalidate throws partway, the remaining dependents stay cached and stale, and nothing records
      that they depend on this key any more.
    - Fix: try/catch per dependent collecting failures, and restore the edges if the invalidate fails.

- [ ] **9. `set()` doesn't invalidate dependents or notify in-flight loads** (decision needed)
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why: `setInternal` never calls `markInvalidated` or cascades. A load that read key X while someone called
      `set(X, ...)` caches a value built from stale data, and keys derived from X keep their old values.
    - Fix: if not intentional, treat `set` like an invalidate for tracking and cascade purposes. If intentional,
      document it on `Cache.set`.

- [ ] **10. `MemoryCacheStorage` expiry race, no cleanup, inconsistent notifications**
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why:
        - `readFromPrimaryCache` checks expiry then calls `mData.remove(pKey)`; a concurrent fresh `put` can be deleted.
          Use `mData.remove(pKey, dataRecord)`.
        - Entries only expire when read and there is no size bound, so per-user keys with a TTL leak.
        - Lazy expiry fires no event, so listeners are never told.
        - `invalidate(null)` does `mData.clear()` and fires no events, but Ehcache fires `REMOVED` per entry, so
          `invalidateAll` listener behaviour differs between backends (decision needed on the intended behaviour).

- [ ] **11. Locking in `lookup`**
    - Category: Concurrency design
    - Created: 2026-10-15
    - Why:
        - It holds an interned-string monitor for the whole load. Interned strings are JVM-global, so unrelated code
          locking the same literal contends with it. Use a `ConcurrentHashMap<String, Lock/Future>` single-flight.
        - No timeout: one hung loader blocks every waiter for that key.
        - If a loader throws, each waiter retries the loader in turn (thundering herd on a failing source). Share the
          failure or add a short backoff.
        - A loader returning not-found stores nothing, so every `get` takes the lock and calls the loader again. A hot
          missing key is processed serially. Consider an optional negative TTL.
        - No cycle detection: a dependency cycle gives `StackOverflowError` on one thread and a deadlock across threads.
          Add a thread-local "currently loading" set and throw a clear error with the chain.

- [ ] **12. Dependency tracking is thread-local (`sMonitored`)**
    - Category: Concurrency design
    - Created: 2026-10-15
    - Why: if a loader reads other keys from another thread (executor, `parallelStream`, `CompletableFuture`) those
      reads
      are not recorded. The `LoadTracker` can't see them either, because it compares against the same `dependencies`
      set. The result is stale forever, silently.
    - Fix: propagate the dependency context explicitly, or document the restriction.

- [ ] **13. Listener state can end up wrong** (`callCallbacks`)
    - Category: Concurrency design
    - Created: 2026-10-15
    - Why: events run concurrently on the executor. Each task reads the current value and then updates `lastValue`,
      which
      isn't atomic. A slower task holding an older value can overwrite `lastValue` and notify the listener of the stale
      value last.
    - Fix: serialize delivery per key, or use a version stamp with `compareAndSet`.

- [ ] **14. Every write event deserializes the whole value and schedules a task** (`handleEvent`)
    - Category: Performance
    - Created: 2026-10-15
    - Why: `deserializeEntry` runs on the writer's thread, including the Kryo conversion of the value bytes for a
      serializing store, when only the key is needed. Then `executor.submit` runs for every write, including `CacheInfo`
      writes and metadata.
    - Fix: return early when `mCallbacks` has no entry for the key string and the loader isn't a `CacheInvalidator`.
      Otherwise decode only the key.

- [ ] **15. The whole `CacheInfo` is stored on every load with dependencies and every invalidation with a `depSet`**
  (`CacheEngineImpl:639`, `:744`)
    - Category: Performance
    - Created: 2026-10-15
    - Why: O (graph size) per load. With a serializing storage it serializes the whole dependency graph each time while
      other threads mutate it, and it triggers an item 14 event each time. The graph also holds full `KeySPI` objects,
      which pins `KeyDetails` and so loaders and storages.
    - Fix: persist deltas, or write behind with coalescing and a periodic flush. Consider storing string keys instead of
      `KeySPI` objects.

- [ ] **16. The read path does extra work**
    - Category: Performance
    - Created: 2026-10-15
    - Why:
        - `queryForKey` goes through `deserializeEntry`, which rebuilds the whole `Key` (two `split("/")` calls plus the
          key parts) on every hit, though only the value is used. Add a value-only decode.
        - `CompositeKey.toString()` and `hashCode()` aren't cached and are called several times per lookup. Keys are
          immutable, so cache both.
        - For placeholder keys, `resolvePlaceholders` builds a new `CompositeKey` and calls `setupKey` on every call,
          and
          `get(key, holder, value)` then calls `setupKey` again. Cache the details by template.
        - `mContextFactory.newContext(...)` runs on every `get`. Profile its cost.

- [ ] **17. `LoadTracker` overhead under heavy invalidation** (low priority)
    - Category: Performance
    - Created: 2026-10-15
    - Why: `markInvalidated` notifies every active load for every invalidation and each tracker keeps all the keys, so a
      long load under heavy invalidation holds a growing set. Leave unless profiling shows a problem.

- [ ] **18. Key details are never refreshed when registrations change**
    - Category: OSGi / dynamic registration
    - Created: 2026-10-15
    - Why: only tests call `clearKeyDetails`. After a dynamic `remove*` or replace of a storage, path or loader,
      existing keys keep strong references to the old one. `addPathConfiguration` and `addCacheStorage` throw if the
      storage or bean-name locator isn't registered yet, and DS gives no ordering guarantee for `DYNAMIC` references, so
      binding can fail depending on order.
    - Fix: park pending items and retry, and bump a registration generation that makes keys redo `setupKey`.

- [ ] **19. Minor cleanups from the review**
    - Category: OSGi / dynamic registration
    - Created: 2026-10-15
    - Why:
        - `removePathConfiguration` never removes the serializer entry: `add` stores the default when the serializer is
          null, and `remove` skips null.
        - `mCacheInfo` and `mRegistrationChangeListener` aren't `volatile`.
        - `Instant.now().plus(duration)` can overflow for very large TTLs and uses wall-clock time.

- [ ] **20. Caffeine-backed caches never deliver `onChange` callbacks** (`cachly-micronaut-caffeine`)
    - Category: Correctness / reliability bugs
    - Created: 2026-10-15
    - Why: only `MemoryCacheStorage` and the Ehcache factory call `CacheCallbackHandler.handleEvent`. Nothing is wired
      for Caffeine, so `Cache.registerOnChange` on a key stored in a Caffeine cache never fires after the initial call.
      Item 1 stopped writers from hanging on this (`AbstractCacheStorage.supportsEvents()` is false when the native
      cache isn't registered through `CacheCallbackHandler.registerEventSource`), but the callbacks are still silently
      missing.
    - Fix: register a Caffeine `removalListener` (REMOVED/EVICTED/EXPIRED/REPLACED) and a write hook (ADDED/MODIFIED)
      that call `CacheCallbackHandler.handleEvent`, and call `registerEventSource` for the native cache. Check how
      `CachlySyncCache` / the Caffeine cache factory is created so the listener can be set at build time. Add a test in
      `cachly-testing` equivalent to `TestCallbacks`/`TestCallbackWait` running against Caffeine.

## Suggested order

1. Bugs that hang or corrupt: items 1, 4, 5, 6, 7 and 2.
2. Biggest cost win: items 14, 15 and 16 together.
3. Semantics to decide first: item 9 (`set` and dependents) and item 10's inconsistent notifications.

Each fix should get a regression test in `cachly-testing`.
