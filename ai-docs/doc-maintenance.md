# Doc Maintenance

The docs should stay complete without becoming too large to load every session.

## Read Every Session

These files should remain short:

- `start-here.md`
- `memory/active.md`
- `memory/branches.md`
- The linked Linear task
- The current PR description or status, if a PR is active

## Read When Relevant

These files can grow moderately, but should stay indexed:

- `decisions.md`
- `contracts.md`
- `setup.md`
- `task-state-contract.md`
- `session-lifecycle.md`
- `memory/archive/`

## Archive Rule

When a doc grows because it is accumulating history:

1. Keep the current summary at the top of the original file.
2. Move old detail to `memory/archive/`.
3. Link the archive entry from the original file.
4. Keep Linear as the detailed task timeline when task history is the main reason for the growth.

## Split Rule

Create a new doc only when one of these is true:

- A file mixes unrelated responsibilities.
- A section is large enough that future sessions should read it only when relevant.
- A contract needs its own stable reference.
- A decision is superseded but still historically important.

Do not split docs just because a category might exist someday.
