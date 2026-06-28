# CV Helper Modular Architecture (Standard)

The default playbook for the `cvhelpermod` plugin and any future work that extends it.
Goal: every source file is small enough to **read in full / fit in context** — hard ceiling
**2–3k lines, target well under 2k** — while preserving the reference plugin's behavior 1:1.

## Why this exists

The first modular rewrite (`cvhelpermod`, scrapped) was re-derived from scratch by a weak model
and drifted off parity. The replacement is built by **verbatim decomposition** of the proven
reference (`cvhelper`): copy working code, reorganize it, never re-implement it.

## Source-of-truth status

PR #2 is the cutover checkpoint for the modular implementation. After merge, all new CV Helper work starts from `cvhelpermod` on `master`. The old `cvhelper` package remains available only for legacy comparison; it is not an alternate implementation target or an open feature bucket.

## Current state (branch `feature/cvhelper-clean-modular`)

- Package `net.runelite.client.plugins.cvhelpermod`, plugin `CvHelperModPlugin`,
  `enabledByDefault = false`. Built off **pristine `master`** reference.
- **Parity is achieved by construction**: the entire reference plugin + collaborators were copied
  verbatim (type renames only) and compile clean. So the modular plugin is already behaviorally
  identical to `cvhelper`; the remaining work is purely splitting the big file for maintainability.
- Shares config group `"cvhelper"` (see `CvHelperModConfig.GROUP`) so saved settings and the
  verifier dashboard transfer seamlessly. After the PR #2 cutover, reference `cvhelper` is legacy
  comparison code only and must not receive new features.
- `CvHelperModPlugin` **directly extends `Plugin`** (required — see Splitting technique) and holds
  the shared state/constants/inner-types. `PathfindingService` is split out as a composition
  `@Singleton`. (An earlier `CvHelperModData` inheritance base was reverted because it broke plugin
  loading.)

## The decomposition standard

1. **Verbatim-port discipline.** When relocating a method, copy the body **unchanged**. Only rewire
   *references* (field/method qualification, visibility). Never change logic, ordering, thresholds,
   or JSON keys. If something *looks* like a bug, leave it and note it — do not "fix" it silently.
   This is the rule that keeps parity intact.
2. **Compile after every cut.** `./gradlew :client:compileJava`
   (JAVA_HOME = `C:\Program Files\Android\openjdk\jdk-21.0.8`). The compiler is the correctness net:
   since bodies never change, a green compile + matching JSON shape ⇒ behavior preserved.
3. **Checkpoint each green step** with a focused commit. Never commit a non-compiling state.
4. **JSON-parity diff** before cutover: snapshot every endpoint from the running reference, re-run
   against the new plugin in the same client state, diff keys/shape.
5. **Live-smoke** per `cv-helper-spec.md` "Debugging In Game" before deleting the reference.

## Splitting technique (and the caveat that shapes it)

Java has no partial classes. **Hard RuneLite constraint (learned the hard way, at runtime): the
`@PluginDescriptor` class must DIRECTLY extend `Plugin`.** `PluginManager` checks
`clazz.getSuperclass() == Plugin.class`. An inheritance chain where the plugin extends a shared base
(e.g. `CvHelperModPlugin extends CvHelperModData extends Plugin`) **compiles but fails to load** —
the log shows `"has plugin descriptor, but is not a plugin"` and the plugin silently never appears
in the list. **So inheritance-via-Plugin-base is out; decomposition must use composition.**

**Composition**: each concern is a separate `@Singleton` class that injects the *same-named* RuneLite
services (so `client`/`config`/… references in moved bodies stay unchanged). For shared mutable state
and cross-concern calls it either injects collaborator services (preferred) or holds a back-reference
to the plugin (`plugin.field` / `plugin.method()`, with the callees made package-private). The
compiler flags every reference to wire. `PathfindingService` is the working template — it was a clean
first cut because it needed only `client` plus a couple of plugin-nested types (qualified as
`CvHelperModPlugin.PathingResult`).

**Caveat (why this is iterative, not a quick slice):** the reference's call graph is *tangled*,
not cleanly layered by line order. Concrete examples found:
- mob-farmer methods span lines ~3093–6385 **and** ~8275–8895, with pathfinding/skill-farmer code
  interleaved;
- `capturePayload` (near the file end) calls `getPlayerStatus` (line ~655), which calls the
  tail's status builders — a back-reference that defeats naive top-down or bottom-up slicing;
- `automationStatus` (tail) calls `getMobFarmerStatus`/`getSkillFarmerStatus` (upper).

So composition needs compiler-guided wiring per concern: extract one concern, compile, fix what the
compiler flags, commit. Do not attempt the whole file in one pass.

