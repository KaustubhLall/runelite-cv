# CV Helper Spec

This is the working product/specification note for `OSR-2`.

## Purpose

`CV Helper` is a RuneLite plugin that helps an external Python computer-vision/autonomous-agent process play OSRS by exporting reliable RuneLite UI geometry and interaction affordances over localhost.

The Python process should not have to infer every UI coordinate from pixels. When RuneLite already knows widget bounds, names, actions, and visibility, `CV Helper` should expose that data as a stable helper API.

## Implementation Baseline

As of the OSR-48 / PR #2 cutover, `net.runelite.client.plugins.cvhelpermod` is the source of truth for CV Helper behavior and WebHelper v3 is its primary human verification surface. The older `net.runelite.client.plugins.cvhelper` package is retained only as legacy/reference code and must not receive new feature work by default.

## Current Verified Baseline

- Custom RuneLite launches through `scripts/launch-dev-runelite.ps1`.
- Jagex Launcher credential bootstrap works for `CoreDump` / `C0REDUMPED`.
- Plugin Hub loads through stable service overrides while running the local snapshot jar.
- `CV Helper` appears in the right-side plugin bar and renders mouse coordinate overlay.
- Localhost export works.
- Screenshot capture works through the local endpoint.
- Default World profile config is pinned to world `301` in the local RuneLite profiles.
- The right-side panel includes live overlay toggles, nested prayer toggles, nested spell toggles, server/webhook settings, and player status refresh.
- Large prayer/spell settings start collapsed so the panel stays compact.
- Prayer and spell target overlays are drawn live when the relevant interfaces are visible; target endpoints still provide pull-based snapshots for Python.
- Optional target labels draw target names next to visible target rectangles.
- Spellbook state is exported in `/status`, `/player/status`, and `/targets/spell`.
- Minimap/orb, inventory, and equipment target surfaces are exported and have independent overlay toggles.
- Capture endpoints now cover framed client, raw screen, and minimap crops.
- Inventory and equipment target export should only include slot-sized child widgets. Do not export parent inventory/equipment containers as if they were slots; those large container boxes are misleading for Python click planning.
- Verified on 2026-06-17: dragging/hovering the dragon axe exported one inventory target with `36x32` bounds and no giant duplicate inventory panel target.
- Verified on 2026-06-17: fresh client on port `55312` exported 17 real prayer targets with the prayer tab visible. Non-prayer panel controls such as `Filters` and the prayer meter are filtered out of `/targets/prayer`.
- Verified on 2026-06-17: browser-facing CORS headers are present on CV Helper JSON responses, allowing the static verifier to call the local API.
- `OSR-6` tracks the local verifier client site in `tools/cv-helper-verifier/`.
- Implemented on 2026-06-17: CV Helper prefers fixed localhost port `11777`; if that port is busy, it falls back to an open port and reports the active port in `/status`.
- Implemented on 2026-06-17: capture endpoints save explicit `cv-helper-*.png` files under RuneLite's screenshot directory and expose saved path/status in `/status`.
- Implemented on 2026-06-17: `/player/status` and `/status` export mouse position, current player world/local coordinates, self/player screen bounds if available, current open interface/tab metadata, capture statuses, and target snapshot status.
- Implemented on 2026-06-17: panel/navigation targets, compass/north target, combat option/attack-style targets, and nearby player/NPC entities are exported.
- Implemented on 2026-06-17: target endpoints keep last-known UI target positions cached after panels close and mark whether exported targets are fresh or cached/stale. This lets the controller click a panel/tab target even after the panel has closed, as long as the layout has not changed.
- Implemented on 2026-06-18: configurable CV Helper hotkeys are available for debug status, bounds printing, raw screen capture, nearby entity refresh, nearest-entity click-point logging, and four configurable action-click slots.
- Implemented on 2026-06-18: entity exports include `center`, `canvasTileCenter`, and preferred canvas-space `clickPoint`; `/entities/nearest` returns the closest clickable player/NPC export.
- Implemented on 2026-06-18: the CV Helper side panel has a collapsible `Action hotkeys` section so action slots can be configured without hunting through RuneLite's generic config panel.
- Implemented on 2026-06-18: action slots expose invocation mode (`AUTO`, `WIDGET`, `CLICK`) and a refresh-choices button. Target dropdowns use the same cached/live target snapshots as overlays, verifier tables, and Python exports, with broader Standard spell seed names such as Crumble Undead for hidden-tab setup.
- Implemented on 2026-06-18: `/status` and `/player/status` export grouped vitals (`HP`, prayer points, run, special attack, active prayers), selected-widget state, and coarse inventory/equipment/current-loot/risked-value summaries.

## Core Responsibilities

- Provide a right-side settings panel for CV Helper controls.
- Render optional overlays for mouse coordinates, widget metadata, and discovered target boxes.
- Export current UI targets over localhost for Python consumers.
- Forward target snapshots/events to an optional Python webhook.
- Keep all exported coordinates in RuneLite canvas coordinate space unless a field explicitly says otherwise.

## Initial Target Surfaces

