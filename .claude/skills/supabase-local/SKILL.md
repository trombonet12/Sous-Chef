---
name: supabase-local
description: Arrancar, parar o resetear el Supabase local de Sous Chef con Docker (start|stop|reset) y aplicar db/migraciones con sc migrar --local; para crear migraciones usa supabase-create-migration.
argument-hint: "[start|stop|reset]"
allowed-tools: Read, Bash(npx supabase *), Bash(scripts/sc *), Bash(docker ps *), Bash(docker info *)
---

# supabase-local

Gestiona la pila Supabase local (`$ARGUMENTS`; por defecto `start`). `supabase/config.toml` usa `project_id = "sous-chef"` y puertos 553xx para no chocar con el stack de Trombito.

## Cuándo usarla
- Hace falta la base local para migraciones, tests pgTAP o `sc ... --local`.
- Restaurar un estado limpio tras cambiar migraciones.

## Cuándo NO usarla
- Escribir una migración → `supabase-create-migration` (tabla nueva: `supabase-create-table`).
- Tocar el proyecto remoto (compartido con Trombito): fuera de esta skill; solo `scripts/sc migrar` sin `--local` con orden explícita.

## Precondiciones
- Docker en ejecución (`docker info` responde). Si no, avisar y parar; no instalar nada.
- Si el stack de Trombito está corriendo, los puertos y `project_id` distintos evitan el choque; no tocar su `config.toml`.
- **Prohibido** `supabase db push` y `supabase db reset` como vía de migración: el esquema se aplica solo con `scripts/sc migrar --local`.

## Pasos
1. Resolver `$ARGUMENTS` (`start`, `stop`, `reset`); si falta, `start`; si es otro, pedir uno válido y parar.
2. Ramas:

| Valor | Comando | Efecto |
|-------|---------|--------|
| `start` | `npx supabase start > /dev/null 2>&1` y luego `scripts/sc migrar --local` | Levanta contenedores (primera vez descarga imágenes) y aplica `db/migraciones`. |
| `stop` | `npx supabase stop` | Para los contenedores. |
| `reset` | `npx supabase stop --no-backup`, `npx supabase start > /dev/null 2>&1`, `scripts/sc migrar --local` | **Destructivo local**: recrea la BD desde cero. Confirmar antes si pueden hacer falta los datos de prueba. |

3. `start` se redirige a `/dev/null` porque imprime URL y claves (anon, service_role, JWT secret). **Nunca** pegar esa salida ni la de `supabase status` en chats, logs, docs o commits; resumir "arrancado" y, si hace falta, los puertos.
4. Comprobar con `scripts/sc migrar --local --estado` (no imprime secretos) y `docker ps` (contenedores `supabase_*_sous-chef`).
5. Si un contenedor falla: `docker ps -a`, `npx supabase stop` y volver a `start`; si persiste, consultar la documentación oficial.

## Validaciones / tests
- `scripts/sc migrar --local --estado` sin pendientes tras `start`/`reset`.
- Tras `reset`: `npx supabase test db db/tests` en verde.

## Definition of Done
- [ ] La acción pedida se ejecutó y el estado coincide (corriendo / parado / reseteado).
- [ ] Migraciones aplicadas con `sc migrar --local`, no con la CLI de Supabase.
- [ ] Ninguna clave ni secreto impreso o copiado.
- [ ] `supabase/config.toml` sin cambios no pedidos.

## Docs a actualizar
- `docs/herramientas.md` si cambia el flujo local · docs/ESTADO.md (skill `session-handoff`)
