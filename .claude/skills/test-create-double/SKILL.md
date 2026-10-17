---
name: test-create-double
description: Crear dobles deterministas reutilizables (fixture, mock o fake) sin azar ni tiempo real; no para escribir los tests (test-write) ni para tests flaky (test-debug-flaky).
argument-hint: "[fixture|mock|fake]"
allowed-tools: Read, Grep, Glob, Write, Edit, Bash(./gradlew *)
---

# test-create-double

Crea el doble `$ARGUMENTS` (por defecto `fake` si falta) en el código de test compartido del módulo: `receta/src/testFixtures` o `src/test` (dominio) o el directorio de test compartido de `app/`.

## Cuándo usarla
- Varios tests necesitan el mismo dato, repositorio, reloj, generador de ids o programador de alarmas.
- Un test usa `Instant.now()`, `System.currentTimeMillis()`, `Random` o red real y hay que sustituirlo.

## Cuándo NO usarla
- Escribir el test que consume el doble → usa `test-write`.
- Un test intermitente → usa `test-debug-flaky`.
- Dependencias nuevas de test (MockK, Turbine, etc.) → skill `dependency-add`.

## Precondiciones
- Identificar la interfaz/puerto a sustituir y leer su definición real (no inventar firmas).
- Mirar antes lo existente en los directorios de test de `receta/` y `app/`. Reutilizar, no duplicar.

## Pasos
1. Elegir el tipo con `$ARGUMENTS`:

| Valor | Cuándo | Cómo |
|---|---|---|
| `fixture` | Datos de ejemplo (receta, plan, recipiente, producto de despensa) | Función `make<Entidad>(overrides)` con valores constantes; fechas fijas, nunca `now()` |
| `fake` | Colaborador con comportamiento (preferido para repositorios) | Implementación en memoria de la interfaz real; estado inspeccionable; reloj e ids inyectados |
| `mock` | Solo en bordes sin lógica (red, SDK, sistema Android) | MockK; verificar la llamada de salida, no detalles internos |

2. Reloj e ids: nunca leer tiempo ni azar reales. Inyectar un `Clock` fijo/avanzable y un generador de ids determinista.
3. Fakes específicos:
   - Reloj/cronómetro: `Clock` avanzable a mano para probar el motor del cronómetro sin esperas.
   - Programador de alarmas: registra las alarmas pedidas (hora, recipiente) y permite "disparar" o "perder" alarmas para simular reinicio o muerte del proceso.
   - Repositorio: mapa en memoria con las mismas reglas de `user_id` que el real.
4. Ubicación: dominio puro en `receta/`; lo que dependa de Android en el directorio de test compartido de `app/`, no en `src/test` de una feature.
5. Añadir un test mínimo del propio doble (respeta el contrato de la interfaz) y usarlo en al menos un test real.

## Validaciones / tests
- `./gradlew :receta:test testDebugUnitTest`.
- Determinismo: ejecutar dos veces seguidas y comprobar el mismo resultado.

## Definition of Done
- [ ] Doble sin azar, reloj real, `Thread.sleep` ni red.
- [ ] Mocks solo en bordes; repositorios, reloj e ids como fakes.
- [ ] Ubicado en el módulo de test compartido y usado por un test real.
- [ ] Sin dependencia nueva sin `dependency-add`.
- [ ] Gate qa fast en verde.

## Docs a actualizar
- `docs/testing.md` si existe · docs/ESTADO.md (skill `session-handoff`)