**Chosen approach: composition services** (`@Singleton`), because that is what makes the codebase
cheap for Copilot/smaller models to work on — self-contained files with explicit dependencies, no
hidden shared-state coupling. This is the architecture the scrapped `cvhelpermod` had right; we keep
it and fill it with verbatim-ported logic.

### Worked recipe (template: `PathfindingService.java`)

A repeatable, low-risk procedure — mechanical enough for Copilot/a cheaper model to follow:

1. Pick a concern; list its methods + the fields it owns + what it calls.
2. New `@Singleton public class XxxService` in package `cvhelpermod`. Inject the **same-named**
   RuneLite services it uses (`@Inject private Client client;` …) so the moved bodies are byte-
   identical for those references.
3. Move the methods **verbatim**. Make externally-called ones `public`. The only edits to bodies:
   - shared *types/constants* that still live in the plugin → qualify as
     `CvHelperModPlugin.PathingResult`, `CvHelperModPlugin.MOB_FARMER_PATH_DIRECTIONS`, etc.
     (mind substrings — qualify `InteractionPathingResult` before `PathingResult`, use `\b`).
   - cross-concern calls to methods still elsewhere → route via the collaborator/`plugin` ref.
4. Add `@Inject private XxxService xxx;` to `CvHelperModPlugin`; delete the moved blocks from the
   plugin; rewire call sites to `xxx.method(...)` (compiler lists every one).
5. `:client:compileJava` → green → commit. Never change logic; if a body looks wrong, leave it.

Status: `PathfindingService` done (8 methods) — it extracted cleanly because it was an effectively
*closed* set (only `client` + base constants/types, no shared-helper calls).

### Sequence correction (learned the hard way)

Most other concerns do **not** extract cleanly yet, for two measured reasons:
1. **Ubiquitous shared helpers.** Nearly every method calls tiny shared helpers — `intValue`,
   `longValue`, `mapValue`, `boundsMap`, `pointMap`, `safeValue`, `normalize`, `itemName`,
   `spellbookName`. While those live in the leaf, no concern is closed.
2. **Method-level interleaving.** Concerns are *not* contiguous. Example: the ~136 mob-farmer
   config accessor get/set wrappers (lines ~2419–3092) are interleaved with `refreshPrayerTargets`,
   `getMobFarmerStatus`, etc., which call leaf-level code — so you cannot slice a contiguous "config"
   block; you must gather methods by concern.

**Therefore the correct order is:**
1. **Move the ubiquitous pure helpers into a static util** (e.g. `CvJson`) and `import static` it
   everywhere — moved bodies keep calling `intValue(...)`/`boundsMap(...)` unqualified, so zero
   churn, and any future composition service can `import static` it too. Pure, closed — lowest risk.
   *Do this first.* (Helpers that need `client`/`itemManager` like `itemName`/`spellbookName` aren't
   static — leave them in the plugin and back-ref, or pass the value in.)
2. Then extract concerns by **gathering their methods** (not slicing line ranges) into composition
   `@Singleton` services (pathfinding ✓). Inject collaborator services where possible; back-ref the
   plugin for the rest.
3. The mob-farmer *loop* logic comes last — once its dependencies (helpers, collectors, login,
   config accessors) are factored, it drops in cleanly instead of needing a heavy god-object back-ref.

Remaining concerns, after step 1: config accessors, status builders, target collectors, entities,
capture, login recovery, action executor, skill farmer, mob farmer (split ~2 files). Each
compile-verified and independently committable. Tracked in Linear (OSR).

## Target module map

Group by concern. `CvHelperModPlugin` (extends `Plugin`) stays as the coordinator — lifecycle,
`@Subscribe`, hotkey register/dispatch, `@Provides`, server start + HTTP route wiring, and (for now)
the shared state — and delegates to composition `@Singleton` services: `CvJson` util (pure helpers),
pathfinding ✓, target collectors, status builders, entities, capture, login recovery, action
executor, skill farmer, mob farmer (split ~2 files). `ItemSafetyService`, `InventoryDropService`,
`ChatResponderService` are already separate and stay so. The goal is the plugin shrinks toward just
coordination while each concern is a self-contained, Copilot-loadable file under the ceiling.

## Extending the plugin

New endpoints / automation modules follow the same boundaries and the same size budget: add the
logic to the matching concern file (or a new one if it would push a file over ~2k lines), keep the
HTTP route wiring in the server/leaf layer, and update `contracts.md` + `cv-helper-spec.md` when the
API shape changes. Mirror the verifier's existing JSON contract — do not break v2.

## Related docs

- API/behavior spec: `cv-helper-spec.md`
- Cross-process contracts + verification commands: `contracts.md`
- Decision log: `decisions.md`
- Branch/session memory: `memory/branches.md`, `memory/active.md`
