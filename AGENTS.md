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

## Code style

- Fix IntelliJ warnings rather than ignoring or suppressing them, even when the inspection is wrong. Ignored warnings hide the real ones later. Use `get_file_problems` from the IntelliJ MCP to check a file you changed.
- The exception is an "unused" warning on a method that could reasonably be expected to be used in future, or that is part of an interface where its usefulness is clear. Mark that method (never the class) with `@SuppressWarnings("unused")`.
- When a warning must be suppressed, suppress it at the narrowest, most specific location, so that real problems nearby aren't hidden. Use `@SuppressWarnings` when it can sit on something that small (a local variable, a field, or a method that is itself the target of the warning). When the only annotation target would cover a wider block of code (for example a whole method, just to cover one cast), use an IntelliJ `//noinspection` comment on that line instead. Place it so it attaches to the right element: for a warning on a `switch` rule's branch (e.g. an unreachable `default -> { }`), the comment goes between the `->` and the `{`, not above the label or the `switch`. IntelliJ reports a misplaced one as "Redundant suppression". Put a short comment above it explaining why the warning is wrong.
- Extract a method only when it needs documentation to explain what it does. If the code is short and obvious enough that a doc comment would add nothing, keep it inline, even if that duplicates it. A method extracted from obvious code would itself need a comment explaining why it exists, and readers would wonder.
- Report problems through the `ContextFactory` that is injected (pass it through the constructor), never `ContextFactory.getInstance()` or `System.Logger`/other logging frameworks. Use `reportWarn` / `reportThrowable` for rare problems.
- Don't create a new `Context` in tight loops or performance-sensitive paths.
- Prefer IntelliJ (the MCP tools) to find references and usages when it is faster than grepping.

## Environment notes

- Development is on Windows; the project lives under `C:\DQGitHub\cachly`. `.idea/` is checked in, so avoid committing incidental IDE changes (e.g. `.idea/compiler.xml`).
