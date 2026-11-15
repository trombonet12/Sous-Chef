# Auditoría final de la ronda 19

> Propósito: comprobar que la ronda 19 (legibilidad del cronómetro y de los textos) está bien hecha, que las cinco quejas de Joan no siguen y que las 33 recetas se cocinan con el modelo nuevo · Audiencia: Joan y Claude Code · Actualizado: 2026-11-15 · Relacionado: docs/analisis/legibilidad-crono-y-textos.md, docs/adr/0017-dos-textos-por-paso-orden-compuesta.md, docs/adr/0018-el-plan-sigue-al-cocinero.md, docs/decisiones.md, docs/cocina/esquema-receta.md

## 1. Veredicto
- **Publicable como 1.8 con dos arreglos de una línea antes**: B1 (la app se cierra con «Chef, info» en los 8 pasos de `nota` vacía) y B2 (quesadillas: el relleno hecho vuelve «al plato», que en las tarjetas es el del pollo crudo).
- Recomendable en la misma tanda, porque también son de una línea: apagar la sartén pequeña de huevos rotos (el piloto), el ajo de la pizza, la comprobación del pollo del wrap y el bol del costillar (§3, R-H1 a R-H4).
- Las cinco quejas: tres resueltas (1, 3 y 5), una resuelta con una salvedad de motor (4) y una parcial (2: el diagrama ya es uno; quedan formatos sueltos).
- El trabajo de la ronda está bien hecho: gate verde (470 tests en `:receta`, 1.185 en `:app`, ktlint y detekt), `sc validar --estricto` 0/0 con las reglas 30-36 dentro, `sc potencias` 0/0, ni una cadena huérfana y ningún resto de FOCO, APILADA, marquesina ni de los pintores viejos del diagrama.
- Lo más serio que queda sin ser bloqueante: el plan sigue al cocinero cuando va tarde, pero no cuando va adelantado en 45 pasos de 30 recetas (H1), y la «i» no se puede tocar en la tarjeta grande (H2).
- Recetas: 5 limpias, 17 con tropiezos que no estropean el plato y 11 con algún hallazgo alto o bloqueante (§4). El patrón común son instrucciones previas escritas en la SEÑAL y `saco` usados para «apaga el fuego».
- No se ha probado en el móvil ni con la pantalla apagada: los escenarios de ADR-0018 (Doze, muerte del proceso, cocinada real) siguen pendientes.

## 2. Las cinco quejas
**Cómo se ha mirado.** Build debug 1.7.57 de este árbol en el AVD `trombito-36` (411 × 914 dp), con huevos rotos (3 carriles), arroz a la cubana (4) y tortilla (2), letra a 1,0× y 1,3×, tema claro y oscuro. El reloj del emulador se movió con `adb shell date` para vencer pasos. Lectura del código de `ui/crono`, `ui/ficha`, `ui/comun` y `receta/…/crono`, y batida de las 33 recetas (cuatro revisores `chef-reviewer` más un barrido mecánico). Método y límites, en el anexo A.

| Queja | Estado | Pruebas |
|---|---|---|
| 1. Bloques de texto ilegibles | **Resuelta** | `p` va en párrafos y listas en 392 de 407 pasos, con tope de 600 y párrafos de 240; la lectura cabe en una pantalla en tres bloques ([captura](../capturas/auditoria-final-ficha-lectura.png)); la descripción baja de 363 a 190 de mediana y va en redonda ([captura](../capturas/auditoria-final-ficha-resumen.png)); en la tarjeta ningún bloque pasa de 2 líneas. Resto menor: 26 líneas de `mep` pasan de 240 caracteres sin regla que las limite (L7) |
| 2. Lo mismo con formatos distintos; el diagrama cambia | **Parcial** | El diagrama es el mismo dibujo en la [lista](../capturas/auditoria-final-lista-huella.png), el [Resumen](../capturas/auditoria-final-ficha-diagrama-resumen.png), el [completo](../capturas/auditoria-final-ficha-diagrama-detalle.png), la [cocina](../capturas/auditoria-final-crono-diagrama-vivo.png) y el [papel](../capturas/auditoria-final-impresa-hoja.png): resuelto. Siguen: METE se compone distinto en la tarjeta y en la ficha (M3), el aviso tiene tres formatos de hora y la duración dos (M4), CLÁSICA conserva METO y SACO (L2) y dos carriles salen del mismo azul en 10 recetas (M7) |
| 3. Nada se lee de un vistazo; el texto se mueve | **Resuelta** | No hay `basicMarquee` en el código. Con [dos en marcha](../capturas/auditoria-final-carriles-dos-en-marcha.png) cada tarjeta enseña reloj, aviso y SEÑAL quietos; igual con [4 carriles](../capturas/auditoria-final-4carriles-tres-en-marcha.png), a [1,3×](../capturas/auditoria-final-carriles-letra13.png) y en [oscuro](../capturas/auditoria-final-carriles-oscuro.png). Pulidos: la pausa no se distingue (M1), «a 02:00» junto a un reloj que descuenta (M2) y el reparto del alto (M5) |
| 4. No se ve qué arrancar ni qué espera a qué | **Resuelta, con una salvedad** | Franja «Ahora / Luego» con hora y relación, un solo botón lleno y borde grueso en el carril que [toca ya](../capturas/auditoria-final-carriles-toca-ya.png), «espera a ①» y «en 22 min · 14:59» en los demás. Arrancando 13 minutos antes, las horas y «Llegas a las» se [mueven con el cocinero](../capturas/auditoria-final-carriles-adelantado.png). Salvedad: si te adelantas a mitad de cocinada, 45 pasos no se adelantan (H1) |
| 5. Ficha rica y cronómetro escueto, con botón al detalle | **Resuelta** | La ficha lleva los [pasos desplegados](../capturas/auditoria-final-ficha-pasos.png) con su porqué y [«Solo órdenes»](../capturas/auditoria-final-ficha-solo-ordenes.png); la tarjeta solo enseña METE, fuego, tapa y duración antes de empezar, SEÑAL en marcha y SACA al [vencer](../capturas/auditoria-final-carriles-vencido.png); el [detalle](../capturas/auditoria-final-crono-detalle-i.png) pone lo ejecutivo antes que el porqué. Peros: la «i» no responde en la tarjeta grande (H2) y hay recetas con órdenes previas en la SEÑAL (§4) |

