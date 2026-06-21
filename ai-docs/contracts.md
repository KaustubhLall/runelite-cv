# Permanent Contracts

This file records durable contracts between moving parts of the project. Update it whenever server, Python, plugin, or automation boundaries change.

## Contract Types

- Server API contracts: endpoints, request/response shapes, auth assumptions, error formats, and versioning.
- Python bridge contracts: input/output schemas, process lifecycle, logs, health checks, and failure behavior.
- RuneLite plugin contracts: plugin configuration, expected server behavior, UI integration points, and user-visible states.
- MCP/tool contracts: tools that must be available for automation, setup requirements, and fallback behavior.
- Data contracts: file formats, persisted state, cache rules, and migration expectations.

## Entry Template

```md
## Contract: Short Name

- Status: draft | active | deprecated
- Owners: Linear task, branch, or PR
- Components: server | Python | plugin | MCP | data
- Last updated: YYYY-MM-DD

### Purpose

What this contract protects.

### Contract

The stable behavior, schema, protocol, or expectation.

### Verification

How to confirm the contract still holds.

### Notes

Open questions, migration notes, or known limitations.
```

## Active Contracts

## Contract: CV Helper Local Export

- Status: active
- Owners: `OSR-2`, `OSR-3`, `OSR-4`, `OSR-5`, `OSR-6`
- Components: plugin | Python | data
- Last updated: 2026-06-17

### Purpose

Expose lightweight RuneLite UI capture state to local development tools without relying on OS-level screenshots.

### Contract

`CV Helper` serves local-only HTTP endpoints on `127.0.0.1` when localhost export is enabled. The plugin is enabled by default during local development so the right-side RuneLite navigation panel is available immediately after startup. Coordinates are RuneLite canvas coordinates unless a field explicitly says otherwise.

