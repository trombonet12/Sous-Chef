---
name: supabase-review
description: Revisar el backend Supabase de Sous Chef (esquema sous_chef, db/migraciones, db/tests) sin modificarlo, con findings por severidad; argumento security|performance|indexes|explain.
argument-hint: "[security|performance|indexes|explain]"
allowed-tools: Read, Grep, Glob, Bash(scripts/sc *), Bash(npx supabase *), Bash(docker exec *)
---

# supabase-review

Revisión de solo lectura del backend según `$ARGUMENTS` (por defecto `security`). Nunca edita migraciones, tests ni configuración: solo informa.

## Cuándo usarla
- Antes de fusionar migraciones, políticas o funciones SQL.
- Auditoría periódica de RLS, grants, índices o consultas críticas.

## Cuándo NO usarla
- Crear tabla, migración o política → `supabase-create-table`, `supabase-create-migration`, `supabase-create-policy`.
- Revisar Android → `android-review`.
- Seguridad transversal (secretos, auth en cliente) → `security-review`.

## Precondiciones
- Reglas: CLAUDE.md (toda tabla de usuario con `user_id` y RLS; `recetas`/`tecnicas` solo SELECT) y ADR-0002 (esquema `sous_chef` en proyecto compartido).
- Alcance: `db/migraciones/**`, `db/tests/**`, `supabase/config.toml`. `explain` requiere stack local (`supabase-local`).
- Nunca mostrar valores de secretos; citar solo el nombre.

## Pasos
1. `Glob db/migraciones/*.sql`; si hay diff, limitar a lo cambiado. Comprobar numeración consecutiva y que no hay archivos en `supabase/migrations/`.
2. Ejecutar la rama de `$ARGUMENTS`; si el valor no está en la tabla, avisar y usar `security`.
3. Findings `SEVERIDAD | archivo:línea | problema | corrección propuesta` (CRITICAL, HIGH, MEDIUM, LOW).
4. Resumen: nº por severidad y ramas no comprobables.
5. Cualquier fallo de RLS o grants: recomendar revisión de seguridad con los findings.

### Ramas por argumento
| Valor | Qué comprobar |
|---|---|
| `security` | 1) Toda tabla de `sous_chef` con `user_id` tiene `enable row level security`. 2) Políticas `to authenticated` con `(select auth.uid()) = user_id`; insert y update con `with check`; `recetas`/`tecnicas` solo select. 3) Ninguna `using (true)`/`with check (true)`. 4) `security definer` justificada, con `set search_path = ''`. 5) Grants: nada a `anon`; `authenticated` sin más operaciones de las necesarias; `alter default privileges` presentes. 6) Nada creado en `public` ni tocado de Trombito. 7) Sin secretos en migraciones ni `config.toml`. 8) Ninguna migración aplicada editada (checksum de `_migraciones`). |
| `performance` | 1) `(select auth.uid())`, no `auth.uid()` directo (MEDIUM). 2) Sin funciones volátiles ni subconsultas correlacionadas en políticas. 3) Triggers sin trabajo por fila evitable (`clock_timestamp()` ok). 4) Consultas del cliente (`app/`, `tools/`) por columnas sin índice o sin límite. |
| `indexes` | 1) Índice en `user_id` de toda tabla de usuario (HIGH si falta). 2) Índice en cada FK. 3) `(user_id, updated_at)` para el pull incremental. 4) Duplicados o redundantes. |
| `explain` | 1) Elegir 3-5 consultas críticas (pull por `updated_at`, despensa, historial). 2) Con datos de prueba, `docker exec -i supabase_db_sous-chef psql -U postgres -d postgres` dentro de `begin; ... rollback;`, rol `authenticated` y `request.jwt.claims` de un usuario de prueba: `explain (analyze, buffers) <consulta>`. 3) Marcar `Seq Scan` en tablas grandes, estimaciones muy erróneas y `SubPlan` por fila. |

## Validaciones / tests
- `npx supabase test db db/tests` en verde da contexto; si falla, finding HIGH.
- Cada finding cita `archivo:línea` real (`Grep -n`) y una corrección concreta.
- `explain`: solo el fragmento relevante del plan, sin datos personales.

## Definition of Done
- [ ] Rama de `$ARGUMENTS` completada (o motivo documentado).
- [ ] Findings en el formato indicado, ordenados por severidad.
- [ ] Ningún archivo modificado.
- [ ] Sin valores de secretos en la salida.
- [ ] Escalado a revisión de seguridad si hubo hallazgos de RLS o grants.

## Docs a actualizar
- Excepción aprobada (p. ej. `security definer`) en un ADR (`create-adr`) · docs/ESTADO.md (skill `session-handoff`)
