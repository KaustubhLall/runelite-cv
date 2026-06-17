# Decision Log

Record decisions that future sessions should not have to rediscover.

## Entry Template

```md
## YYYY-MM-DD: Short Decision Title

- Status: proposed | accepted | superseded
- Related: Linear task, branch, PR, or contract

### Context

What forced the decision.

### Decision

What we chose.

### Tradeoffs

What this improves, what it costs, and what alternatives were considered.

### Follow-up

Anything that should be revisited later.
```

## Decisions

## 2026-06-16: Jagex Launcher Credential Bridge For Dev Login

- Status: accepted
- Related: `OSR-2`

### Context

The user's `CoreDump` / `C0REDUMPED` account is managed by Jagex Launcher. Directly launching the custom RuneLite jar starts the client but does not establish game-login credentials.

### Decision

Use RuneLite's official `--insecure-write-credentials` development flow. First launch official RuneLite through Jagex Launcher with that client argument so it writes `C:\Users\kaust\.runelite\credentials.properties`; then launch the custom jar.

### Tradeoffs

This uses a sensitive local credential file and requires careful handling, but it matches RuneLite's documented development path for Jagex accounts. The custom launch should be considered credential-ready only when the log contains `injected-client - read 5 credentials from disk`.

### Follow-up

Remove `credentials.properties` after development if the user wants RuneLite to return to normal launcher-only behavior.

## 2026-06-16: Stable RuneLite Service Version For Snapshot Dev Builds

- Status: accepted
- Related: `OSR-2`

### Context

The local custom RuneLite jar is built as `1.12.29-SNAPSHOT`, but RuneLite Plugin Hub and API/config services are versioned around stable releases. Game login can work through Jagex credentials while Plugin Hub/config sync still fails because the snapshot service path is not available.

### Decision

Keep the local jar version as-is, but launch development builds with stable service overrides:

- `-Drunelite.pluginhub.version=1.12.28`
- `-Drunelite.http-service.url=https://api.runelite.net/runelite-1.12.28`

The wrapper script `scripts/launch-dev-runelite.ps1` defaults to `-ServiceVersion 1.12.28` and passes those JVM properties before `-jar`.

### Tradeoffs

This avoids mutating Maven project version or dependency coordinates just to talk to live services. If RuneLite protocol compatibility changes, the stable service version may need to be updated alongside the local source checkout.

### Follow-up

If Plugin Hub/config sync still fails after launch overrides, inspect the latest RuneLite log for the actual HTTP URL/status before changing credentials or account-login flow.
