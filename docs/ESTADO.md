# Estado

| Fase | Estado | Último commit |
|---|---|---|
| F0 Repo y gobierno | completada | ver `git log -1` |
| F1 Esqueleto Android | en curso | — |

## Completado
- 2026-10-13 · F0: `.claude/` (49 skills, 18 agentes, hooks con 98 tests), CLAUDE.md, docs (spec de la app vieja,
  reglas de cocina, 6 ADRs, arquitectura, herramientas), datos legados en `data/` y `docs/legado/`.
- 2026-10-13 · Trombito: `sous_chef` en `[api] schemas` de `supabase/config.toml` (commit local `c447863`, sin push).

## Entorno
| Herramienta | Estado |
|---|---|
| JDK 21 (Gradle) | OK vía `~/.gradle/gradle.properties` |
| Android SDK 36 / build-tools 36.0.0 | OK |
| Emulador | AVD `trombito-36` |
| Docker | OK (29.8.1) |
| `~/.souschef/supabase.env` | **falta** → se trabaja contra el Supabase local hasta que exista |
| `local.properties` | generado por `scripts/preparar-local-properties.py` (URL y clave de Trombito) |

## Pendiente del usuario
- Crear `~/.souschef/supabase.env` (`SUPABASE_DB_URL`, `SOUS_CHEF_EMAIL`, `SOUS_CHEF_PASSWORD`) y exponer `sous_chef`
  en Supabase → Data API (ver `docs/pasos-manuales.md`).
- Push del commit de Trombito `c447863` cuando lo revises.

## Problemas conocidos
- Ninguno.

## Próxima acción exacta
F1: keystore en `~/.souschef/`, workflows de CI y release, spike de alarmas en el emulador, gate completo.