- `GET /status` returns JSON containing plugin name, latest status, active local port, endpoint discovery, player status summary, spellbook metadata, current interface/tab metadata, latest capture paths/statuses, current cached target counts, vitals, selected widget state, and coarse wealth/risk summaries.
- `GET /player/status` returns client/player state including login state, world, base/player coordinates, mouse canvas position, optional local-player/self screen bounds, run energy, special attack energy/enabled state, HP/prayer boosted and real levels, weight, spellbook metadata, selected widget state, inventory/equipment value summaries, skill stats, and prayer active/export state where available. Inventory summaries treat the standard player inventory as 28 slots even if the backing item array only contains occupied entries.
- `POST /capture` queues an in-client screenshot capture with the RuneLite frame and returns JSON acknowledgement. Capture status should include the saved path when the asynchronous save completes.
- `POST /capture/screen` queues a raw client-canvas capture and records the saved path when available.
- `POST /capture/minimap` queues a minimap crop using visible minimap widget bounds and records the saved path when available.
- `GET /targets/prayer` returns visible prayer-related widget targets with canvas bounds and center click points.
- `GET /targets/spell` returns visible spellbook target widgets plus current spellbook metadata. Spell configuration is spellbook-level; individual spell targets are discovered dynamically from visible widgets.
- `GET /targets/minimap` returns visible minimap/orb controls, including compass/north, HP, prayer, quick prayer, run, special attack, world map, and minimap draw area where visible.
- `GET /targets/inventory` returns slot-sized inventory child widgets only. It must not emit the parent inventory container as a fallback slot target.
- `GET /targets/equipment` returns slot-sized equipment/equipped-item widgets only. It follows the same no-parent-container fallback rule as inventory.
- `GET /targets/panels` returns side-panel tab/navigation controls for opening interfaces such as combat options, stats, quests, inventory, equipment, prayer, and magic.
- `GET /targets/combat` returns visible combat option / attack-style widgets where available.
- `GET /targets` returns a combined target snapshot for verifier/Python convenience.
- `GET /entities` returns nearby players and NPCs around the local player with names, ids where available, world/local location, world area, combat level, animation, pose animation, health ratio/scale, dead/effectively-dead state, interacting actor summary, attackability where available, distance from the local player, canvas bounds where RuneLite exposes them, `center`, `canvasTileCenter`, and a preferred canvas-space `clickPoint`.
- `GET /entities/nearest` returns the closest exported player/NPC with a usable canvas-space `clickPoint`, intended as a first controller-friendly target for Python click planning.
- Target payloads should include freshness metadata such as `fresh`, `lastSeenAt`, or equivalent. Last-known panel positions may be served after panels close, and should be marked cached/stale.
- The server should prefer a fixed/default local development port. If binding fails, it may fall back to an ephemeral port, but `/status` and the RuneLite panel must expose the active port.
- Screenshot files are written as explicit `cv-helper-*.png` files under `C:\Users\kaust\.runelite\screenshots\<player>\manual\`, and `/status` exposes each latest capture `savedPath`.
- If RuneLite is on the login screen, `/status` should still respond but live widget/polling surfaces may be empty until the user logs in. This is expected, not a credential or CV Helper failure. Screenshot capture is intentionally blocked and the plugin status reports `capture-blocked:login-screen`.
- `POST /login/click` or `GET /login/click` queues a guarded click on RuneLite's visible click-to-play/login widget when the client is on a login screen. If that widget is not visible, it may press Enter only when the client is on a safe login screen/world; otherwise it must report/skip rather than clicking arbitrary screen coordinates.
- Mob-farmer login recovery is separate from anti-idle handling. When enabled for a live loop, it may queue the guarded click-to-play/login-widget action after a normal logout/login-screen state, and it may queue a guarded Enter press for RuneLite `CONNECTION_LOST` inactivity/disconnect recovery when that setting is enabled. It must not generate random mouse movement or fake input. The recovery status reports current client login state, detected screen/state, macro running/live/worker state, click-to-play/disconnect/auto-resume settings, preferred/current world, `preferredWorldReady`, `worldBlockReason`, whether the F2P/special-world guard allows recovery, `pausedReason`, `willAttemptOnNextRecoveryTick`, and `lastClickAttempt` action/failure details. Users should configure RuneLite's Logout Timer plugin/settings if they want a longer idle window.
- Mob-farmer progress diagnostics include `progress` and `recentIntents` in `/automation/mob-farmer/status`. The intent tracker records attack/loot decisions, detects recent attack/loot alternation, reports active loot-chase distance improvement, and opens a short make-progress preference window only when oscillation is observed.
- Mob-farmer scheduling is tick-aware while logged in: live farming steps are driven from RuneLite `GameTick`, while the background loop is retained only for logged-out/login/recovery states where game ticks may not advance. `/automation/mob-farmer/status.scheduler` reports the current tick, last step tick/source, per-action-kind minimum tick policy, last issued action ticks, and the reason a combat, movement, loot pickup, inventory, survival, UI/config, or login-recovery action was allowed or paused.
- Mob-farmer death/loot timing diagnostics are exposed as `/automation/mob-farmer/status.deathLootTiming`. When the current target is effectively dead or zero-HP, the loop stops treating it as attackable, records the detected tick, expected loot tile, HP/death state, and waits through a short loot-spawn grace window before abandoning the expected drop. The current implementation records the expected movement opportunity but does not yet issue speculative tile movement clicks.
- Java-to-Python push is optional and best-effort through a configured local webhook URL. Python-to-Java pull remains the source of truth for current target geometry.
- Current webhook events are `server-started`, `prayer-targets`, `spell-targets`, other `*-targets` surfaces, and `capture-saved`.
- `OSR-3` owns the first Python receiver contract for consuming local endpoints and webhook payloads.
- `OSR-4` owns the future tick-synchronized delivery contract: state changes enqueue snapshots immediately, then queued exports flush on RuneLite `GameTick` for coherent tick-aligned Python decisions.
- `OSR-5` owns hotkey action work. Current CV Helper hotkeys include configurable RuneLite `Keybind`s for debug status, bounds printing, raw screen capture, entity refresh, nearest-entity click-point logging, side-panel switching, and 22 configurable action slots. Each action slot has an enabled toggle, keybind, target surface, editable target dropdown populated from the same exported target snapshots used by the verifier, a refresh-choices action, sequence-memory reset, invocation mode (`AUTO`, `WIDGET`, `CLICK`), click-after mode (`AUTO`, `ALWAYS`, `NEVER`), optional return-to-previous-panel behavior, and optional restore-mouse-to-original-position behavior. Action slots can click exported prayer/spell/UI targets directly; spell-like flows can click a spell target and then click the current OS mouse position captured before the robot moves.
- Prayer/spell/inventory/equipment/combat action slots must open the required side panel before resolving targets if that panel is not already active. The click-after target must be the exact OS mouse position captured at hotkey press time, with no random offset. Randomized safe-circle clicks apply only to exported widget/entity targets, not the user's hovered target.
- Combat spell action slots must force click-after in `AUTO`, retry target resolution after opening the spellbook, and wait briefly after selecting the spell before clicking the captured mouse target. This path specifically covers Wind Strike-style "hover target, press hotkey" casts.
- Spell action slots default to `AUTO`. For self-resolving spells such as teleports, `AUTO` may use normal RuneLite widget operations. For targeted/non-teleport spells, `AUTO` must use physical spell selection because live testing showed widget selection does not reliably select combat spells. `WIDGET` may try widget selection but must fall back to physical spell selection before the target click if RuneLite does not report a selected widget. `CLICK` forces Java Robot target clicks for debugging or fallback. The OS mouse should only be used for the optional current-target click, click-based panel fallback, and mouse restore unless `CLICK` mode is selected.
- Prayer action slots should invoke the prayer widget through RuneLite widget actions and prefer direct friendly prayer component targets over generic child-widget labels. Targeted spell action slots must select the spell, click the captured mouse target if click-after is enabled, and only then restore the previous side tab. Mouse clicks, widget actions, or menu actions on side-panel tabs must never occur while a targeted spell is selected but not yet consumed because that interaction cancels/casts the spell into the UI. If click-after is disabled for a targeted spell, `Return tab` may use the configured panel keybind but must not click a side-panel tab.
- Required panel opening should prefer exported side-panel tab click-points because this happens before a prayer/spell/action is selected and avoids wrong RuneLite profile key mappings. Return-to-previous-panel should prefer exported tab click-points only after the selected action has been consumed by a target click or self-resolving action. If the selected action is still unconsumed, return may use a configured keybind but must not mouse-click a tab. If the previous panel is unknown, default return target is inventory. Target resolution must prefer freshly collected surface targets, then fall back to last-known exported targets for that surface so verifier-visible targets remain usable during panel-open races.
- CV Helper action hotkeys use a pre-dispatcher so configured CV keys are seen before other RuneLite plugins can consume them, while still suppressing hotkeys during chat/text input. Keep the normal RuneLite `KeyManager` listeners as a fallback.
- `NEAREST_ENTITY` actions support an empty target or `Nearest clickable entity` for the nearest clickable actor, partial name matching such as `goblin`, and numeric NPC id matching such as `id:1234`. Matching chooses the nearest clickable entity among candidates.
- Action timing values should be configurable: panel-open delay, target-resolve retry delay, mouse-settle delay, widget-target delay, selected-widget timeout, return-panel delay, and mouse-restore delay. The selected-widget timeout defaults short because non-teleport targeted spells should fall back quickly to physical selection when RuneLite widget selection does not report selected state. These timings are the first local form of the future external action-sequence executor contract.
- Only one CV Helper action sequence should run at a time. Rapid repeated hotkeys while an action is executing are ignored so they cannot interrupt panel switching or click-after targeting.
- Action target matching should use one normalized target haystack including explicit semantic label, widget name/text, and actions. Explicit labels provided by CV Helper (for example `Thick Skin`) must beat generic widget metadata.
- Action target fields can contain fallback lists separated by `|`, comma, semicolon, or newline. Candidates are resolved in order against fresh targets, then cached targets. Action target fields can contain memory sequences separated by `->`; a successful action advances that slot's sequence pointer. Inventory/equipment item matching is dose-agnostic for potion names and prefers lower-dose matches before fresh potions.
- Prayer action slots expose `TOGGLE`, `ON_ONLY`, and `OFF_ONLY` modes. Guarded modes must resolve the actual prayer state and skip no-op clicks rather than toggling accidentally.
- Spell action slots expose a spell availability guard. Exported spell targets include widget metadata (`opacity`, `textColor`, `clickMask`, `spellUnavailable`) so clients can inspect or override the guard. The default guard only blocks clearly unavailable/shaded widget targets.
- `/status` and the verifier expose `selectedWidget` so targeted spell failures can distinguish "spell widget invoked but not selected before timeout" from bad target-click geometry.
- Wealth values are exported as a coarse tracker: `currentLootValueGe/Ha` is inventory value, equipment is separate, total carried combines inventory and equipment, and `riskedValue*Approx` currently equals total carried until an exact kept-items/death-risk model is implemented.
- CV Helper hotkeys must not fire while RuneLite chat/message-layer input is active or while CV Helper text fields have keyboard focus. Widget/entity click points must be converted from RuneLite real-canvas coordinates into displayed screen coordinates, including stretched-mode scaling, before Java Robot clicks.
- Action slot clicks should randomize within a safe circle inside the target bounds rather than always clicking the exact center point. The CV Helper side panel must expose these slots in a collapsible section so users do not need to discover them through RuneLite's generic configuration panel.
- `OSR-10` owns the first mob-farmer automation. Current CV Helper exposes `/automation/mob-farmer/status`, `/automation/mob-farmer/step`, `/automation/mob-farmer/start`, `/automation/mob-farmer/stop`, and `/automation/panic-stop`. The farmer accepts a `target` query parameter with partial name, `id:<npc id>`, or target-list semantics and a `live=true|false` query parameter. It must fail closed unless logged in, local player exists, current combat state is safe according to the configured aggro response, a matching NPC passes validity filters, and no other CV action is running. Validity filters include attackable/interactible NPC action metadata, dead/zero-HP filtering, already-engaged target handling, single-vs-multi combat policy, optional line-of-sight, and optional max distance. When attack interaction is `DIRECT_CLICK`, the selected NPC must have a click point; when attack interaction is `MENU_ACTION`, the selected NPC must expose an `Attack` action index and CV Helper invokes the corresponding RuneLite NPC menu action. `/automation/mob-farmer/status` must expose the latest combat decision, candidate skip reasons, target candidates, `survivalDecision`, `intermediateDecision`, `lootDecision`, `lastActionAttempt`, `recentMenuEntries`, `inventory`, and `lootCandidates`. Dry mode reports the target/click/menu action without clicking and prints it to in-game chat; live mode performs guarded interactions. Panic stop stops CV loops, invalidates queued loop steps, interrupts the loop sleeper, and clears the action-in-progress guard. Auto-eat and first-pass loot filters are implemented: auto-eat opens inventory when needed, invokes matching inventory menu actions (`Eat`, `Drink`, or `Consume`) below the HP threshold, and either stops or warns/continues on no food depending on `Stop if no food`; loot scans visible ground items using allowlist/min-value/blacklist/ownership/radius/inventory-capacity rules. Loot interaction defaults to RuneLite `Take` menu actions using scene coordinates so hidden/deprioritized Ground Items do not rely on left-click visibility. Before a live `Take`, CV Helper must revalidate that the target ground item still exists on the same scene tile and remains selectable; stale or rejected loot attempts return control to the state machine instead of trapping the loop. Ground Items integration imports highlighted items as supplemental always-loot candidates and records hidden-list, hide-under-value, and show-highlighted-only suppression metadata; hidden/suppressed matches only block pickup when `Respect hidden Ground Items` is enabled and the item is not explicitly allowed by CV Helper. Optional intermediate inventory actions use configurable item-to-action mappings such as `bones -> Bury` and `ashes -> Scatter|Bury`; status reports selected item, slot, matched rule, configured action(s), available item actions, intended/actual action, menu params, result, and failure reason. Missing configured actions are skipped rather than falling back to `Use` or `Drop`, and `Drop` remains blocked for intermediate actions even if it is visible. Login recovery runs while the live farmer loop is active and reports `macroRunning`, `macroLive`, `recoveryWorkerActive`, `pausedReason`, `willAttemptOnNextRecoveryTick`, preferred/current world readiness, `worldBlockReason`, and `lastClickAttempt` diagnostics including selected login widget, click point, Enter fallback, actual action, and failure reason. Real route pathing, automatic world switching beyond guarded preferred-world validation, emergency teleport, automatic inventory dropping, config profiles, and external plan composition are follow-up contracts.
- Click reliability uses menu-action primitives for first-pass NPC attacks and ground-item pickup. Keep `DIRECT_CLICK` as a debug/fallback interaction mode, but prefer `MENU_ACTION` when left-click geometry is obstructed by drops, players, hidden Ground Items, or scene/UI overlap.
- Farmer loot/survival flow should remain ordered and inspectable: HP/food guard first, optional intermediate inventory actions, high-priority loot override, optional loot during combat, configurable attack-before-loot while idle, then loot fallback when no valid attack target exists. Auto-eat reports whether survival preempts actions, current HP/threshold, selected food, preempted decision/intents, no-food behavior, and the actual inventory action. High-priority loot can be triggered by explicit always-loot, Ground Items highlight, GE threshold, despawn urgency, or cleanup pile pressure; each candidate reports `highPriority` and `priorityReasons`. Current inventory handling reports free slots, protected never-drop items, and the lowest-value unprotected drop candidate, but automatic dropping is intentionally deferred.
- Mob target selection includes a conservative local collision-path check for matched NPCs. The first implementation runs a bounded BFS over `WorldArea.canTravelInDirection(...)` and requires a collision-valid route to melee reach, not just straight-line distance or line-of-sight. Candidate reports expose `reachable`, `pathDistance`, `pathSearchLimit`, `pathVisited`, and `pathFailureReason`; unreachable matched targets are rejected with `unreachable:<reason>`, and reachable-but-long routes can be rejected with `path-too-far`. Door/object-opening navigation and repeated failed-target blacklisting remain follow-up bricks.
- Successful live ground-item `Take` actions queue a pickup-resolution hold before any reattack. `/automation/mob-farmer/status.reattachAfterPickup` reports whether a reattack is pending, queued/attempt ticks, last pickup/attack ticks, estimated cooldown, selected target, result, and clear reason; while the exact item/tile is still visible inside the short resolution window, the farmer holds instead of interrupting its own walk-to-loot. If the item remains visible after the resolution window, the exact `loot:itemId@sceneX,sceneY` key is temporarily rejected with `unresolved-loot-cooldown:Nt` and exposed in `loot.temporaryLootSkips`. `/automation/mob-farmer/status.stabilization` reports combat leases, attack reissue holds, loot-resolution holds, and temporary loot skips. Survival still runs before these stabilizers; weapon-specific attack speed modelling remains a follow-up beyond the current approximate reissue guard.
- `OSR-6` owns the verifier client site. Browser consumers call CV Helper through `http://127.0.0.1:<port>`, so plugin JSON responses include permissive local-development CORS headers.
- `OSR-7` owns the first reusable automation loop: a configurable auto mob farmer, initially validated on Lumbridge cows. It is intentionally blocked by `OSR-5`, `OSR-3`, and `OSR-4` until action primitives, Python receiver state, and coherent/tick-aware snapshots are reliable enough.
- Automation work must be built as bricks: action primitives, entity targeting, combat state, HP/food interrupts, loot policy, and exit/pathing policy. Do not hide broken clicking or panel switching under a larger automation loop.
- The first mob-farmer implementation order is dry-run state machine, mob target selection from `/entities`, safe click-to-attack, combat-state monitor, HP/food interrupt, loot pickup policy, stop/report exit, then marked-tile/pathing integration.

