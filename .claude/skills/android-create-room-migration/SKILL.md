---
name: android-create-room-migration
description: Crear una migración Room manual (versión N a N+1) en data/db/Migrations.kt con schema JSON versionado y test Robolectric JVM; no para entidades nuevas completas (android-create-data-layer).
allowed-tools: Read, Write, Edit, Grep, Glob, Bash(./gradlew *), Bash(git status*), Bash(git diff*)
---

# android-create-room-migration

Crea la migración Room de la versión actual a la siguiente, con el schema JSON exportado y versionado en git y un test que la valida.

## Cuándo usarla
- Cambió el esquema del `@Database` (entity nueva, columna, índice, tabla borrada) y hay datos del usuario que no deben perderse.

## Cuándo NO usarla
- Crear entity, DAO y repository desde cero → `android-create-data-layer` (te remite aquí al subir la versión).
- Migraciones del lado Supabase → `supabase-create-migration`.

## Precondiciones
- Conocer la `version` actual del `@Database` y que existe el JSON de esa versión en el directorio de esquemas de `:app` (`exportSchema = true`). Si no existe, parar y avisar: no se reconstruye sin la versión previa.
- Nunca editar ni borrar un schema JSON ya publicado; solo se añade el de la nueva versión.
- Una migración jamás descarta pendientes del outbox ni filas con `deletedAt` (ADR-0004).

## Pasos
1. Comprobar en `app/build.gradle.kts` que `room.schemaLocation` está configurado, el directorio versionado en git y expuesto como assets de test unitario (los tests corren en JVM con Robolectric, no en `androidTest`). Si falta, reutilizar el patrón existente.
2. Aplicar el cambio a las entities y subir `version` a N+1.
3. `./gradlew assembleDebug` para que Room genere el JSON de N+1. Revisar su diff y confirmar que el de N no cambió.
4. Escribir `MIGRATION_N_N1 = object : Migration(N, N+1)` en `data/db/Migrations.kt` con SQL explícito (copiar datos, renombrar, valores por defecto coherentes con `deletedAt` y `editadoEn`). Sin `@AutoMigration` salvo cambio trivial acordado.
5. Registrarla en `addMigrations(...)` del builder. Prohibido `fallbackToDestructiveMigration`.
6. Test en `src/test` (JUnit4 + Robolectric + `MigrationTestHelper` con los schemas JSON): `createDatabase(nombre, N)`, insertar filas representativas (con soft delete y con fila de outbox), cerrar, `runMigrationsAndValidate(nombre, N+1, true, MIGRATION_N_N1)` y comprobar los datos con SQL.
7. Si hay varias versiones, test de la cadena desde la más antigua soportada hasta la actual.
8. Si el campo nuevo viaja al servidor, coordinar con `supabase-create-migration` (mismo nombre y tipo) y con el DTO del `SyncWorker`.

## Validaciones / tests
- `./gradlew testDebugUnitTest` (incluye el test de migración).
- `./gradlew ktlintCheck detekt lintDebug assembleDebug`.
- `git status`: JSON de N+1 nuevo y ningún schema anterior modificado.

## Definition of Done
- [ ] `version` incrementada, schema N+1 en git; el de N intacto.
- [ ] Migración manual registrada; sin `fallbackToDestructiveMigration`.
- [ ] Test de migración en verde con datos previos conservados (soft deletes y outbox incluidos).
- [ ] `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug` en verde.

## Docs a actualizar
- `docs/arquitectura.md` (modelo local) · docs/ESTADO.md (skill `session-handoff`)