- Prayer tab and individual prayer widgets.
- Quick-prayer orb and quick-prayer selection widgets.
- Inventory panel and inventory slots.
- Equipment panel.
- Minimap/orbs.
- Compass/north control.
- Side-panel tab controls: combat options, stats, quests, inventory, equipment, prayer, magic, etc.
- Combat options panel and attack-style widgets.
- Chat box and action prompts.
- Current mouse cursor position.
- Current player/self screen bounds where RuneLite exposes them.
- Nearby players, NPCs, and game objects.
- Viewport/player/NPC/object affordances when RuneLite exposes stable geometry.

## Local API Shape

All endpoints are local-only and bind to `127.0.0.1`.

The preferred default port is fixed for local development. If that port cannot be bound, CV Helper may fall back to an ephemeral port but must expose the active port in `/status` and the RuneLite panel.

### `GET /status`

Returns plugin health and endpoint discovery.

```json
{
  "plugin": "CV Helper",
  "status": "server@65510",
  "port": 65510,
  "endpoints": [
    "/status",
    "/capture",
    "/capture/screen",
    "/capture/minimap",
    "/player/status",
    "/targets/prayer",
    "/targets/spell",
    "/targets/minimap",
    "/targets/inventory",
    "/targets/equipment"
  ],
  "spellbook": {"name": "Standard", "varbit": 0, "spellbookId": 1234, "visible": true}
}
```

### `POST /capture`

Queues an in-client screenshot with the RuneLite client frame.

```json
{"ok": true, "queued": true}
```

### `POST /capture/screen`

Queues a raw client-canvas capture without the RuneLite client frame.

### `POST /capture/minimap`

Queues a minimap crop using the current visible minimap widget bounds.

Capture responses and webhook payloads should include `type`, optional `crop`, `savedPath` when known, `player`, `spellbook`, `prayerTargets`, and `spellTargets`.

### `GET /targets/prayer`

Returns visible prayer-related target widgets with click centers.

```json
{
  "surface": "prayer",
  "generatedAt": "2026-06-17T02:38:02Z",
  "gameState": "LOGGED_IN",
  "count": 1,
  "targets": [
    {
      "surface": "prayer",
      "widgetId": 35454976,
      "parentId": -1,
      "index": 0,
      "type": 0,
      "name": "Protect from Magic",
      "text": "",
      "actions": ["Activate"],
      "spriteId": 123,
      "bounds": {"x": 700, "y": 240, "width": 34, "height": 34},
      "center": {"x": 717, "y": 257}
    }
  ]
}
```

### `GET /targets/spell`

Returns visible spellbook target widgets with click centers and the current spellbook metadata. The overlay draws spell targets in orange. Spell config is spellbook-level (`Standard`, `Ancient`, `Lunar`, `Arceuus`), while individual spells are discovered dynamically from the visible spellbook widgets.

### `GET /targets/minimap`

Returns visible minimap and orb controls with click centers, including minimap area, compass/north, HP orb, prayer orb, quick prayer orb, run orb, run toggle, special attack orb, and world map orb where visible.

### `GET /targets/inventory`

Returns visible inventory slot targets with click centers, item id, quantity, widget ids, and bounds. This may include regular inventory, bank inventory, or equipment-side inventory slot containers depending on the open interface.

Important: inventory export is slot-only. Parent inventory panels must not be emitted as fallback targets because they can appear as a giant duplicate `inventory slot 1` overlay and break downstream click planning.

### `GET /targets/equipment`

Returns visible equipped-item/equipment slot targets with click centers, item id, quantity, widget ids, and bounds. Equipment export follows the same slot-only rule as inventory export.

### `GET /player/status`

Returns player and client state, including login state, world info, base coordinates, player world/local coordinates, mouse canvas position, optional self/player screen bounds, run energy, special attack energy/enabled state, HP/prayer boosted and real levels, weight, current spellbook, selected widget state, current visible interface/tab metadata, coarse inventory/equipment/current-loot/risked-value summaries, all skills, and prayer active/export state where available.

Inventory/equipment summary counts should reflect real slot capacity, not just the number of occupied `Item[]` entries. The standard player inventory is 28 slots, so `freeSlots` should remain meaningful even when the exported item list is sparse or only partially refreshed.

### `POST /login/click` / `GET /login/click`

Queues a guarded click on RuneLite's visible click-to-play/login widget. This is a convenience for local testing when the client is waiting at the login screen. It must skip and report status if the client is not on a login screen or if RuneLite does not expose a visible login widget. When no visible login widget is exported but the client is on a safe login screen/world, CV Helper may press Enter as a bounded login recovery fallback and records that exact fallback in status.

The mob farmer can optionally reuse this guarded login click for live-loop recovery after a normal logout/login-screen state. It also has a separate inactivity-disconnect recovery setting for RuneLite `CONNECTION_LOST`, which queues a guarded Enter press to advance the disconnect/login flow. Recovery is cooldown-gated, reports current client login state, detected screen/state, click-to-play/disconnect/auto-resume settings, whether the live macro worker is active, current/preferred world, `preferredWorldReady`, `currentWorldAllowed`, `worldBlockReason`, and F2P guard decisions under `/automation/mob-farmer/status`. `loginRecovery.lastClickAttempt` records the selected login widget, canvas/screen click point, Enter fallback eligibility, actual Robot action invoked, and failure reason when available. Recovery is intentionally separate from idle-extension behavior. To extend the idle logout window, configure RuneLite's Logout Timer plugin/settings rather than relying on CV Helper to move the mouse or generate fake activity.

