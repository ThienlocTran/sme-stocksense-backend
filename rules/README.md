# SME Stock Sense Rules

This folder is the source of truth for AI agents and contributors.

Read order:
1. AGENTS.md
2. rules/README.md
3. rules/AI_PROJECT_RULES.md
4. rules/GIT_WORKFLOW_RULES.md
5. rules/ENV_CONFIG_RULES.md
6. rules/DB_MIGRATION_POLICY.md
7. rules/VALIDATION_CHECKLIST.md
8. rules/SPRINT_INTEGRATION_CHECKLIST.md when preparing merges
9. docs/inbound-workflow.md when touching inbound/import receipt/inventory flow

The `rules/` folder contains operating rules.

The `docs/` folder and task READMEs contain business and implementation context. Use them to understand workflows, schema decisions, and task-specific behavior before changing code.

If files in `rules/` conflict with old README text, follow `rules/`.

If a rule file and module doc appear to conflict on current behavior, stop and ask for clarification before coding or changing project state.