## 3. Hallazgos por severidad
BLOCKER es «no publicar sin arreglarlo». Cada hallazgo dice qué pasa, dónde y cómo se arregla. Los de receta llevan «R-».

### BLOCKER
**B1 · «Chef, info» cierra la app en los pasos sin señal** · código · una línea y un test
- `OrdenPaso.senalHablada` llama a `cerrar("")`, y `cerrar` hace `frase.last()` (`receta/src/main/kotlin/app/trombito/souschef/receta/crono/OrdenPaso.kt:108-109` y `:305`). Con `nota` igual a `""` lanza `NoSuchElementException`. Reproducido con el `receta.jar` de este build: `nota ""` y `nota "   "` dan la excepción; `null` no.
- Lo llama `RespuestasVoz.info` (`app/src/main/kotlin/app/trombito/souschef/ui/crono/voz/RespuestasVoz.kt:91`) sin `try`, desde el retorno del reconocedor (`ui/crono/voz/ControlVoz.kt:190`). Los tests solo prueban `senal = null` (`OrdenPasoTest.kt:174`).
- Alcance: los 8 pasos con `"nota": ""`, uno en cada receta: `albondigas-en-salsa:aceite-salsa`, `arroz-a-la-cubana-con-bacon:chup-chup-ultimo-tramo`, `ensalada-de-noquis-con-atun-y-feta:tomate`, `focaccia-cherry-olivas:cortar`, `lasana-carne-berenjena:aceite-ragu`, `natillas-galleta-maria:infusionar`, `salmon-glaseado-soja-miel:arrancar` y `tacos-de-carne:limon-fuera-fuego`.
- Escenario: Joan pregunta «Chef, info» con uno de esos pasos en el carril en foco y la app se cierra a mitad de cocinada. No se ha reproducido por voz en el emulador (sin micrófono); sí en la función.
- Corrección: `senal?.let { limpio(it, cocina) }?.takeIf { it.isNotEmpty() }?.let(::cerrar)`, test con `""` y con espacios, y leer la `nota` en blanco como `null` al cargar la receta.

**B2 · quesadillas: el relleno hecho vuelve al plato del pollo crudo** · `quesadillas-pollo-pimiento:especias`
- `saco`: «el relleno, extendido, al plato». En las tarjetas «el plato» es el del pollo (`mise-en-place.saco` «el pollo, en su plato», `pollo.meto` «el plato: 250 g de pechuga de pollo»). El plato limpio solo existe en el `mep`, y que el del pollo va al fregadero, solo en su `porq`.
- Después el relleno solo pasa 4 minutos al {5} dentro de la tortilla.
- Corrección: `saco` «el relleno, a un plato limpio» (29) o «el relleno, extendido, a otro plato» (35), y que `montar-1.meto` nombre el mismo plato: «el plato limpio: medio relleno» (30).

### HIGH
**H1 · El plan no se adelanta en los pasos que vienen de un hueco en su propio carril** · motor · ADR-0018
- La arista de cola conserva el hueco de la receta (`receta/src/main/kotlin/app/trombito/souschef/receta/crono/Prevision.kt:144-148`) y la hora es el máximo de las aristas (`:129`). Si el carril estuvo parado, ese hueco hace de suelo: el paso no puede tocar antes de «fin real del paso anterior del carril + hueco», aunque sus `dep` ya estén hechos.
- Medido en arroz a la cubana: con la sartén y la cazuela terminadas, «Aliñar el arroz y montar» dice «en 51 min · 15:18» y solo ofrece «Empezar antes»; la cabecera dice «Llegas a las 15:20» cuando todo acaba hacia las 14:40 ([captura](../capturas/auditoria-final-4carriles-espera-con-todo-hecho.png)). Es un caso forzado, pero el mecanismo es el mismo con 4 o 5 minutos de adelanto: con más de 3 minutos el paso pasa de «ahora» a «en N min» y el aviso «Toca empezar» suena tarde.
- Alcance: 45 pasos de 30 recetas tienen un hueco de 4 minutos o más en su cola y un `dep` de hueco 0; 22 son el último paso del carril (montar, servir, reposar). Ejemplos: `huevos-rotos:reposo-patata` (12 min), `tacos-de-carne:montaje` (23), `espaguetis-carbonara:servir` (17).
- Con retraso funciona, y también adelantando el arranque. Las novedades de la 1.8 prometen las dos cosas («si vas adelantado o tarde, se mueven contigo»).
- Corrección: que la cola de un paso con `dep` o `junto` aporte solo «carril libre» (`finEf(q)`, sin hueco) y el hueco quede para los pasos sin enlaces. Antes de tocar: test con el escenario de arriba y con `reposo-patata` 5 minutos antes, y regla de validador que exija a todo paso con `dep` un enlace de hueco 0 o un `junto` que explique su `off` (para no romper el invariante «sin desvíos, `previsto = O_plan + off`»).

**H2 · La «i» no se puede tocar en la tarjeta grande** · UI · queja 5
- En la tarjeta completa (la de 2 carriles, o cuando sobra alto) la «i» está en `FilaDatos`, una fila sin `clickable` (`app/src/main/kotlin/app/trombito/souschef/ui/crono/TarjetaCarril.kt:169-182`); lo que abre el detalle es el título (`:309`). En la compacta y en la de «por empezar» sí responde, porque toda la cabecera es pulsable (`TarjetaCompacta.kt:91` y `:245`).
- Además mide 22 dp de ancho (`TarjetaCarril.kt:320-334`). Comprobado en el código, no pulsado en el emulador. [Captura de la tarjeta grande](../capturas/auditoria-final-2carriles-dos-en-marcha.png).
- Corrección: un `IconButton` de 48 dp con su `contentDescription`, igual en los tres diseños de tarjeta.