Mob-farmer status also includes progress diagnostics under `progress` and `recentIntents`. These fields record recent high-level intents such as `ATTACK_TARGET` and `LOOT_ITEM`, expose target keys/distances, flag attack/loot alternation, and report whether the short make-progress preference window is active.

Mob-farmer status includes tick-aware scheduling diagnostics under `scheduler`. Logged-in farming steps run from RuneLite `GameTick`; the background loop is retained for login/recovery states where ticks may not advance. The scheduler payload reports the current tick, last step tick/source, action-kind minimums, last issued action ticks, and the last allow/wait reason for combat, movement, loot pickup, inventory, survival, UI/config, and login-recovery actions.

Mob-farmer status includes death/loot timing diagnostics under `deathLootTiming`. When the current target is effectively dead or zero-HP, it records the detected tick, expected loot tile, target HP/death state, and a short loot-spawn grace window. This is diagnostic and conservative for now: it does not yet issue speculative movement clicks toward the expected tile.

Mob-farmer status also reports configurable recovery-loop delay, autorun, startup focus-click, and after-loot combat policy. `autorun` includes enabled/min-energy/current energy/run-enabled/last result. `startupFocus` includes enabled/needed/last result. `afterLootCombatMode` controls whether the farmer resumes normal targeting after loot, holds when an attacker is already on the player, or stops when tagged.

### `GET /automation/mob-farmer/config`

Returns the current mob-farmer config, field schema, and action-slot settings in a readable versioned JSON shape:

```json
{
  "version": 1,
  "settings": {
    "target": "cow",
    "recoveryLoopDelayMs": 1200,
    "autorunEnabled": false,
    "focusClickAfterLogin": false,
    "afterLootCombatMode": "STAY_ON_CURRENT_ATTACKER"
  },
  "schema": [{"key": "target", "label": "Mob targets", "type": "text", "description": "..."}],
  "actionSlots": [{"slot": 1, "enabled": true, "hotkey": "1", "surface": "PRAYER", "target": ""}]
}
```

### `POST /automation/mob-farmer/config`

Accepts the same readable JSON shape for WebHelper import/save. The plugin validates enum values, booleans, numbers, and text hotkeys before applying any setting. On failure it returns HTTP `400` with `{"ok":false,"applied":false,"errors":[...]}` and does not mutate current settings. Hotkeys use text such as `F12`, `CTRL+1`, `ALT+Q`, or `NOT_SET`.

Mob-farmer config now includes stack-aware loot thresholds (`lootMinSingleGe`, `lootMinStackGe`, `lootMinStackQuantity`, `lootAlwaysStackGe`, `lootNeverStackBelowGe`) and a safe High Alchemy policy (`highAlchEnabled`, `highAlchMinHa`, `highAlchMinDelta`, `highAlchMaxLoss`, `highAlchItems`, `highAlchBlacklist`). Loot candidates report quantity, GE each, GE stack, HA each, HA stack, allowlist/denylist match, Ground Items classification, final decision, and rejection reasons. High Alchemy is currently diagnostic/policy-only and does not cast until spell/rune guards are live-verified.

WebHelper stores named mob-farmer profiles locally in browser storage. Profiles are readable versioned JSON payloads and can be saved, loaded into the draft, duplicated, deleted, imported, and exported. Loading a profile does not mutate RuneLite until the user clicks Apply draft.

### `POST /automation/mob-farmer/focus-click`

Queues a guarded canvas-center focus click for the current RuneLite client. This is intended for the startup/login focus issue where movement or menu actions may not register until one manual click has occurred. The loop can also require this click after login via `focusClickAfterLogin`; survival/auto-eat still runs before startup focus helpers.

### `GET/POST /automation/mining/config`

Returns or applies a versioned mining farmer config:

```json
{
  "version": 1,
  "skill": "mining",
  "settings": {
    "target": "iron rocks|iron ore rocks",
    "live": false,
    "scanRadiusTiles": 24,
    "maxCandidates": 80,
    "protectedItems": "coins|rune pouch",
    "inventoryPolicy": "REPORT_ONLY"
  },
  "presets": [{"name": "Iron", "target": "iron rocks|iron ore rocks|id:11364|id:11365"}]
}
```

Mining status chooses the nearest reachable visible tile object with a matching name or `id:<object id>` and a `Mine` action. It scans relevant game/wall/decorative/ground object layers inside the configured `scanRadiusTiles` limit so odd object placements still appear in diagnostics without walking the entire scene. Status exposes selected rock, object type, tile, straight-line distance, path distance, reachability, visible flag, canvas bounds/click point, selected object menu action, candidate rejection reasons, inventory GE/HA value, scan radius, max candidate count, and lowest safe drop candidate. Live mode invokes the guarded object menu action for the matched action index; dropping ores and respawn-timer integration are report-only/follow-up in this pass.

### `GET/POST /automation/woodcutting/config`

