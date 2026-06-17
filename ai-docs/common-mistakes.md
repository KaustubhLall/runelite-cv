# Common Mistakes

Track mistakes that keep recurring so the harness can teach the better default.

## Current Entries

- Do not confuse "custom RuneLite jar launches" with "custom RuneLite can log into a Jagex Launcher-managed character." For `CoreDump` / `C0REDUMPED`, the stop condition is a custom client launch after `credentials.properties` exists and the log shows `injected-client - read 5 credentials from disk`.
- Do not continue broad CV Helper feature work while the launch/login bridge is broken. Fix and document the Jagex Launcher credential bootstrap first.
- Do not use `SessionManager - No session file exists` as proof that Jagex game credentials are missing. That is the RuneLite account sync session, not the Jagex Launcher credential file.
- After the user confirms game login in the custom client, do not loop back to Jagex credentials for Plugin Hub/config sync failures. Treat those as RuneLite service-version/API compatibility or RuneLite account-sync problems.
- Do not leave a local-development plugin disabled by default when the user needs a right-sidebar panel for verification. Enable `CV Helper` by default during this phase so the navigation button appears automatically.

## Update Rule

Add a note here when a mistake has happened more than once or when a fix should become the default behavior in `AGENTS.md` or a policy file.
