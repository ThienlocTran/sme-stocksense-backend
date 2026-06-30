# Environment And Config Rules

## Source-controlled files

These files may be committed:

- src/main/resources/application.yml
- src/main/resources/application-neon.yml.example
- .env.example

These files must not contain real secrets.

## Local-only files

These files are local-only and must not be committed:

- .env
- .env.*
- src/main/resources/application-neon.yml
- src/main/resources/application-local.yml
- src/main/resources/application-prod.yml
- src/main/resources/application-production.yml
- src/main/resources/application-*.local.yml

## application.yml

application.yml is the base shared Spring config.

Rules:
- Commit application.yml.
- Do not put DB password, real DB URL, JWT secret, production credential, or personal local config in application.yml.
- Use placeholders or profile-specific examples for environment-specific values.

## Neon local setup

Preferred local setup:

1. Copy .env.example to .env.
2. Copy src/main/resources/application-neon.yml.example to src/main/resources/application-neon.yml.
3. Fill real credentials only in local ignored files.
4. Run with profile neon.
5. Never commit .env or application-neon.yml.

Alternative setup:
- Configure DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, JWT_EXPIRATION_SECONDS, and FLYWAY_ENABLED in the IDE Run Configuration environment variables.
- Keep application-neon.yml using placeholders.

## Secret handling

- Do not paste real DB passwords or JWT secrets into committed docs.
- Do not screenshot secrets in PRs/issues.
- If a secret is exposed, rotate it.
- If a secret file was tracked, remove it from Git index with git rm --cached, not by deleting the local file unless the user confirms.

## Flyway setting

- FLYWAY_ENABLED should normally be true.
- Do not use spring.flyway.enabled=false to hide migration errors.
