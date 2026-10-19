# Revisión de recetas

> Propósito: propuestas de cambio de plato (tipo b) pendientes de decisión de Joan · Audiencia: Joan y Claude Code · Actualizado: 2026-10-19 · Relacionado: docs/cocina/reglas.md, .claude/skills/receta-revisar/SKILL.md

## lasana-carne-berenjena (2026-10-19)

Correcciones objetivas (a) ya aplicadas: hito del sofrito que destapaba sin volver a tapar; «el horno ya está encendido» en el minuto 73 (se enciende en el 76); mantequilla «cortada» sin medida (ahora dados de 2 cm); «Apaga la olla» en la nota del remojo; minuto de entrada de la sal y la nuez moscada de la bechamel (73, no 71); orden «apaga y luego papel» en la sartén; etiqueta de `datos` (incluye el reposo); «emulsión» en el porqué del reposo; «fuego medio-bajo» por «al 4» en la técnica `bechamel`.

| # | Receta · campo | Problema | Propuesta | Motivo |
|---|---|---|---|---|
| 1 | lasana-carne-berenjena · pasos `capa1`/`capa23`/`cubrir` (l. 626) | Las placas se sacan del agua «a medida que las uses»: las de la última capa pasan unos 16-18 min en remojo (del 73 al 89-91), cuando el envase dice unos 10 y la propia receta dice «ni antes ni después» (l. 80) y «más de diez minutos y la lasaña queda pastosa». | En el minuto 83, antes de la bechamel del fondo, sacar todas las placas del agua y dejarlas extendidas en una sola capa sobre un paño limpio, sin amontonarlas. | La placa sigue absorbiendo agua mientras está sumergida; aunque el agua se enfríe, las últimas se pasan de punto y se rompen al montarlas. Regla: «Cierra la receta primero: … tiempos». |
| 2 | lasana-carne-berenjena · grupo FUENTE `a: 14` (l. 199) y nota del remojo (l. 565) | 14 placas entre 4 capas salen a 3,5 por capa: no cuadra. Tampoco está comprobado que 14 placas quepan planas en la bandeja «sin amontonarlas». | Antes de cerrar la receta, Joan pone placas secas en la fuente de 28×20 y cuenta cuántas cubren una capa recortando (o mide una placa). Total = placas por capa × 4. Comprobar también en la bandeja; si no caben en una sola capa, remojar en dos tandas. | Una cantidad sin cerrar incumple «Cierra la receta primero: ingredientes, cantidades» y «Pregunta antes de proponer». Placas amontonadas en el remojo se pegan entre ellas. |
| 3 | lasana-carne-berenjena · paso `reposo` `min: 10` (l. 720-723) | La bechamel es fluida (60 g/l) y las placas van remojadas, así que en el horno beben menos. Con 10 min de reposo, el centro de una fuente de 4 capas sigue por encima de los 80 °C que la propia receta pone como umbral para que cuaje. | Reposo de 15 min, con hito a los 10 («toca el centro con el dorso del cuchillo»). `totalMin` pasa a 136. De paso, suavizar el porqué de la l. 747: con placa remojada, 80-100 g/l da una lasaña más compacta, no «seca y con las esquinas duras». | Una bechamel de almidón solo gelifica al enfriarse. Con poca harina y poca absorción de las placas, 10 min se quedan cortos para que la porción salga entera. |
| 4 | lasana-carne-berenjena · pasos `hornear` (l. 675) y `gratinar` (l. 705) | El queso entra al principio y pasa 25 min a 200 °C sin tapar, así que ya llega dorado al grill; 5 min de grill fuerte se arriesgan a quemarlo. Además, la alternativa sin grill («cuatro minutos más») descuadra el cronómetro. | Añadir al hito de `hornear` a los 1380 s: «si el queso ya está dorado, sáltate el grill». Quitar la alternativa sin grill, o confirmar con Joan si su horno tiene grill y cerrar una sola opción. | Arriba y abajo a 200 °C ya gratina el queso rallado en ese tiempo. Regla: «Los tiempos parciales suman el total». |
| 5 | técnica bechamel · paso 4 (l. 25) | Pone «para gratinar» en el grupo de 60 g/l y «de cubrir» en el de 80-100 g/l. Se contradice: para gratinar suele pedirse la de cubrir. | «60 g: fluida, para lasaña con placa precocida; 80-100 g: para cubrir y gratinar (canelones, gratenes); más de 100 g: croquetas». | La de 60 g/l resbala de una superficie inclinada y no gratina en capa. |

Sin hallazgo: aritmética (offsets, `s`/`min`, `totalMin` 131, `lectura`, `datos`, 22 pasos); 3 fuegos solo del 67 al 73, con la sartén grande en el fuego grande y la honda en el mediano, como dice perfil.md; ragú con 400 g de tomate (32 min de reducción, con hito de espesor); bechamel de 60 g/l con 1 l de leche (es suficiente para 4 capas en 28×20); vetos; despensa; `mep` completo; reglas 9 y 22 (la 22 se corrigió en el sofrito).

Dudas para Joan: equivalencia del queso, «unas 15 cucharadas soperas» para 150 g (una cucharada rasa de queso rallado pesa unos 6-7 g, así que serían unas 20-25 rasas o 15 colmadas). ¿Qué tamaño tiene la bolsa que compras?

**Estado (2026-10-19, agente principal):**
- **Aplicadas**:
  - la 1: al acabar el remojo, todas las placas al paño;
  - la 3: reposo de 15 min, con un hito a los 10 y `totalMin` de 136;
  - la 4: hito «si el queso ya está dorado, sáltate el grill», sin la alternativa sin grill;
  - la 5: corregida la técnica.
- **Pendientes de Joan**:
  - la 2: cuántas placas caben por capa y si caben planas en la bandeja;
  - la equivalencia del queso: la bolsa que compra.