**R-H1 · huevos rotos: nadie apaga la sartén pequeña** · `huevos-rotos:freir-padron`
- Último paso del carril, al {8}, del minuto 32 al 36. `saco`: «los pimientos, a un plato + 1 g de sal, por encima». La sartén se queda vacía al {8} los 11 minutos de la segunda fritura y los huevos. `freir-huevos` tampoco apaga la grande.
- Corrección: `freir-padron.saco` «… + la sartén pequeña, apagada» y `freir-huevos.saco` «… + la sartén grande, apagada». El patrón general está en M9.

**R-H2 · pizza: METE manda el ajo y el jengibre al empezar** · `pizza-umami-asiatica:saltear-pina`
- METE: «150 g de piña, en la grasa del bacon + 3 g de ajo · el platito + 5 g de jengibre». Entran en el aviso de 2:30, y lo dice una SEÑAL («solo en el aviso») que antes de empezar se corta o se cae en cuanto falta alto.
- Corrección: `meto` «150 g de piña, en la grasa del bacon»; aviso «Entra el platito: ajo y jengibre» (32).

**R-H3 · wrap César: el pollo no se comprueba** · `wrap-cesar-pollo:tanda-1` y `tanda-2`
- «Abre el más grueso: blanco por dentro y sin rosa» solo está en `p`. Los filetes pasan de la plancha al wrap.
- Corrección: aviso a 3:30 «Abre el más grueso: sin rosa por dentro» (39) en las dos tandas, como ya hace `quesadillas-pollo-pimiento:pollo`.

**R-H4 · costillar: la salsa de mesa va al bol del rub** · `costillar-bbq-con-boniato:capa-1`
- `saco`: «un tercio (50 g), al bol enjuagado». Es el bol pequeño del rub, que se reparte frotando carne cruda; ninguna tarjeta lo lava (`envolver.saco` solo lava tabla y cuchillo) y ese tercio no vuelve al fuego.
- Corrección: `envolver.saco` «… + el bol pequeño, lavado» y `capa-1.saco` «un tercio (50 g), al bol lavado».

**R-H5 · lasaña: no dice dónde se remojan las placas** · `lasana-carne-berenjena:remojo`
- La tarjeta dice «METE 2 l de agua caliente · la olla + 16 láminas de lasaña»: se lee como remojar en la olla. La bandeja solo aparece en el SACA final y en `p`. Además «Apaga la olla» va en la SEÑAL.
- Corrección: `t` «Remojar las placas en la bandeja» (32), `agua.saco` «la olla, apagada» y `nota` «Placas de una en una, sin amontonar.».

**R-H6 · albóndigas: los aliños de la masa no pasan por ninguna tarjeta** · `albondigas-en-salsa:cortar` y `masa`
- Los 8 g de sal, el ajo, el perejil, la pimienta y la nuez moscada solo están en el detalle de la casilla «Bol grande» de la portada, tras su «i».
- Corrección: `masa.meto` «las bandejas: 750 g de carne picada + el bol grande: pan, huevo y aliños» y las cantidades, en `cortar.meto`.

**R-H7 · pollo con almendras: dos órdenes que llegan tarde** · `pollo-almendras:salsa` y `arroz-y-salsa`
- «Remueve el vaso antes de verterlo» es SEÑAL de un paso de 2 minutos: la maicena se queda en el fondo. Y enjuagar los 150 g de arroz solo está en `p`.
- Corrección: `salsa.t` «Remover el vaso y verter la salsa» (33); `arroz-y-salsa.meto` con «150 g de arroz, enjuagado al colador».

**R-H8 · emperador a 55 °C en el centro con pescado fresco** · `emperador-plancha-salmoriglio:plancha-emperador` · decide Joan
- `porque` deja el centro «a unos 55 °C, apenas rosado» y la despensa no admite pescado congelado. Por debajo de 60 °C no se garantiza inactivar el anisakis. Es un cambio de plato: pedirlo ya congelado de origen o llevar el centro a 60 °C.

**R-H9 · tacos de fiesta: el pollo puede acabar apilado** · `tacos-de-fiesta:asar-pollo`
- METE «tiras de pollo marinadas · la fuente»; la bandeja con papel y la capa única están en la SEÑAL, con el horno ya contando.
- Corrección: `t` «Asar el pollo, en bandeja con papel» (35) y `meto` «la fuente: el pollo, en una capa».

**R-H10 · boloñesa y lasaña: «Verdura a un borde» está en la SEÑAL** · `macarrones-bolonesa:carne`, `lasana-carne-berenjena:carne`
- Hay que apartarla antes de echar la carne; si no, se cuece gris sobre el sofrito.
- Corrección: `ajo.saco` «la verdura y el ajo, a un borde» y `nota` «Extendida y quieta hasta el primer aviso.».

### MEDIUM
**M1 · La pausa no se ve** · `ui/crono/TarjetaCompacta.kt:56-67`, `AccionesCarril.kt:340`. En pausa la tarjeta solo cambia «HECHO» por «SEGUIR» y pierde el botón «Pausa»: mismo color, mismo reloj, ninguna etiqueta. La franja solo dice «en pausa» si todo está parado. [Captura](../capturas/auditoria-final-carriles-pausa.png). Corrección: rótulo «EN PAUSA» junto al reloj y reloj apagado.

**M2 · «a 02:00 · …» junto a un reloj que descuenta** · `res/values/strings.xml:205`, `ui/crono/DetalleTarjeta.kt:56-66`, `TarjetaCompacta.kt:137-141`. El aviso da el minuto transcurrido y el reloj lo que queda: con «17:58» en pantalla, «a 02:00» suena cuando marque 16:00. La auditoría previa proponía «En 3:07 …». [Captura](../capturas/auditoria-final-carriles-dos-en-marcha.png). Corrección: cuenta atrás hasta el aviso.

