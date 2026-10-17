---
name: session-start
description: Al empezar una sesión o retomar trabajo: leer ESTADO.md, fase activa y ADRs, ver git y verificar el entorno; resumen ≤ 10 líneas. No para cerrar sesión (session-handoff).
allowed-tools: Read, Grep, Glob, Bash(git status*), Bash(git log*), Bash(node --version), Bash(docker info*), Bash(docker --version)
---

# session-start

Carga el contexto mínimo para trabajar sin repetir investigación y entrega un resumen corto antes de actuar.

## Cuándo usarla
- Al empezar una sesión, tras `/clear` o compactación, o al retomar un trabajo a medias.
- Antes de lanzar una oleada o subagentes: el estado real manda sobre la memoria de la conversación.

## Cuándo NO usarla
- Cerrar o pausar la sesión → usa `session-handoff`.
- Preparar el encargo de un subagente → usa `delegate-task`.
- Subagentes: no la ejecutan; reciben el contexto en su brief.

## Precondiciones
- Directorio de trabajo en la raíz del repo. Solo lectura: no se modifica ni instala nada.

## Pasos
1. Leer `docs/ESTADO.md` entero; extraer Fase, Subfase, Estado, Pendiente (incluido `§ Pendiente del usuario`), Problemas conocidos y **Próxima acción exacta**.
2. Localizar la fase activa en `docs/fases/`. Si la carpeta o el archivo no existen, anotarlo como hueco y seguir.
3. Leer solo los ADRs que la fase cita en `docs/adr/`. Si no hay, anotarlo.
4. Git: `git status --short` y `git log -5 --oneline`. Contrastar con "Último commit" de ESTADO.md; discrepancias o cambios sin commitear no explicados se marcan.
5. Entorno mínimo, según lo que toque la próxima acción:
   - Siempre: `node --version` (los `scripts/*.mjs` lo usan).
   - Android o `tools/`: JDK 21 visible para Gradle (`~/.gradle/gradle.properties`) y SDK (`ANDROID_HOME` o `sdk.dir` en `local.properties`); comprobar solo su existencia, nunca imprimir valores.
6. Si falta algo, no instalar ni arreglar: listar el bloqueo y quién lo resuelve.
7. Responder con un resumen **≤ 10 líneas**: fase/subfase y estado · último commit y git · próxima acción exacta · bloqueos y discrepancias · entorno (OK o qué falta). Después, empezar por la próxima acción o pedir confirmación si hay discrepancias.

## Validaciones / tests
- El resumen cabe en 10 líneas y la próxima acción citada es literalmente la de `docs/ESTADO.md`.
- No ejecutar el gate completo aquí (es de cierre de tarea); solo si ESTADO.md lo marca como primer paso.

## Definition of Done
- [ ] ESTADO.md, fase activa y ADRs citados leídos (o su ausencia anotada).
- [ ] `git status` y `git log -5` contrastados con ESTADO.md.
- [ ] Entorno mínimo verificado según el ámbito; sin instalar nada.
- [ ] Resumen ≤ 10 líneas entregado antes de tocar código.

## Docs a actualizar
- Ninguno. Si ESTADO.md está desfasado, corrígelo con `session-handoff`.
