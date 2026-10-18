---
name: despensa
description: Lee la despensa interna de Joan (data/despensa-semilla.md) y la mantiene al día con lo que diga que tiene o se le ha acabado, para el validador. Úsala para saber qué hay o qué falta.
---

# despensa

La despensa vive solo en `data/despensa-semilla.md`: desde la v1.0 la app no tiene pantalla de despensa ni lista de la compra. No hay `sc despensa` ni `sc compra`.

## Cuándo usarla
- Saber qué hay en casa o qué falta antes de diseñar o hablar de una receta.
- Joan dice que se le ha acabado algo, que ha comprado algo o pregunta qué hay.

## Cuándo NO usarla
- Diseñar una receta → `receta-cronometrada` (ella usa la despensa).
- Cambiar las reglas de cocina o los vetos → editar `docs/cocina/reglas.md`.

## Precondiciones
- `data/despensa-semilla.md` existe: es la despensa que comprueba el validador (regla 12; «agua» siempre vale).

## Pasos
1. **Fuente:** `data/despensa-semilla.md`, más lo que Joan diga en la conversación.
2. **Alinear la semilla:** si lo que dice Joan no coincide, ofrece actualizar `data/despensa-semilla.md` para que `scripts/sc validar --estricto` coincida con la realidad. Edítala solo con su OK.
3. Los vetos (garbanzos, guisantes, champiñones, coliflor, brócoli, casquería, anchoas, alcaparras, marisco) son `vetado`: no los pongas como `hay` aunque Joan los mencione de pasada; pregunta.
4. **Compra:** si Joan pide la compra de una receta, sepárala en «ya hay» y «hay que comprar», con cantidades, agrupada por zona del súper y marcando sustitutos (`docs/cocina/reglas.md § Compra`). Se la das en el chat; la app no tiene lista.
5. Confirma a Joan en una línea lo que has leído o cambiado.

## Validaciones / tests
- Tras tocar la semilla: `scripts/sc validar --estricto` sin errores de despensa.

## Definition of Done
- [ ] Fuente indicada (pegada o semilla) y ningún vetado como `hay`.
- [ ] Semilla alineada solo si Joan lo aprobó.

## Docs a actualizar
- docs/ESTADO.md si queda algo pendiente.