Woodcutting mirrors the mining config/status shape with target tree lists, configurable scan radius/candidate limit, and presets for normal trees, oak, willow, maple, and custom targets. It requires a `Chop down` action, scans relevant tile-object layers instead of only plain game objects, chooses by path distance rather than straight-line distance, and exposes the same pathing/inventory/visibility/bounds diagnostics plus object type in candidate rows. Live mode invokes the guarded object menu action for the matched action index; dropping logs and tree-respawn integration are report-only/follow-up in this pass. The RuneLite sidebar exposes `Skilling target boxes`, backed by the `showSkillFarmerTargets` config option, to draw latest mining/woodcutting candidate bounds after scans.

WebHelper v3 renders each skill farmer's configured drop allowlist and protected/never-drop list directly in the overview. It parses the raw policy strings with the same pipe/comma/semicolon/newline delimiters as config input, shows explicit empty states, and uses labeled allow/protect chips with exact-token tooltips. This presentation is diagnostic only and does not alter drop-policy decisions.

### `GET /targets/panels`

Returns side-panel tab and navigation controls, including combat options, stats, quests, inventory, equipment, prayer, magic, clan, settings, and other visible fixed/resizable layout tab buttons where available. These targets should use last-known caching so they remain available after a panel closes unless the client is resized or the layout changes.

### `GET /targets/combat`

Returns visible combat option / attack style widgets, including style names/actions where RuneLite exposes them.

### `GET /targets`

Returns a combined snapshot of known target surfaces with fresh/cached metadata. This is the preferred verifier/Python convenience endpoint once implemented.

### `GET /entities`

Returns nearby entities around the local player where RuneLite exposes them. Initial entity types include nearby players and NPCs with names, ids where available, world/local location, combat level, animation, distance from the local player, screen/canvas bounds where RuneLite exposes them, `center`, `canvasTileCenter`, and preferred canvas-space `clickPoint`. Game objects remain a follow-up because they require scene tile traversal rather than actor lists.

### `GET /entities/nearest`

Returns the closest exported player/NPC with a usable canvas-space `clickPoint`.

```json
{
  "surface": "entities/nearest",
  "generatedAt": "2026-06-18T12:00:00Z",
  "gameState": "LOGGED_IN",
  "count": 4,
  "entity": {
    "type": "npc",
    "name": "Guard",
    "distance": 3,
    "worldLocation": {"x": 3222, "y": 3218, "plane": 0},
    "canvasBounds": {"x": 420, "y": 215, "width": 38, "height": 72},
    "center": {"x": 439, "y": 251},
    "canvasTileCenter": {"x": 440, "y": 286},
    "clickPoint": {"x": 439, "y": 251}
  }
}
```

## Bidirectional Bridge Design

Python-to-Java pull:

- Python calls local endpoints when it needs current UI geometry.
- First-class reads should include `status`, `capture`, target surfaces, current open interface metadata, last-known panel targets, and nearby entity snapshots.
- Future command endpoints can request a target by semantic name, but clicking should initially remain outside the plugin so the Python/controller layer owns action execution.
- `OSR-3` tracks the first Python receiver that consumes these endpoints and webhook payloads.

Java-to-Python push:

- CV Helper can be configured with a local Python webhook URL.
- On meaningful events, Java can POST target snapshots or state changes forward.
- Push should be optional and best-effort so a missing Python process does not destabilize RuneLite.
- `OSR-4` tracks changing push/export delivery to a tick-synchronized queue. The intended model is: UI/state changes update an internal queue immediately, then queued snapshots are flushed on `GameTick` so Python receives coherent tick-aligned state for tick-perfect decisions.

Initial push candidates:

- Plugin/server startup.
- Prayer target refresh.
- Screenshot capture saved.
- New target surface discovered.

Current push events:

- `server-started`
- `prayer-targets`
- `spell-targets`
- `capture-saved`

## Verifier Client Site

Tracked by `OSR-6`.

The first verifier is a static WebHelper app in `tools/cv-helper-verifier/`, served by `serve.ps1` at `http://127.0.0.1:8765/`. The helper exposes `/api/discover`, which probes `11777` first and then falls back through known browser ports and live local Java listeners until it finds a valid CV Helper `/status` response. The app uses top-level navigation for Dashboard, Farmers, Inventory, Actions, Configuration, Debug, and Raw Data, polls the active localhost endpoints, shows target counts and payload tables, and flags suspicious target geometry such as oversized inventory/equipment boxes or unnamed equipment slots.

The plugin adds CORS headers to JSON responses so the browser can call `http://127.0.0.1:<port>` directly. The verifier UI must clearly separate "transport is broken" from "transport is healthy but RuneLite is at `LOGIN_SCREEN`", because empty widget-dependent targets are expected until login completes.

Configuration editing in WebHelper separates Live State from Draft Config. Live state may refresh continuously, but editable draft fields must not be rebound to polling payloads once a draft exists. Users explicitly load current config into the draft, apply the draft, reset the draft, import JSON into the draft, or export the current draft. If the live config changes while a draft is dirty, WebHelper warns and preserves the draft. Raw payloads stay available in Raw Data or expandable debug views, while default views use structured cards/tables/badges.

## Hotkey Action Investigation

Tracked by `OSR-5`.

RuneLite has existing hotkey primitives (`Keybind`, `HotkeyButton`, `KeyManager`, and `HotkeyListener`), so configurable key capture for CV Helper is feasible. The first implemented slice is:

