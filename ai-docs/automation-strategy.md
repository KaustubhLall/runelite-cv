# Automation Strategy

This document captures the automation architecture direction for CV Helper and the Python controller.

## Current Priority

`OSR-7` is the first automation task: build a configurable auto mob farmer, initially validated on Lumbridge cows.

The automation must not be a one-off cow script. It should prove reusable bricks that later automations can compose.

## Foundation Gates

Do not build the live automation loop on unstable primitives. Verify these first:

- `OSR-5`: CV Helper action primitives are reliable. Panel switching must not clear selected spell/action context. Entity targeting by name/id must work.
- `OSR-3`: Python can consume CV Helper status, entities, targets, vitals, inventory, wealth, and captures.
- `OSR-4`: snapshots are coherent enough for decision loops, ideally synchronized to game ticks.

## Brick Order

1. Dry-run automation state machine that only reports the next intended action.
2. Mob target selection from `/entities` by partial name or NPC id.
3. Safe click-to-attack using existing CV Helper action/click primitives.
4. First implemented slice: CV Helper right-panel and `/automation/mob-farmer/*` endpoints expose dry/live step and dry/live loop controls. The loop only clicks a matching entity when logged in, not already interacting, and no CV action is running.
5. Panic stop: always provide a sidebar button, hotkey, and localhost endpoint before expanding longer loops.
6. Combat-state monitor: not in combat, current target, target death/end condition. First pass is implemented in CV Helper with NPC attackability/dead/engaged filters, single-vs-multi policy, and candidate diagnostics.
7. Real pathing: replace line-of-sight/straight-distance heuristics with route distance or path existence using collision maps or a pathing plugin integration.
8. Click reliability: add a right-click/menu-option path so `Attack <npc>` can be selected when drops/players/overlap obstruct the left-click point.
9. Loot loop: attack the next mob first, wait for drops to spawn, then pick up allowed/highlighted/minimum-value drops.
10. HP and food interrupt: eat above stop threshold, stop/report if no healing remains.
11. Inventory fullness and potion/food matching, including dose-agnostic potion use and never-drop protected items.
12. Optional highlighted-drop integration with plugins such as Ground Items if feasible.
13. Login/reconnect/world recovery: click play/login safely, wait until in-game, then resume only if loop policy allows.
14. Exit policy: first stop/report, later marked safe tile or pathing integration.
15. Verifier UI for automation state, last decision, interrupt reason, and next planned action.

## First Live Scenario

- Location: Lumbridge cow area.
- Target: cow by partial name or NPC id.
- Loop: if not in combat, click nearest matching cow, monitor until combat ends, repeat.
- Safety: if HP is below threshold, eat; if no food is available, stop/report.
- Loot: start with explicit item names and minimum-value filters.

## Design Rules

- Prefer observable state over assumptions.
- Fail closed on missing targets, stale geometry, unsafe HP, or unknown click results.
- Keep each automation brick independently testable in dry-run mode.
- Never let a higher-level automation hide a broken primitive. Fix the primitive first.