### Verification

- `.\gradlew.bat :client:compileJava`
- `powershell -ExecutionPolicy Bypass -File .\scripts\launch-dev-runelite.ps1` after Jagex credentials exist.
- Confirm logs contain `injected-client - read 5 credentials from disk`, external plugin load lines, `CV Helper starting`, and `CV Helper local export listening on http://127.0.0.1:<port>/status`.
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/status`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/player/status`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/capture -Method Post`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/capture/screen -Method Post`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/capture/minimap -Method Post`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/targets/prayer`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/targets/spell`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/targets/minimap`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/targets/inventory`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/targets/equipment`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/entities`
- `Invoke-RestMethod -Uri http://127.0.0.1:<port>/entities/nearest`
- Confirm status changes to `saved@...` and a `cv-helper ...png` appears in `C:\Users\kaust\.runelite\screenshots\`.
- For live widget verification, ask the user to log in to the newest launched custom client before treating empty target counts as meaningful.
- For inventory verification, an item hover/drag should show only slot-sized overlays. A large inventory-panel-sized duplicate is a contract violation.
- Open `tools/cv-helper-verifier/index.html` and connect to the fixed/default CV Helper port to verify browser access, status polling, target tables, warnings, capture paths, and capture buttons.

### Notes

The next feature step is a combined target endpoint, stable semantic target names for Python, equipment label cleanup, Python receiver implementation, tick-synchronized export flushing, and hotkey intent prototyping.
