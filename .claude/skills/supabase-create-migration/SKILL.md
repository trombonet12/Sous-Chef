---
name: supabase-create-migration
description: Crear una migración SQL en db/migraciones/NNNN_nombre.sql (esquema sous_chef) con rollback documentado y test pgTAP, aplicada con sc migrar; tablas nuevas → supabase-create-table.
argument-hint: "[nombre_migracion]"
allowed-tools: Read, Write, Edit, Glob, Bash(scripts/sc *), Bash(npx supabase *), Bash(ls *)
---

# supabase-create-migration

Crea la migración `$ARGUMENTS` sobre objetos ya existentes del esquema `sous_chef`, reproducible y con rollback escrito en el propio archivo. Contexto: ADR-0002 (proyecto compartido con Trombito).

## Cuándo usarla
- Añadir/alterar columnas, índices, constraints, funciones, triggers o hacer backfill en `sous_chef`.

## Cuándo NO usarla
- Tabla de usuario nueva → `supabase-create-table`.
- Solo políticas RLS o grants → `supabase-create-policy`.
- Levantar o resetear la base local → `supabase-local`.

## Precondiciones
- `$ARGUMENTS` en snake_case, verbo + objeto (`anadir_nota_fijada`). Si falta, pedirlo y parar.
- **Prohibido**: `supabase db push`, `supabase migration new` en `supabase/migrations/` y `apply_migration` del MCP: romperían el historial de Trombito, que comparte proyecto.
- Stack local en marcha (`supabase-local`) para probar. Contra producción solo `scripts/sc migrar` (sin `--local`) y con orden explícita.

## Pasos
1. Siguiente número: `ls db/migraciones` y usar `NNNN` + 1 (cuatro dígitos). Crear `db/migraciones/NNNN_$ARGUMENTS.sql` a mano.
2. SQL reproducible, solo `sous_chef.<objeto>` (nunca `public` ni objetos de Trombito), `set search_path = ''` en funciones, sin `update`/`delete` sin `where`. Sin `begin`/`commit`: el runner usa una transacción por archivo.
3. Columna `not null` en tabla con filas: añadir nullable, backfill, luego `set not null` (o `default` constante).
4. Tabla de usuario: conservar `user_id` y RLS; no debilitar políticas sin sustituirlas (`supabase-create-policy`).
5. Documentar el rollback al final, como comentario con el SQL inverso:
   ```sql
   -- ROLLBACK (manual, orden inverso):
   --   alter table sous_chef.despensa drop column fijada;
   ```
   Si es destructivo o irreversible, indicar que el rollback es otra migración nueva.
6. **Nunca editar una migración ya aplicada**: `sc migrar` guarda su checksum en `sous_chef._migraciones` y falla si cambia. Corregir = migración nueva.
7. Test pgTAP en `db/tests/<NNNN>_<tema>.test.sql` (`has_column`, `col_not_null`, `has_index`, `col_type_is`; si hay backfill, comprobar datos). Plantilla en `.claude/skills/supabase-create-table/table-template.sql`.
8. Aplicar en local y probar (ver Validaciones). Si el cambio es destructivo o toca RLS, pedir revisión de seguridad.
9. Si el campo viaja al cliente, coordinar con `android-create-room-migration` y los DTO de sync.

## Validaciones / tests
- `scripts/sc migrar --local --estado` lista pendientes; `scripts/sc migrar --local` las aplica sin errores.
- `npx supabase test db db/tests` en verde (si la CLI no acepta la ruta, ver `supabase-test`).

## Definition of Done
- [ ] Archivo `db/migraciones/NNNN_*.sql` con número consecutivo; ninguna migración previa editada.
- [ ] Solo objetos de `sous_chef`; nada en `public` ni en `supabase/migrations/`.
- [ ] Rollback documentado en el propio archivo.
- [ ] Test pgTAP del cambio en verde; RLS y `user_id` intactos.
- [ ] `scripts/sc migrar --local` limpio.

## Docs a actualizar
- `docs/arquitectura.md` (modelo de datos) · `docs/herramientas.md` si cambia `sc migrar` · docs/ESTADO.md (skill `session-handoff`)
