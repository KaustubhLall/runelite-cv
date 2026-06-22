# Branch Memory

Track active or paused branches here so session memory does not bleed across workstreams.

## Branch Registry

| Branch | State | Purpose | Linear | Last Updated | Notes |
| --- | --- | --- | --- | --- | --- |
| `codex/osr-1-set-up-ai-docs-harness-and-default-mcp-tooling` | archived | Initial AI docs harness setup and MCP defaults | [OSR-1](https://linear.app/hutechventures/issue/OSR-1/set-up-ai-docs-harness-and-default-mcp-tooling) | 2026-06-17 | Merged PR: https://github.com/KaustubhLall/runelite-cv/pull/1 |
| `master` | active | CV Helper plugin scaffold, target/action exports, action hotkeys, verifier UX, mob-farmer polish, and first mining/woodcutting farmer shells | [OSR-2](https://linear.app/hutechventures/issue/OSR-2/plan-example-runelite-plugin-with-broadcast-server-and-character) | 2026-06-22 | Local uncommitted CV Helper work. Verifier has helper-based port discovery, synced mob-farmer config, local named profiles, JSON import/export, WebHelper action hotkey editing, GP/HA inventory and stack-aware loot diagnostics, policy-only High Alchemy candidates, and mining/woodcutting dry/live cards with config/presets/path-distance candidate tables. It now keeps `/status` transport health separate from missing auxiliary endpoints, prefers the most verifier-compatible listening client when multiple RuneLite builds are open, and expands skill-farmer object scans beyond plain `GameObject` tiles so visible trees/rocks are less likely to disappear from status. Fresh build smoke still needs a newly relaunched client because older running clients will not expose the newest endpoints. |

## Rules

- Every active branch should have a linked Linear task or an explicit reason why not.
- Before switching away from a branch, update its state and next action.
- Before resuming a branch, review its Linear task, PR, and latest notes.
- If a branch is merged or abandoned, move its state to `archived`.
