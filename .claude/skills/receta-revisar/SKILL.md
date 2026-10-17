---
name: receta-revisar
description: Revisa una receta existente de data/recetas contra las reglas y el esquema; aplica correcciones objetivas, propone cambios de plato en docs/revision-recetas.md y cierra con validar --estricto.
---

# receta-revisar

## Cuándo usarla
- Pedir revisar, auditar o «dejar al listón» una o varias recetas ya en `data/recetas/`.
- Tras cambiar una regla de `docs/cocina/reglas.md` y querer aplicarla a las recetas existentes.

## Cuándo NO usarla
- Receta nueva → `receta-cronometrada`.
- Técnica nueva → `tecnica-nueva`.

## Precondiciones
- Leídos `docs/cocina/reglas.md` y `docs/cocina/esquema-receta.md` (y la despensa: la que Joan pegue desde la app o `data/despensa-semilla.md`).
- Receta existente en `data/recetas/<slug>.json`.

## Pasos
1. `scripts/sc validar --estricto <slug>` para ver el estado de partida.
2. Lee la receta entera y repasa contra las reglas: vetos, equivalencias caseras, un corte por ingrediente con medida, potencia y tapa por paso, ingredientes en `mep` con cantidad y minuto, hitos o `sinHitos` en tramos de 6 min o más, `saco` en el último paso, `p` argumentado, aritmética de `totalMin`, `s` = `min`·60, ingredientes en despensa o `compra`.
3. **Separa los hallazgos en dos grupos:**
   - **Correcciones objetivas** (incumplen una regla o el esquema, sin cambiar el plato): aplícalas directamente en el JSON.
   - **Cambios de plato** (sustituir ingrediente, cambiar potencia o tiempo con efecto en el resultado, quitar o añadir algo): no los apliques. Anótalos como propuesta en `docs/revision-recetas.md` (crea el archivo si no existe) con receta, qué cambiaría, por qué y qué efecto tiene en el plato.
4. `scripts/sc formatear <slug>` y `scripts/sc validar --estricto <slug>`.
5. Informa a Joan en pocas líneas: qué se corrigió y qué propuestas hay pendientes de su decisión. Si decide aplicar una, trátala como edición de receta y repite el paso 4.
6. Con su OK: publicar = commit del JSON, subir `versionCode`/`versionName` en `app/build.gradle.kts`, gate `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug`, push a main y `node scripts/gh.mjs workflow run release-android.yml`.

## Validaciones / tests
- `scripts/sc validar --estricto <slug>` sin errores.
- Sin `sc comparar` (ADR-0007): el repo es la única copia.

## Definition of Done
- [ ] Correcciones objetivas aplicadas y `validar --estricto` en verde.
- [ ] Cambios de plato solo como propuestas en `docs/revision-recetas.md`, ninguno aplicado sin OK.
- [ ] Publicada solo si Joan lo ha aprobado.

## Docs a actualizar
- `docs/revision-recetas.md` · docs/ESTADO.md (skill session-handoff).
