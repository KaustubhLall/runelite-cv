# Setup

This file tracks the local and connected setup needed for automated work.

## Required Local Setup

- Java and Gradle for the RuneLite codebase.
- Moonlight development environment.
- Python runtime for any bridge or companion process.
- Browser automation for visual verification.
- GitHub access for branches, PRs, and review workflows.
- Linear access for task state and session memory.

## MCP And App Setup

These tools are useful for the intended workflow:

- Linear: create, read, comment on, and update tasks.
- GitHub MCP: repository, issue, and PR context for agent workflows.
- `gh`: CLI fallback for branches, PRs, CI, and auth checks.
- `agent-browser`: browser automation for checking local output and GitHub PR pages.
- `mcp-remote`: compatibility bridge for clients that need stdio wrappers around remote MCP servers.
- Filesystem/shell: run builds, tests, servers, and Python programs.
- Optional Gmail/Notion/etc.: only if a task explicitly needs those workspaces.

## Automation Expectations

- Server startup commands should be documented once they exist.
- Python bridge startup commands should be documented once they exist.
- Health checks should be documented for both server and Python process.
- Any required environment variables should be listed with purpose, not secret values.
- Browser verification targets should be listed with expected URLs.

## Open Setup Items

- Define Moonlight plugin run command.
- Define server run command.
- Define Python bridge run command.
- Define health check endpoints.
- Confirm GitHub authentication path.
- Confirm Linear task workflow and labels.
- Confirm browser verification workflow.
- Confirm MCP server configuration for GitHub, Linear, and `agent-browser`.
- Document required environment variables with names and purpose, excluding secret values.
- Add smoke-test commands for the server, Python bridge, and plugin once they exist.
