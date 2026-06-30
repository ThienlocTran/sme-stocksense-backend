# DB Migration Policy

This is the most important rule file for the backend.

## Strict Flyway Rules

- Flyway versioned migrations are immutable after being applied to any shared DB.
- Never edit an already-applied migration.
- Never create duplicate migration versions.
- New schema change = new migration with the next unused version.
- Do not disable Flyway to make the app run.
- Do not commit `spring.flyway.enabled=false`.
- Do not hide Flyway errors by changing app code.
- Do not bypass Flyway validation to make tests, startup, or packaging appear healthy.

## Stop Conditions

STOP immediately and report if:

- duplicate migration version appears
- checksum mismatch appears
- migration exists in `flyway_schema_history` but local file content differs
- a task requires `flyway repair`
- a task requires `flyway clean`
- a task requires editing `flyway_schema_history`

## Forbidden Without Explicit Lead Confirmation

- `flyway repair`
- `flyway clean`
- `drop table`
- `truncate`
- `delete/update flyway_schema_history`
- manual DB data mutation

## Duplicate Migration Handling

When duplicate migration versions appear:

1. Identify duplicate versions.
2. Keep the migration that was already applied.
3. Rename the not-yet-applied migration to the next unused version.
4. Clean `target/classes` after rename.
5. Validate with Flyway enabled.

Do not rename an already-applied migration unless the lead explicitly confirms the shared DB state and source state.

## Checksum Mismatch Handling

- Do not repair automatically.
- Inspect `flyway_schema_history` read-only.
- Use Git history to restore already-applied migration content.
- If the current file contains legitimate new schema changes, move them into a new migration version.
- Only consider repair after the lead confirms DB state and source state are intentionally aligned.

## Root Cause Rule

If startup fails with downstream bean errors, inspect the deepest cause.

Example:

```text
JwtAuthenticationFilter failed
-> EmployeeRepository failed
-> EntityManagerFactory failed
-> flywayInitializer failed
-> real root cause is Flyway
```

Fix the root cause. Do not patch downstream symptoms.

## Migration Review Checklist

- Check existing migration versions before creating a new migration.
- Confirm the target version number is unused locally and on the shared branch.
- Do not modify old migrations to add new constraints, columns, indexes, enum values, or seed data.
- Keep migration names descriptive and stable.
- If migrations changed, validate with Flyway enabled after DB safety is confirmed.
