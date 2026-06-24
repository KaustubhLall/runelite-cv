# CV Helper Modular Architecture (Standard)

The default playbook for the `cvhelpermod` plugin and any future work that extends it.
Goal: every source file is small enough to **read in full / fit in context** — hard ceiling
**2–3k lines, target well under 2k** — while preserving the reference plugin's behavior 1:1.

## Why this exists

The first modular rewrite (`cvhelpermod`, scrapped) was re-derived from scratch by a weak model
and drifted off parity. The replacement is built by **verbatim decomposition** of the proven
reference (`cvhelper`): copy working code, reorganize it, never re-implement it.

## Current state (branch `feature/cvhelper-clean-modular`)

- Package `net.runelite.client.plugins.cvhelpermod`, plugin `CvHelperModPlugin`,
  `enabledByDefault = false`. Built off **pristine `master`** reference.
- **Parity is achieved by construction**: the entire reference plugin + collaborators were copied
  verbatim (type renames only) and compile clean. So the modular plugin is already behaviorally
  identical to `cvhelper`; the remaining work is purely splitting the big file for maintainability.
- Shares config group `"cvhelper"` (see `CvHelperModConfig.GROUP`) so saved settings and the
  verifier dashboard transfer seamlessly. Reference `cvhelper` stays enabled as the fallback until
  cutover.
- Already split out: `CvHelperModData` (abstract base holding all constants, ~80 instance fields,
  inner classes/enums, static helpers as `protected`). `CvHelperModPlugin extends CvHelperModData`.

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

Java has no partial classes, and RuneLite needs the single `@PluginDescriptor` class to extend
`Plugin`. Two viable mechanisms:

- **Inheritance chain** (lowest churn): an abstract base holds all shared fields/inner-types as
  `protected`; concern classes move up the chain; method bodies stay byte-identical because they
  access inherited members unqualified. Verified to work with the framework:
  `EventBus.register` walks the superclass chain (`eventbus/EventBus.java:106`, scans
  `getDeclaredMethods()` per level), so inherited `@Subscribe` handlers register; Guice injects
  inherited `@Inject` fields. **Constraint: an ancestor cannot call a descendant's methods.**
- **Composition** (no ordering constraint): a concern class injects the *same-named* RuneLite
  services (so `client`/`config`/… references are unchanged) and holds a back-reference `p` to the
  plugin for shared mutable state (`p.field`) and cross-concern calls (`p.method()`). The compiler
  guides every `p.` prefix and every `private`→package-private callee change.

**Caveat (why this is iterative, not a quick slice):** the reference's call graph is *tangled*,
not cleanly layered by line order. Concrete examples found:
- mob-farmer methods span lines ~3093–6385 **and** ~8275–8895, with pathfinding/skill-farmer code
  interleaved;
- `capturePayload` (near the file end) calls `getPlayerStatus` (line ~655), which calls the
  tail's status builders — a back-reference that defeats naive top-down or bottom-up slicing;
- `automationStatus` (tail) calls `getMobFarmerStatus`/`getSkillFarmerStatus` (upper).

So a clean **inheritance** split needs a topological grouping of mutually-recursive concerns (cycles
co-locate), and a **composition** split needs compiler-guided `p.` prefixing per concern. Either
way: extract one concern, compile, fix what the compiler flags, commit. Do not attempt the whole
file in one pass.

## Target module map

Group by concern (each becomes its own file, < ceiling). Suggested layers, lowest first:
`Data` (done) → JSON/map/string/numeric primitives → export (target collectors, status builders,
entities, capture) → pathfinding → action executor + login recovery → skill farmer → mob farmer
(may need 2 files) → leaf `CvHelperModPlugin` (lifecycle, `@Subscribe`, hotkey
register/dispatch + listener fields, `@Provides`, server start + HTTP handlers — the entry points
that call everything). `ItemSafetyService`, `InventoryDropService`, `ChatResponderService` are
already separate and stay so.

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
