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

- `GET /status` returns JSON containing plugin name, latest status, active local port, endpoint discovery, player status summary, spellbook metadata, current interface/tab metadata, latest capture paths/statuses, and current cached target counts.
- `GET /player/status` returns client/player state including login state, world, base/player coordinates, mouse canvas position, optional local-player/self screen bounds, run energy, weight, spellbook metadata, skill stats, and prayer active/export state where available.
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
- `GET /entities` returns nearby players and NPCs around the local player with names, ids where available, world/local location, combat level, animation, distance from the local player, canvas bounds where RuneLite exposes them, `center`, `canvasTileCenter`, and a preferred canvas-space `clickPoint`.
- `GET /entities/nearest` returns the closest exported player/NPC with a usable canvas-space `clickPoint`, intended as a first controller-friendly target for Python click planning.
- Target payloads should include freshness metadata such as `fresh`, `lastSeenAt`, or equivalent. Last-known panel positions may be served after panels close, and should be marked cached/stale.
- The server should prefer a fixed/default local development port. If binding fails, it may fall back to an ephemeral port, but `/status` and the RuneLite panel must expose the active port.
- Screenshot files are written as explicit `cv-helper-*.png` files under `C:\Users\kaust\.runelite\screenshots\<player>\manual\`, and `/status` exposes each latest capture `savedPath`.
- If RuneLite is on the login screen, screenshot capture is intentionally blocked and the plugin status reports `capture-blocked:login-screen`.
- Java-to-Python push is optional and best-effort through a configured local webhook URL. Python-to-Java pull remains the source of truth for current target geometry.
- Current webhook events are `server-started`, `prayer-targets`, `spell-targets`, other `*-targets` surfaces, and `capture-saved`.
- `OSR-3` owns the first Python receiver contract for consuming local endpoints and webhook payloads.
- `OSR-4` owns the future tick-synchronized delivery contract: state changes enqueue snapshots immediately, then queued exports flush on RuneLite `GameTick` for coherent tick-aligned Python decisions.
- `OSR-5` owns hotkey action work. Current CV Helper hotkeys include configurable RuneLite `Keybind`s for debug status, bounds printing, raw screen capture, entity refresh, nearest-entity click-point logging, and four configurable action slots. Each action slot has a keybind, target surface, editable target dropdown, click-after mode (`AUTO`, `ALWAYS`, `NEVER`), optional return-to-previous-panel behavior, and optional return-mouse-to-canvas-center behavior. Action slots can click exported prayer/spell/UI targets directly; spell-like flows can click a spell target and then click the current mouse canvas position.
- Action slot clicks should randomize within a safe circle inside the target bounds rather than always clicking the exact center point. The CV Helper side panel must expose these slots in a collapsible section so users do not need to discover them through RuneLite's generic configuration panel.
- `OSR-6` owns the verifier client site. Browser consumers call CV Helper through `http://127.0.0.1:<port>`, so plugin JSON responses include permissive local-development CORS headers.

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
