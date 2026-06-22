@echo off
set "URL=jdbc:postgresql://ep-young-field-aom1b92m-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channelBinding=require"
"%~dp0mvnw.cmd" -q "-Dflyway.url=%URL%" -Dflyway.user=neondb_owner -Dflyway.password=npg_1abRrCHGiT5s flyway:repair
