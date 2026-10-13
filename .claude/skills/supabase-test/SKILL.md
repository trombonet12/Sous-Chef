---
name: supabase-test
description: Escribir y ejecutar tests pgTAP de la BD de Sous Chef (migration|rls|function) en db/tests/*.test.sql contra el stack local; no para tests de Android ni para crear tablas (supabase-create-table).
argument-hint: "[migration|rls|function]"
allowed-tools: Read, Write, Edit, Glob, Bash(scripts/sc *), Bash(npx supabase *), Bash(docker exec *)
---

# supabase-test

Crea o ejecuta tests pgTAP en `db/tests/` según `$ARGUMENTS` (por defecto `migration`).

## Cuándo usarla
- Una migración, política RLS o función SQL nueva o modificada necesita su test.
- Reproducir un fallo de BD con un test rojo antes de corregirlo (RLS y migraciones son lógica crítica: TDD).

## Cuándo NO usarla
- Crear la tabla con su test inicial → `supabase-create-table`.
- Tests de Android o `:receta` → `android-run-tests`.
- Revisión sin escribir tests → `supabase-review`.

## Precondiciones
- Stack local en marcha y esquema aplicado: `npx supabase start > /dev/null 2>&1` y `scripts/sc migrar --local` (`supabase-local`).
- Un test = un archivo `db/tests/<NNNN>_<nombre>.test.sql` con `begin; ... rollback;`.

## Pasos
1. Cabecera: `begin;`, `create extension if not exists pgtap with schema extensions;`, `select plan(N);` ... `select * from finish();`, `rollback;`. Modelo: bloque B de `.claude/skills/supabase-create-table/table-template.sql`.
2. Rama por `$ARGUMENTS`:

| Valor | Qué probar | Funciones pgTAP |
|---|---|---|
| `migration` | tabla, columnas, índices, triggers, constraints, grants | `has_table`, `has_column`, `col_not_null`, `has_index`, `has_trigger`, `col_type_is`, `has_table_privilege` |
| `rls` | RLS activa, `policies_are`, aislamiento con dos usuarios y `anon` | `relrowsecurity`, `policies_are`, `is`, `is_empty`, `throws_ok` |
| `function` | resultado, errores, permisos, `security invoker/definer` | `has_function`, `function_returns`, `results_eq`, `throws_ok`, `lives_ok` |

3. `rls`: insertar dos usuarios en `auth.users` como postgres; `set local role authenticated` + `set_config('request.jwt.claims', '{"sub":"<uuid>","role":"authenticated"}', true)`; A inserta y lee lo suyo; A no inserta con `user_id` de B (`42501`); B no ve ni actualiza lo de A; `reset role; set local role anon` falla con `42501`. En tablas con LWW, probar que un `editado_en` más viejo se ignora.
4. `function`: ejecutar como `authenticated` con claims de dos usuarios; si es `security definer`, probar que no filtra datos ajenos y que `search_path` está fijado.
5. Primero el test (rojo), luego la migración (verde).
6. Ejecutar todo: `npx supabase test db db/tests`. Un archivo: `npx supabase test db db/tests/<NNNN>_<nombre>.test.sql`.
7. Si la CLI no admite la ruta o no encuentra pgTAP: `docker exec -i supabase_db_sous-chef psql -U postgres -d postgres -f - < db/tests/<archivo>` (el nombre del contenedor es `supabase_db_<project_id>`) y leer las líneas `ok`/`not ok`.

## Validaciones / tests
- Todos los tests en verde; `plan(N)` coincide con los asserts.
- Cada archivo termina en `rollback` y no depende del orden.
- Un test de RLS debe fallar si se elimina o relaja una política.

## Definition of Done
- [ ] Test creado en `db/tests/` con numeración consecutiva.
- [ ] RLS probada con dos usuarios y `anon` (si `rls`).
- [ ] Suite completa en verde sin desactivar otros tests.
- [ ] Ninguna clave impresa.

## Docs a actualizar
- `docs/herramientas.md` si cambia la forma de ejecutar los tests · docs/ESTADO.md (skill `session-handoff`)
