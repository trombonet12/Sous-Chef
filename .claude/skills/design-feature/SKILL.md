---
name: design-feature
description: Diseñar una feature antes de implementarla (contratos, flujo de datos, impacto, plan de lotes); no registra decisiones (create-adr) ni revisa código (architecture-review).
allowed-tools: Read, Grep, Glob
---

# design-feature

Produce un diseño previo para la feature descrita en `$ARGUMENTS`, sin escribir código. Entrega: contratos, flujo de datos, impacto y plan de lotes delegables.

## Cuándo usarla
- Antes de implementar una feature que toca varios módulos (`app/`, `receta/`, `tools/`), el esquema Room, el cronómetro o las alarmas.
- Antes de repartir trabajo en subagentes: el plan de lotes sale de aquí.

## Cuándo NO usarla
- Decisión puntual que hay que registrar → usa `create-adr` (se invoca desde el paso 6).
- Revisar código ya escrito → usa `architecture-review`.
- Bugfix o cambio local sin contratos nuevos → impleméntalo directamente (`bugfix-flow`).

## Precondiciones
- `$ARGUMENTS` con la descripción de la feature; si falta, pedirla y parar.
- Haber leído `docs/ESTADO.md`, la fase activa (`docs/fases/`), sus ADRs, `docs/arquitectura.md` y `docs/decisiones.md`. La feature debe caber en la fase activa; si no, parar y avisar.

## Pasos
1. Acotar: objetivo, usuario (uno solo), fuera de alcance, criterios de aceptación comprobables.
2. Localizar código y contratos existentes (Grep/Glob) en `app/`, `receta/`, `tools/`. No suponer: citar archivo.
3. Contratos: modelos y funciones de `receta/`, entidades Room, subcomandos de `sc`. Indicar dueño y consumidores de cada uno.
4. Flujo de datos: Room como única persistencia (ADR-0007); lecturas, escrituras y comportamiento offline. Para el cronómetro: estado que sobrevive a la muerte del proceso y alarmas exactas reprogramables (reinicio, actualización del APK, cambio de zona horaria).
5. Impacto: fases afectadas, ADRs que contradice o que faltan, migraciones Room, tests críticos (TDD en validador, escalado, motor del cronómetro), permisos Android y seguridad.
6. Si hay decisión estructural nueva, proponer ADR y delegar en `create-adr`. Si el riesgo es alto, pedir `architecture-review` con un revisor [opus].
7. Plan de lotes con los límites de CLAUDE.md § Delegación: 1 objetivo, ≤ 5 archivos, 1 comando de validación, respuesta ≤ 15 líneas, modelo explícito (haiku/sonnet/opus). Paralelo solo con archivos disjuntos (máx. 5). Los archivos reservados quedan para el agente principal.
8. Redactar cada lote con `docs/_templates/BRIEF.template.md` (skill `delegate-task`).

## Validaciones / tests
- Cada contrato tiene dueño, consumidor y test asociado.
- Ningún lote supera 5 archivos ni tiene dos comandos de validación.
- Los lotes paralelos no comparten archivos.

## Definition of Done
- [ ] Diseño con objetivo, contratos, flujo de datos, impacto y plan de lotes.
- [ ] Encaja en la fase activa y no contradice ADRs (o propone ADR).
- [ ] Cada lote cumple los límites de delegación y nombra modelo y validación.
- [ ] Riesgos y preguntas abiertas listadas; lo no verificable marcado `VERIFICAR`.
- [ ] Entregado en la respuesta (no se modifican archivos salvo petición).

## Docs a actualizar
- Fase activa en `docs/fases/` (si el diseño la amplía) · ADR vía `create-adr` · docs/ESTADO.md (skill `session-handoff`)