- `Debug status hotkey`: writes plugin status, mouse position, and target counts to in-game chat.
- `Print bounds hotkey`: writes current/cached overlay bounds and major widget summaries to in-game chat.
- `Capture screen hotkey`: queues a raw client-canvas capture.
- `Click login`: guarded helper action exposed in the panel and through `/login/click`; clicks RuneLite's visible click-to-play/login widget only when the client is on a login screen.
- `Refresh entities hotkey`: refreshes nearby player/NPC exports and forwards them to the webhook if configured.
- `Nearest entity hotkey`: writes the nearest exported entity and preferred canvas `clickPoint` to in-game chat.
- `Action 1-22 hotkeys`: each slot has an enabled toggle, keybind, target surface dropdown, editable target-label dropdown, refresh-choices button, sequence-memory reset, invocation mode, click-after mode, optional return-to-previous-panel toggle, optional restore-mouse-to-original-position toggle, and a manual run button. The slot resolves the current exported target, converts its canvas point into a screen point, and clicks automatically when using physical clicks.
- Fresh profiles default the action grid to `1 2 3 4 5`, `q w e r t`, `a s d f g`, `z x c v`, backquote/tilde, caps lock, and tab. Saved user keybinds override these defaults.
- `Panic stop`: exposed as a right-panel button, native RuneLite config hotkey defaulting to `F12`, and `GET/POST /automation/panic-stop`. It stops CV Helper loops, invalidates queued mob-farmer loop work, interrupts the loop sleeper, and clears the action-in-progress guard. It cannot interrupt an OS click already in progress, but stale queued loop steps must self-discard instead of resuming after the stop.
- Action timing controls are available in RuneLite config: panel-open delay, widget-target delay, selected-widget timeout, return-panel delay, and mouse-restore delay. These are deliberately configurable because targeted spells depend on client state becoming selected before the follow-up target click.
- Side-panel key controls are available in RuneLite config as `Panel key: combat`, `Panel key: inventory`, `Panel key: equipment`, `Panel key: prayer`, and `Panel key: magic`. CV Helper uses these as the safe fallback for returning while a targeted spell/action is still unconsumed. Normal panel opens and safe returns prefer exported panel-tab geometry so RuneLite profile key remaps do not send actions to the wrong tab.
- Clicks are randomized inside a safe circle around the exported target point. The circle is derived from the target bounds and capped to a small radius so repeated hotkeys do not click the exact same pixel while still staying inside the target.
- Click-after mode is `AUTO`, `ALWAYS`, or `NEVER`. `AUTO` does not click after prayers or self-resolving spells such as teleports, but does click after most other spell targets so the current OS mouse position can be used as the spell target.
- Spell, prayer, inventory, equipment, and combat actions first open the required side panel if it is not currently active, wait briefly, then resolve and click the target.
- Click-after uses the exact OS mouse position captured at hotkey press time. It is not randomized; this is what makes "hover target, press spell hotkey" click the hovered target.
- Combat spells such as Wind Strike/Bolt/Blast/Surge force click-after in `AUTO`, retry target resolution after opening the spellbook, and wait briefly after selecting the spell before clicking the captured mouse target.
- Invocation mode is `AUTO`, `WIDGET`, or `CLICK`. `AUTO` prefers RuneLite widget actions for prayers and self-resolving spells such as teleports, but uses physical spell selection for targeted/non-teleport spells because those spells are click-selection sensitive. `WIDGET` tries widget invocation and falls back to physical spell selection for targeted spells when RuneLite does not report selected-widget state. `CLICK` forces Java Robot physical clicks for debugging or fallback.
- Spell action slots use RuneLite widget actions for self-resolving spells such as teleports, but targeted/non-teleport spells use physical selection by default because widget selection is unreliable for combat spells. Java Robot is only used for required physical selection, the optional follow-up mouse-target click, safe panel-key presses, and mouse restore unless `CLICK` mode is selected.
- Prayer action slots also invoke the target prayer widget through RuneLite widget actions, and direct friendly prayer component targets are preferred over ambiguous child widgets.
- Prayer action slots support prayer mode: `TOGGLE`, `ON_ONLY`, or `OFF_ONLY`. `ON_ONLY` skips if the resolved prayer is already active; `OFF_ONLY` skips if already inactive. If the prayer state cannot be resolved, guarded modes fail closed instead of guessing.
- Spell action slots export widget `opacity`, `textColor`, `clickMask`, and `spellUnavailable`. The default spell guard skips a spell only when widget metadata clearly indicates the target is unavailable/shaded; `ALLOW_ATTEMPT` disables that guard for debugging or edge cases.
- Targeted spell actions select the spell, click the captured mouse target if click-after is enabled, then restore the previous tab. Click-based side-panel tab restores, widget actions, and menu actions are never allowed while a targeted spell is selected but unconsumed because that interaction cancels/casts the spell into the UI. If click-after is disabled for a targeted spell, `Return previous tab` may use the configured panel keybind but must not click a tab target.
- The legacy boolean `click mouse after` config is hidden; `Click-after` (`AUTO`, `ALWAYS`, `NEVER`) is the source of truth.
- Required panel opens prefer exported side-panel tab click-points because they happen before a target-consuming spell/action is selected. Return-tab behavior also prefers exported side-panel tab click-points after the selected action has been consumed by a target click or self-resolving action. If the action is still selected and unconsumed, return is keybind-only or skipped. If the previous side panel is unknown, the return target defaults to inventory. Target lookup uses fresh surface targets first, then last-known targets from the same exported surface if live collection races panel opening.
- Action hotkeys are captured through a CV Helper pre-dispatcher before the normal RuneLite plugin key chain, so profile/plugin key conflicts are less likely to block CV actions. Chat and text-field suppression still applies.
- Entity actions accept `Nearest clickable entity`, partial entity names, or `id:<npc id>` labels and pick the nearest clickable matching entity.
- Action sequences are single-flight; repeated hotkeys while an action is executing are ignored.
- All action delays are configurable, including panel open, target-resolve retry, mouse settle, widget-to-target, selected-widget timeout, return tab, and mouse restore.
- Target dropdowns filter as the user types, and action target matching uses the same normalized semantic labels that are exported to the verifier.
- Target labels can contain fallback lists separated by `|`, comma, semicolon, or newline. The resolver tries each candidate in order, which allows one spell hotkey to use `Bind | Ice Barrage` depending on the current spellbook/visible exports.
- Target labels can contain memory sequences separated by `->`. On each successful action, the slot advances to the next candidate, enabling flows such as `food -> brew`. Inventory/equipment matching is dose-agnostic for potion names and prefers the lowest remaining dose before a fresh potion.
- The CV Helper side panel is the full 22-slot action editor. RuneLite's native config panel exposes core global timings/panel keys/panic hotkey and the first four action slots; expanding all 22 slots into native config would require static duplicated config items, so deeper native-editor parity is tracked as a follow-up rather than duplicated now.
- Hotkeys are suppressed while RuneLite chat/message-layer input is active or while a CV Helper side-panel text field has focus, so typing in chat/config does not fire action slots.
- Robot clicks convert RuneLite real-canvas widget coordinates into displayed screen coordinates using stretched-mode dimensions when needed. This keeps action clicks aligned after resizing/fullscreening the client.
- Return-to-previous-panel captures the active side panel when the command is issued. After the target-consuming part of the action finishes, it clicks that previous panel's exported tab target. If the action is still selected and unconsumed, it uses only the configured panel keybind or skips the return to avoid cancelling/casting into the UI.
- Restore-mouse moves the mouse back to the original screen position captured at hotkey press time after all action clicks complete.

