-- ===== A) OWN: db/migraciones/NNNN_politicas_<tabla>.sql (tabla de usuario con user_id) =====
-- Una migración NUEVA; nunca editar una ya aplicada (el runner guarda su checksum).
-- Sin begin/commit: el runner usa una transacción por archivo.
alter table sous_chef.<tabla> enable row level security;

create policy "<tabla>_select_own" on sous_chef.<tabla>
  for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "<tabla>_insert_own" on sous_chef.<tabla>
  for insert to authenticated
  with check ((select auth.uid()) = user_id);

create policy "<tabla>_update_own" on sous_chef.<tabla>
  for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
-- Sin política delete: borrado lógico con deleted_at. Si algún día hiciera falta delete físico, añadir
-- "<tabla>_delete_own" (for delete to authenticated using ((select auth.uid()) = user_id)) y su grant.

revoke all on sous_chef.<tabla> from public, anon;
grant select, insert, update on sous_chef.<tabla> to authenticated;
grant all on sous_chef.<tabla> to service_role;

-- ===== B) CATALOGO: recetas / tecnicas (la app solo lee; escribe `sc publicar` con service_role/DB URL) =====
alter table sous_chef.<tabla> enable row level security;

create policy "<tabla>_select_own" on sous_chef.<tabla>
  for select to authenticated
  using ((select auth.uid()) = user_id);

revoke all on sous_chef.<tabla> from public, anon;
grant select on sous_chef.<tabla> to authenticated;
grant all on sous_chef.<tabla> to service_role;

-- ===== C) TEST OWN: db/tests/<NNNN>_<tabla>_policies.test.sql =====
-- Mismo patrón que el bloque B de .claude/skills/supabase-create-table/table-template.sql.
begin;
create extension if not exists pgtap with schema extensions;
select plan(8);

select policies_are('sous_chef', '<tabla>',
  array['<tabla>_select_own', '<tabla>_insert_own', '<tabla>_update_own']);
select is(has_table_privilege('anon', 'sous_chef.<tabla>', 'select'), false, 'anon sin grants');
select is(has_table_privilege('authenticated', 'sous_chef.<tabla>', 'delete'), false, 'sin delete físico');

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-00000000000a', 'a@test.local'),
  ('00000000-0000-0000-0000-00000000000b', 'b@test.local');

-- Fila de A insertada como postgres (evita depender de columnas not null del cliente).
insert into sous_chef.<tabla> (id, user_id) values
  ('10000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000a');

set local role authenticated;
select set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-00000000000a","role":"authenticated"}', true);
select is((select count(*) from sous_chef.<tabla>), 1::bigint, 'A ve lo suyo');

select set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-00000000000b","role":"authenticated"}', true);
select is((select count(*) from sous_chef.<tabla>), 0::bigint, 'B no ve lo de A');
select throws_ok($$insert into sous_chef.<tabla> (user_id) values
  ('00000000-0000-0000-0000-00000000000a')$$, '42501', null, 'B no inserta como A');
select is_empty($$update sous_chef.<tabla> set deleted_at = now() returning 1$$, 'B no actualiza lo de A');

reset role;
set local role anon;
select throws_ok($$select 1 from sous_chef.<tabla>$$, '42501', null, 'anon denegado');

select * from finish();
rollback;
-- Para CATALOGO (bloque B): además, A no puede insertar ni actualizar (throws_ok 42501 por falta de grant).
