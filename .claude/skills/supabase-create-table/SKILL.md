---
name: supabase-create-table
description: Crear una tabla nueva en el esquema sous_chef (user_id, RLS, updated_at, editado_en con LWW, soft delete, grants) como migración en db/migraciones más su test pgTAP; no para alterar tablas.
argument-hint: "[nombre_tabla]"
allowed-tools: Read, Write, Edit, Glob, Bash(scripts/sc *), Bash(npx supabase *), Bash(ls *)
---

# supabase-create-table

Crea la tabla `$ARGUMENTS` con migración, RLS y test pgTAP. Plantilla SQL completa (migración, variante catálogo y test): [table-template.sql](table-template.sql).

## Cuándo usarla
- Hace falta una tabla nueva con datos propiedad del usuario (`user_id`), o una de catálogo de solo lectura (`recetas`, `tecnicas`).

## Cuándo NO usarla
- Alterar una tabla existente → `supabase-create-migration`.
- Solo añadir o cambiar políticas → `supabase-create-policy`.

## Precondiciones
- `$ARGUMENTS` en snake_case, sin prefijo de esquema. Si falta, pedirlo y parar.
- Proyecto Supabase compartido con Trombito: todo en `sous_chef` (ADR-0002). **Prohibido** `supabase db push`, `migration new` en `supabase/migrations/` y `apply_migration` del MCP.
- Stack local en marcha (`supabase-local`). Decidir si es tabla de usuario con escritura de la app (sync) o de catálogo (solo SELECT).

## Pasos
1. Número siguiente: `ls db/migraciones`; crear `db/migraciones/NNNN_crear_$ARGUMENTS.sql` con el bloque A (o A2 si es catálogo) de la plantilla, sustituyendo `<tabla>` y añadiendo columnas de negocio.
2. Comprobar el DDL: `user_id uuid not null default auth.uid() references auth.users on delete cascade`; `editado_en` (cliente), `updated_at` (servidor, trigger con `clock_timestamp()`) y `deleted_at`; trigger que ignora updates con `editado_en` más viejo; índice en `user_id` y `(user_id, updated_at)`; `enable row level security`.
3. Políticas con `(select auth.uid()) = user_id`: select, insert y update `to authenticated` (sin delete: borrado lógico). Catálogo: solo select.
4. Grants: `revoke all ... from public, anon`; `grant select, insert, update` (catálogo: `select`) a `authenticated`; `grant all` a `service_role`. Los `alter default privileges` del esquema van en la primera migración, no aquí.
5. Test `db/tests/NNNN_$ARGUMENTS.test.sql` con el bloque B; ajustar los insert a las columnas `not null` y el `plan(N)`.
6. Aplicar y probar (ver Validaciones). Los ids los genera el cliente; el default solo sirve para pruebas.
7. Si la tabla viaja a Android, seguir con `android-create-data-layer` (mismos nombres y tipos).
8. Pedir revisión de seguridad de migración y test (RLS es crítico).

## Validaciones / tests
- `scripts/sc migrar --local` aplica la migración sin errores.
- `npx supabase test db db/tests` en verde (alternativa en `supabase-test`).
- El test cubre: `has_table`, RLS activa, `policies_are`, índice, LWW, aislamiento entre dos usuarios, `anon` denegado.

## Definition of Done
- [ ] Migración `db/migraciones/NNNN_crear_*.sql` consecutiva, en `sous_chef`, rollback comentado.
- [ ] RLS activa; ninguna política `using (true)`; nada concedido a `anon`.
- [ ] Test pgTAP en verde con aislamiento de usuarios.
- [ ] Revisión de seguridad hecha.
- [ ] `scripts/sc migrar --local` limpio.

## Docs a actualizar
- `docs/arquitectura.md` (modelo de datos) · docs/ESTADO.md (skill `session-handoff`)
