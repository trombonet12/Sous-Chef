---
name: bugfix-flow
description: Corregir un bug: reproducir, test que falla, arreglo, regresión y causa raíz documentada; no para tests intermitentes (test-debug-flaky) ni para features nuevas (design-feature).
argument-hint: "[descripción-del-bug]"
allowed-tools: Read, Grep, Glob, Write, Edit, Bash(./gradlew *), Bash(scripts/sc *), Bash(npx supabase *)
---

# bugfix-flow

Flujo para el bug `$ARGUMENTS`. Lo ejecuta un agente de ingeniería [sonnet]; si no hay uno cargado: `general-purpose` + modelo sonnet.

## Cuándo usarla
- Hay un comportamiento incorrecto reproducible que debe corregirse (app, dominio `receta/`, CLI `tools/` o esquema SQL).

## Cuándo NO usarla
- Intermitente (pasa a veces) → usa `test-debug-flaky`.
- Comportamiento nuevo o cambio de diseño → usa `design-feature`.
- Una receta que no valida: es contenido, se corrige en `data/recetas/` con `scripts/sc validar --estricto`.

## Precondiciones
- `$ARGUMENTS` con descripción, pasos y resultado esperado vs. obtenido. Si falta, pedirlos y parar.
- Leer `docs/ESTADO.md` y la fase activa; el bug debe caer dentro de ella o registrarse si no.

## Pasos
1. Reproducir: localizar el fallo en local con el menor caso posible (test existente, `scripts/sc`, dispositivo). Anotar el comando exacto.
2. Escribir primero un test que falle por la razón correcta (ver `test-write`; dobles con `test-create-double`). Ejecutarlo y confirmar el rojo:
   - Dominio: `./gradlew :receta:test --tests "<clase>"`.
   - Android: `./gradlew testDebugUnitTest --tests "<clase>"`.
   - BD/RLS: `npx supabase start > /dev/null 2>&1 ; npx supabase test db`.
3. Buscar la causa raíz (no el síntoma): leer el flujo y contrastar hipótesis con el test; no parchear con condiciones ad hoc.
4. Corregir con el cambio mínimo. El test debe pasar (verde).
5. Regresión: gate qa fast. Si otro test cae, entender por qué antes de tocarlo; nunca desactivarlo.
6. Documentar la causa raíz: mensaje de commit `fix(<ámbito>): <resumen>` con causa, arreglo y test añadido (lo hace el agente principal) y entrada en `docs/ESTADO.md` vía `session-handoff`.
7. Si el bug revela un hueco más amplio (validación, RLS, migración), anotarlo como pendiente en ESTADO.md; no ampliar el alcance.

## Validaciones / tests
- El test nuevo falla antes del arreglo y pasa después (evidenciar ambos).
- Gate qa fast: `./gradlew ktlintCheck testDebugUnitTest :receta:test` · `scripts/sc validar --estricto` · `node scripts/check-claude-assets.mjs` · `bash scripts/test-hooks.sh`.

## Definition of Done
- [ ] Bug reproducido con comando documentado.
- [ ] Test de regresión añadido, visto en rojo y luego en verde.
- [ ] Cambio mínimo, sin tests desactivados ni errores ignorados.
- [ ] Causa raíz en el commit y en `docs/ESTADO.md`.
- [ ] Gate qa fast en verde.

## Docs a actualizar
- docs/ESTADO.md (skill `session-handoff`) · docs de la feature afectada si el comportamiento esperado se aclara.
