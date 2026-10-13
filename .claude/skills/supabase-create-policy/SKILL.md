---
name: supabase-create-policy
description: Crear o corregir políticas RLS y grants de una tabla de sous_chef (own|catalogo) en una migración nueva, con tests pgTAP de aislamiento entre usuarios; tabla nueva completa → supabase-create-table.
argument-hint: "[own|catalogo] [tabla]"
allowed-tools: Read, Write, Edit, Glob, Bash(scripts/sc *), Bash(npx supabase *), Bash(ls *)
---

# supabase-create-policy

Crea políticas RLS: `$0` = `own` (por defecto; tabla de usuario con escritura) o `catalogo` (`recetas`/`tecnicas`, solo SELECT); `$1` = tabla. Plantillas SQL y tests: [policy-templates.sql](policy-templates.sql).

## Cuándo usarla
- Faltan o hay que cambiar políticas o grants de una tabla existente de `sous_chef`.

## Cuándo NO usarla
- Tabla nueva con DDL, índices y RLS → `supabase-create-table`.
- Cambios de columnas o datos → `supabase-create-migration`.

## Precondiciones
- `$1` presente; si falta, pedirlo y parar. Stack local en marcha (`supabase-local`).
- Reglas: CLAUDE.md (§ Reglas de trabajo: `user_id` + RLS; `recetas`/`tecnicas` solo SELECT para la app) y ADR-0002.
- Van en una migración nueva `db/migraciones/NNNN_politicas_$1.sql`, nunca editando una aplicada. **Prohibido** `supabase db push` y `apply_migration` del MCP.

## Pasos
1. Ramas por `$0`:

| Valor | Qué crear |
|-------|-----------|
| `own` | `enable row level security`; políticas select, insert y update `to authenticated` con `(select auth.uid()) = user_id` (`with check` en insert y update); sin delete (borrado lógico). Bloque A. |
| `catalogo` | Solo la política select; grant `select` a `authenticated`; escrituras solo por `service_role`/DB URL de `sc publicar`. Bloque B. |

2. Nombrar cada política `<tabla>_<operación>_own`. Prohibido `using (true)`, `to public` o `to anon`, y políticas que mencionen `service_role` (lo omite RLS).
3. Grants coherentes: `revoke all ... from public, anon`; `authenticated` solo con las operaciones permitidas; `service_role` completo.
4. Test `db/tests/NNNN_$1_policies.test.sql` con el bloque C: dos usuarios (A no ve/inserta/actualiza lo de B), `anon` denegado y `policies_are` exacto. Catálogo: además, escribir como `authenticated` falla.
5. Aplicar y probar (ver Validaciones).
6. Pedir revisión de seguridad de migración y test (RLS es crítico).

## Validaciones / tests
- `scripts/sc migrar --local` aplica sin errores.
- `npx supabase test db db/tests` en verde (alternativa en `supabase-test`).
- `policies_are` coincide con las políticas esperadas.

## Definition of Done
- [ ] RLS habilitada y políticas `to authenticated` con `(select auth.uid())`; ninguna permisiva global.
- [ ] Grants mínimos; nada a `anon`.
- [ ] Test de aislamiento entre usuarios y `anon` en verde.
- [ ] Revisión de seguridad hecha.
- [ ] Migración nueva y consecutiva; ninguna previa editada.

## Docs a actualizar
- `docs/arquitectura.md` (modelo de seguridad) · docs/ESTADO.md (skill `session-handoff`)
