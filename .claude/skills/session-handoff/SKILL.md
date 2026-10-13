---
name: session-handoff
description: Actualizar docs/ESTADO.md al cerrar tarea o sesión [normal|compact]; compact es la versión mínima antes de compactar contexto. No para arrancar (session-start).
argument-hint: "[normal|compact]"
allowed-tools: Read, Edit, Bash(git status*), Bash(git log*)
---

# session-handoff

Deja `docs/ESTADO.md` listo para que otra sesión continúe sin contexto. Modo: `$ARGUMENTS` (por defecto `normal`).

## Cuándo usarla
- `normal`: al terminar una tarea, subfase u oleada, o al cerrar la sesión.
- `compact`: justo antes de compactar o limpiar el contexto, con poco margen.

## Cuándo NO usarla
- Empezar sesión → usa `session-start`.
- Registrar una decisión de arquitectura → usa `create-adr` (aquí solo se enlaza).
- Eres un subagente: ESTADO.md es reservado del agente principal (CLAUDE.md § Delegación). No lo edites; devuelve el texto propuesto.

## Precondiciones
- Agente principal. `$ARGUMENTS` es `normal` o `compact`; si falta o es otro, usar `normal`.
- Leer el `docs/ESTADO.md` actual antes de editar y conservar lo vigente.

## Pasos
1. `git status --short` y `git log -1 --oneline` para fijar el **Último commit** real y los cambios sin commitear.
2. Según `$ARGUMENTS`:
   - `normal`: rellenar todos los campos con datos comprobados.
   - `compact`: solo Fase · Subfase · Estado · Último commit · Pendiente · Archivos importantes · **Próxima acción exacta** (~15 líneas añadidas como máximo).
3. Campos de ESTADO.md: Fase · Subfase · Oleada · Estado · Último commit · Completado · Pendiente · Tests · Problemas conocidos · Decisiones recientes · Archivos importantes · Próxima acción exacta. Lo que bloquea al usuario va en `§ Pendiente del usuario`.
4. Reglas por campo:
   - Último commit: hash corto y asunto, o "sin commitear" con la lista de archivos.
   - Completado: una línea por hito; condensar lo antiguo.
   - Tests: comando ejecutado y resultado real (p. ej. el gate de `qa`); si no se ejecutó, "no ejecutado".
   - Problemas conocidos: síntoma y workaround; quitar los resueltos. Los TODOs se registran aquí.
   - Decisiones recientes: una línea cada una, con enlace al ADR o a `docs/decisiones.md`.
   - Próxima acción exacta: **ejecutable sin contexto**: comando o archivo concreto, criterio de éxito y bloqueos. Nada de "continuar con X".
5. Releer como si fuera una sesión nueva: ¿se puede ejecutar la próxima acción solo con ESTADO.md? Si no, concretar.
6. Sin secretos ni valores de entorno. No hacer commit; lo decide el agente principal.

## Validaciones / tests
- Los 12 campos existen (en `compact`, los 7 mínimos) y no quedan marcadores sin rellenar.
- Si se editaron skills o agentes: `node scripts/check-claude-assets.mjs`.
- "Último commit" coincide con `git log -1`.

## Definition of Done
- [ ] ESTADO.md refleja el estado real de git y de los tests.
- [ ] Próxima acción exacta ejecutable sin contexto.
- [ ] Sin secretos ni información obsoleta.
- [ ] Gate de `qa` fast en verde, o su fallo anotado en Problemas conocidos.

## Docs a actualizar
- docs/ESTADO.md · el archivo de la fase activa en `docs/fases/` si cambió su estado