**M3 · METE y SACA se componen distinto en la tarjeta y en la ficha** · `ui/comun/DetallePaso.kt:193-203`, `ui/ficha/FichaHtml.kt:408-414`. Tarjeta: «500 g de patata · la fuente». Detalle, ficha e impresa: «la fuente: 500 g de patata», que con la etiqueta delante se lee «METE la fuente» ([captura](../capturas/auditoria-final-ficha-pasos-mete.png)). Corrección: que los tres usen `OrdenPaso.pantalla()`.

**M4 · Formatos que siguen sueltos.** Hora del aviso: «a 02:00 · texto» (tarjeta), «02:00 texto» (detalle y ficha), «+2:00 texto» (impresa, `FichaHtml.kt:411`). Duración: «18 min» en la app, «18′» en la impresa (`strings.xml:125`) y «02:00» como reloj en la fila de un carril que aún no ha empezado (`ui/crono/FilaCarrilCompacta.kt:130-139`, [captura](../capturas/auditoria-final-4carriles-antes.png)). «fuego 4 → 5» sale en la tarjeta y en el detalle, pero no en la impresa ni en la lista del diagrama.

**M5 · El carril que te necesita no crece** · `ui/crono/CronoCarriles.kt:20-24` y `:96-103`. El reparto deja en tarjeta todo lo que cabe con su alto mínimo y reparte a partes iguales. Con 3 carriles, la tarjeta en marcha sale compacta, con el título en letra pequeña, y las que esperan lo llevan a 22 sp; con 4, un carril bloqueado ocupa un cuarto de la pantalla con hueco en blanco mientras a las tres en marcha se les corta la SEÑAL a una línea ([3 carriles](../capturas/auditoria-final-carriles-dos-en-marcha.png), [4 carriles](../capturas/auditoria-final-4carriles-tres-en-marcha.png)). Ajustes promete «los que esperan quedan en una línea». Corrección: pasar a fila los carriles en ESPERA o BLOQ mientras alguno en marcha no llegue a su alto cómodo. De paso garantiza la SEÑAL a 2 líneas antes de empezar, que quita hierro a R-H7 y R-H10.

**M6 · El «fuego a → b» del detalle no es el de la tarjeta** · `ui/comun/DetallePaso.kt:95`. El detalle mira el paso anterior del carril sin más; la tarjeta corta la cadena si hubo hueco o fuego apagado (`OrdenPaso.kt:316-344`). Difieren en 6 pasos: `lasana-carne-berenjena:roux` («7 → 4» frente a «4»), `natillas-galleta-maria:cuajar` («0 → 4»), `pizza-umami-asiatica:tostar-sesamo`, `pollo-almendras:precalentar`, `quesadillas-pollo-pimiento:sarten-lista` («0 → 5») y `tortilla-de-patatas:calentar-sarten`. Corrección: usar `OrdenPaso.delCarril` en `contextoPaso`.

**M7 · Dos carriles del mismo azul en 10 recetas** · `ui/crono/DatosCrono.kt:63-79`. MESA, que es neutra, recibe `#7FB7D6`; la olla o la cazuela de esas recetas es `#6FBEE8` (ΔE 6-8). Se confunden en el diagrama, en la franja y en las insignias ([captura](../capturas/auditoria-final-4carriles-dos-en-marcha.png)). Recetas: arroz a la cubana, arroz con leche, bunyols, crema, curry, carbonara, lasaña, macarrones, pollo con almendras y salmón glaseado. Corrección: elegir el sustituto del neutro lejos de los colores que ya usa la receta.

**M8 · La SEÑAL lleva lo que hay que hacer antes de empezar** · recetas. La SEÑAL es la tercera prioridad antes de empezar y se corta o se cae. Doce son el montaje del horno («Rejilla a media altura, calor arriba y abajo»): berenjenas, costillar, emperador, focaccia, lasaña, pastel de patata, pizza, quiche, salmón con costra, tacos de fiesta (dos) y tarta. Otras: `crema-de-calabacin:triturar` («Batidora al fondo antes de encenderla»), `pollo-almendras:zanahoria` y `ajo-jengibre`, `quesadillas-pollo-pimiento:pollo`, `salteado-pollo-tres-pimientos:s7-cebolla` y `s10-ajo-laminado`, `curry-de-pollo-con-arroz-blanco:limon`, `tortilla-de-patatas:arrancar-confitado` y `escurrir`. Corrección: el modo del horno al `t` y la rejilla al `meto`; lo demás, al `saco` del paso anterior. Ejemplo malo nuevo en `textos-del-paso.md`.

**M9 · `saco` que no es algo que sale: 31 casos** · recetas y `OrdenPaso`. 19 «apaga el fuego / el horno», 5 «vapor», 4 «el alcohol» o «su agua, evaporada», más «unos 100 ml, evaporados», «deja 1 cucharada en la sartén» y «apaga y limpia la sartén». La tarjeta dice «SACA apaga el horno», la voz «Saca apaga la olla, sin escurrir.» y el aviso «hecho · saca vapor» (comprobado con el `receta.jar`). Y al revés: en 12 carriles de fuego u horno el último paso no apaga ni lo dice (albóndigas, arroz a la cubana ×2, huevos rotos ×2, lasaña, macarrones ×2, pizza, tacos de carne, tacos de fiesta y tarta; en la lasaña lo dice la SEÑAL de `remojo` y en la tarta el `saco` de `sacar-pincelar`, dos minutos después). Corrección: escribir el estado («la olla, apagada») y quitar los que no son cosas; regla nueva de validador (elemento de `saco` que empieza por imperativo o es «vapor»). Son 31 de los 217 pasos con `saco`, el 8 % de los 407: cerca del 1 de cada 10 que ADR-0017 pone como umbral para abrir el campo `orden`.

