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

Spring Boot does not automatically load `.env` by itself unless the IDE/plugin/run configuration exports those values as environment variables.

Preferred safe options:

Option A - Local ignored application-neon.yml:
1. Copy src/main/resources/application-neon.yml.example to src/main/resources/application-neon.yml.
2. Replace placeholders with real local credentials in application-neon.yml.
3. Never commit application-neon.yml.

Option B - Environment variables:
1. Copy .env.example to .env for reference.
2. Configure IntelliJ Run Configuration, EnvFile plugin, terminal environment, or OS environment variables to export DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, JWT_EXPIRATION_SECONDS, and FLYWAY_ENABLED.
3. Keep application-neon.yml using placeholders.
4. Never commit .env.

## Secret handling

- Do not paste real DB passwords or JWT secrets into committed docs.
- Do not screenshot secrets in PRs/issues.
- If a secret is exposed, rotate it.
- If a secret file was tracked, remove it from Git index with git rm --cached, not by deleting the local file unless the user confirms.

## Flyway setting

- FLYWAY_ENABLED should normally be true.
- Do not use spring.flyway.enabled=false to hide migration errors.
