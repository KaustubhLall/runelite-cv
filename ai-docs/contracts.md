# Permanent Contracts

This file records durable contracts between moving parts of the project. Update it whenever server, Python, plugin, or automation boundaries change.

## Contract Types

- Server API contracts: endpoints, request/response shapes, auth assumptions, error formats, and versioning.
- Python bridge contracts: input/output schemas, process lifecycle, logs, health checks, and failure behavior.
- RuneLite plugin contracts: plugin configuration, expected server behavior, UI integration points, and user-visible states.
- MCP/tool contracts: tools that must be available for automation, setup requirements, and fallback behavior.
- Data contracts: file formats, persisted state, cache rules, and migration expectations.

## Entry Template

```md
## Contract: Short Name

- Status: draft | active | deprecated
- Owners: Linear task, branch, or PR
- Components: server | Python | plugin | MCP | data
- Last updated: YYYY-MM-DD

### Purpose

What this contract protects.

### Contract

The stable behavior, schema, protocol, or expectation.

### Verification

How to confirm the contract still holds.

### Notes

Open questions, migration notes, or known limitations.
```

## Active Contracts

No active contracts recorded yet.
