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
7. Survival and loot policy: implemented in CV Helper with auto-eat threshold/food matching/no-food stop-or-continue behavior, loot allowlist/min-value/blacklist/ownership/radius checks, Ground Items highlighted/hidden/list-value reuse, inventory-full guards, stale-loot revalidation, and candidate diagnostics.
8. Real pathing: replace line-of-sight/straight-distance heuristics with route distance or path existence using collision maps or a pathing plugin integration.
9. Click reliability: implemented first-pass RuneLite menu-action primitives for `Attack <npc>` and `Take <item>` so hidden/deprioritized drops and obstructed NPCs do not depend on left-click geometry. Keep direct-click mode as a fallback for debugging.
10. Inventory processing: first-pass optional intermediate actions invoke matching inventory menu actions directly (`Bury`, `Scatter`, then `Use`) instead of generic left-clicks. Automatic low-value dropping, item priority tiers, and protected never-drop enforcement remain follow-up work. Current implementation reports the drop candidate but does not drop automatically.
11. Ground Items integration: implemented first-pass import/reuse of highlighted, hidden, show-highlighted-only, and hide-under-value behavior. Highlighted Ground Items can supplement CV Helper always-loot; hidden/suppressed items are reported and only block pickup when the CV Helper setting explicitly respects hidden items unless CV Helper explicitly allowlists them.
12. Login/reconnect/world recovery: click play/login safely, wait until in-game, then resume only if loop policy allows.
13. Exit policy: first stop/report, later marked safe tile or pathing integration.
14. Verifier UI for automation state, last decision, interrupt reason, and next planned action.

## First Live Scenario

- Location: Lumbridge cow area.
- Target: cow by partial name or NPC id.
- Loop: if not in combat, click nearest matching cow, monitor until combat ends, repeat.
- Safety: if HP is below threshold, open inventory, eat matching food, and either stop/report or warn/continue based on `Stop if no food`.
- Loot: start with explicit item names, Ground Items highlighted-list imports, and minimum-value filters. Default flow attacks first when idle, then picks allowed drops during combat windows or when no valid target exists. Failed or stale loot attempts should fall through cleanly instead of trapping the loop when `Attack before loot` is off.

## Design Rules

- Prefer observable state over assumptions.
- Fail closed on missing targets, stale geometry, unsafe HP, or unknown click results.
- Keep each automation brick independently testable in dry-run mode.
- Never let a higher-level automation hide a broken primitive. Fix the primitive first.

## Skill Farmer Presets

Every new skill farmer must ship with exhaustive default presets for all trainable targets in that skill, derived from the current OSRS wiki target list. Presets live in `CvHelperPlugin` (`woodcuttingProfiles()`, `miningProfiles()`, and equivalent helpers for future skills) and are exposed through the existing `/automation/<skill>/config` endpoint.

- Woodcutting: all choppable trees from the OSRS wiki `Trees` article (Normal, Achey, Oak, Willow, Teak, Maple, Arctic pine, Hollow, Mahogany, Yew, Blisterwood, Camphor, Magic, Ironwood, Redwood, Rosewood).
- Mining: all mineable rocks/ores from the OSRS wiki `Mining` article (Clay, Copper, Tin, Blurite, Iron, Silver, Coal, Gold, Mithril, Adamantite, Runite, Amethyst, Gem, Granite, Sandstone, Lovakite, Daeyalt, Limestone, Volcanic sulphur, Rune essence, Pure essence, Lead, Nickel, Ancient essence, etc.).
- Before marking any future skill farmer complete, repeat this preset exercise and update the decision log with the accepted target list.

## 3-Tick Mining Support

3-tick mining offers the fastest Mining experience from level 45 onwards. The method uses swamp tar on a clean herb (or other suitable items) to set up a 3-tick cycle, then moves to a rock to mine it. When timed correctly, the player skips the movement delay and has a chance to receive a rock every three ticks.

### Key Locations
- **Granite**: Cape Conch mine (requires partial Troubled Tortugans) or Bandit Camp Quarry (requires desert heat protection)
- **Iron**: Legends' Guild mine or Isle of Souls mine (4 rocks close together), or triangle spots with 3 rocks

### Implementation Requirements
- Herb + tar interaction to start 3-tick cycle
- Movement timing to align with rock clicks
- Drop-between-mines pattern (drop 1-2 granite pieces at a time)
- Movement stall mechanics for second resource roll
- 3-tick mode toggle in mining farmer config
- Herb/tar inventory slot targeting
- Rock sequence cycling (4 rocks for granite, 3-4 for iron)
- Tick-aware scheduling (3-tick alignment)
- Varrock armour detection for double-ore chance
- Infernal pickaxe detection for reduced dropping
- Desert heat protection checks for Bandit Camp
- Herb/tar inventory validation before starting
- Emergency stop if herb/tar runs out