Action slot examples:

- Prayer toggle: `Surface = PRAYER`, `Target label = Protect from Magic`, `Click mouse after = false`.
- Targeted spell: `Surface = SPELL`, `Target label = Wind Strike`, `Click-after = AUTO` or `ALWAYS`. Pressing the hotkey clicks the spell widget, waits briefly, then clicks the current mouse position captured before the robot moved.
- Teleport spell: `Surface = SPELL`, `Target label = Varrock Teleport`, `Click-after = AUTO`. Pressing the hotkey clicks only the teleport because `AUTO` treats teleport-like spells as self-resolving.
- Nearest actor click: `Surface = NEAREST_ENTITY`, no target label required.

The action slot implementation intentionally uses exported geometry as the source of truth, so the same target contract used by Python is exercised by the hotkey layer. If a surface depends on a visible tab, the tab must be visible or the target must be available from a cached snapshot before the action can resolve.

### Action Sequence Direction

The hotkey executor is the first local implementation of the broader Python-driven "hands and arms" action layer. Future external action sequences should use the same stages:

1. Resolve semantic target from exported/cached target state.
2. Open the required side panel if needed.
3. Invoke UI widgets through RuneLite widget actions when possible.
4. Wait for required client state, such as selected spell/widget.
5. Perform only the physical OS mouse clicks that must target world/entity/canvas positions.
6. Optionally return panel with exported tab geometry after action consumption, or key-only/skip if a selected action is still unconsumed, then restore mouse.
7. Report success/failure with enough detail for Python to choose the next action.

Sequences should fail closed: if a required state transition does not happen before timeout, skip unsafe downstream clicks rather than guessing.

The CV Helper right-side panel includes a collapsible `Action hotkeys` section with compact action cards. Core controls are visible immediately; less-common invocation/click-after/return options are collapsed behind each card's `Advanced` toggle. Slots 1-4 are also represented in RuneLite's native config under the `Action hotkeys` section; slots 5-22 are managed from the CV Helper panel to avoid making the native config page unwieldy.

### First Mob Farmer

The first automation slice is a guarded mob-farmer controller, not a full unattended combat bot. It exposes:

- `GET /automation/mob-farmer/status`
- `GET/POST /automation/mob-farmer/config`
- `POST /automation/mob-farmer/focus-click`
- `POST/GET /automation/mob-farmer/step?target=cow&live=false`
- `POST/GET /automation/mob-farmer/start?target=cow&live=false`
- `POST/GET /automation/mob-farmer/stop`
- `POST/GET /automation/panic-stop`

The farmer refuses to click unless the client is logged in, the local player exists, a matching NPC target passes the target-validity filter, the target has a screen click point, and no other CV Helper action is currently running. Dry mode reports the chosen target/click point without clicking. Live mode performs the same guarded target click.

Current target-validity checks include:

