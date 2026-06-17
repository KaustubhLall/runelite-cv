# Task Creation Policy

Use Linear for task intake and tracking. Keep task creation light, but consistent enough that future sessions can understand why a task exists.

## When To Create A Task

Create a task when work is:

- A meaningful feature, bug fix, or workflow change.
- Large enough to benefit from a dedicated branch.
- Likely to need review, verification, or follow-up comments.
- Related to a durable contract, decision, or setup change.

Do not create a task for every tiny edit. Merge tiny cleanup work into the nearest active task when it is clearly part of the same outcome.

## Task Shape

A good task should usually include:

- A short title that names the outcome.
- A brief purpose or context.
- Clear acceptance criteria.
- Links to relevant docs, branches, PRs, or contracts.
- A note if it is a planning task rather than an implementation task.

## Task Levels

- Planning task: define scope, decisions, contracts, and acceptance criteria.
- Implementation task: make the code or docs change.
- Verification task: validate behavior, tests, browser checks, or PR review.

## Task Rules

- Prefer one task with a clear outcome over several overlapping tasks.
- If a task needs to branch into unrelated work, create a new task instead of mixing scopes.
- If a task changes a contract or decision, update the relevant doc in the same session when possible.
- If a task is blocked, log the blocker in Linear and in the active memory file.

## Suggested Minimum Fields

- Title
- Goal
- Acceptance criteria
- Related branch or PR, once it exists
- Related contract or decision, if applicable

## Completion Rule

A task should not be marked done until the implementation, docs, verification, and handoff notes are all in place.
