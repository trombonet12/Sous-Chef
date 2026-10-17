---
name: create-adr
description: Registrar una decisión de arquitectura como ADR numerado en docs/adr/ y enlazarlo en los docs; no para diseñar features (design-feature) ni revisar código (architecture-review).
allowed-tools: Read, Write, Edit, Grep, Glob
---

# create-adr

Crea `docs/adr/NNNN-<titulo-kebab>.md` desde `docs/_templates/ADR.template.md`. `$ARGUMENTS` (opcional): título o tema de la decisión.

## Cuándo usarla
- Se cambia o añade algo de arquitectura (capas, módulos, esquema Room, migraciones Room, dependencias estructurales, contratos entre `app/`, `receta/` y `tools/`): CLAUDE.md exige ADR antes.
- `design-feature` o `architecture-review` detectan una decisión no registrada o que contradice un ADR.

## Cuándo NO usarla
- Diseñar una feature completa (contratos, flujo, lotes) → usa `design-feature`.
- Revisar código existente → usa `architecture-review`.
- Cambios sin impacto arquitectónico (bugfix, refactor local) → no hace falta ADR.

## Precondiciones
- Título/tema claro en `$ARGUMENTS`; si falta, pedirlo y parar.
- Haber leído `docs/decisiones.md` y los ADR existentes relacionados.
- `docs/decisiones.md` y `docs/ESTADO.md` son reservados del agente principal; un subagente no los edita (ver paso 7).

## Pasos
1. Listar `docs/adr/` con Glob. Siguiente número = mayor `NNNN` + 1, 4 dígitos (`0001` si está vacía).
2. Buscar duplicados o ADRs afectados (`Grep` del tema en `docs/adr/`, `docs/decisiones.md`, `docs/arquitectura.md`). Si sustituye a otro, anotarlo.
3. Copiar la estructura de `docs/_templates/ADR.template.md` a `docs/adr/NNNN-<titulo-kebab>.md` (título en imperativo, kebab-case sin tildes).
4. Rellenar: `Estado: propuesto`, `Fecha` (AAAA-MM-DD), `Relacionado` (secciones de `docs/decisiones.md`, fases, ADRs; por referencia, sin copiar texto), Contexto con datos y fuente, Decisión en pocas frases, tabla de Alternativas (mínimo 2, con motivo de descarte), Consecuencias (positivas, costes, seguimiento).
5. Lo no verificable se marca `VERIFICAR` en Seguimiento; no inventar versiones ni APIs.
6. Enlazar el ADR (`[ADR-NNNN](adr/NNNN-....md)`) en cada doc afectado no reservado (`docs/arquitectura.md`, la fase en `docs/fases/`). Un ADR sustituido pasa a `Estado: sustituido por ADR-NNNN`.
7. Si la decisión modifica algo ya cerrado, no editar `docs/decisiones.md`: pedir el cambio al agente principal. Registrar el ADR en ESTADO.md vía `session-handoff`.

## Validaciones / tests
- `Grep` de `NNNN` en `docs/adr/`: un solo archivo con ese número.
- Todas las secciones de la plantilla presentes; ningún `<...>` sin rellenar.
- Cada enlace añadido apunta a un archivo existente (skill `docs-check`).

## Definition of Done
- [ ] `docs/adr/NNNN-*.md` con las secciones de la plantilla y metadatos completos.
- [ ] Numeración correlativa, sin huecos ni duplicados.
- [ ] Enlazado desde todos los docs afectados no reservados.
- [ ] Cambios pedidos en archivos reservados comunicados al agente principal.
- [ ] `node scripts/check-docs.mjs` sin errores nuevos.

## Docs a actualizar
- `docs/arquitectura.md` y la fase activa en `docs/fases/` · docs/ESTADO.md (skill `session-handoff`)
