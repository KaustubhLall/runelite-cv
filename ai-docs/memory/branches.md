# Branch Memory

Track active or paused branches here so session memory does not bleed across workstreams.

## Branch Registry

| Branch | State | Purpose | Linear | Last Updated | Notes |
| --- | --- | --- | --- | --- | --- |
| `codex/osr-1-set-up-ai-docs-harness-and-default-mcp-tooling` | archived | Initial AI docs harness setup and MCP defaults | [OSR-1](https://linear.app/hutechventures/issue/OSR-1/set-up-ai-docs-harness-and-default-mcp-tooling) | 2026-06-17 | Merged PR: https://github.com/KaustubhLall/runelite-cv/pull/1 |
| `master` | destination | Modular CV Helper source of truth after PR #2 | [OSR-48](https://linear.app/hutechventures/issue/OSR-48/cut-over-cvhelpermod-as-cv-helper-source-of-truth-after-pr-2) | 2026-06-27 | After PR #2 merges, all new CV Helper work starts from `cvhelpermod` on `master`; the old `cvhelper` package is legacy reference only. |
| `feature/cvhelper-clean-modular` | merging | PR #2 modular cutover checkpoint | [OSR-48](https://linear.app/hutechventures/issue/OSR-48/cut-over-cvhelpermod-as-cv-helper-source-of-truth-after-pr-2) | 2026-06-27 | Merge into `master` after compile, diff hygiene, `/status`, and WebHelper v3 smoke. Do not add follow-up feature work to this PR. |

## Rules

- Every active branch should have a linked Linear task or an explicit reason why not.
- Before switching away from a branch, update its state and next action.
- Before resuming a branch, review its Linear task, PR, and latest notes.
- If a branch is merged or abandoned, move its state to `archived`.
