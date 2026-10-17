---
name: phase
description: Gestionar docs/fases/NN-*.md (start = arranque | plan = lotes | split = subfases | complete = gates y DoD); nunca empieza una fase no pedida; no es session-start/handoff.
argument-hint: "[start|plan|split|complete] [NN]"
allowed-tools: Read, Grep, Glob, Edit, Write, Bash(./gradlew *), Bash(scripts/sc *), Bash(node scripts/*), Bash(git status *), Bash(git log *)
---

# phase

Operación `$0` sobre la fase `$1` (`docs/fases/NN-*.md`, estructura de `docs/_templates/PHASE.template.md`). Sin `$0`: pedirlo y parar. Sin `$1`: usar la fase activa de `docs/ESTADO.md`; si no consta, parar y preguntar.

## Cuándo usarla
- Arrancar, planificar en lotes, dividir o cerrar una fase solicitada por el usuario.

## Cuándo NO usarla
- Retomar o cerrar una sesión sin cambiar de fase → usa `session-start` / `session-handoff`.
- Editar docs técnicos → usa `docs-update`. Registrar decisiones → usa `create-adr`.
- Diseñar una funcionalidad antes de tener fase → usa `design-feature`.

## Precondiciones
- La fase la pidió el usuario de forma explícita: **nunca empezar una fase no solicitada** ni la "siguiente" por iniciativa propia.
- `docs/fases/NN-*.md` existe (salvo que la tarea sea crearlo desde la plantilla) y se ha leído entero, junto a `docs/ESTADO.md` y los ADRs enlazados.
- `docs/ESTADO.md` es reservado: esta skill redacta el texto a añadir y lo entrega al agente principal (`session-handoff`).

## Pasos
### start
1. Leer la cabecera `Estado · Depende de · ADRs` y `## Precondiciones`; comprobarlas (fases previas `completada`, pasos manuales del usuario hechos, entorno, `git status` limpio).
2. Si falta alguna precondición: parar e informar cuál; no cambiar el estado.
3. Listar los `VERIFICAR` y los pasos manuales del usuario (cuentas, claves en `~/.souschef/`) antes de seguir.
4. Cambiar `Estado:` a `en curso` y redactar la fila de `ESTADO.md` (fase, subfase, siguiente paso).
5. Si el plan de lotes está vacío, encadenar `plan`.

### plan
1. Convertir `## Tareas` en lotes en `## Plan de lotes`: Lote · Agente [modelo] · Archivos (propiedad exclusiva) · Validación · Paralelo con.
2. Cada lote cumple CLAUDE.md § Delegación: 1 objetivo, brief ≤ 40 líneas, ≤ 5 archivos, 1 comando de validación, respuesta ≤ 15 líneas.
3. Agente y modelo explícitos (`haiku` mecánico · `sonnet` ingeniería · `opus` seguridad/arquitectura/revisión culinaria). Si un script puede hacerlo, ningún modelo.
4. Paralelo solo con archivos disjuntos (máx. 5); `gradle/libs.versions.toml` y `settings.gradle.kts` son del agente principal.
5. Completar `## Agentes`, `## Skills`, `## Contratos` y `## Tests` coherentes con los lotes; cambios sensibles → revisión de seguridad [opus].

### split
1. Estimar el tamaño: > 5 lotes secuenciales, > 15 archivos o varios dominios (BD + Android + CLI) indican que no cabe en una sesión.
2. Dividir en subfases `NNA`, `NNB`… cortando por contrato: cada una deja el repo en verde, con criterios propios y `Depende de` explícito.
3. Un archivo por subfase desde `PHASE.template.md`, repartiendo tareas, lotes y tests sin duplicar.
4. La fase madre queda como índice con enlaces; comprobar con `docs-check links`.

### complete
1. Recorrer `## Criterios de aceptación` y `## Definition of Done`; marcar `[x]` solo lo comprobado con evidencia (comando, test, archivo).
2. Ejecutar los gates de la fase: `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :receta:test assembleDebug`, `scripts/sc validar --estricto`, `node scripts/check-claude-assets.mjs`, `bash scripts/test-hooks.sh`, `node scripts/check-docs.mjs`.
3. Cambios sensibles sin revisar → revisión de seguridad [opus] antes de cerrar.
4. Si algo falla o queda abierto: no cerrar; listarlo en `## Riesgos` o como TODO registrado en ESTADO.md.
5. Si todo pasa: `Estado: completada` y entregar al agente principal el texto de ESTADO.md (completado, decisiones, siguiente fase propuesta sin iniciarla).

## Validaciones / tests
- `start`/`plan`/`split`: `node scripts/check-docs.mjs`. `complete`: los gates de arriba, todos en verde.

## Definition of Done
- [ ] Solo se operó sobre la fase pedida; estado de la cabecera coherente con la operación.
- [ ] `plan`: todos los lotes con agente, modelo, archivos y validación.
- [ ] `split`: cada subfase cabe en una sesión y es verificable por separado.
- [ ] `complete`: gates en verde y DoD marcado con evidencia; nada cerrado en falso.
- [ ] Texto para `docs/ESTADO.md` entregado al agente principal.

## Docs a actualizar
- `docs/fases/NN-*.md` · docs/ESTADO.md (skill `session-handoff`, agente principal) · ADRs si cambia una decisión (`create-adr`)
