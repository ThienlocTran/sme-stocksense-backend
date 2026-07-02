# Validation Checklist

## Always Run

```powershell
git diff --check
rg -n "Ãƒ|Ã„|Ã†|Ã¡Âº|Ã¡Â»|ï¿½" src docs
```

For docs-only changes that touch `AGENTS.md` or `rules/`, include those paths in the mojibake scan:

```powershell
rg -n "Ãƒ|Ã„|Ã†|Ã¡Âº|Ã¡Â»|ï¿½" AGENTS.md rules docs README.md README_backend.md
```

When scanning files that document this exact command, matches on the command example itself are expected. Any other matches must be treated as mojibake and fixed.

## Java Code Changed

Run targeted tests:

```powershell
.\mvnw.cmd "-Dtest=<targeted-tests>" test
```

Choose tests that cover the changed controller, service, repository, entity, or security behavior.

## Shared Or Risky Files Changed

If `pom.xml`, security config, entity, migration, or shared config changed:

```powershell
.\mvnw.cmd -DskipTests clean package
```

Do not edit protected files or run this command without confirmation when the task rules require confirmation.

## Flyway Migration Changed

- Check duplicate versions.
- Confirm no already-applied migration was edited.
- Clean `target/classes`.
- Package.
- Start the app with Flyway enabled only after DB safety is confirmed.

## Reporting

- Do not claim DONE if tests/build were skipped.
- Report exactly what was not tested.
- Report `git diff --check` result.
- Report mojibake scan result.
- Report package/build result, or state that package/build was intentionally skipped.
