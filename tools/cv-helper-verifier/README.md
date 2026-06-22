# CV Helper Verifier

Local static dashboard for verifying `CV Helper` localhost exports before a Python agent relies on them.

## Open

Start the local verifier helper so the dashboard can auto-discover the active CV Helper port:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\cv-helper-verifier\serve.ps1 -OpenBrowser
```

That serves the dashboard at [http://127.0.0.1:8765/](http://127.0.0.1:8765/).

You can still open `index.html` directly, but full fallback discovery needs the helper because the browser alone cannot scan live local Java listeners.

## What It Checks

- `GET /status`
- `GET /targets/prayer`
- `GET /targets/spell`
- `GET /targets/minimap`
- `GET /targets/inventory`
- `GET /targets/equipment`
- `GET /automation/mob-farmer/status`
- `GET/POST /automation/mob-farmer/config`
- `POST /automation/mob-farmer/focus-click`
- `POST/GET /automation/mob-farmer/step?target=cow&live=false`
- `POST/GET /automation/mob-farmer/start?target=cow&live=false`
- `POST/GET /automation/mob-farmer/stop`
- `GET/POST /automation/mining/config`
- `POST/GET /automation/mining/step?target=iron%20rocks&live=false`
- `POST/GET /automation/mining/start?target=iron%20rocks&live=false`
- `POST/GET /automation/mining/stop`
- `GET/POST /automation/woodcutting/config`
- `POST/GET /automation/woodcutting/step?target=oak&live=false`
- `POST/GET /automation/woodcutting/start?target=oak&live=false`
- `POST/GET /automation/woodcutting/stop`
- `POST /capture`
- `POST /capture/screen`
- `POST /capture/minimap`
- `POST /login/click`

The dashboard highlights likely export problems such as oversized inventory/equipment boxes and unnamed equipment slots.

## Notes

- The RuneLite plugin must include CORS headers for browser fetches to work.
- If a target surface shows zero results, first verify the corresponding RuneLite tab/interface is visible.
- If `/status` shows `LOGIN_SCREEN`, the plugin can be healthy while live widget targets remain empty. Use `Click login` or log in manually before judging target polling.
- The verifier prefers `11777`, then automatically falls back if the live plugin had to bind another port.
- If an older RuneLite process still owns `11777`, auto-discovery will land on that healthy preferred-port export. Enter the newer fallback port from `/status` or the launch log manually to verify fresh Java endpoint changes.
- The `Port discovery` card shows which ports were probed and whether the helper had to recover off the preferred port.
- The app is organized into Dashboard, Farmers, Inventory, Actions, Configuration, Debug, and Raw Data sections so large target/config/debug surfaces are reachable without one long scroll.
- The mob-farmer panel shows the latest decision or guard reason, plus controls that always use the active CV Helper port.
- The config editor keeps Live State separate from Draft Config. Polling can refresh live cards and raw payloads, but editable fields only change when you use `Load current into draft`, `Apply draft`, `Reset draft`, `Import JSON`, or `Export JSON`.
- Applying draft config saves validated changes back to RuneLite; JSON import loads the draft only. Action slot hotkeys support text such as `F12`, `CTRL+1`, `ALT+Q`, or `NOT_SET`.
- Mob-farmer profiles are saved locally in the browser and can be saved, loaded into the draft, duplicated, deleted, imported, and exported before applying to RuneLite.
- Loot and inventory tables show GP/HA values, stack totals, rejection reasons, high-priority loot, protected items, safe drop candidates, and policy-only High Alchemy candidates.
- Mining and woodcutting cards expose their own dry/live controls, presets, draft config, import/export JSON, configurable scan radius/max candidate limits, reachable target tables, visibility/bounds/click diagnostics, path distance, inventory GP/HA value, and drop-candidate diagnostics.
- The `Focus click` button queues the guarded one-click startup/login focus workaround exposed by `/automation/mob-farmer/focus-click`.
- Use the Raw Data section or `Show raw JSON` when you need exact payloads. Default pages favor structured cards, tables, and badges over raw dumps.
- Java plugin changes require a fresh RuneLite launch; they do not hot-load into an already-open client.
