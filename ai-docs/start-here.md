# Start Here

This folder is the operating manual for AI-assisted work on this project. Keep it lightweight, current, and useful during real sessions.

## Quick Start

1. Read this file.
2. Check `memory/active.md` for the current branch, task, and next action.
3. Check `memory/branches.md` before switching branches or continuing paused work.
4. Review the linked Linear task and recent comments.
5. Follow `session-lifecycle.md` for start, work, and end-of-session requirements.

## Working Rules

- Linear is the live source of truth for task state, progress comments, blockers, and handoff context.
- `ai-docs` is the durable source of truth for project rules, decisions, contracts, setup, and session memory.
- No Linear task is complete until related documentation is updated and any associated PR is created or clearly marked as not needed.
- Move quickly, but leave enough context that the next session can continue without a fresh explanation.
- Prefer small tasks that can be completed, reviewed, and documented independently.
- When a task is being defined, use `task-creation.md`.
- When the session is planning instead of implementing, use `planning-and-subagents.md`.
- When a task changes behavior across process boundaries, update `contracts.md`.
- When a task changes how or why the project works, update `decisions.md`.

## Cheat Sheet

- Current task stack: `memory/active.md`
- Branch state: `memory/branches.md`
- Decisions: `decisions.md`
- Task creation policy: `task-creation.md`
- Planning and subagents: `planning-and-subagents.md`
- Permanent server/Python/plugin contracts: `contracts.md`
- Session rules: `session-lifecycle.md`
- Task states: `task-state-contract.md`
- Keeping docs small: `doc-maintenance.md`
- Default tools: `gh`, Linear MCP, GitHub MCP, `agent-browser`
- MCP and local tool setup: `setup.md`

## Session Completion Checklist

- Linear task has a current status comment.
- Relevant docs are updated.
- Contracts are updated if server/Python/plugin boundaries changed.
- Decisions are logged if a durable project choice was made.
- Tests or manual verification are recorded.
- PR is created, prepared, or explicitly deferred with a reason.
- `memory/active.md` and `memory/branches.md` reflect the next action.

## Context Budget Rule

Keep this file and `memory/active.md` short enough to read at the start of every session. When a file starts becoming a history log, summarize the current state at the top and move older detail into `memory/archive/` or a linked Linear task.
