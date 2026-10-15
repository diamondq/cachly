# Cachly

DiamondQ Cachly is a Java caching library: a typed, path-keyed cache API (`Cache`, `Key`, `CacheLoader`) backed by pluggable storage and serializers, with Micronaut integrations. It also supports OSGi setup.

## Build

- Maven multi-module reactor; root `pom.xml` is `cachly-root`. Java 25 (see `.github/workflows/maven.yml`; CI uses a Corretto 25 toolchain).
- Version is `${revision}${sha1}${changelist}`, inherited from `cachly-masterparent`, which extends `com.diamondq:diamondq-parent` (the shared DiamondQ master parent, resolved from the DiamondQ Nexus).
- `.mvn/extensions.xml` loads the `maven-profile-activator` extension; keep it when changing the build.
- Build: `mvn clean install`. Run one test class: `mvn -pl cachly-testing -am test -Dtest=TestGet`.
- `dep_check.bat` reports dependency and plugin updates (Windows, hard-coded paths).

## Modules

| Module | Purpose |
|---|---|
| `cachly-masterparent`, `cachly-parent`, `cachly-bom` | Build/dependency management poms (no code) |
| `cachly` | Core API (`com.diamondq.cachly`), `base/` abstract loaders and storage, `impl/` (`CacheEngineImpl`, `MemoryCacheStorage`), `spi/` (`CacheEngine`, `CacheStorage`, keys, `OSGiEngineSetup`) |
| `cachly-micronaut` | Micronaut integration (`CachlySyncCache`, `MicronautCacheStorage`, bean name locator) |
| `cachly-micronaut-caffeine`, `cachly-micronaut-ehcache` | Caffeine and Ehcache storage for Micronaut |
| `cachly-serializer-kryo` | Kryo serializer |
| `cachly-testing` | Shared tests (`Test*.java` in `com.diamondq.cachly.test`) run against the implementations |

## Conventions

- Every package has a `package-info.java`. Do not add `org.eclipse.jdt.annotations` there; they were deliberately removed.
- Public API lives in `com.diamondq.cachly`; implementation details go in `impl/`, and extension points in `spi/`.
- Match the surrounding code's style, naming and comment density.
- Tests go in `cachly-testing`, not in the module under test.
- New modules must be added to the root `pom.xml` `<modules>` and to `cachly-bom`.
- Properties registered on the Cache/CacheEngine (OSGi) use a `String[]` value, not a `String`.

## Environment notes

- Development is on Windows; the project lives under `C:\DQGitHub\cachly`. `.idea/` is checked in, so avoid committing incidental IDE changes (e.g. `.idea/compiler.xml`).