**M10 · Cantidades que solo están en la portada, tras una «i»** · recetas. 9 pasos de preparación tienen el METE vacío (`curry-de-pollo-con-arroz-blanco:cortar`, `pollo-almendras:cortar-pollo` y `cortar-verduras`, `quesadillas-pollo-pimiento:mise-en-place`, `salteado-pollo-tres-pimientos:cortar-pollo` y `cortar-verduras`, `tacos-de-carne:mise-en-place`, `tacos-de-fiesta:cortar-base` y `wrap-cesar-pollo:mise-en-place`) y 6 «calentar» dan por hecho lo que ya está dentro (entre ellos `huevos-rotos:calentar-aceite`, 500 ml; `curry-de-pollo-con-arroz-blanco:aceite` y `agua`, 270 ml medidos; y `pollo-almendras:agua`). La portada lo cubre, pero solo enseña el título de cada casilla; la cantidad está tras su «i» y desde la cocina no se vuelve a la lista sin salir a la ficha. Corrección: METE completo en los pasos de preparación (la tarjeta lo corta a 2 líneas y el resto está en la «i») y acceso a la lista desde el menú ⋮.

**M11 · Señales de 73 a 80 caracteres cortadas aunque sobre sitio** · `ui/crono/PilaPrioritaria.kt:42`. `LINEAS_BLOQUE = 2` y a 18 sp caben unos 36 caracteres por línea: 43 de las 399 señales pasan de 72. En la tarjeta de 2 carriles se ve «…Láminas de 3 mm, parej…» con media tarjeta en blanco ([captura](../capturas/auditoria-final-2carriles-dos-en-marcha.png)). Corrección: 3 líneas para la SEÑAL si hay alto, o tope de 72.

**M12 · Al abrir la cocina antes de hora no hay ningún botón lleno** · `ui/crono/AccionesCarril.kt:142-148`. Sin nada arrancado y a más de 3 minutos de la hora del plan, la única forma de empezar es el enlace «Empezar antes» ([captura](../capturas/auditoria-final-carriles-antes-de-hora.png)). Como el plan sigue al primer arranque, ese primer paso debería llevar EMPEZAR lleno.

**M13 · METE y SACA no escalan con las raciones** · `OrdenPaso.kt:177-194`. 15 recetas admiten más de una ración. La ficha avisa («Los gramos de los pasos… son para N raciones», `strings_cocina.xml:3`); la tarjeta, que ahora es donde se leen los gramos, no. Corrección mínima: aviso en la cabecera de la cocina cuando las raciones no son las de base.

**M14 · Otras de receta.** Aceite a secas en 5 METE (`albondigas-en-salsa:aceite-salsa` y `aceite-freir`, `lasana-carne-berenjena:aceite-ragu` y `aceite-berenjena`, `tortilla-de-patatas:calentar-sarten`); la regla no mira `meto`. El origen solo cubre el primer elemento, y lo ya mezclado parece un añadido aparte, con riesgo de salar dos veces (`tacos-de-carne:guiso-tomate`, `tacos-de-fiesta:alubias` y `guiso`, `salmon-glaseado-soja-miel:agua`, `arroz-a-la-cubana-con-bacon:agua-al-fuego`). Enlaces que faltan: `espaguetis-carbonara:bacon` con `espaguetis`, `salmon-glaseado-soja-miel:sesamo` con `arroz` y `tacos-de-fiesta:bacon` con `sofrito`. El resto, en §4.

### LOW
- **L1 · El eje del diagrama vivo no cuadra con su subtítulo.** «Empezar 14:30 · servir 15:19» y la última marca dice 15:17: el eje cuenta desde el arranque real con minutos de receta (`ui/crono/DiagramaCrono.kt:87-97`). Y «servir» ahí es el «Llegas a las» de la cabecera.
- **L2 · CLÁSICA conserva lo viejo:** METO y SACO con el texto crudo, «SIN TAPA», «sugerido 14:43» y «09:00 · …» (`ui/crono/clasica/RelojClasica.kt:144-147`, [captura](../capturas/auditoria-final-clasica.png)). Se retira en la siguiente versión.
- **L3 · Código muerto:** `LineasMetoSaco` y `NotaDelPaso` (`ui/crono/DetalleTarjeta.kt:98-122`); los campos `todoJunto`, `meto`, `saco` y `rango` de `TextosFicha` (`ui/ficha/TextosFicha.kt`) con sus cadenas «Meto» y «Saco»; el parámetro `carriles` de `planTarjeta`, que ya no decide nada (`ui/crono/PlanTarjeta.kt:53-60`); `minutosPaso` duplica `OrdenPaso.duracionCorta` (`ui/crono/CronoLogica.kt:92`).
- **L4 · Impresa:** la última hora del eje, la de servir, sale cortada («15:») porque la marca se centra sobre el borde (`FichaHtml.kt:125` y `:304-309`); «sin fuego» en los 194 pasos que no son de fuego, 34 de ellos de horno (`:426`); la ficha completa deja media página 1 en blanco.
- **L5 · Relaciones:** «cuando acabe ①» no dice qué paso de ① (`ui/crono/FranjaAhora.kt:372-384`); «a la vez que ②» sigue saliendo con ② terminado; conviven tres numeraciones (paso de receta, paso del carril e insignia).
- **L6 · Letra a 1,3×:** el número de la insignia va en sp y el círculo en dp, así que el número lo llena o lo pisa (`ui/crono/InsigniaCarril.kt:33-47`); «Pausa» sigue ofreciéndose en un paso vencido; los cortes con «…» caen a media palabra («peg…», «parej…»).
- **L7 · Sin tope:** 26 de las 406 líneas de `mep` pasan de 240 caracteres (máximo 466, `tacos-pollo-pina`); `cortes.p` sigue en la columna estrecha junto al dibujo (`ui/ficha/FichaPorque.kt:44-56`).
- **L8 · Documentos desfasados:** `docs/cocina/entregables.md` describe el cronómetro de un solo paso, METO y SACO y una ficha sin horas; `docs/ESTADO.md` da por pendientes los lotes 4 a 9; el comentario de `VistaCarril.sugeridoMs` aún dice «servir − total + off».
- **L9 · Recetas, en casi todas:** `porque` repite el porqué de algún paso y 104 `p` repiten la potencia de `f`. No confunde ni cambia la cocina.
- **L10 · En marcha la tarjeta ya no enseña METE.** Es lo decidido en ADR-0017, pero hay METE de hasta 195 caracteres: o se lee entero antes de pulsar EMPEZAR o hay que abrir la «i».

