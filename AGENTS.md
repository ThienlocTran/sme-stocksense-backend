# AGENTS.md

This file is the entry point for AI agents.

Before any task:
1. Read this file.
2. Read `rules/README.md`.
3. Read the specific rule file relevant to the task.
4. Read relevant module docs/source.
5. Do not guess.

For Git, Flyway, validation, and integration rules, follow the files in `rules/`.

## Global AI Rules

- Save tokens.
- Search before reading large files.
- Prefer `rg` for exact names, routes, classes, methods, errors, and env keys.
- Use semantic search only when the feature location or behavior flow is unclear.
- Use MCP only when it is useful, and ask before any MCP write action.
- Do not scan the whole repo unless necessary.
- Patch minimally.
- Keep existing style.
- Do not touch unrelated modules.
- Do not touch production config, secrets, DB, Docker, deploy files, or protected files without confirmation.
- Do not run build, dev, test, deploy, Maven, Docker, reset, clean, or destructive commands without confirmation.
- Do not modify Java source, migrations, application config, `.env`, or feature code during docs-only rules tasks.
- Preserve Vietnamese UTF-8. Never create mojibake or replacement characters.

## Required Reading

- `rules/README.md`
- `rules/AI_PROJECT_RULES.md`
- `rules/GIT_WORKFLOW_RULES.md`
- `rules/DB_MIGRATION_POLICY.md`
- `rules/VALIDATION_CHECKLIST.md`
- `rules/SPRINT_INTEGRATION_CHECKLIST.md` when preparing merges or integration branches

Module docs such as `docs/inbound-workflow.md` and task READMEs provide business and implementation context. The `rules/` folder contains operating rules.
