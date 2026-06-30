# Git Workflow Rules

## Branch Rules

- Never commit directly to `dev`, `main`, or `master`.
- Use feature, fix, chore, or test branches only.
- Do not push, merge, rebase, amend, or force-push unless explicitly asked.
- Do not use `git add .`.
- Add explicit files only.
- Do not commit `.env`, `.idea`, `target`, `*.iml`, `application-neon.yml`, local files, private files, or generated files.
- If there are staged or unstaged source, migration, or code changes before a docs/rules task, stop and report.
- If a task branch is completed and the next task depends on it, push the completed branch before creating the next stacked branch.

## Switch Commands

```text
git switch <branch> = switch to existing branch
git switch -c <new-branch> = create new branch from current HEAD
```

Examples:

```powershell
git switch feature/T158-excel-import-validation
git push -u origin feature/T158-excel-import-validation
git switch -c feature/T159-excel-import-error-persistence
```

## Stacked PR Guidance

- PR T158 -> T157
- PR T159 -> T158
- After the base branch merges into `dev`, rebase/update the next branch and change the PR base.
- Keep dependency order clear in PR descriptions.
- Do not mix Sprint 2 integration fixes with Sprint 3 feature work.

## Commit Safety

- Check branch and status before committing.
- Commit only the requested scope.
- Use explicit `git add <file>` commands.
- Review `git diff --stat` and `git diff --check` before commit.
- Do not push unless the user explicitly asks.