### Lo que funciona
- Nada se mueve, en ninguna combinación probada: lo que no cabe se corta con «…» y el resto está en la «i».
- El estado vencido y el «toca ya» se entienden sin leer: rojo y «+00:18»; borde grueso, «ahora» y el único botón lleno.
- Adelantar el arranque mueve todas las horas y la llegada; el retraso también ([vencido](../capturas/auditoria-final-carriles-vencido.png): de 15:17 a 15:18).
- El detalle y la ficha son el mismo componente, con las horas reales del paso.
- La hoja de cocina de huevos rotos son 2 páginas y la ficha completa 6 (antes, 7). [Hoja](../capturas/auditoria-final-impresa-hoja.png), [completa](../capturas/auditoria-final-impresa-completa.png).
- Los 255 orígenes de `meto` y `saco` son recipientes o lugares: no queda ninguna condición ni reparto delante de los dos puntos.
- Los 29 cambios de enlace (27 `junto` nuevos, un `dep` nuevo y uno quitado) son razonables: ya no hay pasos que solo sigan al reloj (de 28 a 0).

## 4. Recetas
OK = se cocina bien solo con el cronómetro y lo que queda es pulido. Aquí va lo principal de cada receta; el detalle de la batida (162 hallazgos con cita literal y corrección) quedó en archivos temporales de la sesión (anexo A).

| Receta | Estado | Lo principal |
|---|---|---|
| albondigas-en-salsa | Alto | R-H6 aliños de la masa. `guisar`: la sal entra en el METE y el aviso la pide a los 10 min. `desglase`: «Fondo negro: tíralo» llega con el vino dentro. `chup-salsa`: SACA «vapor» |
| aros-de-cebolla | Medio | `montar-platos`: el destino va en el primer elemento de cada plato y los sueltos se leen con el siguiente |
| arroz-a-la-cubana-con-bacon | Medio | Sal del agua que puede ir doble; el arroz no se manda enjuagar hasta el minuto 48; sal de los huevos sin cantidad; SACA «vapor» ×2; dos sartenes sin apagar |
| arroz-con-leche | OK | «Quitar la piel» se confunde con la de la leche; canela sin gramos |
| berenjenas-rellenas-atun | Medio | Cebolla y ajo entran dos veces (`cortar.saco` y `sofrito.meto`) y el aceite cae encima; modo del horno en la SEÑAL |
| bunyols-de-les-verges | OK | `freir` no dice cuántas bolas por tanda; «1 L» no pasa a palabra en la voz |
| costillar-bbq-con-boniato | Alto | R-H4 bol de la salsa. `abrir`: el jugo se vierte al vencer, sin tiempo para desgrasar |
| crema-de-calabacin | Medio | `triturar`: «Batidora al fondo antes de encenderla» va en la SEÑAL |
| curry-de-pollo-con-arroz-blanco | Medio | `aceite` y `agua` sin METE (M10); `reducir`: SACA «unos 100 ml, evaporados»; `limon`: la espera va antes del METE |
| emperador-plancha-salmoriglio | Alto | R-H8 punto a 55 °C (decide Joan); montaje del horno en la SEÑAL |
| ensalada-de-noquis-con-atun-y-feta | OK | La cebollita crujiente entra 7 min antes del aliño |
| espaguetis-carbonara | Medio | Falta `junto` del bacon con los espaguetis; `espaguetis.saco` «apaga la olla, sin escurrir» |
| focaccia-cherry-olivas | Medio | Dónde hace el segundo levado solo está en `p`; rejilla y modo en la SEÑAL |
| hamburguesa-smash | Medio | `pochar-destapada.saco` «apaga el fuego mediano + la cebolla espera en la sartén» |
| huevos-rotos | Alto | R-H1 sartenes sin apagar; `calentar-aceite` (M10); `freir-huevos`: «la mitad de los huevos» sin número |
| lasana-carne-berenjena | Alto | R-H5 remojo; R-H10 verdura a un borde; aceite a secas ×2; SACA «vapor», «el alcohol», «su agua, evaporada»; `reposo`: «Toca el centro» con la bechamel a casi 100 °C |
| macarrones-bolonesa | Alto | R-H10; `chup1.saco` «vapor» y `chup2.saco` «la tapa», 10 min tarde; `macarrones.nota` «Un minuto menos que el paquete» contra una duración fija |
| natillas-galleta-maria | Medio | Canela sin cantidad en las tarjetas |
| pastel-de-patata-gratinado | Medio | `patatas.saco` «las patatas, en su líquido» se lee como sacarlas; modo del horno en la SEÑAL; la carne se dora sobre la cebolla |
| pechugas-empanadas | OK | — |
| pizza-umami-asiatica | Alto | R-H2 ajo y jengibre; montaje del horno en la SEÑAL |
| pollo-almendras | Alto | R-H7; `cortar-verduras` sin METE y sin el jengibre en la SEÑAL; `agua` cuelga de un `dep` con 7 min de hueco |
| quesadillas-pollo-pimiento | Bloqueante | B2 plato del pollo; `mise-en-place` sin METE; `pollo` y `tanda-2` con decisiones previas en la SEÑAL |
| quiche-bacon-hojaldre | Medio | Rejilla en la SEÑAL; «Remueve la crema antes de verterla» solo en `p` |
| salmon-costra-de-pesto | Medio | Montaje del horno en la SEÑAL; `reposar-servir`: «Reposa 2 min» sin aviso y el corte por la mitad no sale |
| salmon-glaseado-soja-miel | Medio | `agua`: la sal parece un segundo añadido; falta `junto` de la sartén con el arroz |
| salteado-pollo-tres-pimientos | Medio | `cortar-verduras` sin cantidades; el pollo pierde el rastro entre `cortar-pollo`, `marinada` y `escurrir`; nadie comprueba el punto tras devolverlo; «No laves la sartén» en la SEÑAL |
| tacos-de-carne | Medio | `mise-en-place` sin METE (el encurtido); `guiso-tomate`: especias y sal parecen añadido aparte; plancha sin apagar |
| tacos-de-fiesta | Alto | R-H9; `cortar-base` sin METE y con SACA de cinco cosas; falta `junto` del bacon; rejillas del horno en la SEÑAL |
| tacos-pollo-pina | OK | `pina` necesita las rodajas de `encurtido` y no lo declara |
| tarta-fina-de-manzana | Medio | Rejilla en la SEÑAL; el horno se apaga dos minutos después de sacar la tarta |
| tortilla-de-patatas | Medio | «Desliza sin dejar caer» y «Vuelca despacio» llegan después del gesto; aceite a secas |
| wrap-cesar-pollo | Alto | R-H3 punto del pollo; `mise-en-place` sin METE |

