# Session Lifecycle

Every session should have a clear start, working loop, and closeout. The goal is continuity without heavy process.

## Start

1. Identify the current git branch.
2. Read `start-here.md`.
3. Read `memory/active.md`.
4. Read `memory/branches.md`.
5. Open the linked Linear task and review recent comments.
6. Confirm whether the session is continuing active work, switching branches, or starting a new task.

## Work

- Keep changes scoped to the current task and branch.
- Add Linear comments when meaningful context appears: decisions, blockers, verification results, or changed scope.
- Update `contracts.md` before relying on a server/Python/plugin interface that should remain stable.
- Update `decisions.md` when a decision would be expensive or confusing to rediscover later.
- Prefer incremental verification over large end-of-session surprises.

## End

Before ending a session:

1. Update the Linear task with status, verification, blockers, and next action.
2. Update `memory/active.md`.
3. Update `memory/branches.md` if branch state changed.
4. Update docs touched by the task.
5. Create or prepare the associated PR, unless explicitly deferred.
6. Record any unfinished work in a way the next session can resume directly.

## Branch Switching

- Treat branch switching as a context switch.
- Before switching away, update branch status in `memory/branches.md`.
- After switching, reload the branch's Linear task and notes before editing.
- Do not assume memory from one branch applies to another branch unless `branches.md` or Linear explicitly says so.
