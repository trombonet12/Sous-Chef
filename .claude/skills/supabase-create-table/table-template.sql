-- ===== A) MIGRACIÓN: db/migraciones/NNNN_crear_<tabla>.sql (tabla de usuario, sincronizada) =====
-- Sustituir <tabla> (snake_case, plural). El runner (`sc migrar`) aplica cada archivo en UNA transacción:
-- no escribir begin/commit. Todo en el esquema sous_chef; nada en public.
-- Las funciones set_updated_at / ignorar_editado_en_antiguo se crean una sola vez (la primera tabla);
-- si ya existen, `create or replace` no cambia nada.

create or replace function sous_chef.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = clock_timestamp();
  return new;
end;
$$;

-- LWW por editado_en (instante del cliente): un update más viejo que la fila se descarta en silencio.
create or replace function sous_chef.ignorar_editado_en_antiguo()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.editado_en < old.editado_en then
    return null;
  end if;
  return new;
end;
$$;

create table sous_chef.<tabla> (
  id uuid primary key default gen_random_uuid(),            -- el cliente aporta su UUID
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  -- columnas de negocio aquí
  editado_en timestamptz not null default clock_timestamp(), -- reloj del cliente
  updated_at timestamptz not null default clock_timestamp(), -- reloj del servidor (cursor de pull)
  deleted_at timestamptz                                     -- soft delete
);

create index <tabla>_user_id_idx on sous_chef.<tabla> (user_id);
create index <tabla>_user_id_updated_at_idx on sous_chef.<tabla> (user_id, updated_at);

-- Orden alfabético: primero decide si el update es viejo, después sella updated_at.
create trigger <tabla>_0_ignorar_antiguo
  before update on sous_chef.<tabla>
  for each row execute function sous_chef.ignorar_editado_en_antiguo();

create trigger <tabla>_1_set_updated_at
  before update on sous_chef.<tabla>
  for each row execute function sous_chef.set_updated_at();

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
-- Sin política delete: el borrado es lógico (deleted_at) y el físico queda denegado.

-- Grants: nada a anon; authenticated sin delete; service_role completo.
revoke all on sous_chef.<tabla> from public, anon;
grant select, insert, update on sous_chef.<tabla> to authenticated;
grant all on sous_chef.<tabla> to service_role;

-- Solo en la PRIMERA migración del esquema (no repetir aquí):
--   grant usage on schema sous_chef to authenticated, service_role;
--   alter default privileges in schema sous_chef revoke all on tables from anon;
--   alter default privileges in schema sous_chef grant select, insert, update on tables to authenticated;
--   alter default privileges in schema sous_chef grant all on tables to service_role;

-- ROLLBACK (manual, orden inverso): drop table sous_chef.<tabla>;

-- ===== A2) VARIANTE catálogo (recetas, tecnicas): la app solo lee; escribe `sc publicar` =====
-- Mismas columnas base y soft delete, pero SOLO la política select y solo grant select:
--   create policy "<tabla>_select_own" on sous_chef.<tabla>
--     for select to authenticated using ((select auth.uid()) = user_id);
--   revoke all on sous_chef.<tabla> from public, anon;
--   grant select on sous_chef.<tabla> to authenticated;
--   grant all on sous_chef.<tabla> to service_role;
-- (sin triggers de editado_en: no hay escrituras desde clientes)

-- ===== B) TEST: db/tests/<NNNN>_<tabla>.test.sql =====
begin;
create extension if not exists pgtap with schema extensions;
select plan(15);

select has_table('sous_chef', '<tabla>', 'existe la tabla');
select is((select relrowsecurity from pg_class where oid = 'sous_chef.<tabla>'::regclass),
  true, 'RLS habilitada');
select policies_are('sous_chef', '<tabla>',
  array['<tabla>_select_own', '<tabla>_insert_own', '<tabla>_update_own']);
select has_index('sous_chef', '<tabla>', '<tabla>_user_id_idx', 'user_id', 'índice en user_id');
select col_has_default('sous_chef', '<tabla>', 'user_id', 'user_id tiene default auth.uid()');
select has_column('sous_chef', '<tabla>', 'deleted_at', 'soft delete');
select has_trigger('sous_chef', '<tabla>', '<tabla>_0_ignorar_antiguo', 'trigger LWW');
select is(has_table_privilege('anon', 'sous_chef.<tabla>', 'select'), false, 'anon sin grants');

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-00000000000a', 'a@test.local'),
  ('00000000-0000-0000-0000-00000000000b', 'b@test.local');

-- Usuario A: inserta y lee lo suyo (si hay columnas not null, añadirlas al insert).
set local role authenticated;
select set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-00000000000a","role":"authenticated"}', true);
select lives_ok($$insert into sous_chef.<tabla> (id, editado_en) values
  ('10000000-0000-0000-0000-00000000000a', '2026-01-02 10:00+00')$$, 'A inserta su fila');
select is((select count(*) from sous_chef.<tabla>), 1::bigint, 'A ve su fila');
select throws_ok($$insert into sous_chef.<tabla> (user_id) values
  ('00000000-0000-0000-0000-00000000000b')$$, '42501', null, 'A no inserta con user_id de B');

-- LWW: un update con editado_en más viejo se ignora; uno más nuevo se aplica.
update sous_chef.<tabla> set editado_en = '2026-01-01 10:00+00'
  where id = '10000000-0000-0000-0000-00000000000a';
select is((select editado_en from sous_chef.<tabla>), '2026-01-02 10:00+00'::timestamptz,
  'update más viejo ignorado');

-- Usuario B: no ve ni modifica lo de A.
select set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-00000000000b","role":"authenticated"}', true);
select is((select count(*) from sous_chef.<tabla>), 0::bigint, 'B no ve filas de A');
select is_empty($$update sous_chef.<tabla> set deleted_at = now() returning 1$$,
  'B no actualiza filas de A');

-- anon: sin acceso.
reset role;
set local role anon;
select throws_ok($$select 1 from sous_chef.<tabla>$$, '42501', null, 'anon denegado');

select * from finish();
rollback;