Resumen: 1 bloqueante, 10 con algún alto, 17 con medios y 5 OK. En la ficha, las 33 explican bien; lo que se repite es `porque` con el `p` de algún paso (L9).

## 5. Métricas antes y después
Mismo script que la auditoría previa (§6.1 de aquel informe), sobre `b4ea72d` y sobre el árbol actual. Caracteres como los ve el usuario.

| Campo | Antes: mediana / P90 / máximo | Después | Tope |
|---|---|---|---|
| `t` | 27 / 49 / 76 | 24 / 32 / 36 | 36 |
| `nota` | 110 / 197 / 449 | 55 / 73 / 80 (8 vacías) | 80 |
| `meto` (campo entero) | 61 / 125 / 304 | 36 / 100 / 195 | 36 por elemento |
| `saco` (campo entero) | 44 / 87 / 130 | 34 / 71 / 153 | 36 por elemento |
| `hitos[].t` | 50 / 77 / 113 | 36 / 42 / 44 | 44 |
| `p` | 407 / 582 / 1.275 | 362 / 497 / 600 | 600 |
| `lectura` | 587 / 945 / 1.285 | 631 / 744 / 798 | 800 |
| `descripcion` | 363 / 467 / 519 | 190 / 199 / 200 | 200 |
| `porque` (texto) | 185 / 397 / 741 (340 entradas) | 188 / 265 / 280 (296 entradas) | 280 |
| `mep.porq` | 198 / 309 / 628 | 198 / 261 / 279 | 280 |
| `cortes.p` | 311 / 391 / 492 | 250 / 277 / 341 | — |
| `mep.l[]` | 132 / 217 / 466 | sin cambios | — |

| Medida | Antes | Después |
|---|---|---|
| `p` con párrafos o listas | 0 de 407 | 392 de 407 |
| `nota` de más de 80 caracteres | 78 % | 0 |
| `nota` de más de 2 frases | 42 | 0 |
| Pasos con una frase de la `nota` casi literal en `p` | 36 % | 0 |
| Pasos de fuego con la potencia escrita en `t` o `nota` | 159 de 213 | 30 de 213 |
| `nota` que habla de la tapa / repite tiempos | 80 / 59 | 11 / 16 |
| `t` con duración escrita | 26 | 0 |
| Cantidades de `meto` repetidas en `p` | 19 % | 0 |
| Avisos de más de 40 caracteres | 63 % | 21 % (ninguno pasa de 44) |
| Pasos con `junto` (recetas) | 13 (9) | 40 (21) |
| Pasos que solo siguen al reloj | 28 | 0 |
| Prosa total de las 33 recetas | 102.176 palabras | 82.846 (−19 %) |
| Avisos de legibilidad (reglas 30-36) | 1.948 tras el piloto (`docs/ESTADO.md`) | 0, ya dentro de `--estricto` |
| Pintores del diagrama | 4 y 2 listas | 1 (`DiagramaPlan`) y su gemelo en HTML |
| Disposiciones del cronómetro | 3 | 2 (Carriles y CLÁSICA, que se retira) |
| Líneas de texto en movimiento en la tarjeta | hasta 4 a la vez | 0 |
| Botones llenos a la vez (huevos rotos) | 3 iguales | 1; 2 si dos carriles tocan a la vez y ninguno si se abre antes de hora (M12) |
| Impresa de huevos rotos | 7 páginas | hoja de cocina, 2; ficha completa, 6 |
| Cadenas sin referencia | — | 0 de 796 |
| Tests | — | 470 en `:receta` y 1.185 en `:app`, todos verdes |

Lo que no mejora: las muletillas de `p` bajan poco («en vez de» de 72 a 59, «es lo que» de 55 a 36) y los dos puntos suben de 594 a 659. 110 de los 209 pasos que arrancan con otro carril en marcha siguen sin `dep` ni `junto` (antes, 137): el orden lo da su cola.