- NPC target list matching by partial name or `id:<npc id>`, including list separators already used by action targets.
- NPC composition has an `Attack` action and is interactible.
- NPC is not dead and does not have visible health ratio `0`.
- Local-player combat is handled separately: continue a desired current target; configurable response for undesired aggressive attackers.
- After a live pickup, `afterLootCombatMode` can prevent re-targeting when the player is already interacting with an attacker or an NPC is tagging the player. `STAY_ON_CURRENT_ATTACKER` holds instead of selecting a new target; `STOP_WHEN_TAGGED` stops the farmer.
- Single-combat skips mobs already engaged by someone else. Multi-combat uses the configured engaged-mob policy: `FREE_ONLY`, `PREFER_FREE`, or `ALLOW_ENGAGED`.
- Optional line-of-sight guard, enabled by default, plus a conservative local collision-path check for matched NPCs. The pathing pass uses a bounded BFS over RuneLite `WorldArea.canTravelInDirection(...)` and requires a collision-valid route to melee reach before selecting a target.
- Optional max-distance guard, default `20` tiles.

`/automation/mob-farmer/status` reports the latest decision plus candidate diagnostics, including per-NPC selectability, reasons, health/death state, attackability, engaged-by-other state, line-of-sight state, score, click point, and world location. Use this report before trusting live clicks in crowded or obstructed areas.

Candidate reports also include `reachable`, `pathDistance`, `pathSearchLimit`, `pathVisited`, `pathFailureReason`, and `playerWorldLocation`. Matched NPCs behind ordinary fences/walls/collision blockers are rejected with `unreachable:<reason>` or `path-too-far` instead of being selected. OSR-14 Part E adds one local `Open`/`Close` door transition: unknown, unallowlisted, denylisted, or disabled doors are rejected with `blockedByDoor`, `manualActionRequired`, `manualActionReason`, and `blockingDoor`; explicitly allowlisted doors can produce `doorTransition` metadata for one guarded live menu-action attempt flow. The live flow rechecks the real transition, uses a 1.2-second retry interval, caps attempts at five, and holds after timeout. `/pathing/grid` and WebHelper render currently reachable, reachable-via-door, blocked-door, ordinary obstacle, and unreachable states distinctly. Multi-door routes and `Enter`/`Pass`/`Climb-over` transitions remain follow-up bricks.

First-pass survival, menu interaction, and loot processing is implemented:

- Auto-eat runs before loot or attack decisions. If current HP is at or below the configured percent threshold, the farmer opens inventory if needed, finds a matching food item with an `Eat`, `Drink`, or `Consume` action, and invokes that inventory widget menu action. If no food is found, the loop either stops automatically or records `warning:no-food-continue` and keeps farming based on `Stop if no food`. Status includes whether survival is configured to preempt actions, current HP/threshold, selected food, preempted decision/intents, and no-food behavior.
- Loot pickup scans visible scene ground items and reports every candidate in `/automation/mob-farmer/status`. Selection uses explicit always-loot names/ids, Ground Items highlighted/hidden list metadata, minimum GE value, blacklist, ownership policy, loot radius, inventory capacity, stackability, interaction mode, and score. High-priority loot can override Attack-before-loot when it is explicitly allowlisted, highlighted by Ground Items, above the configured high-priority GE threshold, close to despawning, or when enough selectable loot piles are visible; candidates expose `highPriority` and `priorityReasons`.
- Attack and loot interaction modes default to `MENU_ACTION`. NPC attacks use the exact `Attack` action index exported from NPC composition metadata, and loot uses `GROUND_ITEM_THIRD_OPTION`/`Take` with scene coordinates. Before a live loot action fires, the target is revalidated on its scene tile; stale or newly rejected loot attempts fall through so the loop can attack instead of getting stuck. `DIRECT_CLICK` remains available as a debug/fallback path.
- Ground Items list reuse is first-pass implemented. `SUPPLEMENT` treats Ground Items highlighted entries as additional always-loot candidates. Ground Items hidden-list, hide-under-value, and show-highlighted-only suppression metadata are reported on candidates; they block pickup only if `Respect hidden Ground Items` is enabled and the item is not explicitly listed by CV Helper. `Never-loot` still wins over all allowlist/highlight/value rules.
- Optional intermediate inventory actions can use matching items with configurable item-to-action mappings. Defaults preserve `bones -> Bury`, `big bones -> Bury`, and `ashes -> Scatter|Bury`, but future entries can map other item names or ids to different inventory actions. If the configured action is unavailable, the farmer skips the item and reports selected item, slot, matched rule, configured action(s), available item actions, intended/actual action, menu params, result, and failure reason instead of falling back to `Use` or `Drop`.
- Default loot flow prioritizes survival first, then intermediate inventory actions, then high-priority loot that may be missed, then attacking first when idle, then collecting allowed drops during combat windows or when no valid target is available. This supports the desired "attack next mob first, then loot after the drop appears" rhythm without letting important drops expire behind combat.
- Successful live `Take` actions queue a next-valid-tick reattack attempt. The next logged-in tick still runs survival first, then `reattachAfterPickup` selects a valid reachable combat target and issues the attack before normal high-priority-loot/attack-before-loot flow. `/automation/mob-farmer/status.reattachAfterPickup` reports pending state, queued/attempt ticks, last pickup and attack ticks, selected target, result, and clear reason. This is currently based on the shared 1-tick scheduler gate; weapon-specific cooldown modelling is a follow-up.
- Inventory status reports occupied/free slots, protected never-drop list, and the lowest-value unprotected drop candidate for future drop-processing work. The first pass does not drop inventory items automatically.

