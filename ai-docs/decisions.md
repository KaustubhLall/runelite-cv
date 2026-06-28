# Decision Log

Record decisions that future sessions should not have to rediscover.

## Entry Template

```md
## YYYY-MM-DD: Short Decision Title

- Status: proposed | accepted | superseded
- Related: Linear task, branch, PR, or contract

### Context

What forced the decision.

### Decision

What we chose.

### Tradeoffs

What this improves, what it costs, and what alternatives were considered.

### Follow-up

Anything that should be revisited later.
```

## Decisions

## 2026-06-16: Jagex Launcher Credential Bridge For Dev Login

- Status: accepted
- Related: `OSR-2`

### Context

The user's `CoreDump` / `C0REDUMPED` account is managed by Jagex Launcher. Directly launching the custom RuneLite jar starts the client but does not establish game-login credentials.

### Decision

Use RuneLite's official `--insecure-write-credentials` development flow. First launch official RuneLite through Jagex Launcher with that client argument so it writes `C:\Users\kaust\.runelite\credentials.properties`; then launch the custom jar.

### Tradeoffs

This uses a sensitive local credential file and requires careful handling, but it matches RuneLite's documented development path for Jagex accounts. The custom launch should be considered credential-ready only when the log contains `injected-client - read 5 credentials from disk`.

### Follow-up

Remove `credentials.properties` after development if the user wants RuneLite to return to normal launcher-only behavior.

## 2026-06-16: Stable RuneLite Service Version For Snapshot Dev Builds

- Status: accepted
- Related: `OSR-2`

### Context

The local custom RuneLite jar is built as `1.12.29-SNAPSHOT`, but RuneLite Plugin Hub and API/config services are versioned around stable releases. Game login can work through Jagex credentials while Plugin Hub/config sync still fails because the snapshot service path is not available.

### Decision

Keep the local jar version as-is, but launch development builds with stable service overrides:

- `-Drunelite.pluginhub.version=1.12.28`
- `-Drunelite.http-service.url=https://api.runelite.net/runelite-1.12.28`

The wrapper script `scripts/launch-dev-runelite.ps1` defaults to `-ServiceVersion 1.12.28` and passes those JVM properties before `-jar`.

### Tradeoffs

This avoids mutating Maven project version or dependency coordinates just to talk to live services. If RuneLite protocol compatibility changes, the stable service version may need to be updated alongside the local source checkout.

### Follow-up

If Plugin Hub/config sync still fails after launch overrides, inspect the latest RuneLite log for the actual HTTP URL/status before changing credentials or account-login flow.

## 2026-06-22: Skill Farmer Presets and Batch Drop Policy

- Status: accepted
- Related: `OSR-10`, `OSR-12`

### Context

Woodcutting and mining farmers were shipping with only a handful of target presets and a single-slot drop policy. Users had to manually type targets for common trees/rocks, and dropping while woodcutting interrupted the chopping animation because the policy only dropped one item per tick and triggered while the axe was still animating.

### Decision

1. **Exhaustive presets**: Every skill farmer must ship with presets for all trainable targets in that skill. For woodcutting this means every tree type from the OSRS wiki that has a `Chop down` option; for mining this means every ore/rock type. Presets are added to `CvHelperPlugin` in `woodcuttingProfiles()` and `miningProfiles()` and exposed through the existing `/automation/woodcutting/config` and `/automation/mining/config` endpoints.
2. **Batch drop**: Inventory dropping is not tick-limited for `Drop` menu actions. The plugin should drop all slots of the same droppable item in a single client-thread invocation, issuing one `client.menuAction` per slot. This is implemented in `dropInventorySlots(List<Integer>, int)`.
3. **Idle-only drop for woodcutting**: The default woodcutting drop policy is `WHEN_IDLE`, which only triggers when the player is not in a woodcutting animation. This avoids interrupting active chopping and naturally batches drops during the movement window between depleted and next trees.

### Tradeoffs

- Presets make the UI longer but remove manual target entry for common training spots.
- Batch drop can empty the inventory very quickly; protected-items and max-value guards remain the safety layer.
- `WHEN_IDLE` delays drops until the tree finishes, so a player could fill up while chopping a very long tree. The user can switch to `WHEN_FULL` if they prefer the old behavior.

### Follow-up

- Add a mining animation list and extend `WHEN_IDLE` to mining when that skill farmer is validated.
- For every new skill farmer (fishing, hunter, etc.), repeat the preset exercise: enumerate all trainable targets from the OSRS wiki and add them as default profiles before the feature is considered complete.

## 2026-06-22: Woodcutting Farmer Target Sticking and No Re-click

- Status: accepted
- Related: `OSR-10`, `OSR-12`

### Context

The woodcutting farmer was selecting the best tree every tick and re-clicking it every tick. This repeatedly interrupted the active chopping animation, wasted ticks, and made the player slower than clicking once and letting the axe swing.

### Decision

1. **Track the current target**: Store the last selected woodcutting target (ID + world location) and compare it against the current scan.
2. **Stick to the current tree while chopping**: If the player is in a woodcutting animation and the last target still matches the configured target list and is reachable, the farmer keeps that target instead of switching to a closer/different tree.
3. **No re-click while chopping**: When the active animation matches the same target, the farmer does not send another `Chop down` menu action unless the user explicitly enables `woodcuttingReclickWhenActivelyChopping`.
4. **Configurable**: Two new settings are exposed in the WebHelper config and schema:
   - `woodcuttingStickToTarget` (default true): don't switch trees while actively chopping.
   - `woodcuttingReclickWhenActivelyChopping` (default false): allow repeated clicks while the axe animation is running.

### Tradeoffs

- Prevents the animation-interrupting re-click problem and improves XP/hour.
- May delay a switch to a closer tree if the current tree is still animating; the user can disable stick-to-target if they prefer aggressive re-targeting.
- Animation detection is the sole signal, so brief lag or animation gaps can cause a re-click; this is acceptable because the alternative is worse.

### Follow-up

- Apply the same pattern to mining once a mining animation list is added.
- Consider a small post-animation cooldown/hold window to avoid re-clicking during the brief gap between tree depletion and the next animation.

## 2026-06-22: Fix DeduplicationFilter NoClassDefFoundError Crash

- Status: accepted
- Related: `OSR-10`

### Context

During combat (and occasionally elsewhere), the client crashed with `java.lang.NoClassDefFoundError: net/runelite/client/util/DeduplicationFilter$LogException` thrown from the logback deduplication turbo filter. The outer class and its inner class were both present in the shaded jar, but the context classloader used during some logging calls could not resolve the private static inner class.

### Decision

1. Convert the `LogException` private static inner class to a package-private top-level class named `DeduplicationLogException`.
2. Remove Lombok annotations (`@RequiredArgsConstructor`, `@EqualsAndHashCode`) and write the constructor, `equals`, and `hashCode` explicitly.
3. Wrap the `decide()` body in a try/catch that returns `FilterReply.NEUTRAL` on any `Throwable`, so a class-loading failure in the filter cannot take down the client thread.

### Tradeoffs

- Loses the convenience of Lombok annotations on this tiny class; the explicit code is small and easier to reason about.
- The safety catch means deduplication might be disabled in the rare case the class still cannot load, but the game keeps running.

### Follow-up

- Monitor whether the crash reappears; if it does, investigate whether the RuneLite launcher/plugin classloader is missing the `runelite-client` jar from its delegation chain during some client ticks.
