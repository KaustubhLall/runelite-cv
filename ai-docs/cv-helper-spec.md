# CV Helper Spec

This is the working product/specification note for `OSR-2`.

## Purpose

`CV Helper` is a RuneLite plugin that helps an external Python computer-vision/autonomous-agent process play OSRS by exporting reliable RuneLite UI geometry and interaction affordances over localhost.

The Python process should not have to infer every UI coordinate from pixels. When RuneLite already knows widget bounds, names, actions, and visibility, `CV Helper` should expose that data as a stable helper API.

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

### `POST /login/click` / `GET /login/click`

Queues a guarded click on RuneLite's visible click-to-play/login widget. This is a convenience for local testing when the client is waiting at the login screen. It must skip and report status if the client is not on a login screen or if RuneLite does not expose a visible login widget.

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

The first verifier is a dependency-free static dashboard at `tools/cv-helper-verifier/index.html`. It polls the CV Helper localhost endpoints, shows target counts and payload tables, and flags suspicious target geometry such as oversized inventory/equipment boxes or unnamed equipment slots.

The plugin adds CORS headers to JSON responses so the browser can call `http://127.0.0.1:<port>` directly.

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
- `POST/GET /automation/mob-farmer/step?target=cow&live=false`
- `POST/GET /automation/mob-farmer/start?target=cow&live=false`
- `POST/GET /automation/mob-farmer/stop`

The farmer refuses to click unless the client is logged in, the local player exists, the local player is not already interacting with something, a matching NPC target is exported with a screen click point, and no other CV Helper action is currently running. Dry mode reports the chosen target/click point without clicking. Live mode performs the same guarded target click. Auto-eat, loot filters, highlighted-drop integration, pathing/exit tiles, and composable external action plans remain follow-up bricks.

The verifier dashboard groups `/status` data into connection, vitals, wealth, and interface sections. It shows HP/prayer, run energy, special attack energy/enabled state, active prayers, current loot/equipment/total carried/risked-value approximation, selected widget state, and latest capture preview/path.

## Debugging In Game

- The open client must be a freshly launched custom jar; Java changes do not hot-load into an already-open RuneLite window.
- Use the newest `CV Helper local export listening on http://127.0.0.1:<port>/status` log line to identify the active debug port.
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
- Open `tools/cv-helper-verifier/index.html` after relaunch and confirm it can poll the fresh CV Helper port.
