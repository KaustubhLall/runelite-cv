# CV Helper WebHelper Console v2

A redesigned, self-contained rewrite of the CV Helper verifier dashboard. It keeps the same underlying API contracts and functionality as the original (`../`) but presents a cleaner, modern UI with better spacing, typography, and responsive layout.

## Open

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\cv-helper-verifier\serve-v2.ps1 -OpenBrowser
```

The v2 console is served at [http://127.0.0.1:8769/](http://127.0.0.1:8769/). The original remains available on port `8765`.

## Functionality preserved from v1

- Auto-discovery and fallback port scanning via the helper on `8765` (or browser fallback)
- Dashboard: live state, target counts, warnings, capture previews
- Farmers: Mob, Mining, Woodcutting with dry/live step/start/stop and focus click
- Inventory: slot summary, inventory/equipment tables with GE/HA values
- Actions: prayer, spell, minimap, inventory, equipment, panels, combat surfaces
- Configuration: draft/live config editor, profiles, import/export JSON, action hotkeys
- Debug: port discovery attempts, action log, nearby entities
- Raw Data: collapsible raw JSON payloads

## Design changes

- Top app bar with global actions and connection status
- Icon rail navigation with keyboard-focusable items
- Card-based layout with improved visual hierarchy
- Better data tables with sticky headers, column sorting, and inline filters
- Modern dark theme with navy/slate backgrounds and amber/cyan accents
- Responsive grid that adapts from desktop to mobile without horizontal overflow

## Notes

- The v2 app uses the same CV Helper endpoints (`http://127.0.0.1:11777` by default) as v1.
- Discovery still relies on the v1 helper running on `8765`. If you serve v2 alone, browser fallback will still probe `11777` and remembered ports.