### Tracking
- Linear task: `OSR-25` - Add 3-tick mining support for granite and iron
- Branch: `kaustubhlall/osr-25-add-3-tick-mining-support-for-granite-and-iron`

## 1.5-Tick Woodcutting Support

1.5-tick woodcutting on Fossil Island is the fastest Woodcutting training method in the game. The method uses a 3-tick cycle (such as using swamp tar on a clean herb) and alternates tiles next to the hardwood patches. When timed correctly, the player has a chance to receive two logs at once every three ticks. Planted teak trees have a 1/8 chance to fell and do not have a depletion timer, unlike teak trees found elsewhere.

### Key Location
- **Fossil Island hardwood patches**: Requires partial completion of Fossil Island access
- Best felling axe available (dragon felling axe for levels 61–71, crystal felling axe from level 71 onwards)
- Forester's rations for faster felling and run energy restoration

### Implementation Requirements
- Herb + tar interaction to start 3-tick cycle
- Tile alternation pattern next to hardwood patches
- Movement timing to align with tree clicks
- Double-log chance exploitation (1/8 felling chance per tick)
- 1.5-tick mode toggle in woodcutting farmer config
- Herb/tar inventory slot targeting
- Tile sequence cycling (alternating tiles next to patches)
- Tick-aware scheduling (3-tick alignment)
- Felling axe detection (dragon vs crystal)
- Forester's ration detection and consumption
- Stamina potion detection (if not using forester's rations)
- Planted tree detection (vs wild teak trees)
- Experience rate optimization based on axe tier
- Herb/tar inventory validation before starting
- Forester's ration inventory validation
- Emergency stop if herb/tar runs out
- Emergency stop if forester's rations run out
- Inventory full handling (drop vs stop)
- Fossil Island access verification

### Experience Rates
- Levels 61–71: Dragon felling axe
- Level 71+: Crystal felling axe
- Maximum efficiency: ~255,000 XP/hour at level 99
- Expected long-term rates: 230,000–240,000 XP/hour
- Without tick manipulation: teak is fastest only up to level 65

### Tracking
- Linear task: `OSR-26` - Add 1.5-tick woodcutting support for teak trees
- Branch: `kaustubhlall/osr-26-add-15-tick-woodcutting-support-for-teak-trees`

## Firemaking Support

Firemaking training involves lighting logs with a tinderbox or bow. Players can either drop logs and light them, or use a tinderbox/bow directly on logs in inventory. Firemaking can be trained standalone or integrated with woodcutting by burning logs instead of dropping them.

### Core Methods
- **Tinderbox Firemaking**: Use tinderbox on log, or log on tinderbox (drops and lights); drop log, right-click 'light log'; drop log, use tinderbox on log
- **Bow Firemaking (Barbarian Training)**: Use most bows on a log (drops and kindles); requires Barbarian Training completion; requires 20 levels higher in Firemaking than normal (caps at 99)
- **Forester's Campfire**: Use log on existing fire or permanent fire locations; continuous log addition without additional player action; slower rate but low-effort training

### Implementation Requirements
- Firemaking mode toggle in config
- Log selection by type (Normal, Achey, Oak, Willow, Teak, Jatoba, Arctic pine, Maple, Mahogany, Yew, Blisterwood, Camphor, Magic, Ironwood, Redwood, Rosewood)
- Tinderbox inventory slot targeting
- Bow inventory slot targeting (for barbarian firemaking)
- Fire creation methods: tinderbox, bow, or campfire
- Bank integration: withdraw logs and tinderbox
- Inventory management: use logs instead of dropping
- Fire location validation (empty ground, not in doorways, not near plants/vines)
- Movement prediction after fire creation (west/east/south/north step)
- Rapid fire chaining (prepare next fire while current is lighting)
- Dual tinderbox optimization (opposite inventory corners)
- Woodcutting integration: burn-after-cut mode in woodcutting farmer
- Barbarian firemaking detection: detect Barbarian Training completion, bow eligibility, level requirement validation

### Log Types and Requirements
- Level 1: Normal (40 XP), Achey tree (40 XP)
- Level 15: Oak (60 XP)
- Level 30: Willow (90 XP)
- Level 35: Teak (105 XP)
- Level 40: Jatoba (120 XP)
- Level 42: Arctic pine (125 XP)
- Level 45: Maple (135 XP)
- Level 50: Mahogany (157.5 XP)
- Level 60: Yew (202.5 XP)
- Level 62: Blisterwood (96 XP)
- Level 66: Camphor (180 XP)
- Level 75: Magic (303.8 XP)
- Level 80: Ironwood (220.5 XP)
- Level 90: Redwood (350 XP)
- Level 92: Rosewood (268 XP)

### Tracking
- Linear task: `OSR-38` - Add firemaking automation support
- Branch: `kaustubhlall/osr-38-add-firemaking-automation-support`

## Pyre Ship Extension

Pyre ships are part of Barbarian Training and require burning logs with bones on pyre sites north of Otto's Grotto. This is a dangerous activity in the Ancient Cavern that grants a prayer bonus of up to 300% on the next bone burial.

### Requirements
- Items: Logs, tinderbox/bow, hatchet, chewed bones (ideal) or mangled bones (substitute)
- Skills: Firemaking and Crafting levels (11-97 depending on log type)
- Location: Pyre sites north of Otto's Grotto; Ancient Cavern for bone acquisition (high risk)

### Implementation Requirements
- Hardcoded extension (not user-configurable for safety)
- Pyre site detection and targeting
- Log selection based on available bones and skill levels
- Hatchet, tinderbox/bow, and bone inventory validation
- Chewed bones preference (safer)
- Mangled bones mode with Protect from Melee activation
- Prayer bonus tracking
- High-risk area warning before starting
- Emergency stop on combat or low HP
- Emergency teleport on critical danger
- Ancient Cavern navigation and bone acquisition
- Death recovery procedures

### Safety Features
- High-risk area warning
- Chewed bones preferred over mangled bones
- Auto-activate Protect from Melee for mangled bones
- Food and prayer potion validation for mangled bones
- Emergency teleport on low HP
- Combat detection and emergency stop
- Inventory protection for valuable items

### Tracking
- Linear task: `OSR-39` - Add pyre ship automation extension
- Branch: `kaustubhlall/osr-39-add-pyre-ship-automation-extension`

## Verifier UI Improvements

### Auto-Scaling Based on Resolution

The verifier UI is optimized for 1080p displays. When viewed on higher-resolution monitors (1440p, 4K), text and icons appear too small. Browser zoom can compensate but is not ideal for dashboard UX.

#### Implementation Requirements
- Resolution detection: `window.screen.width` and `window.screen.height` on load and resize
- Scale calculation: base reference 1080p (1920x1080) = 100% scale
- Proposed formula: `scale = height / 1080` (height-based, simpler)
- Alternative formula: `scale = Math.sqrt((width² + height²) / (1920² + 1080²))` (diagonal-based)
- Clamp scale to reasonable range (100% to 150%)
- Apply scale factor as CSS variable `--scale-factor` on root
- Use `rem` units for text and spacing that respect the scale
- Scale icon sizes using `em` or `rem` relative to base font size
- Manual override control in settings
- Persist user preference in localStorage
- Reset to auto-scale button

#### Acceptance Criteria
1. Auto-scale applies on page load based on detected resolution
2. 1080p displays get 100% scale (no change from current)
3. 1440p displays get ~125-130% scale
4. 4K displays get appropriate scale (~150% or higher)
5. Scale recalculates on window resize
6. CSS variable `--scale-factor` is set on root element
7. Text, icons, and spacing scale proportionally
8. Manual override control in settings
9. User preference persists in localStorage
10. Reset to auto-scale button works

#### Tracking
- Linear task: `OSR-40` - Add auto-scaling to verifier based on screen resolution
- Branch: `kaustubhlall/osr-40-add-auto-scaling-to-verifier-based-on-screen-resolution`

### Bulk Preset Update

Add bulk preset update capability to the CV Helper verifier configuration UI. For droppable and protected items with max drop values, add an option to save a field to all presets, enabling bulk updates across multiple presets.

#### Implementation Requirements
- Applicable fields: droppable items lists, protected items lists, max drop values, other shareable configuration fields
- UI controls: "Save to all presets" checkbox/button next to applicable fields
- Confirmation dialog before bulk update showing affected presets
- Selective preset targeting with checkboxes for specific presets
- Backend integration: extend preset save API to accept bulk update flag
- Preset management: preset selection UI with "Select all"/"Deselect all" controls
- Filter presets by type (mob farmer, mining, woodcutting)
- Safety features: confirmation dialog, preview of changes, undo/rollback capability, bulk update history logging
- Validation to prevent breaking changes

#### Acceptance Criteria
1. "Save to all presets" option appears next to applicable fields
2. Bulk update updates the field across all presets
3. Confirmation dialog shows affected presets
4. Selective preset targeting works (checkboxes for specific presets)
5. Preview shows changes before applying
6. Undo/rollback capability for bulk updates
7. Bulk update history is logged
8. Validation prevents breaking changes
9. UI clearly indicates which fields support bulk updates
10. Works for droppable items, protected items, and max drop values

#### Tracking
- Linear task: `OSR-41` - Add bulk preset update capability
- Branch: `kaustubhlall/osr-41-add-bulk-preset-update-capability`