Real pathing/exit tiles, emergency teleport, automatic dropping, config profiles/presets, and composable external action plans remain follow-up bricks.

Dry mob-farmer steps also print the selected target/click point to in-game chat so the user can verify targeting without watching the localhost response.

Known farmer follow-ups:

- Validate menu-action attack and pickup in crowded/hidden-drop cases and keep direct-click fallback available for debugging. Use `/automation/mob-farmer/status.lastActionAttempt` and `.recentMenuEntries` to compare the synthetic action parameters against exact RuneLite-generated menu entries.
- Expand Ground Items integration beyond first-pass highlighted/hidden/show-highlighted-only/hide-under-value reuse if needed, including stronger parity with Ground Items value tiers or ownership display settings.
- Add automatic low-value dropping, item priority tiers, and safe pathing/exit strategies before longer unattended loops.
- Replace conservative line-of-sight filtering with real route/path distance using collision maps or a pathing plugin integration.
- Add emergency teleport and configuration profiles/presets after the loop primitives are stable.
- Add explicit world-switching/reconnect policy after the core combat loop is stable. Login-screen recovery already reuses the guarded `/login/click` helper when enabled.

The verifier dashboard groups `/status` data into connection, vitals, wealth, and interface sections. It shows HP/prayer, run energy, special attack energy/enabled state, active prayers, current loot/equipment/total carried/risked-value approximation, selected widget state, and latest capture preview/path. Inventory and equipment should have one authoritative default display with sortable/filterable rows for item name, quantity, slot, and available value fields; duplicate raw inventory dumps belong in Raw Data.

The mob-farmer verifier panel shows status, controls, runtime details, configuration entry points, and debug data as separate surfaces. Its config editor reads current settings from `/automation/mob-farmer/config` only into an explicit draft, renders structured config controls with labels/descriptions/tooltips instead of a raw dump, supports JSON import/export without implicit apply, exposes a focus-click button, and allows action slot hotkey/options editing through WebHelper. Raw JSON remains available behind debug/raw views.

## Debugging In Game

- The open client must be a freshly launched custom jar; Java changes do not hot-load into an already-open RuneLite window.
- Start `tools/cv-helper-verifier/serve.ps1` and let the verifier auto-discover the active debug port. Only fall back to log inspection if the helper itself is not available.
- In the CV Helper right-side panel, `Debug overlay` writes current state, mouse position, and target counts to in-game chat.
- `Print overlay bounds` writes current mouse, prayer panel, quick-prayer panel, spellbook, and cached target bounds to in-game chat.
- Green rectangles are prayer targets. Orange rectangles are spell targets. Mouse coordinates are cyan.
- Minimap/orb boxes are cyan, inventory boxes are yellow, and equipment boxes are violet.
- Enable `Target labels` to draw the exported target label beside prayer/spell boxes.
- If target endpoints return `count: 0`, make sure the relevant in-game tab or quick-prayer selection interface is visible.
- If spellbook reports `Unknown(-1/-1)`, the client is probably not fully logged in yet.
- If action clicks land high/left after resizing/fullscreening, verify the rebuilt client includes the stretched-mode coordinate conversion and then refresh the relevant target surface.
- Before runtime verification, ask the user to log in to the newest launched client. Avoid interpreting logged-out export state as a plugin bug.

## Engineering Constraints

- RuneLite widget access must happen on the client thread.
- HTTP handler threads should wait briefly for client-thread snapshots and return timeout errors instead of blocking indefinitely.
- Bind localhost only; do not expose remote interfaces.
- Avoid automating gameplay directly in the plugin until the helper contract is stable. The first goal is geometry/export, not action execution.
- Docs and Linear should be updated whenever the API shape changes.

## Next Implementation Slice

- Verify `/targets/prayer` while the user has the prayer tab visible.
- Direct prayer component fallback for `InterfaceID.Prayerbook.PRAYER1` through `PRAYER30` is verified on a fresh RuneLite launch. Continue filtering `/targets/prayer` to real prayer widgets only.
- Verify `/targets/spell` while the user has the magic tab visible.
- Verify current spellbook name/id after login for Standard, Ancient, Lunar, and Arceuus where available.
- Verify `/capture/screen` and `/capture/minimap` after login.
- Verify `/targets/minimap`, `/targets/inventory`, and `/targets/equipment` after login with relevant tabs/interfaces visible.
- Verify the inventory duplicate-container fix: dragging or hovering an item such as the dragon axe should show one slot-sized inventory overlay, not a second giant panel-sized `inventory slot 1` box.
- Add endpoint for all currently known target surfaces, likely `GET /targets`.
- Add stable target IDs/names so Python can ask for a semantic click point.
- Prototype `OSR-5` hotkey logging for one prayer and one spell before implementing direct action execution.
- Run `powershell -ExecutionPolicy Bypass -File .\tools\cv-helper-verifier\serve.ps1` after relaunch and confirm the dashboard auto-discovers the fresh CV Helper port, distinguishes `LOGIN_SCREEN` from transport failure, and still polls the live status surfaces.
