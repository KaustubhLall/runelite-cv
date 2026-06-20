# Agent Guide: runelite-cv

## Big Picture
- This repo is a Gradle composite build: root includes `:client` and `:jshell`, plus included builds `cache`, `runelite-api`, and `runelite-gradle-plugin` (see `settings.gradle.kts`).
- `runelite-api` generates interface/component constants from `runelite-api/src/main/interfaces/interfaces.toml` via the custom `component` Gradle plugin.
- `runelite-client` consumes those generated interfaces and builds a shaded runnable jar (`:client:shadowJar`) with `Main-Class` `net.runelite.client.RuneLite`.
- Client startup order matters (`runelite-client/src/main/java/net/runelite/client/RuneLite.java`): load session/config, load plugins, then start UI and plugins.
- Plugin lifecycle is Guice + EventBus driven (`RuneLiteModule`, `PluginManager`): plugins are scanned from `net.runelite.client.plugins`, dependency-sorted, started/stopped on EDT.

## Project-Specific Architecture (CV Focus)
- The custom work is primarily in `runelite-client/src/main/java/net/runelite/client/plugins/cvhelper/`.
- `CvHelperPlugin` owns local HTTP export (`HttpServer` bound to `127.0.0.1`), target collection, capture endpoints, action hotkeys, and mob-farmer loop.
- Overlay rendering is split into `CvHelperOverlay`; side-panel UI is `CvHelperPanel`; config contract is `CvHelperConfig`.
- Endpoint handlers marshal work onto the RuneLite client thread with `clientThread.invokeLater(...)` and `CountDownLatch` timeouts (~1500ms) before responding.
- Target snapshots are cached (`targetSnapshots`) so panel/combat surfaces can remain usable after temporary UI closure; preserve `fresh` vs cached semantics when changing exports.

## Developer Workflows
- Build all modules: `./gradlew buildAll` (root task in `build.gradle.kts`).
- Fast compile for active plugin work: `./gradlew :client:compileJava`.
- Produce runnable client jar: `./gradlew :client:shadowJar` -> `runelite-client/build/libs/client-<version>-shaded.jar`.
- Windows dev launch path is scripted: `scripts/launch-dev-runelite.ps1` (builds shaded jar, applies stable service overrides, launches Java).
- Jagex account bridge for local dev uses `scripts/bootstrap-jagex-credentials.ps1` and `--insecure-write-credentials` flow.
- CI build (`ci/build.sh`) sets `ORG_GRADLE_PROJECT_glslangPath` before `:buildAll`; GPU-related tests may rely on that property.

## Integration Boundaries
- External RuneLite services are configurable in `RuneLiteModule` via system properties (`runelite.http-service.url`, `runelite.pluginhub.url`, etc.).
- `launch-dev-runelite.ps1` pins service compatibility using `-Drunelite.pluginhub.version` and `-Drunelite.http-service.url`.
- CV Helper exposes localhost endpoints like `/status`, `/targets/*`, `/entities*`, `/automation/*` in `CvHelperPlugin.startServer()`.
- Browser verifier lives in `tools/cv-helper-verifier/` and expects CORS headers from plugin responses.
- Optional push integration uses webhook POSTs from CV Helper (`sendWebhook(...)` in `CvHelperPlugin`).

## Conventions You Must Follow Here
- Java indentation is tabs, not spaces (`config/checkstyle/checkstyle.xml`, `RegexpSinglelineJava` rule).
- Keep RuneLite widget/client reads on the client thread; never access mutable client state directly from HTTP handler threads.
- For plugin additions, follow existing pattern: `@PluginDescriptor` + Guice injection + EventBus subscriptions + EDT-safe start/stop behavior.
- For CV Helper changes, update both machine-facing exports and human verifier paths (`/status` payload + `tools/cv-helper-verifier/index.html` expectations).
- Java/plugin edits require full client relaunch; no hot reload into an already-open RuneLite process.

## AI Session Conventions In This Repo
- Reuse `ai-docs/AGENTS.md` as process policy for task/memory hygiene.
- Before resuming branch work, check `ai-docs/memory/active.md` and `ai-docs/memory/branches.md`.
- When behavior/contracts change across plugin/Python/local API boundaries, update `ai-docs/contracts.md` and `ai-docs/cv-helper-spec.md` in the same change.

