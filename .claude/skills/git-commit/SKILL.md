---
name: git-commit
description: Revisar y crear un commit local Conventional Commits tras el gate qa fast y secret scan; push solo a origin main con gate en verde. Para el PR usa git-prepare-pr; para auditar, git-check.
allowed-tools: Read, Grep, Bash(git status*), Bash(git diff*), Bash(git log*), Bash(git add*), Bash(git commit*), Bash(git push origin main), Bash(./gradlew *), Bash(scripts/sc *), Bash(node scripts/*), Bash(bash scripts/*)
disable-model-invocation: true
---

# git-commit

Pre-commit review y commit local. Solo lo ejecuta el agente principal (CLAUDE.md § Delegación); los subagentes no hacen commit.

## Cuándo usarla
- El usuario pide explícitamente un commit de cambios ya terminados.

## Cuándo NO usarla
- Sin petición explícita de commit: no commitear por iniciativa propia.
- Descripción de PR → usa `git-prepare-pr`.
- Solo ver el estado del árbol o archivos sospechosos → usa `git-check`.
- Ejecutando como subagente → devolver al agente principal.

## Precondiciones
- Cambios entendidos y de un solo propósito (commits pequeños). Si no se entiende un cambio, parar y preguntar.
- No reescribir historia (`--amend`, `rebase`) sin petición explícita. **Nunca force-push.**

## Pasos
1. `git status` y `git diff --staged` (y `git diff` para lo no preparado): cada archivo pertenece al cambio.
2. Preparar solo lo necesario con `git add <archivos>` explícitos; nunca `git add -A` a ciegas.
3. Revisar el diff: nada generado (`build/`, `.gradle/`, `tools/build/`), nada grande o binario sin justificar, sin `.env*`, keystores, `local.properties` ni claves, sin tests desactivados.
4. Secret scan rápido: `security-secret-scan` sobre el diff preparado. Cualquier hallazgo → parar; nunca mostrar el valor.
5. Gate qa fast: `./gradlew ktlintCheck testDebugUnitTest :receta:test`, `scripts/sc validar --estricto`, `node scripts/check-claude-assets.mjs`, `bash scripts/test-hooks.sh`. Si algo falla → no commitear; corregir o informar.
6. Mensaje Conventional Commits: `tipo(ámbito): resumen` en imperativo, ≤ 72 caracteres.
   - Tipos: feat, fix, refactor, perf, test, docs, build, ci, chore.
   - Cuerpo opcional para el porqué. Añadir al final las líneas de atribución que indique el contexto de la sesión, si las hay.
7. Commit: `git commit -m "tipo(ámbito): resumen"` (varias líneas con `-m` repetido). Sin `--no-verify`.
8. `git status` y `git log -1` para confirmar.
9. Push: solo si el usuario lo pide y el gate está en verde sobre ese commit: `git push origin main`. Nunca `--force`.

## Validaciones / tests
- Gate qa fast en verde justo antes del commit.
- `git diff --staged` coincide con lo descrito en el mensaje.
- Mensaje validado contra el formato `tipo(ámbito): resumen`.

## Definition of Done
- [ ] Commit local con un único propósito y mensaje Conventional Commits.
- [ ] Gate en verde; sin secretos, generados ni archivos grandes injustificados en el diff.
- [ ] Sin `--no-verify` ni force-push; push solo si se pidió y con gate verde.
- [ ] Árbol limpio o con los cambios restantes explicados al usuario.

## Docs a actualizar
- docs/ESTADO.md si el commit cierra una subfase (skill `session-handoff`)
