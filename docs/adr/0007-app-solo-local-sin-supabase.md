# ADR-0007: Dejar la app solo en local, sin Supabase

- Estado: aceptado
- Fecha: 2026-10-17
- Relacionado: `docs/decisiones.md` § 2026-10-17 · Ronda 6, sustituye a [ADR-0002](0002-supabase-compartido-esquema-sous-chef.md) y [ADR-0004](0004-sync-outbox-lww.md), revisa [ADR-0003](0003-el-repo-manda-en-las-recetas.md), [arquitectura](../arquitectura.md)

## Contexto
- El MVP local-first (2026-10-14) funciona entero sin backend. Las recetas y técnicas van empaquetadas en el APK desde
  `data/`, y la despensa, la compra, el historial y el estado del crono viven en Room.
- F3/F4 (Supabase, sync, login) llevaba aplazada desde entonces por presupuesto. En el código solo quedaban dependencias
  Gradle, campos de BuildConfig y un buildType `localDebug`. No había clientes, outbox, migraciones SQL ni `db/`.
- Hay un solo usuario y un solo móvil. El APK se actualiza por Obtainium desde las Releases de GitHub.
- Joan lo decide en la revisión de backlog del 2026-10-17.

## Decisión
- Sous Chef es solo local. No hay backend, sync ni login.
- **Publicar una receta** consiste en que pase `sc validar --estricto` en el repo y salga una release nueva del APK, que
  empaqueta `data/`. Al arrancar con un hash de datos nuevo, la app vuelve a sembrar recetas y técnicas.
- Se retiran:
  - las dependencias supabase-kt, ktor y postgresql;
  - los campos `SUPABASE_*` de BuildConfig, el buildType `localDebug` y los secretos opcionales de Supabase de la release;
  - las skills `supabase-*` y el agente `database-engineer`;
  - los comandos previstos `sc publicar/comparar/despensa/migrar`.
- Room sigue siendo la única persistencia de usuario. Las copias de seguridad, si hacen falta, salen por el backup de
  Android, sin servidor.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Mantener F3/F4 aplazada | No cierra la puerta | Dependencias, docs y skills muertas que mantener y revisar en cada ronda | Joan lo descarta |
| Supabase compartido con Trombito (ADR-0002) | Datos en la nube, multi-dispositivo | Runner de migraciones, RLS, secretos y sesión para un solo móvil | Coste sin necesidad real |
| Backend propio ligero | Control total | Servidor que mantener | Peor que Supabase y tampoco hace falta |
| Solo local con APK que empaqueta `data/` | Sin secretos, sin red, sin mantenimiento de servidor | Sin sync entre dispositivos; los datos del usuario viven en el móvil | Elegido |

## Consecuencias
- Positivas: build y CI más simples, sin secretos de backend, menos superficie de seguridad, y menos skills y docs que
  mantener.
- Negativas / costes: si se pierde el móvil sin backup, se pierden el historial y la despensa. Cada cambio de receta exige
  una release nueva.
- Seguimiento: si algún día hacen falta varios dispositivos, abrir un ADR nuevo. Este no se reabre.
