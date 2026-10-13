# ADR-0002: Compartir el proyecto Supabase de Trombito con el esquema `sous_chef` y un runner de migraciones propio

- Estado: aceptado
- Fecha: 2026-10-13
- Relacionado: `docs/decisiones.md` § Supabase, [ADR-0003](0003-el-repo-manda-en-las-recetas.md), [ADR-0004](0004-sync-outbox-lww.md), [arquitectura](../arquitectura.md)

## Contexto
- Sous Chef usa el mismo proyecto Supabase y la misma cuenta que Trombito (un único usuario, plan gratuito, un solo proyecto).
- Trombito gestiona su historial con `supabase/migrations/` y el CLI de Supabase. `supabase db push`, `db diff`/`db pull` y el `apply_migration` del MCP operan sobre el historial `supabase_migrations` compartido y podrían romperlo o mezclar objetos de ambos proyectos.
- Un `config push` de Trombito sobrescribe los esquemas expuestos de la Data API: si `sous_chef` no figura en su `config.toml`, lo quitaría.

## Decisión
- Todo vive en el esquema `sous_chef`; nada en `public`.
- Las migraciones están en `db/migraciones/*.sql`, no en `supabase/migrations/`. El runner propio `sc migrar [--local]` (JDBC):
  - conecta por el session pooler (puerto 5432) con `prepareThreshold=0`;
  - toma un advisory lock y ejecuta una transacción por archivo;
  - registra cada archivo en `sous_chef._migraciones` con checksum y falla si el checksum de uno ya aplicado cambia;
  - termina con `notify pgrst, 'reload schema'`.
- Nunca `supabase db push` ni `apply_migration` del MCP contra el proyecto compartido.
- `sous_chef` se añade a Exposed schemas en el panel (Data API) y a `[api] schemas` del `supabase/config.toml` de Trombito (commit local, sin push). En Trombito, `db diff`/`db pull` se usan con `--schema public`.
- Entorno local: `npx supabase start` con `project_id = "sous-chef"` en puertos 553xx, y tests pgTAP en `db/tests/`.
- Grants: uso y privilegios para `authenticated` y `service_role` más `alter default privileges`; nada para `anon`.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| `supabase db push` con `supabase/migrations/` | Flujo estándar | Comparte historial con Trombito; riesgo de borrarlo o desincronizarlo | Rompe el historial de Trombito |
| `apply_migration` del MCP | Cómodo desde Claude Code | Escribe en el historial compartido y no es reproducible en local | Mismo riesgo; sin checksum |
| Segundo proyecto Supabase | Aislamiento total | Segundo proyecto y otra sesión de usuario | Se prefiere una sola cuenta y sesión |
| Tablas en `public` con prefijo | Sin exponer otro esquema | Mezcla con Trombito; RLS y grants confusos | Poco aislamiento |
| Flyway/Liquibase | Herramientas maduras | Dependencia nueva para una necesidad pequeña | Un runner pequeño basta |
| Runner propio en `sc migrar` | Aislado, con checksum y lock | Código propio que mantener | Elegido |

## Consecuencias
- Positivas: el historial de Trombito queda intacto; migraciones reproducibles en local y en remoto.
- Negativas / costes: runner propio que mantener y testear (TDD); dos pasos manuales (panel y `config.toml` de Trombito); el session pooler exige `prepareThreshold=0`.
- Seguimiento: `VERIFICAR` antes de la primera migración remota que `sous_chef` está expuesto (GET a PostgREST con `Accept-Profile: sous_chef`); revisar si Trombito cambia su flujo de migraciones.
