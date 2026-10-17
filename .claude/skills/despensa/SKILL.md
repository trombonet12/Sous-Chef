---
name: despensa
description: Lee la despensa de Joan (pegada desde la app o semilla) y mantiene data/despensa-semilla.md alineada para el validador. Úsala para saber qué hay o qué falta.
---

# despensa

La despensa real vive en la app (pantalla Despensa). Solo local (ADR-0007): no hay `sc despensa` ni `sc compra`.

## Cuándo usarla
- Saber qué hay en casa o qué falta antes de diseñar o hablar de una receta.
- Joan pega su despensa, dice que se le ha acabado algo o que hay que apuntar compra.

## Cuándo NO usarla
- Diseñar una receta → `receta-cronometrada` (ella usa la despensa).
- Cambiar las reglas de cocina o los vetos → editar `docs/cocina/reglas.md`.

## Precondiciones
- `data/despensa-semilla.md` existe: es la despensa que comprueba el validador (regla 12; «agua» siempre vale).

## Pasos
1. **Fuente:** si Joan pega el texto de «Compartir despensa» (markdown agrupado por estado), esa es la verdad. Si no, lee `data/despensa-semilla.md` y dilo.
2. **Alinear la semilla:** si pegó una despensa distinta, ofrece actualizar `data/despensa-semilla.md` para que `scripts/sc validar --estricto` coincida con la realidad. Edítala solo con su OK.
3. Los vetos (garbanzos, guisantes, champiñones, coliflor, brócoli, casquería, anchoas, alcaparras, marisco) son `vetado`: no los pongas como `hay` aunque Joan los mencione de pasada; pregunta.
4. **Compra:** lo que falte para una receta va en su `compra`. Al listarla separa «ya hay» y «hay que comprar», con cantidades, agrupado por zona del súper y marcando sustitutos (`docs/cocina/reglas.md § Compra`). La lista viva se edita en la app.
5. Confirma a Joan en una línea lo que has leído o cambiado.

## Validaciones / tests
- Tras tocar la semilla: `scripts/sc validar --estricto` sin errores de despensa.

## Definition of Done
- [ ] Fuente indicada (pegada o semilla) y ningún vetado como `hay`.
- [ ] Semilla alineada solo si Joan lo aprobó.

## Docs a actualizar
- docs/ESTADO.md si queda algo pendiente.
