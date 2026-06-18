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

Returns player and client state, including login state, world info, base coordinates, player world/local coordinates, mouse canvas position, optional self/player screen bounds, run energy, weight, current spellbook, current visible interface/tab metadata, all skills, and prayer active/export state where available.

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
- `Refresh entities hotkey`: refreshes nearby player/NPC exports and forwards them to the webhook if configured.
- `Nearest entity hotkey`: writes the nearest exported entity and preferred canvas `clickPoint` to in-game chat.
- `Action 1-4 hotkeys`: each slot has a keybind, target surface dropdown, target-label substring, and optional "click mouse after target" toggle. The slot resolves the current exported target, converts its canvas point into a screen point, and clicks automatically.
- Clicks are randomized inside a safe circle around the exported target point. The circle is derived from the target bounds and capped to a small radius so repeated hotkeys do not click the exact same pixel while still staying inside the target.

Action slot examples:

- Prayer toggle: `Surface = PRAYER`, `Target label = Protect from Magic`, `Click mouse after = false`.
- Targeted spell: `Surface = SPELL`, `Target label = High Level Alchemy`, `Click mouse after = true`. Pressing the hotkey clicks the spell widget, waits briefly, then clicks the current mouse canvas position.
- Nearest actor click: `Surface = NEAREST_ENTITY`, no target label required.

The action slot implementation intentionally uses exported geometry as the source of truth, so the same target contract used by Python is exercised by the hotkey layer. If a surface depends on a visible tab, the tab must be visible or the target must be available from a cached snapshot before the action can resolve.

The CV Helper right-side panel includes a collapsible `Action hotkeys` section with four slots. Each slot exposes a hotkey capture button, surface dropdown, target-label text field, mouse-after checkbox, and `Run action` button for manual testing.

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
