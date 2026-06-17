# Planning And Subagents

Planning work is separate from implementation work. This keeps the project fast without forcing every session to start coding immediately.

## Planning Sessions

Use a planning session when the goal is to:

- Understand scope.
- Break a larger goal into tasks.
- Identify contracts or dependencies.
- Decide what should be built first.
- Prepare a clean implementation branch or PR plan.

Planning sessions should produce one or more of:

- Linear tasks.
- A decision log entry.
- A contract draft.
- A small implementation outline.

## Planning Outputs

Planning is successful when the next implementation session can start without re-deriving the shape of the work.

## Subagent Policy

Use a subagent only when a task benefits from separation of concern, such as:

- Researching an option while the main session keeps the plan moving.
- Drafting a task breakdown or implementation outline.
- Summarizing a long decision or contract set.

Subagents should not silently mutate shared state. Their output should land in:

- Linear comments.
- A decision entry.
- A contract draft.
- A planning note in `ai-docs`.

## Coordination Rules

- Keep one primary task owner for each branch.
- If a subagent works on a different branch or scope, record that explicitly in branch memory.
- Do not mix planning output across unrelated tasks.
- When a planning decision becomes stable, write it down before implementation starts.

## Stop Rule

Stop planning and switch to implementation once the scope, first task, and acceptance criteria are clear enough to act on.
