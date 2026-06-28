# CV Helper WebHelper Console — v3

A ground-up rewrite of the verifier console with a proper module structure and a
themed "engraved bronze on weathered wood" RuneScape-adjacent UI. It speaks the
**exact same HTTP contract** as v1/v2, so it stays compatible with every CV
Helper plugin build — no plugin changes are required to run it.

## Open

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\cv-helper-verifier\serve-v3.ps1 -OpenBrowser
```

Served at <http://127.0.0.1:8770/>. v1 (`8765`) and v2 (`8769`) remain available.
A static server is required (ES modules + `fetch` cannot run from `file://`).

## Status

Built and verified live against the running plugin:

- **Dashboard** — Client-ready hero, Connection, Vitals, Wealth, Interface,
  live Target Counts (prayer/spell/minimap/inventory/equipment/panels/combat/
  entities) and Warnings. Matches the dashboard reference.
- **Mob Farmer (Overview)** — hero metric strip, Target & Combat, Loot/Inventory
  policy, **Recent Decisions** timeline, **Pathing/Reachability 2D grid**,
  Survival / Intermediate / Loot, Target & Loot candidate tables, Recent Menu
  Entries, High-Alch candidates, Inventory overview, Events, **functional Quick
  Controls** (toggles + selects POST live config), System Health. Start / Stop /
  Step / Focus / target / mode wired.
- **Mining & Woodcutting** — shared themed renderer: metric strip, Selected
  Object, Candidate Summary, the same 2D path grid, Candidates table, Drop
  Policy, Inventory. Start / Stop / Step / target / mode wired.
- **Inventory** — carried/equipment value cards + item tables.
- **Raw Data** — collapsible live JSON for status + all three farmers.
- **Debug** — port discovery + attempts.

Staged for the next pass (heavier ports from v2): the Mob Farmer
Configuration / Inventory Policy / Targeting / Debug sub-tabs, the Actions
surface page, and the global Configuration editor (profiles + full schema).
Common config editing already works today via each farmer's Quick Controls.

## Assets

Signature emblems (skull, pickaxe, willow, crest, swords, coins, shield, scroll,
heart, wand, crosshair, package) are **hand-authored inline SVG**, baked into
`js/icons.js` — a real vector sprite sheet, no slicing needed. They're used for
the brand crest, farmer nav items, and page emblems. Utility glyphs fall through
to Lucide (matching the reference's clean header icons). `ASSETS.md` still holds
a generation prompt if you want a bespoke raster set later (one-line swap).

## The 2D path grid

The grid plots the player at centre and every target candidate at its **true
relative tile offset** (north = up), colour-coded:

| marker            | meaning                            |
| ----------------- | ---------------------------------- |
| cyan dot + ring   | you (player)                       |
| gold diamond      | selected / engaged target          |
| green dot         | reachable candidate                |
| red ✕             | unreachable candidate              |
| amber ring        | engaged by another player          |
| dashed edge ring  | candidate is off the visible grid  |

It is built entirely from the existing contract: `status.player.worldLocation`
plus each `mobFarmer.candidates[].worldLocation` / `reachable` / `pathDistance`.
**Requires a current plugin build** (these fields exist in source but a stale
running client may omit them — relaunch RuneLite). The grid degrades gracefully
to "awaiting location" when they are absent.

If a future plugin build adds `mobFarmer.pathing.route` (tile list) and
`mobFarmer.pathing.doors`, the grid renders the walked route and door markers
automatically — see `js/pathGrid.js` (`drawRoute` / `drawDoors`). That is the
one planned plugin addition for full fidelity; nothing else needs exposing.

## Structure

```
v3/
  index.html            shell markup (rail, topbar, page, footer)
  css/
    tokens.css          design tokens (colours, type, spacing)
    base.css            reset, typography, atoms
    shell.css           rail / topbar / footer chrome
    components.css      panels, metrics, tables, badges, controls
    mob-farmer.css      page header, tabs, decisions, path grid
  js/
    format.js           pure formatters (no DOM)
    icons.js            Lucide wrapper
    api.js              transport: discovery, fetch, remembered ports
    components.js       HTML-string UI builders
    pathGrid.js         the 2D reachability grid (SVG)
    pages/
      mobFarmer.js      Mob Farmer Overview
      dashboard.js      Dashboard
    app.js              bootstrap: routing, polling, deck wiring
```

Icons use Lucide via CDN (matches v1/v2). See `ASSETS.md` for a sprite-sheet
prompt if you want to replace them with bespoke engraved emblems later.
