# PR 2 Systematic Review Memory

Date: 2026-06-26
Branch/PR: `feature/cvhelper-clean-modular` / PR #2 (`Stabilize Mining/Woodcutting farmers and WebHelper v3 console`)

Verification label: **Not verified**. This review was performed from GitHub PR/file inspection only. No local compile, endpoint smoke, RuneLite relaunch, or live Mining/Woodcutting test was run by this reviewer.

## Confirmed findings

### 1. Skill farmer config apply/reload bug is real

The Mining/Woodcutting config apply path currently mixes persistent config and volatile runtime state:

- Mining scan radius and max candidates are read through `CvHelperModConfig` (`config.miningScanRadius()`, `config.miningMaxCandidates()`), but `POST /automation/mining/config` writes the new values only to volatile runtime fields (`miningFarmerScanRadius`, `miningFarmerMaxCandidates`).
- Woodcutting target/radius/max candidates are also volatile-only and do not have matching persistent config keys in the inspected implementation.
- The apply path clamps `maxCandidates` to a minimum of 10, so a requested value of 3 cannot stick.
- Mining scan radius has a native config range max of 20, while the requested/current workflow wants `scanRadiusTiles=25`.

Effect: applying `scanRadiusTiles=25` and `maxCandidates=3` can appear to apply, then immediately reload/reset through `GET /automation/<skill>/config` or after a client restart.

### 2. Mining retarget/depletion fix is partial

The PR adds Mining XP-drop invalidation and a short depleted-tile cooldown. This is the right direction, but it is not enough to fully explain or prevent the reported behavior where the farmer swaps between two rocks or retries an empty/depleted rock while a valid open rock is available nearby.

Risks still present:

- Candidate collection truncates to `maxCandidates` before selection. If stale/invalid candidates are among the nearest candidates, they can consume the configured display/evaluation slots and hide the next valid candidate.
- Depletion detection is tied to object ID at the same tile and may miss cases where the same object ID remains but no valid action/resources are available.
- The runtime still refers to volatile `miningFarmerTarget` in some re-check paths, so persistence cleanup must ensure every path reads the same current config source.

### 3. Woodcutting likely has a similar stale/config issue

Woodcutting has a validity check for sticking to the current tree, but no equivalent completion/invalidation diagnostics to Mining's XP-drop target lifecycle. It also stores target/radius/max candidates in volatile fields, so it is subject to the same apply/reload problem.

### 4. Reachability grid still does not satisfy “every square has info”

The WebHelper grid now renders candidate object footprints and centerpoint markers, which helps with large objects. However, the grid is still built from player tile + candidates + optional route/doors. Intermediate/no-object tiles are inferred only as empty SVG grid cells; they do not have per-tile diagnostics such as reachable/path distance/collision/object occupancy.

Required follow-up: add a real tile diagnostics payload for every tile in radius and render that grid first, then layer candidates, footprints, selected target, route, and doors above it.

### 5. WebHelper skill config editor is structurally fine; backend is the blocker

The v3 skill config tab preserves draft edits and only reloads on explicit force/load/reset. It posts versioned `{ version: 1, settings: ... }` payloads. The observed reset is caused by backend apply/read mismatch and clamps, not by the draft editor auto-overwriting edits.

### 6. Global config editor is not complete in this PR

The PR adds skill farmer config tabs, but a reusable global config editor for shared CV Helper/runtime settings is still deferred/missing from the inspected surface. Implement it after the skill config persistence brick, reusing the schema-driven draft editor pattern.

## Required next brick

Focused stabilization patch, not a broad rewrite:

1. Add persistent per-skill config keys for target, scan radius, and max candidates for both Mining and Woodcutting.
2. Make `GET /automation/<skill>/config`, `POST /automation/<skill>/config`, `/automation/<skill>/status`, selection, presets, and runtime behavior all read the same source of truth.
3. Allow `maxCandidates=3` by changing backend clamp to `1..300` (or another explicit range that includes 3).
4. Allow `scanRadiusTiles=25` by raising config/backend/UI schema max to at least 64.
5. Decide whether `maxCandidates` means displayed candidates or evaluated candidates. Recommended: evaluate all objects in radius, select the nearest valid reachable target, then cap the displayed/debug candidate list.
6. Add/expand Mining stale-target rejection reasons: no matching action, no click point/bounds, unreachable, tile cooldown, different object/resource state, repeated action failure.
7. Add analogous Woodcutting stale/completion diagnostics, but keep it focused and do not redesign the farmer loop.
8. Add real tile-grid diagnostics for every tile in radius as a separate follow-up unless needed to debug this patch.

## Acceptance tests

Endpoint-tested expectations after the patch:

- `POST /automation/mining/config` with `{ version:1, settings:{ target:"exact:Iron rocks|iron", scanRadiusTiles:25, maxCandidates:3 } }` returns `ok:true` and echoed config values `25` and `3`.
- Immediate `GET /automation/mining/config` returns the same target/radius/candidate values.
- Same flow works for `/automation/woodcutting/config`.
- Relaunch RuneLite, then `GET /automation/<skill>/config` still returns the saved values.
- `/automation/<skill>/status` reports the same scan radius/max candidates as config.
- Live-test Mining: after an XP drop, the previous rock is marked completed/cooldowned, the Debug tab shows `completionReason: xp-drop`, and the next valid reachable rock is selected instead of spam-clicking the depleted tile.
- Live-test Woodcutting: when the current tree depletes/disappears, the farmer does not keep clicking a stale target and status explains the retarget decision.

## Scope boundaries

Do not touch Mob Farmer behavior, chat responder, login recovery, or action hotkeys except where shared config/status code requires safe read-only integration. Do not claim compile/live verification unless those checks are actually run.