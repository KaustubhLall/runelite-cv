# CV Helper Modular vs Original Parity Audit

**Scope**: Compare the modular rewrite (`cvhelpermod`) against the original monolithic plugin (`cvhelper`) to identify missing features.

**Date**: 2026-06-23

**Status**: In progress

---

## 1. Endpoint Surface

| Endpoint | Original | Modular | Gap |
|----------|----------|---------|-----|
| GET /status | Yes | Yes | Minor field differences |
| GET /player/status | Yes | Yes | OK |
| GET /targets/* | Yes | Yes | OK |
| GET /entities | Yes | Yes | OK |
| GET /entities/nearest | Yes | Yes | OK |
| POST /capture | Yes | Yes | OK |
| POST /capture/screen | Yes | Yes | OK |
| POST /capture/minimap | Yes | Yes | OK |
| GET /capture/latest/* | Yes | Yes | OK |
| /automation/mob-farmer/* | Yes | Yes | OK |
| /automation/mining/* | Yes | Yes | OK |
| /automation/woodcutting/* | Yes | Yes | OK |
| /automation/action/* | No (keybind only) | Yes (extra) | New feature, not a gap |
| POST /automation/panic-stop | Yes | Yes | OK |
| /login/click | Yes | Yes | OK |

**Result**: Endpoint surface is complete. Modular adds action HTTP endpoints; otherwise parity.

---

## 2. Event Subscriptions

| Event | Original | Modular | Gap |
|-------|----------|---------|-----|
| GameTick | Yes | Yes | OK |
| GameStateChanged | Yes (login recovery, focus click) | **No** | **Missing** |
| MenuEntryAdded | Yes (mob farmer diagnostics) | **No** | **Missing** |
| MenuOptionClicked | Yes (mob farmer diagnostics, drop logging) | **No** | **Missing** |

**Result**: Missing event wiring. This breaks login recovery focus-click, menu diagnostics, and manual drop tracking.

---

## 3. Hotkeys

| Hotkey | Original | Modular | Gap |
|--------|----------|---------|-----|
| Action slots 1-22 | Yes | Yes | OK |
| Refresh entities | Yes | Yes | OK |
| Nearest entity | Yes | Yes | OK |
| Panic stop | Yes | Yes | OK |
| Debug status | Yes | **No** | **Missing** |
| Print bounds | Yes | **No** | **Missing** |
| Capture screen | Yes | **No** | **Missing** |
| shouldSuppressHotkey() | Yes | **No** | **Missing** |

**Result**: Missing debug/utility hotkeys and chat suppression guard.

---

## 4. /status Payload Details

| Field | Original | Modular | Gap |
|-------|----------|---------|-----|
| plugin | Yes | Yes | OK |
| status | Yes | Yes | OK |
| port | Yes | Yes | OK |
| preferredPort | config.localPort | serverPort | **Wrong** |
| endpoints[] | Yes | Yes | OK |
| player | Yes | Yes | OK |
| spellbook | Yes | Yes | OK |
| interfaces | Yes | Yes | OK |
| vitals | Yes | Yes | OK |
| wealth | Yes | Yes | OK |
| automation | Yes | Yes | OK |
| selectedWidget | Yes | Yes | OK |
| captures | Yes | Yes | OK |
| prayerTargets..entities flat keys | Yes | Yes | OK |
| targetSnapshots | Yes | Yes | OK |
| loginRecovery | Yes | Yes | OK |

**Result**: Only `preferredPort` should be the configured preferred port, not the actual bound port.

---

## 5. Mob Farmer

The original mob farmer is ~3000+ lines. The modular version is a guarded first slice. Remaining gaps from the original:

- Menu entry diagnostics (`lastMobFarmerMenuEntries`, `lastMobFarmerActionAttempt`)
- Death / pending death handling
- Expected loot tile recording
- Combat lease / attack reissue stabilization
- Full loot policy (ownership, radius, hidden items, Ground Items modes)
- Inventory drop / replacement (InventoryDropService, ItemSafetyService)
- Emergency teleport / danger recovery
- Multi-target lists
- Config profiles / presets
- Login recovery paused-reason logic
- Path distance / real route checks

---

## 6. Woodcutter / Mining

| Feature | Original | Modular | Gap |
|---------|----------|---------|-----|
| Presets | Yes | Yes | OK |
| Scan radius | Yes | Yes | OK |
| Menu action | Yes | Yes | OK |
| Inventory drop policy (`dropPolicy*` config, full/idle opportunity, candidate scan, live Drop) | Yes (`InventoryDropService`+`ItemSafetyService`) | Yes (2026-06-23) | OK — ported as `cvhelpermod.service.{ItemSafetyService,InventoryDropService,CvHelperDropMode}`, wired into `SkillFarmerService.step()`/`evaluateDropPolicy()` |
| WoodcutterInventoryManager (respawn-aware stick-to-target/inventory state machine) | Yes | **No** | **Missing** — separate from the drop policy above; tracks chop-state transitions, not droppable item scanning |
| Respawn timers | No | No | Same |
| Destructive dropping beyond `dropPolicy*` | No | No | Same (deferred) |

---

## 7. UI Panel

The modular panel is intentionally simpler. Missing from original panel:
- Mob farmer controls (start/stop/step/dry-live)
- Skill farmer controls
- Action hotkey editor
- Capture buttons
- Debug tools
- Config sync helpers

These are largely duplicated by the verifier web UI, so they are lower priority for backend parity.

---

## 8. Overlay

| Feature | Original | Modular | Gap |
|---------|----------|---------|-----|
| Prayer/spell/minimap/etc boxes | Yes | Yes | OK |
| Show target labels | Yes | Yes | OK |
| Show hover overlay | Yes | Yes | OK |
| Show widget info | Yes | Yes | OK |
| Show skill farmer targets | Yes | Yes | OK |

---

## Action Plan (Priority)

1. ~~Wire GameStateChanged, MenuEntryAdded, MenuOptionClicked.~~ DONE
2. ~~Add shouldSuppressHotkey and guard hotkeys.~~ DONE
3. ~~Register debug, printBounds, captureScreen hotkeys.~~ DONE
4. ~~Add login-recovery focus-click after login.~~ DONE
5. ~~Add mob farmer menu diagnostics.~~ DONE
6. ~~Fix preferredPort in /status.~~ DONE
7. ~~Add hover overlay and skill farmer target overlay.~~ DONE
8. ~~Port ItemSafetyService/InventoryDropService and wire skill-farmer drop policy.~~ DONE (2026-06-23)
9. Continue with larger mob farmer parity in follow-up bricks (see below). Not attempted in a single pass — each is independently risky to a live-clicking automation tool and needs its own live verification, not a blind ~9000-line port.

### Why mob-farmer parity was not finished in one pass

The original `CvHelperPlugin.java` is 12,527 lines; the modular `MobFarmerService` is 1,830. The remaining gap is not boilerplate — it is interlocking behavioral state (combat lease/reissue timing, death/loot-tile timing, multi-target lists, login-recovery edge cases, config profiles) that the original spec explicitly built and tuned through live play-testing (see `ai-docs/memory/active.md` 2026-06-19 through 2026-06-22 entries). Porting all of it blind, without the same live-tick verification, risks shipping an automation loop that mis-clicks in a live game. Each remaining item below should be its own bounded task with a compile + live-smoke checkpoint, per `ai-docs/task-creation.md`.

Remaining mob-farmer follow-up bricks, roughly in the order the original spec implemented them:
- Menu diagnostics already ported; full combat lease / attack-reissue stabilization (`stabilization` status block) is not.
- Death / pending-death handling and `deathLootTiming` diagnostics.
- Full loot policy: Ground Items precedence tiers, stack-aware GE/HA thresholds, high-priority override reasons.
- `reattachAfterPickup` next-tick reattack scheduling.
- Emergency teleport / danger recovery.
- Multi-target lists and config profiles/presets (mirrors WebHelper local-profile UX already in the verifier).
- Login recovery paused-reason / scheduler diagnostics parity (`scheduler` status block).
- Real path-distance/route checks beyond the bounded BFS already ported into `PathfindingService`.

### 2026-06-23 follow-up: full mob-farmer behavioral port

A second, more aggressive pass ported nearly everything in the list above except multi-target lists/config profiles and emergency teleport. The user explicitly asked for "everything in" so it can be tested brick-by-brick rather than drip-fed. `MobFarmerService.java` grew from ~1830 to ~2400 lines. Ported (adapted to the tick-driven modular architecture rather than the original's background-thread loop):

- **Action scheduler** (`MobFarmerActionKind`, `actionAllowed`/`recordScheduledAction`/`schedulerStatus`) — tick-throttles COMBAT/LOOT_PICKUP/SURVIVAL/INVENTORY/LOGIN_RECOVERY actions. Exposed under `status.scheduler`.
- **Login recovery** — found and fixed a **dead-code bug**: the existing `loginRecoveryPausedReason()` scaffolding read config keys without the `mobFarmer` prefix (`loginRecoveryEnabled` instead of `mobFarmerLoginRecoveryEnabled`, etc.), so it always fell through to `login-recovery-disabled` regardless of saved settings. Fixed the key names, added `loginWorldBlockReason()` (F2P-only guard via `WorldType.MEMBERS`), and wired an actual recovery attempt (`tryLoginRecovery()`, called from `onGameTick` when live and not logged in) that delegates the click/Enter dispatch to the existing `LoginService` with a 4s cooldown. Exposed under `status.loginRecoveryFull`.
- **Autorun toggle** (`tryAutorun`) — checks run energy vs `mobFarmerAutorunMinEnergy`, toggles via the minimap run-orb widget's `CC_OP` action (not a physical Robot click, unlike the original). Exposed under `status.autorun`.
- **Combat stabilization + intent/oscillation** — `recordIntent`/`oscillationDetected`/`updateProgressStatus`/`makeProgressActive`/`clearLootChase` ported; attack-reissue now also gated through the scheduler (`ATTACK_REISSUE_MIN_TICKS`). Exposed under `status.progress`, `status.recentIntents`, `status.multiCombat` (via `VarbitID.MULTIWAY_INDICATOR`).
- **Reattack-after-pickup** (`queueReattackAfterPickup`/`tryReattackAfterPickup`/`clearReattackAfterPickup`) — after a live loot `Take`, queues a wait-then-reattack instead of immediately trying to attack the same tick; checked at the top of `step()` on subsequent ticks.
- **After-loot combat guard** (`CvHelperAfterLootCombatMode`: `RESUME_TARGETING`/`STAY_ON_CURRENT_ATTACKER`/`STOP_WHEN_TAGGED`) — `tryAfterLootCombatGuard()` holds or stops the farmer if an NPC tagged the player within `LOOT_RESOLUTION_MAX_TICKS` of the last pickup.
- **Intermediate inventory actions** (bones→Bury, ashes→Scatter|Bury, plus `mobFarmerIntermediateActionMappings`-driven custom rules) — `tryIntermediateAction()`, simplified vs. the original's multi-rule-class parser but functionally equivalent for the common cases.
- **Loot config status export** (`lootConfigStatus()`) — full diagnostic dump of loot/intermediate/after-loot config, exposed under `status.lootConfig`.

**Deliberately not ported** (still open, now genuinely large/risky enough to warrant their own session): multi-target lists, config profiles/presets, emergency teleport/danger recovery, full Ground Items precedence-tier nuance (the existing always-loot/highlighted/min-value/blacklist handling already in `selectLoot` was left as-is), priority-loot-can-interrupt-combat as a distinct gate (loot-during-combat already works via the existing `lootDuringCombat` flag, just without the original's "only interrupt for allowlist/despawn/high-value reasons" nuance).

**Compiles clean** (`:client:compileJava` → `BUILD SUCCESSFUL`) but **none of this has been live-tested yet**. Suggested brick-by-brick live test order (each is independently toggleable via its own config flag, so you can test one at a time):
1. Auto-eat (`mobFarmerAutoEatEnabled`) — already covered in the prior session's test plan.
2. Login recovery (`mobFarmerLoginRecoveryEnabled` + `mobFarmerAutoResumeAfterLogin` + `mobFarmerLoginClickToPlayEnabled`) — log out manually while the farmer is running live, confirm it clicks back in within ~4s of reaching the login screen, check `status.loginRecoveryFull.last`.
3. Autorun (`mobFarmerAutorunEnabled`, `mobFarmerAutorunMinEnergy`) — drain run energy below threshold, confirm it does NOT toggle; restore energy above threshold with run off, confirm it does.
4. Intermediate actions (`mobFarmerIntermediateActionsEnabled`, default bones/ashes or a custom `mobFarmerIntermediateActionMappings` rule) — kill something that drops bones, confirm `status.intermediate` shows it burying before re-attacking.
5. Reattack-after-pickup + after-loot guard (`mobFarmerAfterLootCombatMode`) — loot during combat with another mob nearby, confirm behavior matches the configured mode via `status.reattackAfterPickup`/`status.afterLootCombatGuard`.
6. Scheduler/intent/oscillation diagnostics (`status.scheduler`, `status.progress`, `status.recentIntents`) — these are read-only diagnostics, just confirm they populate sensibly during a normal farm session.
