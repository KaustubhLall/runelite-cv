# CV Helper Verifier

Local static dashboard for verifying `CV Helper` localhost exports before a Python agent relies on them.

## Open

Open `index.html` directly in a browser:

```text
C:\Users\kaust\IdeaProjects\runelite-cv\tools\cv-helper-verifier\index.html
```

Enter the active CV Helper port from RuneLite logs, for example `56112`.

## What It Checks

- `GET /status`
- `GET /targets/prayer`
- `GET /targets/spell`
- `GET /targets/minimap`
- `GET /targets/inventory`
- `GET /targets/equipment`
- `POST /capture`
- `POST /capture/screen`
- `POST /capture/minimap`

The dashboard highlights likely export problems such as oversized inventory/equipment boxes and unnamed equipment slots.

## Notes

- The RuneLite plugin must include CORS headers for browser fetches to work.
- If a target surface shows zero results, first verify the corresponding RuneLite tab/interface is visible.
- Java plugin changes require a fresh RuneLite launch; they do not hot-load into an already-open client.
