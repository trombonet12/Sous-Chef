---
name: git-prepare-pr
description: Redactar la descripción de un PR (resumen, tipo, fase, DoD, pruebas) para pegar o revisar; no hace push ni gh pr create. Para commits usa git-commit.
allowed-tools: Read, Grep, Bash(git status*), Bash(git log*), Bash(git diff*), Bash(git branch*)
---

# git-prepare-pr

Genera el texto de un PR. No publica nada. El flujo habitual del proyecto es commit directo a `main` con el gate en verde; esta skill sirve cuando se quiere una rama con PR.

## Cuándo usarla
- La rama está lista y hace falta la descripción del PR (para pegar o revisar).

## Cuándo NO usarla
- Crear el commit → usa `git-commit`.
- Revisar la calidad del código → usa `architecture-review` o `security-review`.
- Ejecutar `git push` o `gh pr create`: solo con petición explícita del usuario y fuera de esta skill.

## Precondiciones
- Rama con commits propios respecto a `main`; árbol limpio (si no, avisar y sugerir `git-check`).
- Fase activa conocida: `docs/ESTADO.md` y `docs/fases/`.
- Si existe `.github/pull_request_template.md`, leerlo y seguirlo; si no, usar la estructura del paso 3.

## Pasos
1. Recopilar el cambio: `git log main..HEAD --oneline` y `git diff main...HEAD --stat`; leer el diff de los archivos clave.
2. Resumen: 1-3 frases con qué cambia y por qué.
3. Estructura mínima: Resumen · Tipo (feat, fix, refactor, perf, test, docs, build, ci, chore) · Fase/subfase · Checklist DoD · Cómo se probó.
4. Fase / subfase: indicar la activa y confirmar que el cambio no sale de ella.
5. Checklist DoD: marcar `[x]` solo lo comprobado en esta sesión (gate Android, `:receta:test`, `scripts/sc validar --estricto`, sin secretos, ESTADO.md, ADR, sin tests desactivados). Lo no verificado queda `[ ]` con nota.
6. Cómo se probó: comandos reales ejecutados y su resultado; pruebas manuales en dispositivo; capturas si hay UI. Nada inventado.
7. Entregar el texto en el chat. Preguntar si el usuario quiere push o `gh pr create`; sin respuesta afirmativa explícita, no ejecutarlos.

## Validaciones / tests
- El texto conserva todas las secciones (y casillas, si hay plantilla).
- Cada casilla marcada tiene evidencia (comando o revisión) en "Cómo se probó".
- Gate ejecutado, o dicho expresamente que no se ha ejecutado.

## Definition of Done
- [ ] Descripción completa con resumen, tipo, fase, checklist y pruebas.
- [ ] Checklist honesto, sin secretos en el texto.
- [ ] No se ejecutó `git push` ni `gh pr create` sin petición explícita.

## Docs a actualizar
- docs/ESTADO.md si el PR cierra una subfase (skill `session-handoff`)
