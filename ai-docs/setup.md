# Setup

This file tracks the local and connected setup needed for automated work.

## Required Local Setup

- Java and Gradle for the RuneLite codebase.
- RuneLite development environment.
- Python runtime for any bridge or companion process.
- Browser automation for visual verification.
- GitHub access for branches, PRs, and review workflows.
- Linear access for task state and session memory.

## MCP And App Setup

These tools are useful for the intended workflow:

- Linear: create, read, comment on, and update tasks.
- GitHub MCP: repository, issue, and PR context for agent workflows.
- `gh`: CLI fallback for branches, PRs, CI, and auth checks.
- `agent-browser`: browser automation for checking local output and GitHub PR pages.
- `mcp-remote`: compatibility bridge for clients that need stdio wrappers around remote MCP servers.
- Filesystem/shell: run builds, tests, servers, and Python programs.
- Optional Gmail/Notion/etc.: only if a task explicitly needs those workspaces.

## Automation Expectations

- Server startup commands should be documented once they exist.
- Python bridge startup commands should be documented once they exist.
- Health checks should be documented for both server and Python process.
- Any required environment variables should be listed with purpose, not secret values.
- Browser verification targets should be listed with expected URLs.

## Open Setup Items

- RuneLite plugin run command: `powershell -ExecutionPolicy Bypass -File .\scripts\launch-dev-runelite.ps1`.
- `scripts\launch-dev-runelite.ps1` auto-detects Java from `JAVA_HOME`, `C:\Program Files\Android\openjdk\jdk-21.0.8`, or `C:\Users\kaust\.jdks\corretto-22.0.2`. Pass `-JavaHome <path>` only if using a different JDK.
- Faster CV Helper launch command: `powershell -ExecutionPolicy Bypass -File .\scripts\run-cv-helper.ps1`.
- Desktop shortcut: `C:\Users\kaust\OneDrive\Desktop\RuneLite CV Helper.lnk`.
- RuneLite live-service compatibility override: the dev launch script defaults to `-ServiceVersion 1.12.28`, which maps Plugin Hub and RuneLite API requests to the stable `https://api.runelite.net/runelite-1.12.28` service while running the local snapshot jar.
- Jagex credential bootstrap command: `powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-jagex-credentials.ps1`.
- Define server run command.
- Define Python bridge run command.
- Define health check endpoints.
- Confirm GitHub authentication path.
- Confirm Linear task workflow and labels.
- Confirm browser verification workflow.
- Confirm MCP server configuration for GitHub, Linear, and `agent-browser`.
- Document required environment variables with names and purpose, excluding secret values.
- Add smoke-test commands for the server, Python bridge, and plugin once they exist.
- Record recurring user workflow preferences here when they affect task intake, planning, or handoff so future sessions can reuse them by default.

## RuneLite Jagex Account Development Login

The user plays through a Jagex Launcher-managed account (`CoreDump` / `C0REDUMPED`). A custom-built RuneLite jar cannot log in until the Jagex Launcher credential bridge has been bootstrapped.

Use this sequence:

1. Run `powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-jagex-credentials.ps1`.
2. Launch RuneLite through Jagex Launcher for `CoreDump` / `C0REDUMPED`.
3. Confirm `C:\Users\kaust\.runelite\credentials.properties` exists.
4. Run `powershell -ExecutionPolicy Bypass -File .\scripts\launch-dev-runelite.ps1`.
5. Confirm the fresh client log includes `injected-client - read 5 credentials from disk`.

Do not treat `No session file exists` from `SessionManager` as the Jagex game-login blocker by itself. That line is for the RuneLite account sync session. For this task, the credential signal is the injected client reading or writing credentials.

As of 2026-06-16, the user confirmed game login works in the custom client. If Plugin Hub or config sync fails after that, investigate RuneLite service-version/API compatibility and RuneLite account sync separately instead of repeating the Jagex credential bootstrap.

For snapshot builds, launch with a stable service compatibility version:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\launch-dev-runelite.ps1 -ServiceVersion 1.12.28
```

Expected launch output includes:

- `Launching custom RuneLite with service compatibility version: 1.12.28`
- `RuneLite API base: https://api.runelite.net/runelite-1.12.28`

Expected client log signals include:

- `injected-client - read 5 credentials from disk`
- `ExternalPluginManager - Loading external plugin "..."`
- `CV Helper starting`
- `CV Helper local export listening on http://127.0.0.1:<port>/status`

After the CV Helper port appears in the log, verify localhost export from PowerShell:

```powershell
Invoke-RestMethod -Uri http://127.0.0.1:<port>/status
Invoke-RestMethod -Uri http://127.0.0.1:<port>/capture -Method Post
Invoke-RestMethod -Uri http://127.0.0.1:<port>/capture/screen -Method Post
Invoke-RestMethod -Uri http://127.0.0.1:<port>/capture/minimap -Method Post
Invoke-RestMethod -Uri http://127.0.0.1:<port>/targets/prayer
Invoke-RestMethod -Uri http://127.0.0.1:<port>/targets/spell
Invoke-RestMethod -Uri http://127.0.0.1:<port>/targets/minimap
Invoke-RestMethod -Uri http://127.0.0.1:<port>/targets/inventory
Invoke-RestMethod -Uri http://127.0.0.1:<port>/targets/equipment
```

Expected status shape:

```json
{"plugin":"CV Helper","status":"server@<port>","port":<port>}
```

Expected capture acknowledgement:

```json
{"ok":true,"queued":true}
```

RuneLite writes CV Helper screenshots under `C:\Users\kaust\.runelite\screenshots\`, for example `cv-helper 2026-06-16_19-38-02.png`. If capture is triggered while the client is on the login screen, RuneLite may block the file write; the plugin reports that as `capture-blocked:login-screen`.

`/status` responding while `gameState` is `LOGIN_SCREEN` means CV Helper is running but live widget polling cannot be verified yet. Ask the user to log in, or use the guarded `POST http://127.0.0.1:11777/login/click` helper if the click-to-play widget is visible. Do not treat empty prayer/spell/panel targets at the login screen as plugin failure.

For live checks, the practical login flow is: click `Play Now`, wait for the client to reach the game login flow, then let the user finish login. If this wait is the only blocker, ask a follow-up confirmation question instead of ending the session with no verification checklist.

For prayer target verification, open the prayer tab in RuneLite before calling `/targets/prayer`. The endpoint reports canvas `bounds` and `center` points for visible prayer-related widgets.

For runtime verification that depends on live game widgets, ask the user to log in to the newest launched custom client before interpreting endpoint output. Logged-out state commonly reports unavailable spellbook metadata and empty target surfaces.
