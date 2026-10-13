---
name: test-write
description: Escribir tests por tipo (unit, domain, integration, android-ui, db, cli) con TDD en lógica crítica; no ejecuta QA (qa) ni escenarios en dispositivo (qa-scenario).
argument-hint: "[unit|domain|integration|android-ui|db|cli]"
allowed-tools: Read, Grep, Glob, Write, Edit, Bash(./gradlew *), Bash(scripts/sc *), Bash(npx supabase *)
---

# test-write

Escribe tests del tipo `$ARGUMENTS` para el código indicado. Sin argumento, deducirlo del archivo a probar (`receta/` → `domain`, `app/` → `unit`/`android-ui`, `.sql` → `db`, `tools/` → `cli`).

## Cuándo usarla
- Añadir cobertura a código nuevo o existente, o fijar un bug con un test de regresión antes de arreglarlo.
- TDD obligatorio (CLAUDE.md) en lógica crítica: validador, escalado, motor del cronómetro, sync, migraciones/RLS. Orden: test rojo, código mínimo, verde, refactor.

## Cuándo NO usarla
- Solo ejecutar la validación global → usa `qa`.
- Probar muerte del proceso, reinicio, Doze o actualización del APK en dispositivo → usa `qa-scenario`.
- Test intermitente → usa `test-debug-flaky`. Crear el doble reutilizable → `test-create-double`.

## Precondiciones
- Conocer el comportamiento esperado (criterio de aceptación, ADR o fase activa); si es ambiguo, preguntar antes de fijarlo en un test.
- Librería de test nueva → skill `dependency-add`, no instalar a mano.
- Para `db`: Docker en marcha y `npx supabase start`.

## Pasos
1. Resolver el tipo desde `$ARGUMENTS`; localizar el test vecino más parecido y copiar su estilo.
2. Lógica crítica: escribir primero el test, ejecutarlo y comprobar que falla por la razón correcta; solo entonces implementar.
3. Escribir el test según la tabla.
4. Reglas comunes: nombre que describe el comportamiento; Arrange-Act-Assert; un motivo de fallo por test; deterministas (sin reloj, red ni azar reales: reloj e ids inyectados, ver `test-create-double`).
5. Mutar mentalmente el código: si cambiar una condición no rompe ningún test, falta una aserción.
6. Ejecutar el comando del tipo y luego el gate qa fast.

| Tipo | Dónde y con qué | Comando |
|---|---|---|
| domain | `receta/src/test`: validador, escalado, gantt, motor del cronómetro; casos válidos, inválidos y bordes (2 raciones, 3 fuegos, fuego 0-9) | `./gradlew :receta:test` |
| unit | `app/src/test`: JUnit; Flow con Turbine; corrutinas con dispatcher de test; ViewModels con fakes | `./gradlew testDebugUnitTest` |
| integration | Room en memoria + DAO real; repositorios con fake del backend; WorkManager con `TestListenableWorkerBuilder` | `./gradlew testDebugUnitTest` |
| android-ui | Compose UI test (`createComposeRule`), semántica antes que `testTag`; estados loading/vacío/error, tema oscuro y rotación | `./gradlew connectedDebugAndroidTest` (emulador) |
| db | pgTAP en `supabase/tests/` (`begin; plan(n); … finish(); rollback;`); RLS con dos usuarios y `anon`; `recetas`/`tecnicas` solo SELECT | `npx supabase test db` |
| cli | `tools/src/test`: subcomandos de `sc` con directorio temporal y datos de `data/` mínimos | `./gradlew :tools:test` |

## Validaciones / tests
- El test nuevo falla sin el cambio (rojo verificado) y pasa con él.
- Tres ejecuciones seguidas con el mismo resultado (sin flakiness).
- Gate qa fast en verde; en cambios de BD o Android, también qa full.

## Definition of Done
- [ ] Test del tipo correcto, en la ruta convencional y con estilo del vecino.
- [ ] Casos: camino feliz, bordes (vacío, nulo, límites) y error; en seguridad, acceso denegado.
- [ ] Ningún test desactivado (`@Ignore`, `assumeTrue` espurio); sin secretos en fixtures.
- [ ] En lógica crítica, evidencia de que el test falló antes de implementar.
- [ ] Gate qa fast en verde.

## Docs a actualizar
- `docs/testing.md` si existe y cambia la estrategia · docs/ESTADO.md (skill `session-handoff`)