## 6. Lo que queda para la siguiente ronda
1. **Antes de publicar:** B1 y B2, y las recetas de una línea (R-H1 a R-H4). Con la guarda `scripts/guarda-recetas.mjs` y `sc validar --estricto`.
2. **Recetas, una tanda de `chef-reviewer`:** R-H5 a R-H10 y los patrones M8, M9, M10 y M14. Son textos; solo los tres `junto` que faltan tocan enlaces. R-H8 lo decide Joan.
3. **Motor:** H1, con test primero y anexo a ADR-0018. Decidir también si la tarjeta escala las cantidades (M13).
4. **Tarjeta:** H2, M1, M2, M5, M11 y M12 van juntos: es el mismo archivo de reparto y las mismas tres tarjetas.
5. **Un solo formato:** M3, M4 y M6 se cierran haciendo que detalle, ficha e impresa pinten con `OrdenPaso`. M7, con una función de color que mire los demás carriles.
6. **Validador:** regla para `saco` que no es cosa (M9), aceite a secas en `meto` (M14) y `nota` en blanco como `null`. Si tras M9 siguen leyéndose mal más de 1 de cada 10 pasos, abrir el ADR del campo `orden`.
7. **Retirar CLÁSICA** como estaba decidido, y con ella L2 y parte de L3.
8. **Probar en el móvil** lo que esta auditoría no cubre: voz, avisos con la pantalla apagada, Doze, muerte del proceso y una cocinada real de huevos rotos (seguimiento de ADR-0018).
9. **Documentos:** `docs/cocina/entregables.md`, `docs/ESTADO.md` y la frase de las novedades sobre ir adelantado, que hoy solo es cierta a medias (H1).

## Anexo A. Método y límites
- **Hecho.** `./gradlew assembleDebug :tools:installDist` y, aparte, `:receta:test :app:testDebugUnitTest ktlintCheck detekt`, todo en verde. `scripts/sc validar --estricto` (0 errores, 0 avisos), `--legibilidad` (nada) y `scripts/sc potencias` (0 candidatos, 0 dudosos). Emulador `trombito-36` arrancado con `-no-snapshot-save` y cerrado al acabar, con la letra de vuelta a 1,0× y el tema en claro.
- **Recorrido en el emulador.** Lista con el diseño Clásica; ficha de huevos rotos en sus cinco pestañas, «Solo órdenes», diagrama completo y lectura; portada; Carriles antes de hora, arrancando antes, con un paso vencido, con el que toca ya, con dos en marcha, en pausa, a 1,3× y en oscuro; panel «i»; diagrama vivo; CLÁSICA; arroz a la cubana con 4 carriles en cuatro estados; tortilla con 2; impresión de la hoja de cocina (PDF guardado, 2 páginas) y de la ficha completa (6 páginas en la vista previa).
- **Recetas.** Un volcado por receta compuesto como lo pinta la app, repartido entre cuatro `chef-reviewer` (opus, solo lectura), con 162 hallazgos en total. Los bloqueantes y altos se han comprobado uno a uno contra el JSON y se han vuelto a pesar: los «METE vacío» de la preparación bajan a medio porque la portada los cubre, y «no se apaga» pasa de bloqueante a alto. Un barrido mecánico aparte da los recuentos de M6, M7, M8, M9, M10, M11 y H1.
- **No comprobado.** El móvil de Joan, TalkBack, la voz con micrófono (B1 está reproducido en la función, no hablando), las notificaciones y alarmas con la pantalla apagada, y el toque sobre la «i» de la tarjeta grande (H2 sale del código). El PDF no se ha podido abrir en esta máquina: las páginas se han mirado en la vista previa del sistema.
- **Horas de las capturas.** El reloj del emulador se puso a mano cerca de la hora de empezar de cada receta. En arroz a la cubana los pasos se dieron por hechos seguidos para llegar a los estados con 4 carriles: sus adelantos de 40 minutos son de la prueba; el mecanismo de H1 no depende de ellos.
- **No se ha modificado** código, `data/`, skills ni documentos ajenos. Archivos nuevos: este informe y las 30 capturas `docs/capturas/auditoria-final-*.png`.

## Anexo B. Capturas
- **Lista y ficha:** [lista](../capturas/auditoria-final-lista-huella.png), [resumen](../capturas/auditoria-final-ficha-resumen.png), [diagrama del Resumen](../capturas/auditoria-final-ficha-diagrama-resumen.png), [diagrama completo](../capturas/auditoria-final-ficha-diagrama-detalle.png), [lectura](../capturas/auditoria-final-ficha-lectura.png), [pasos](../capturas/auditoria-final-ficha-pasos.png), [METE en la ficha](../capturas/auditoria-final-ficha-pasos-mete.png), [solo órdenes](../capturas/auditoria-final-ficha-solo-ordenes.png), [preparación](../capturas/auditoria-final-ficha-preparacion.png), [¿por qué?](../capturas/auditoria-final-ficha-porque.png), [pasos a 1,3× en oscuro](../capturas/auditoria-final-ficha-pasos-letra13-oscuro.png).
- **Cocina, huevos rotos:** [portada](../capturas/auditoria-final-crono-portada.png), [antes de hora](../capturas/auditoria-final-carriles-antes-de-hora.png), [arrancando antes](../capturas/auditoria-final-carriles-adelantado.png), [vencido](../capturas/auditoria-final-carriles-vencido.png), [toca ya](../capturas/auditoria-final-carriles-toca-ya.png), [dos en marcha](../capturas/auditoria-final-carriles-dos-en-marcha.png), [pausa](../capturas/auditoria-final-carriles-pausa.png), [panel «i»](../capturas/auditoria-final-crono-detalle-i.png), [diagrama vivo](../capturas/auditoria-final-crono-diagrama-vivo.png), [1,3×](../capturas/auditoria-final-carriles-letra13.png), [oscuro](../capturas/auditoria-final-carriles-oscuro.png), [CLÁSICA](../capturas/auditoria-final-clasica.png).
- **Cuatro y dos carriles:** [arroz, antes](../capturas/auditoria-final-4carriles-antes.png), [dos en marcha](../capturas/auditoria-final-4carriles-dos-en-marcha.png), [tres en marcha](../capturas/auditoria-final-4carriles-tres-en-marcha.png), [todo hecho y la mesa esperando](../capturas/auditoria-final-4carriles-espera-con-todo-hecho.png), [tortilla, dos en marcha](../capturas/auditoria-final-2carriles-dos-en-marcha.png).
- **Papel:** [hoja de cocina](../capturas/auditoria-final-impresa-hoja.png), [ficha completa](../capturas/auditoria-final-impresa-completa.png).
