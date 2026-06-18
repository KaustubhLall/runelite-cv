# Active Memory

Use this file as the short working map for the current session. Keep it concise.

## Current Focus

- Branch: `master` with local uncommitted CV Helper/bootstrap work
- Linear task: `OSR-2`
- State: custom RuneLite game login, Plugin Hub loading, CV Helper startup, localhost status, screenshot capture, and initial prayer target export implemented
- Last merged PR: https://github.com/KaustubhLall/runelite-cv/pull/1
- Current task: `OSR-2` CV Helper plugin and Jagex Launcher credential bootstrap

## Active Task Stack

- `OSR-1` - Set up AI docs harness and default MCP tooling
- `OSR-2` - Plan and begin CV Helper plugin with broadcast/local export support
- `OSR-3` - Build Python receiver for CV Helper localhost/webhook exports
- `OSR-4` - Synchronize CV Helper export delivery to OSRS game ticks
- `OSR-5` - Investigate CV Helper hotkeys for prayers and spell actions
- `OSR-6` - Build CV Helper verifier client site

## Current Notes

- `ai-docs` is the starting point for AI-assisted project memory and workflow.
- The project should support fast iteration without locking future tasks too early.
- Linear should serve as live per-task memory through comments, links, and status updates.
- Branch-specific memory must be explicit to avoid mixing state across parallel work.
- Copilot MCP defaults are now wired locally in the IntelliJ config.
- The current product task is `CV Helper`, an example RuneLite plugin for UI coordinate capture and localhost export.
- The user account is Jagex Launcher-managed: `CoreDump` / `C0REDUMPED`.
- The custom jar previously launched without credentials. The correct bridge is `--insecure-write-credentials` through official RuneLite launched by Jagex Launcher, which creates `C:\Users\kaust\.runelite\credentials.properties`.
- As of 2026-06-16 19:27 PDT, official RuneLite wrote 5 credentials and the custom 1.12.29-SNAPSHOT launch read 5 credentials from disk.
- User confirmed game login works in the custom client. Do not regress this blocker back to Jagex credentials unless the credential file is missing or the client stops logging `read 5 credentials from disk`.
- Plugin Hub 404s from the snapshot build were fixed by launching with stable service overrides. Fresh logs show external plugins loading.
- CV Helper was enabled by default for local development, starts on launch, and logs its right-sidebar/local export startup.
- Verified local export on 2026-06-16: `GET /status` returned `{"plugin":"CV Helper","status":"server@65510","port":65510}`.
- Verified screenshot capture on 2026-06-16: `POST /capture` returned `{"ok":true,"queued":true}`, status changed to `saved@...`, and RuneLite wrote `C:\Users\kaust\.runelite\screenshots\cv-helper 2026-06-16_19-38-02.png`.
- User confirmed the right-side CV Helper panel and mouse coordinate overlay work.
- World 301 default is enforced in code and local RuneLite profiles: `defaultworld.defaultWorld=301`, `defaultworld.useLastWorld=false`.
- New product direction: CV Helper is a Java-side geometry/API helper for a Python computer-vision/autonomous-agent process, not the autonomous player itself.
- API direction: Python pulls current widget geometry from Java over localhost; Java can optionally push event snapshots to a configured local Python webhook.
- First target export implemented: `GET /targets/prayer` returns visible prayer/quick-prayer widget bounds and center click points.
- Current target exports: `GET /targets/prayer` and `GET /targets/spell`; `/status` advertises both plus `/player/status`.
- Current capture endpoints: `POST /capture` for framed client screenshot, `POST /capture/screen` for raw canvas, and `POST /capture/minimap` for visible minimap crop.
- Player status export now includes guarded client state, current spellbook, world/base coordinates, run energy, weight, all skills, and prayer active/export state where available.
- Current target exports: `/targets/prayer`, `/targets/spell`, `/targets/minimap`, `/targets/inventory`, and `/targets/equipment`.
- Inventory and equipment exports should be slot-only. Do not emit parent inventory/equipment containers as fallback targets; user saw that as a misleading giant duplicate inventory slot overlay.
- Verified on 2026-06-17 against the fresh client on port `56112`: `/targets/inventory` returned the dragged/hovered dragon axe as one `36x32` slot target with no giant duplicate.
- Verified on 2026-06-17 against the still-running client on port `56112`: `/targets/equipment` returned 19 target boxes. Equipment export works, but semantic labels need cleanup because many entries are fallback `equipment slot -1` and the surface includes utility buttons.
- Verified on 2026-06-17 against the fresh client on port `55312`: `/targets/prayer` returned 17 real prayer widgets with the prayer tab visible. Non-prayer panel controls such as `Filters` and the prayer meter were filtered out after an additional cleanup patch.
- Spell config is spellbook-level (`Standard`, `Ancient`, `Lunar`, `Arceuus`); individual spell targets are discovered dynamically from visible spellbook widgets.
- The right-side panel has collapsed prayer and spellbook dropdown sections. Prayer toggles filter matched prayer targets; spellbook toggles filter the active spellbook surface.
- Overlay rendering refreshes prayer, spell, minimap/orb, inventory, and equipment target boxes live when visible. Colors: prayer green, spell orange, minimap cyan, inventory yellow, equipment violet. `Target labels` controls whether labels draw beside target boxes.
- In-game debug buttons: `Debug overlay` prints state/mouse/counts; `Print overlay bounds` prints mouse, cached bounds, prayer panel, quick-prayer panel, and spellbook summaries to chat.
- Webhook URL setting and best-effort POST helper are implemented for `server-started`, `prayer-targets`, `spell-targets`, and `capture-saved`. Capture payloads include player/spellbook/status context.
- Python receiver work is tracked in `OSR-3`.
- Tick-synchronized export is tracked in `OSR-4`: queue target/status changes immediately, then flush on RuneLite `GameTick` so Python sees coherent tick-aligned state.
- Hotkey action investigation is tracked in `OSR-5`: RuneLite key capture/config is feasible via existing `Keybind`/`HotkeyButton`/`KeyManager`/`HotkeyListener` patterns, but direct prayer/spell activation needs a deliberate plugin-vs-Python boundary decision.
- Verifier client site is tracked in `OSR-6`: static dashboard lives in `tools/cv-helper-verifier/` and polls CV Helper endpoints from a browser. The plugin adds CORS headers and they were verified on port `55312`; the verifier opened with `?port=55312`.
- Implemented on 2026-06-17: CV Helper prefers fixed localhost port `11777`; if busy, it falls back to an open port and reports the active port in `/status`.
- Implemented on 2026-06-17: capture endpoints save explicit `cv-helper-*.png` files under `C:\Users\kaust\.runelite\screenshots\<player>\manual\` and `/status` exposes latest capture status/path.
- Implemented on 2026-06-17: `/status` and `/player/status` export mouse position, player world/local coordinates, optional self/player screen bounds, current open interface/tab metadata, capture statuses, and target snapshot metadata.
- Implemented on 2026-06-17: `/targets/panels`, `/targets/combat`, `/targets`, and `/entities` were added. Panels include side tab/nav controls; minimap includes a compass/look-north target; combat includes attack styles/auto-retaliate/autocast controls; entities currently include players and NPCs.
- Implemented on 2026-06-17: target endpoints cache last-known target positions and mark snapshots as fresh vs cached/stale. This allows Python to use remembered panel/tab positions after the panel closes.
- Verified on 2026-06-17 against the fresh client on fixed port `11777`: `/status` was logged in as `C0re Dumped` on world `301`; verifier served from `http://127.0.0.1:8765/` connected to `11777`; panels exported 19 boxes; minimap exported 10 boxes; `/entities` exported nearby player/NPC actors; `/capture/screen` saved `C:\Users\kaust\.runelite\screenshots\C0re Dumped\manual\cv-helper-screen 2026-06-17_01-31-14.png` and `/status` reported that exact path.
- Fixed on 2026-06-17: target overlays no longer depend on the mouse-coordinate overlay toggle. Turning off mouse coordinates hides only the crosshair/text, not prayer/spell/minimap/inventory/equipment/panel/combat target boxes.
- Fixed on 2026-06-17: in-game target labels are suppressed for inventory/equipment/panel boxes to avoid overlapping label clutter. The verifier still shows target labels in tables.
- Fixed on 2026-06-17: verifier warnings no longer treat cached target snapshots as errors. Cached state remains visible in each surface header.
- Implemented on 2026-06-17: `/capture/latest/client-frame`, `/capture/latest/screen`, and `/capture/latest/minimap` stream the latest saved PNG. The verifier renders capture preview cards from these endpoints. Verified `/capture/latest/screen` returned HTTP 200 and the verifier rendered a screen capture preview.
- Known bug captured on 2026-06-18: `/capture/minimap` can crop the wrong region, closer to the center of the screen, when compared against a same-angle full screen capture. Do not block the current verifier/API work on this; track a follow-up to fix minimap crop source/bounds.
- Implemented on 2026-06-18: nearby player/NPC entities are now visible in the verifier through a dedicated `/entities` table and can be drawn in-game with the `Nearby entity boxes` overlay toggle.
- Implemented on 2026-06-18: CV Helper has configurable RuneLite hotkeys for debug status, bounds printing, raw screen capture, entity refresh, nearest-entity target logging, and four action-click slots. Each action slot has a keybind, target surface dropdown, target-label substring, and optional "click mouse after target" toggle for spell-then-target flows.
- Implemented on 2026-06-18: action hotkeys are now configurable from a collapsible `Action hotkeys` section in the CV Helper side panel, with key capture, surface dropdown, target label, mouse-after toggle, and a manual Run button per slot.
- Implemented on 2026-06-18: action clicks randomize inside a small safe circle derived from the target bounds so repeated prayer/spell/entity clicks do not always hit the exact same pixel.
- Implemented on 2026-06-18: action slots now use editable target dropdowns and a `Click-after` mode (`AUTO`, `ALWAYS`, `NEVER`). `AUTO` skips click-after for prayers and teleport/self-resolving spells, but clicks the current mouse target for most other spells.
- Implemented on 2026-06-18: action slots can optionally return to the previously active side panel and move the mouse back to the canvas center after the action finishes.
- Fixed on 2026-06-18: click-after now uses the actual OS mouse pointer location captured before Java Robot moves to the spell/widget target. This should make "hover target, press hotkey" click the hovered target instead of a misconverted canvas coordinate near the top of the screen.
- Implemented on 2026-06-18: action slots have enable toggles and the runtime now supports 8 slots. Slots 1-4 appear in RuneLite native config under `Action hotkeys`; slots 5-8 are managed from the CV Helper panel.
- Implemented on 2026-06-18: `/entities` includes canvas-space `center`, `canvasTileCenter`, and preferred `clickPoint` fields for nearby player/NPC actors. `/entities/nearest` returns the closest actor with a usable `clickPoint`, and the verifier shows nearest/per-row click coordinates for Python click planning.
- Fast launcher script: `scripts/run-cv-helper.ps1`. Desktop shortcut: `C:\Users\kaust\OneDrive\Desktop\RuneLite CV Helper.lnk`.
- Workflow preference from user: when runtime verification needs live widgets, ask the user to log in to the newest client before treating logged-out endpoint state as meaningful.
- The dev launcher should pin live RuneLite services to a stable release with `-Drunelite.pluginhub.version=<release>` and `-Drunelite.http-service.url=https://api.runelite.net/runelite-<release>` while the local jar remains a snapshot.
- CV Helper should be enabled by default during local development so the right-side navigation button appears without requiring manual plugin search first.
- Remaining implementation focus: visual verification of panel/combat/compass overlay alignment in the RuneLite window; equipment label cleanup; game-object entity traversal; Python receiver client; tick-synchronized export flushing.
