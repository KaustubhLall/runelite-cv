# Onboarding

This guide helps new developers and AI agents get started with the RuneLite CV project.

## Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/KaustubhLall/runelite-cv.git
   cd runelite-cv
   ```

2. **Set up MCP tools** (for AI-assisted development)
   - Linear MCP: Configured in `.mcp-config.json` using the official Linear hosted server with OAuth authentication
   - GitHub MCP: Configured in your GitHub Copilot IntelliJ config at `C:\Users\kaust\AppData\Local\github-copilot\intellij\mcp.json`
   - GitHub CLI fallback: Already authenticated as KaustubhLall

3. **Install Java and Gradle**
   - Java 21+ required (auto-detected from `JAVA_HOME`, `C:\Program Files\Android\openjdk\jdk-21.0.8`, or `C:\Users\kaust\.jdks\corretto-22.0.2`)
   - Gradle is included via the Gradle wrapper (`./gradlew`)

4. **Build the project**
   ```powershell
   .\gradlew.bat buildAll
   ```

5. **Launch RuneLite with CV Helper**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\launch-dev-runelite.ps1
   ```

## Project Structure

```
runelite-cv/
├── ai-docs/              # AI-assisted development documentation
│   ├── AGENTS.md         # AI collaboration rules
│   ├── setup.md          # MCP and local tool setup
│   ├── contracts.md      # API and system contracts
│   ├── cv-helper-spec.md # CV Helper plugin specification
│   └── memory/           # Session and branch memory
├── runelite-api/         # RuneLite API module
├── runelite-client/      # Main client and CV Helper plugin
│   └── src/main/java/net/runelite/client/plugins/cvhelper/
├── tools/cv-helper-verifier/  # Browser-based verifier dashboard
└── scripts/              # Launch and utility scripts
```

## Key Concepts

### CV Helper Plugin
The CV Helper plugin exports RuneLite UI geometry and interaction affordances over localhost for external computer-vision/autonomous-agent processes. It provides:
- Local HTTP endpoints on `127.0.0.1:11777` (default)
- Target exports for prayer, spell, inventory, equipment, minimap, panels, combat, and entities
- Screenshot capture endpoints
- Configurable action hotkeys
- Mob farmer, mining farmer, and woodcutting farmer automation

### AI-Assisted Development
This project uses AI agents with MCP tools for Linear task management and GitHub integration. Key documents:
- `ai-docs/AGENTS.md` - Working rules for AI sessions
- `ai-docs/setup.md` - MCP and tool setup
- `ai-docs/contracts.md` - Durable contracts between system components
- `ai-docs/memory/active.md` - Current session state
- `ai-docs/memory/branches.md` - Branch tracking

### Linear Integration
Linear is the source of truth for task state, progress comments, and blockers. Each branch should link to a Linear task. Current tasks:
- `OSR-1` - Set up AI docs harness and default MCP tooling (completed)
- `OSR-2` - CV Helper plugin with broadcast/local export support (active)
- `OSR-3` - Python receiver for CV Helper exports
- `OSR-4` - Tick-synchronized export delivery
- `OSR-5` - CV Helper hotkeys for prayers and spell actions
- `OSR-6` - CV Helper verifier client site
- `OSR-7` - First reusable automation (mob farmer)

## Development Workflow

### Starting a New Session
1. Read `ai-docs/AGENTS.md`
2. Check `ai-docs/memory/active.md` for current branch and task
3. Check `ai-docs/memory/branches.md` before switching branches
4. Review the linked Linear task and recent comments
5. Follow `ai-docs/session-lifecycle.md` for session rules

### Making Changes
1. Create or switch to a branch linked to a Linear task
2. Implement changes following the contracts in `ai-docs/contracts.md`
3. Test locally using the verifier at `http://127.0.0.1:8765/`
4. Update relevant documentation
5. Create a PR with a clear description

### Session Completion
- Linear task has a current status comment
- Relevant docs are updated
- Contracts are updated if boundaries changed
- Tests or manual verification are recorded
- PR is created or explicitly deferred
- `memory/active.md` and `memory/branches.md` reflect next action

## Common Commands

### Build
```powershell
.\gradlew.bat buildAll          # Build all modules
.\gradlew.bat :client:compileJava  # Fast compile for plugin work
.\gradlew.bat :client:shadowJar    # Produce runnable jar
```

### Launch
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\launch-dev-runelite.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\run-cv-helper.ps1  # Faster CV Helper launch
```

### Verification
```powershell
powershell -ExecutionPolicy Bypass -File .\tools\cv-helper-verifier\serve.ps1
# Then open http://127.0.0.1:8765/
```

### Git
```powershell
gh pr create                    # Create PR using GitHub CLI
gh pr view                      # View current PR
gh issue list                   # List issues
```

## Important Notes

- **Never delete information** from design documents - prefer additive changes
- **Linear is the live source of truth** for task state
- **ai-docs is the durable source of truth** for project rules and decisions
- **Java indentation uses tabs**, not spaces
- **Keep RuneLite widget reads on the client thread** - never access mutable client state from HTTP handler threads
- **End every implementation response** with: what changed, what to test, and next steps

## Troubleshooting

### RuneLite Login Issues
If the custom jar won't log in:
1. Run `powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-jagex-credentials.ps1`
2. Launch RuneLite through Jagex Launcher for your account
3. Confirm `C:\Users\kaust\.runelite\credentials.properties` exists
4. Relaunch the dev script

### Plugin Hub 404s
Launch with stable service overrides:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\launch-dev-runelite.ps1 -ServiceVersion 1.12.28
```

### CV Helper Not Responding
- Check the RuneLite log for the port number
- Verify `/status` responds: `Invoke-RestMethod -Uri http://127.0.0.1:<port>/status`
- If on login screen, empty widget targets are expected - log in first

## Additional Resources

- [RuneLite Plugin Hub](https://github.com/runelite/runelite)
- [Project README](../README.md)
- [CV Helper Spec](./cv-helper-spec.md)
- [Contracts](./contracts.md)
- [Setup Guide](./setup.md)
