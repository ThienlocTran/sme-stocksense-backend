# AI Project Rules

## Project Context

- SME Stock Sense backend
- Java 21+
- Spring Boot
- Spring Security + JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Maven

## Mandatory AI Behavior

- Search before reading large files.
- Prefer `rg` for exact names, routes, classes, methods, errors, and env keys.
- Read relevant docs/source before coding.
- Do not scan the whole repo unless necessary.
- Do not guess schema, enum, workflow, API contract, or security rules.
- If unclear, stop and report uncertainty.
- Patch minimally.
- Keep existing style.
- Do not touch unrelated modules.
- Preserve Vietnamese UTF-8.
- Never generate mojibake/corrupted Vietnamese strings or the Unicode replacement character.
- Do not mix unrelated feature, fix, migration, integration, or docs work in one branch.
- Do not run Maven, app startup, Docker, or deploy commands unless the task explicitly requires it or the user confirms.

## Before Editing

Before changing files, state:

```text
Root cause:
Files to inspect:
Files to change:
Commands:
Risk:
```

If the root cause is unclear, keep investigating. Do not guess.

## Final Response Format

Use this format for implementation tasks:

```text
1. Branch name
2. Files changed
3. What was implemented
4. What was intentionally not implemented
5. Tests run
6. Package/build result
7. git diff --check result
8. Mojibake scan result
9. Commit SHA/message if committed
10. Remaining risks
```
