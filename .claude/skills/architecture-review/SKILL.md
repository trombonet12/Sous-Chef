---
name: architecture-review
description: Revisar arquitectura sin tocar código, por foco [boundaries|data-flow|domain|concurrency], con findings por severidad; no valida un diseño nuevo (design-feature).
argument-hint: "[boundaries|data-flow|domain|concurrency]"
allowed-tools: Read, Grep, Glob, Agent, Bash(git diff *), Bash(git log *), Bash(git status *)
---

# architecture-review

Revisa el ámbito indicado en `$ARGUMENTS` (por defecto `boundaries`). Solo lectura: no modifica código ni docs.

## Cuándo usarla
- Tras implementar un lote o antes de cerrar una fase, para detectar deriva respecto a `docs/arquitectura.md`, ADRs y `docs/decisiones.md`.
- Cambio de alto impacto (esquema Room, capas, cronómetro): delegar la revisión en un revisor [opus].

## Cuándo NO usarla
- Diseñar lo que aún no existe → usa `design-feature`.
- Registrar una decisión → usa `create-adr`.
- Seguridad/permisos en profundidad → usa `security-review`.

## Precondiciones
- `$ARGUMENTS` es uno de los cuatro valores; si es otro o falta, usar `boundaries` y avisarlo.
- Leer `docs/arquitectura.md`, `docs/adr/` y la fase activa. Acotar el diff (`git diff`, `git log`) o el módulo.

## Pasos
1. Delimitar el alcance: archivos/módulos del diff o los indicados. Si es de alto impacto, delegar con un brief ≤ 40 líneas (skill `delegate-task`) e integrar sus findings.
2. Aplicar el foco de `$ARGUMENTS`:

| Valor | Qué comprobar |
|---|---|
| `boundaries` | Dependencias entre `app/`, `receta/` y `tools/`; `receta/` sin dependencias Android; capas UI → dominio → datos; ciclos; lógica de negocio en UI. |
| `data-flow` | Room como única persistencia de usuario; `recetas`/`tecnicas` empaquetadas desde `data/` (solo lectura); historial, preferencias y estado del crono offline (ADR-0007). |
| `domain` | Modelos (receta, plan, recipiente, técnica, despensa, historial); invariantes del validador y del escalado en `receta/` y no en la UI; nombres coherentes entre Kotlin, JSON de `data/` y SQL. |
| `concurrency` | Corrutinas y scopes, motor del cronómetro y alarmas exactas, receivers, transacciones Room, cancelación y reintentos, muerte del proceso. |

3. Contrastar cada hallazgo con ADRs y `docs/decisiones.md` (por sección). Una desviación sin ADR es finding; si procede, sugerir `create-adr`.
4. Verificar con evidencia (`archivo:línea`); descartar lo que no se pueda demostrar. Lo no verificable → `VERIFICAR`.
5. Emitir la lista ordenada (CRITICO | ALTO | MEDIO | BAJO) como `SEVERIDAD | archivo:línea | problema | corrección propuesta`, y un veredicto (apto / apto con cambios / no apto).

## Validaciones / tests
- Cada finding tiene `archivo:línea` existente y corrección concreta.
- Sin findings: indicar qué se revisó y qué quedó fuera.
- `git status` sin cambios atribuibles a la revisión.

## Definition of Done
- [ ] Foco cubierto según la tabla; alcance declarado.
- [ ] Findings con formato `SEVERIDAD | archivo:línea | problema | corrección propuesta`.
- [ ] Contraste con ADRs/decisiones citado por sección.
- [ ] Ningún archivo modificado.
- [ ] Alto impacto: revisado por un revisor [opus].

## Docs a actualizar
- Ninguno por la revisión; ADR vía `create-adr` si hay decisión pendiente · docs/ESTADO.md (skill `session-handoff`)
