---
name: despensa
description: Consulta y cambia la despensa y la lista de la compra con scripts/sc (despensa, despensa poner, compra añadir). Úsala para saber qué hay, marcar falta o poco, o apuntar compra.
---

# despensa

## Cuándo usarla
- Saber qué hay en casa o qué falta antes de diseñar o hablar de una receta.
- Joan dice que se le ha acabado algo, que ha comprado algo o que hay que apuntar compra.

## Cuándo NO usarla
- Diseñar una receta → `receta-cronometrada` (ella ya lee la despensa).
- Cambiar las reglas de cocina o los vetos → editar `docs/cocina/reglas.md`.

## Precondiciones
- Credenciales en `~/.souschef/supabase.env` para leer y escribir en Supabase; sin ellas la CLI usa `data/despensa-semilla.md` y lo dice (solo lectura). No imprimas nunca secretos.

## Pasos
1. **Consultar:** `scripts/sc despensa` (por categoría y estado: hay, poco, falta, vetado, más la compra pendiente). `--json` para procesarla.
2. **Cambiar estado:** `scripts/sc despensa poner <hay|poco|falta|vetado> <nombre…>`. Si el producto no existe pide `--categoria`.
3. **Apuntar compra:** `scripts/sc compra añadir <nombre> [--cantidad X] [--receta slug]`.
4. Los vetos (garbanzos, guisantes, champiñones, coliflor, brócoli, casquería, anchoas, alcaparras, marisco) son `vetado`: no los pongas como `hay` aunque Joan los mencione de pasada; pregunta.
5. Al listar la compra: separa «ya hay» y «hay que comprar», con cantidades, agrupado por zona del súper y marcando sustitutos de la despensa (`docs/cocina/reglas.md § Compra`).
6. Confirma a Joan lo que cambiaste en una línea.

## Validaciones / tests
- Repite `scripts/sc despensa` y comprueba que el cambio aparece.

## Definition of Done
- [ ] El estado o la compra reflejan lo pedido al releer con `scripts/sc despensa`.
- [ ] Ningún vetado marcado como `hay`.

## Docs a actualizar
- Ninguno salvo que cambie la semilla (`data/despensa-semilla.md`) · docs/ESTADO.md si queda algo pendiente.
