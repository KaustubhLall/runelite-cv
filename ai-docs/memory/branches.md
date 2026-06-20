# Branch Memory

Track active or paused branches here so session memory does not bleed across workstreams.

## Branch Registry

| Branch | State | Purpose | Linear | Last Updated | Notes |
| --- | --- | --- | --- | --- | --- |
| `codex/osr-1-set-up-ai-docs-harness-and-default-mcp-tooling` | archived | Initial AI docs harness setup and MCP defaults | [OSR-1](https://linear.app/hutechventures/issue/OSR-1/set-up-ai-docs-harness-and-default-mcp-tooling) | 2026-06-17 | Merged PR: https://github.com/KaustubhLall/runelite-cv/pull/1 |
| `master` | active | CV Helper plugin scaffold, target/action exports, action hotkeys, and mob-farmer automation stabilization | [OSR-2](https://linear.app/hutechventures/issue/OSR-2/plan-example-runelite-plugin-with-broadcast-server-and-character) | 2026-06-20 | Local uncommitted CV Helper work. Mob farmer now has tick-driven logged-in stepping, priority loot diagnostics, and death-loot timing diagnostics. Next focus is config/profile export/import, real pathing/navigation/anchor behavior, local-area/piling constraints, UI grouping, and then mining/woodcutting/fishing modules using shared primitives. |

## Rules

- Every active branch should have a linked Linear task or an explicit reason why not.
- Before switching away from a branch, update its state and next action.
- Before resuming a branch, review its Linear task, PR, and latest notes.
- If a branch is merged or abandoned, move its state to `archived`.
