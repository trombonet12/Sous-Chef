---
name: tecnica-nueva
description: Crea una ficha de técnica culinaria en data/tecnicas/<slug>.json con el esquema de técnica y la enlaza desde tags.tecnica de las recetas que la usan.
---

# tecnica-nueva

## Cuándo usarla
- Una receta usa una técnica (sofrito, reducción, mantecatura...) sin ficha, o Joan pide explicar una técnica.
- `validar --estricto` avisa de un tag de técnica sin ficha.

## Cuándo NO usarla
- Diseñar o corregir una receta → `receta-cronometrada` o `receta-revisar`.

## Precondiciones
- Leído el esquema de técnica en `docs/cocina/esquema-receta.md § Técnica`.
- Ver una ficha existente de `data/tecnicas/` como modelo de tono y longitud.

## Pasos
1. Comprueba que no existe ya (`data/tecnicas/`). Slug en minúsculas, sin acentos, con guiones.
2. Escribe `data/tecnicas/<slug>.json`: `slug`, `titulo`, `resumen` (una frase), `categoria` (la de las fichas existentes, p. ej. `base`), `pasos[{t,p}]` con el porqué físico en cada `p`, `consejos[]` con señales concretas y errores a evitar, `creada` y `actualizada` en ISO. Opcionales: `cortes`, `pie`.
3. Respeta las reglas de cocina: potencias en escala 0-9 de vitrocerámica y marcadas en el texto como `{N}` («baja al {3}»; `scripts/sc potencias <slug>` sin candidatos), medidas de corte, sin ingredientes vetados.
4. Enlázala: pon el `slug` en `tags.tecnica` de las recetas que la usan (edita `data/recetas/<slug>.json`).
5. `scripts/sc formatear` y `scripts/sc validar --estricto`.
6. Con el OK de Joan, publica como en `receta-cronometrada` (commit, versión, gate, push, release).

## Validaciones / tests
- `scripts/sc validar --estricto` sin errores; ningún tag de técnica sin ficha.

## Definition of Done
- [ ] Ficha creada con formato canónico y válida.
- [ ] Al menos una receta la enlaza desde `tags.tecnica`.
- [ ] Publicada tras el OK de Joan.

## Docs a actualizar
- docs/ESTADO.md (skill session-handoff).
