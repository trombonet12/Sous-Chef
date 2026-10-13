---
name: android-create-worker
description: Crear un Worker de WorkManager (CoroutineWorker + Hilt) con constraints, backoff, idempotencia y test, p. ej. el SyncWorker; no para alarmas del cronómetro (android-create-system-component).
allowed-tools: Read, Write, Edit, Grep, Glob, Bash(./gradlew *)
---

# android-create-worker

Crea un `CoroutineWorker` inyectado con Hilt, su encolado único y su test con `work-testing`. Caso principal: `SyncWorker` que empuja los upserts del outbox y tira por `updated_at` (ADR-0004).

## Cuándo usarla
- Trabajo diferible que debe sobrevivir al cierre del proceso y reintentarse (sync, limpieza).

## Cuándo NO usarla
- Avisos a una hora exacta del cronómetro → `android-create-system-component` (`setAlarmClock`); WorkManager no garantiza la hora.
- Lógica que solo vive con la pantalla abierta → corrutina en el ViewModel (`android-create-feature`).
- Añadir `hilt-work` o `work-testing` al catálogo → `dependency-add`.

## Precondiciones
- Existe el caso de uso o interfaz de dominio que el worker invoca; el worker no contiene lógica de negocio.
- WorkManager y Hilt están en `gradle/libs.versions.toml`. Si faltan `androidx.hilt:hilt-work` o `androidx.work:work-testing`, pedir `dependency-add` y parar.
- Definir antes: nombre único, restricciones (red), puntual o periódico, política de reintentos.

## Pasos
1. `XxxWorker : CoroutineWorker` con `@HiltWorker` y `@AssistedInject constructor(@Assisted ctx, @Assisted params, <caso de uso>)`.
2. `doWork()` delega y mapea: éxito → `Result.success()`; fallo transitorio (red, 5xx) → `Result.retry()` con tope por `runAttemptCount`; permanente (validación, 4xx) → `Result.failure()` sin datos sensibles.
3. Idempotencia: puede ejecutarse varias veces. Los upserts del outbox llevan el id de la fila y `editadoEn`; marcar enviado en la misma transacción Room y tratar "ya aplicado" como éxito.
4. Fábrica Hilt: `Application` implementa `Configuration.Provider` con `HiltWorkerFactory` y el inicializador por defecto se elimina del manifest; si ya existe, no duplicar.
5. Encolado desde un `Scheduler` en `data/` (interfaz en dominio): `Constraints` con `NetworkType.CONNECTED` y solo lo necesario; `setBackoffPolicy(BackoffPolicy.EXPONENTIAL, ...)`; `enqueueUniqueWork(nombre, ExistingWorkPolicy.KEEP|REPLACE, ...)` o `enqueueUniquePeriodicWork`. Nunca `enqueue` suelto para trabajo repetible.
6. Test JVM con `TestListenableWorkerBuilder<XxxWorker>` y `WorkerFactory` de test con fake del caso de uso: éxito, `retry`, `failure` y segunda ejecución idempotente.
7. Test con `WorkManagerTestInitHelper`/`TestDriver` (Robolectric): `setAllConstraintsMet` y que el encolado único no duplica.

## Validaciones / tests
- `./gradlew testDebugUnitTest`.
- `./gradlew ktlintCheck detekt lintDebug assembleDebug`.
- Manifest sin el inicializador por defecto de WorkManager si se usa `Configuration.Provider`.

## Definition of Done
- [ ] Worker `@HiltWorker` sin lógica de negocio; delega en dominio.
- [ ] Constraints mínimas, backoff exponencial y trabajo único declarados.
- [ ] Idempotencia demostrada con test (doble ejecución sin duplicados).
- [ ] Tests de éxito, retry y failure en verde.
- [ ] Nada sensible (tokens, contenido del usuario) en logs ni en `outputData`.
- [ ] `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug` en verde.

## Docs a actualizar
- `docs/arquitectura.md` si cambia el flujo de sync · docs/ESTADO.md (skill `session-handoff`)
