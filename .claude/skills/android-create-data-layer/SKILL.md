---
name: android-create-data-layer
description: Crear la capa de datos Android de una entidad de Sous Chef (entity Room, DAO, repository, mapper, outbox de sync y tests JVM); no para migrar un esquema (android-create-room-migration).
argument-hint: "[nombre_entidad]"
allowed-tools: Read, Write, Edit, Grep, Glob, Bash(./gradlew *)
---

# android-create-data-layer

Crea la capa de datos de `$ARGUMENTS` en `:app`: entity, DAO, repository, mapper, outbox y tests. Sync: ADR-0004.

## Cuándo usarla
- Entidad nueva que se persiste en Room y que la UI o el `SyncWorker` consumen vía repository.

## Cuándo NO usarla
- Cambiar columnas/tablas de una entidad ya publicada → `android-create-room-migration`.
- Pantalla o ViewModel → `android-create-feature`.
- Recetas y técnicas: la app solo las lee (se publican desde el repo con `sc publicar`); aquí solo van tablas de usuario (despensa, compra, historial, estado del cronómetro).

## Precondiciones
- `$ARGUMENTS` presente (singular, PascalCase; tabla en snake_case). Si falta, pedirlo y parar.
- Tabla homóloga en `sous_chef` con `user_id` y RLS (`supabase-create-table`); alinear nombres y tipos.
- Glob de una entidad existente para seguir paquetes y estilo. Sin dependencias nuevas sin `dependency-add`.

## Pasos
1. Modelo `<Entidad>` y `interface <Entidad>Repository` en `domain/` (Kotlin puro, sin Room ni Android): `Flow` para lecturas, `suspend` para escrituras.
2. `<Entidad>Entity` en `data/db/`: `id: String` (UUID generado en cliente), `editadoEn` (instante del cambio en el cliente, lo que usa el servidor para ignorar updates viejos), `updatedAt` (lo pone el servidor; cursor de pull), `deletedAt` (soft delete). Índices explícitos.
3. `<Entidad>Dao`: consultas `Flow` con `deletedAt IS NULL`; `@Upsert`; multi-paso en `@Transaction`. Sin borrado físico desde la app.
4. **Outbox**: tabla/entidad de cola por entidad (id de fila, operación upsert, `editadoEn`, intentos). Contrato: la **escritura de la entidad y el alta en el outbox van en la misma transacción Room** (`@Transaction` o `withTransaction`); jamás una sin la otra.
5. Registrar entity y outbox en el `@Database` con `exportSchema = true`, subir `version` y ejecutar `android-create-room-migration`.
6. `<Entidad>Mapper.kt`: extensiones `toDomain()`/`toEntity()` puras, sin reloj ni UUID dentro.
7. `<Entidad>RepositoryImpl` (`@Inject`, DAO + mapper + dispatcher + reloj inyectados): cada escritura fija `editadoEn` y encola en la misma transacción. El pull del `SyncWorker` fusiona por `updated_at` sin reencolar.
8. Binding `@Binds` y `@Provides` del DAO en `data/di/*Module.kt` (`android-create-hilt-module`).
9. Tests JVM (JUnit4, Turbine, Robolectric con Room in-memory, fakes a mano): mapper ida y vuelta; DAO (upsert, `Flow`, soft delete oculto); repository (UUID válido, `editadoEn`, fila y outbox atómicos: si falla uno, ninguno).

## Validaciones / tests
- `./gradlew testDebugUnitTest` (incluye Room con Robolectric).
- `./gradlew ktlintCheck detekt lintDebug assembleDebug`.

## Definition of Done
- [ ] Interfaz del repository en `domain/` sin dependencias de Room ni Android.
- [ ] UUID de cliente, `editadoEn`, `updatedAt` y `deletedAt` presentes; sin borrado físico.
- [ ] Escritura + outbox en la misma transacción, con test que lo demuestra.
- [ ] Esquema JSON exportado y migración manual si subió la versión.
- [ ] Binding Hilt registrado; mapper, DAO y repository con tests en verde.
- [ ] `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug` en verde.

## Docs a actualizar
- `docs/arquitectura.md` (modelo local y sync) · docs/ESTADO.md (skill `session-handoff`)
