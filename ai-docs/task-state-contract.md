# Task State Contract

Use the same task language in Linear and `ai-docs` so state stays easy to scan.

## States

- `queued`: Known work that has not started.
- `active`: The current focus of a session or branch.
- `blocked`: Work cannot continue until an external decision, dependency, credential, or upstream change is resolved.
- `review`: Implementation is ready for review or PR validation.
- `done`: Code, docs, verification, Linear updates, and PR requirements are complete.
- `archived`: Historical work that is no longer active or relevant.

## Completion Rule

A task is not `done` until:

- The implementation is complete or explicitly not required.
- Relevant docs are updated.
- Relevant contracts are updated.
- Verification is recorded.
- Linear has a final status comment.
- The associated PR exists, is prepared, or is explicitly deferred with a reason.

## Task Shape

Prefer tasks with:

- A single concrete goal.
- Clear acceptance criteria.
- Links to relevant docs, PRs, branches, and contracts.
- A small enough scope to finish or hand off cleanly.
