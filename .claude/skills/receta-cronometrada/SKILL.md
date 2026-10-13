---
name: receta-cronometrada
description: Diseña una receta cronometrada (gramos, cortes, potencia 0-9, tapa, hitos, mise en place, cronología hacia atrás) en data/recetas/<slug>.json, la valida y la publica. Úsala en recetas nuevas.
---

# receta-cronometrada

Una receta normal dice qué lleva. Esto dice **qué haces, cuándo, en qué recipiente, a qué potencia y con la tapa dónde**, con los tiempos cuadrados hacia atrás desde la hora de comer y el paralelismo aprovechado. Joan cocina con el móvil en la encimera. Reglas fijas: `docs/cocina/reglas.md`. Perfil: `docs/cocina/perfil.md`.

## Cuándo usarla
- Piden una receta, un plan con hora de servir, tiempos, potencias, cuadrar fuegos, escalar raciones o «qué hago con esto que tengo».

## Cuándo NO usarla
- Corregir una receta existente → `receta-revisar`.
- Crear una ficha de técnica → `tecnica-nueva`.
- Solo consultar o cambiar despensa y compra → `despensa`.

## Precondiciones
- `scripts/sc despensa` responde (si no, `data/despensa-semilla.md`, y dilo). Diseñar a ciegas es como se cuela un vetado.
- Esquema completo en `docs/cocina/esquema-receta.md` (no se copia aquí).

## Pasos
1. **Pregunta lo que cambia el plan**, con opciones, máx. tres preguntas y ninguna ya respondida: raciones (si no lo dice, 2 y avísalo), hora de servir solo si cambia el plato (defecto 15:30), equipo solo si el plan depende de él, restricciones. Nunca preguntes qué ingredientes hay ni preferencias que puedas decidir y justificar.
2. **Mira lo que hay:** `scripts/sc despensa` (diseña solo con eso; lo que mencione Joan cuenta como disponible) y `scripts/sc historial` (no repitas base ni técnica dos platos seguidos). Mira `data/recetas/` por si el plato ya existe: corrige en vez de duplicar.
3. **Diseña la receta** (ver abajo y `references/`). Lo que haga falta comprar va en `compra`; si un plato saldría claramente mejor con algo que no hay, dilo al final bajo **Para la próxima compra** según `docs/cocina/sugerencias-compra.md`.
4. Escribe `data/recetas/<slug>.json`, luego `scripts/sc formatear <slug>`.
5. `scripts/sc validar --estricto <slug>` y corrige hasta que pase. Comprueba tú la aritmética además.
6. **Enseña la receta completa a Joan** y espera su OK. Sin OK no se publica.
7. Con el OK: `scripts/sc publicar` (usa `--seco` para ver antes qué subiría). Una línea final: qué plato y dónde ha quedado.
8. En el chat: dos a cuatro frases y un resumen breve de tiempos (total, fuegos, ventanas de solape). Sin gráfico, sin cantidades, sin pasos: están en la app.

## La receta (resumen; detalle en references/)
- **Solo lo necesario.** Que algo esté en la despensa no es motivo para meterlo. Por cada cosa que descartas y Joan podría esperar, una línea con el motivo («curry: el comino y la cúrcuma se comen el tomate») en `porque`.
- **Cantidades exactas** en g y ml (y cucharadas) con equivalencia casera entre paréntesis. Nada de «al gusto». Lo que no escala lineal (sal, especias potentes, levadura, gelificantes) lleva `esc:"sub"`.
- **Cortes con dirección y tamaño**, y el porqué físico (hacia dónde va la fibra, si el corte la cruza o la sigue, qué le pasa en la cocción). Un ingrediente, una forma de corte, una medida.
- **Cada paso declara cinco cosas:** recipiente (`carril`), potencia (`f`), qué entra (`meto`), qué sale (`saco`) y la tapa (`tapa`).
- **La tapa es información:** puesta retiene vapor; a medias evita salpicaduras y deja reducir; fuera evapora y espesa. Di cuándo entra, cuándo sale y por qué. Si cambia a mitad de un paso largo, son dos pasos.
- **El porqué de cada decisión**, una frase anclada en física o química (evaporación, Maillard, dirección de la fibra, emulsión del almidón, punto de humo, ácido que frena el ablandamiento).
- **Aceite nunca a secas** (oliva o girasol). **Pimentón: solo picante**, cuenta con el pique; si los gramos no son prácticos con el bote de agujeros, dilo.
- **Frescos perecederos** pueden faltar: si el plato se apoya entero en uno, dilo en una línea.
- Hitos, `sinHitos` y cuándo partir un paso: `references/potencias.md`.
- Tiempos, paralelismo, holgura, escalado de raciones, máx. 3 fuegos: `references/planificacion.md`.
- Recipientes, vaso medidor, tabla, `mep` completo: `references/mise-en-place.md`.
- Potencias 0-9 y la inercia de la vitro: `references/potencias.md`.
- **Lo que hace la app y no escribes:** horas de reloj, avisos de solape («Mientras esto corre»), recuperación de retrasos (`holgura`), marcado y pitidos de hitos, historial, favorito y raciones elegidas.

## Tono
Español, de tú, directo, sin relleno ni «¡delicioso plato!». Si algo puede salir mal, di la señal concreta («cuando el fondo esté casi seco», «si burbujea fuerte, baja al 2»), no un aviso genérico.

## Validaciones / tests
- `scripts/sc formatear <slug>` y `scripts/sc validar --estricto <slug>` sin errores ni avisos.
- Lista numerada de comprobaciones: `docs/cocina/esquema-receta.md § Validación`.

## Definition of Done
- [ ] JSON en `data/recetas/<slug>.json` con formato canónico y `validar --estricto` en verde.
- [ ] Sin vetados; todo ingrediente en despensa o en `compra`; todo ingrediente en `mep` con cantidad y minuto.
- [ ] Joan ha visto la receta completa y dado su OK; publicada con `scripts/sc publicar`.

## Docs a actualizar
- `docs/cocina/*` solo si cambia una regla · docs/ESTADO.md (skill session-handoff).
