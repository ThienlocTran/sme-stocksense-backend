# Sprint Integration Checklist

## Branching

- Do not test directly on `dev`.
- Create `test/sprintX-integration` from latest `dev`.
- Merge feature branches one by one in dependency order.
- Resolve conflicts on the integration branch.
- Do not mix Sprint 2 integration fixes with Sprint 3 feature branches.
- Keep Sprint 3 branches separate until Sprint 2 is stable.
- Merge PRs into `dev` one by one, not all at once.
- After each PR merge, pull latest `dev` before updating the next PR branch.

## Backend Validation

Use this validation when preparing an integration branch or merge:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests clean package
git diff --check
rg -n "\x{00C3}\x{0192}|\x{00C3}\x{201E}|\x{00C3}\x{2020}|\x{00C3}\x{00A1}\x{00C2}\x{00BA}|\x{00C3}\x{00A1}\x{00C2}\x{00BB}|\x{FFFD}" src docs
```

If the full test suite is too heavy, run targeted backend tests first:

```powershell
.\mvnw.cmd "-Dtest=*ImportReceipt*,*Inventory*,*Product*,*Warehouse*,*ExcelImport*" test
.\mvnw.cmd -DskipTests clean package
```

Report clearly when full tests were skipped.

## Migration Validation

If migrations changed:

- Check duplicate versions.
- Confirm no already-applied migration was edited.
- Clean `target/classes`.
- Package.
- Start the app with Flyway enabled only after DB safety is confirmed.

Do not disable Flyway or bypass validation to make integration appear successful.

## Merge Discipline

- Keep each conflict resolution focused on the branch being merged.
- Do not carry opportunistic refactors into an integration branch.
- Keep Sprint 2 stabilization fixes separate from Sprint 3 features.
- After `dev` changes, update dependent branches in order and change stacked PR bases as needed.
