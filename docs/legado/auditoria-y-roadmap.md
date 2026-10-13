# Auditoría y roadmap — Sous Chef

Última revisión: **2026-09-18**. El 17 se hizo un análisis de salud con siete agentes
(arquitectura, datos, tests, frontend, fiabilidad, documentación, y uno adversarial que
intentó tumbar los hallazgos de los demás). El 18 se arreglaron los 2 bloqueantes y los
15 importantes, y se republicó la app. Este documento recoge las dos cosas, y conserva
la revisión de completitud anterior y sus 13 decisiones de cocina.

## Estado hoy, comprobado y fechado

Ejecutado el 2026-09-18, **después** de los arreglos, sobre el espejo completo:

| | resultado |
|---|---|
| `python3 tools/validar.py --estricto` | 9/9 verde, 0 errores, 0 avisos |
| `python3 build.py` | `dist/sous-chef.html` 189 KB, `dist/dev.html` 413 KB |
| `python3 dev/verificar.py` | **161 comprobaciones, 0 fallos**, sin excepción |
| `python3 dev/verificar-recetas.py` | 9/9 |
| `python3 dev/verificar-css.py` | sin fugas |
| La app viva | cargaba el 17 con 9 recetas, 3 en historial, 15 técnicas |

Aviso de método, porque es instructivo: la primera pasada de `verificar.py` del 17 reventó
en la comprobación 25, y no era un fallo de la app — faltaba `dev/semilla-extra.json` en la
copia de trabajo. Estuve a punto de reportar una regresión que no existía. Lo que sí quedó
claro de esa pasada es el hallazgo 7: la batería moría sin imprimir el resumen.

## La app iba 466 líneas por detrás del espejo

Lo más gordo que ha salido, y no estaba en ninguna lista. Al comparar el HTML publicado
con `dist/sous-chef.html`, la versión viva resultó ser anterior a **todo** lo que la
sección «La app» de abajo daba por hecho: sin estrella de favorita, sin raciones
recordadas, sin copiar la lista de la compra, y con los dos fallos de medios minutos
(`2.5′` en la ficha y el «3 min» de un paso de 2:30) todavía dentro. Llevaba semanas sin
publicarse.

Nada se perdió al republicar: las 35 líneas que solo estaban en la app publicada eran las
versiones viejas de código que el espejo ya había reescrito, no trabajo que viviera solo
allí. Comprobado línea a línea antes de tocar nada.

**Publicada la versión 9** con ese backlog más los 17 arreglos, conservando la capacidad
`db`, así que las recetas y el historial siguen donde estaban.

## Los arreglos, aplicados el 2026-09-18

Los 17 hallazgos de abajo están corregidos. Repartidos por archivo entre cinco agentes
para que no se pisaran, y verificados después en conjunto.

| archivo | qué se hizo |
|---|---|
| `vista-crono.js` | cerrojo de una vuelta del bucle de eventos en `hecho`, `play` y `cocinado`; `aria-live` para el estado del paso y `role="alert"` en el banner de vuelta |
| `vista-crono.css` | los tres mandos de la fila baja a 44 px |
| `vista-ficha.css` | `print-color-adjust:exact` en el gráfico; variable nueva `--vf-meta` (4,94:1 en pantalla, 5,68:1 en papel) solo para la línea del fuego y la tapa; fuera el `break-inside:avoid` genérico de `section` |
| `shell.js` | el estado local no se pisa si lo guardado va más avanzado; `arranca()` una sola vez; id de historial con hora; encolar siempre que falle una escritura; la cola solo se vacía de lo confirmado; rescate de colas de versiones viejas |
| `mock-db.js` | `?fallar=N` para probar el camino de «sin conexión» sin publicar nada |
| `validar.py` | solape de dos pasos del mismo carril |
| `completitud.py` | palabra completa en vez de substring; `tags.base` obligatorio |
| `comparar.py` | `creada`/`actualizada` a ignorados, comparación de `tecnicas`, y modo `--historial` |
| `build.py` | corta si falta un marcador, y pasa `validar.py --estricto` antes de construir (`--sin-validar` lo salta) |
| `verificar.py` | `try/finally` que imprime el resumen pase lo que pase; conteos derivados de los datos |
| `README.md`, `esquema.md` | ya no prometen un espejo del historial que no existe; documentan `importada`, los dos comandos que faltaban y el ejemplo de `datos` |

### Dos cosas que solo salen arreglando

**Un bug real que el de substring tapaba.** Al exigir palabra completa en `completitud.py`,
la ensalada de ñoquis empezó a fallar: `raiz_palabra()` le quitaba «-es» a ciegas y dejaba
«tomates» en «tomat», que no es palabra. Con el match por substring colaba como prefijo de
«tomate»; con palabra completa, no. Arreglado con la regla fonotáctica real del plural
(solo se quita «-es» tras `l r n d z s x y j`), así que `limones→limon` sigue bien y
`tomates→tomate` ya también. Afectaba a cualquier ingrediente acabado en «e».

**Una regresión mía, encontrada y corregida.** El primer intento de debounce usaba una
ventana de 400 ms y estaba indexado solo por el nombre del mando, no por carril: eso
rompió 9 comprobaciones —bloqueaba el segundo toque del «¿SEGURO?» del reinicio, y
bloqueaba un «Hecho» en la sartén si venía de un «Hecho» en la mesa 300 ms antes. Ninguna
ventana de tiempo distingue el rebote de un toque de dos toques deliberados rápidos, así
que se cambió por un cerrojo que se abre en la siguiente vuelta del bucle de eventos.
`reiniciar` se quedó sin guardia: ya tenía la suya, mejor, de cuatro segundos.

### Verificación

- `python3 tools/validar.py --estricto` → 9/9, 0 errores, 0 avisos.
- `python3 dev/verificar.py` → **161 comprobaciones, 0 fallos**, sin excepción.
- El bloqueante del doble toque, reproducido a mano antes y después: dos clicks en la
  misma vuelta de JS pasaban de «1 de 3» a «3 de 3»; ahora avanzan un solo paso. Dos
  clicks deliberados separados 200 ms siguen contando los dos.
- Lo que **no** está verificado: que la app viva se vea bien en pantalla. Lo publicado es
  exactamente el build que pasó las 161, pero la ventana estaba minimizada y no se pudo
  mirar. Ábrela una vez antes de cocinar con ella.

## Hallazgos confirmados, por gravedad

Todos corregidos el 18. Se dejan escritos porque el porqué de cada arreglo vale más que
el arreglo.

Cada uno reproducido, no supuesto. El verificador adversarial bajó cuatro
severidades de las que se propusieron y no descartó ninguno.

### Bloqueantes

**1. La despensa de la skill tiene garbanzos.**
`receta-cronometrada/references/despensa.md:19` lista garbanzos, que están vetados
de por vida, y la línea 34 dice «pimentón» a secas cuando en casa solo hay picante.
Le faltan además jamón serrano, tomillo y canela. Esa copia es anterior a la revisión
de hoy. Y es la que de verdad se lee al pedir una receta: `SKILL.md:23` manda leer la
de la skill, no `referencias/despensa.md`. Las instrucciones del proyecto afirman que
son «la misma». No lo son. Arreglo: copiar `referencias/despensa.md` encima de la de
la skill, y volver a hacerlo cada vez que cambie la despensa.

**2. Un doble toque en «Hecho · siguiente» se salta un paso entero.**
`src/vista-crono.js:1183-1213` no tiene debounce, ni `disabled`, ni bandera de
reentrada, y el botón es un nodo persistente que `pinta()` nunca reemplaza. Dos
eventos procesados en la misma vuelta de JS —un toque que rebota con el dedo mojado,
un ghost click de móvil— saltan de «paso 3 de 6» a «paso 5 de 6». El paso 4 no se
pinta, no se ve, se anota con duración de un segundo y `guarda()` lo persiste: ni
recargando se recupera. Reproducido en Chromium. Arreglo: ignorar el click si el
anterior fue hace menos de 400 ms. Tres líneas.

### Importantes

**3. Dos pestañas de la misma receta se pisan el progreso.**
`src/shell.js:858` escribe `sous-chef-estado-<slug>` en localStorage, clave compartida
por todas las pestañas del origen, con un `setItem` ciego que no mira lo que ya había.
No hay `BroadcastChannel`, ni listener de `storage`, ni id de pestaña; el único `lock`
del código es un Screen Wake Lock. Reproducido: pestaña A avanza al paso 4, la pestaña
B —que se quedó en el 3— marca cualquier cosa del mise en place, y el paso 4 desaparece.
La comparación de `ts` de la línea 899 no es el problema: solo decide el punto de
partida al abrir, y esos ISO sí se comparan bien como string.

**4. El cronómetro puede repintarse dos veces y volver atrás.**
`src/shell.js:893-901`: un `setTimeout(arranca, 1200)` y la promesa de la base de datos
llaman ambos a `arranca()`. El `clearTimeout` solo protege el caso en que gana la
promesa; si la base de datos tarda más de 1200 ms, el timeout ya disparó y se llama dos
veces. El segundo `render()` hace `limpia()` y reconstruye desde `api.estado`, que
`vista-crono.js` nunca reescribe. Replicado en Node con la lógica literal: lo que el
usuario tocó entre los dos arranques se pierde. Hace falta una base de datos lenta de
verdad, que es justo lo que hay en una cocina con wifi floja.

**5. `validar.py` no ve dos pasos del mismo carril solapados.**
`tools/validar.py:155-168` cuenta un *set de ids de carril*, no de pasos, así que dos
cosas a la vez en la misma sartén colapsan a un solo elemento. Receta de prueba con
`sarten-a` (off 2, min 5) y `sarten-b` (off 4, min 5): `✓ 4 pasos, 10 min, 1 fuegos`.
Verde sobre algo físicamente imposible. Arreglo: por carril, ordenar por `off` y
comprobar `off[i+1] >= off[i] + min[i]`. Diez líneas.

**6. El listón de completitud tiene un agujero de substring.**
`tools/completitud.py:86-89` usa `k in t`, no palabra completa: «ajo» casa con «trabajo»,
«sal» con «salsa». No es teórico. Receta de prueba con el ajo declarado en `grupos`,
citado en un paso y **sin línea de mise en place**: verde, porque otra línea decía
«bandeja de trabajo». Quitando esa única palabra, el validador sí lo caza:
`✗ «ajo» no aparece en ninguna línea del mise en place`. Es exactamente el fallo del
ajo de los tacos, colándose por la puerta de al lado. Y «salsa» aparece en todas partes.
Arreglo: `re.search(rf"\b{re.escape(k)}\b", t)`.

**7. `verificar.py` no tiene un solo `try` en 1087 líneas.**
Cualquier `wait_for_selector` que falle lanza excepción y se lleva por delante todas las
comprobaciones posteriores —incluidas las que su propio docstring llama «lo importante»:
reloj real, segundo plano, recarga a mitad de guiso— y ni siquiera imprime el resumen.
Es lo que me pasó a mí esta mañana: traceback a medias y 136 comprobaciones sin correr.
Arreglo: un `try/finally` alrededor de `main()` que imprima los fallos antes de salir.

**8. El espejo del historial está muerto.**
`data/historial.json` es `[]` y la app viva enseña 3 platos cocinados. No es un olvido:
**no existe ningún camino** que baje el historial de la app al disco. `build.py:59-79` y
`dev/verificar.py:89` solo leen; `tools/comparar.py:58-61` compara únicamente `recetas`.
El README lo llama «espejo del historial». Arreglo: o un modo `--historial` en
`comparar.py`, o quitar esa promesa del README y de `esquema.md`.

**9. Cocinar el mismo plato dos veces en un día borra la primera vez.**
`src/shell.js:928`: `var id = r.slug + '-' + fecha`, sin hora, y `anotaCocinado` hace un
`set()` completo. Tacos a mediodía y tacos por la noche: queda uno.

**10. Escrituras que se pierden sin encolar y sin avisar.**
`shell.js:105-119` solo encola si `err.code` es `unavailable` o `revoked`; cualquier otro
fallo se descarta, y ningún llamador mira el `false` que devuelve. Y `vuelcaPendientes()`
(líneas 96-101) vacía la cola *antes* de saber si el reintento fue bien, con
`esReintento=true` bloqueando el reencolado: si la conexión se cae a mitad del volcado,
ese «Ya está, a la mesa» no llega nunca y la banda dice «0 pendientes».

**11. Botones de mando de 38 px.**
`src/vista-crono.css:45` y `:415`. «Hecho», «Empezar» y «+30 s» —los que se pulsan
docenas de veces por receta, con las manos mojadas— miden 38 px; el de portada, que se
pulsa una vez, mide 52. Por debajo de los 44 que hacen falta para un toque fiable.

**12. El gráfico de la ficha no se imprime.**
`src/vista-ficha.css:231-244` no tiene `print-color-adjust`. Con «imprimir fondos»
desactivado, que es el valor por defecto de casi todos los diálogos, el gráfico de
carriles entero desaparece del papel: quedan los números y nada más. Comprobado
generando el PDF. Arreglo: `print-color-adjust:exact` en `.barra`, `.pista` y `.leyenda i`.

**13. Contraste de 3,59:1 en la línea que dice el fuego y la tapa.**
`src/vista-ficha.css:14`, `--vf-tinta-3:#857C6A`, usado en `.pasos .meta`. Por debajo del
4,5:1 mínimo, en pantalla y en papel, justo en la línea que se mira a un brazo y sin gafas.

**14. `build.py` falla en silencio en un sentido, y nunca llama al validador.**
El candado de `build.py:53-55` caza un marcador que sobra, pero no uno que falta: si se
añade un nombre a `PIEZAS` sin su comentario en `shell.html`, el `.replace()` no encuentra
nada, no protesta, y el `dist/` sale sin ese CSS o ese JS. Y nada impide construir un
`dev.html` con una receta que no pasaría `--estricto`.

**15. `comparar.py` solo mira `recetas`, y su lista de ignorados no encaja.**
`tools/comparar.py:24`: `IGNORAR` no incluye `creada` ni `actualizada`, que son los campos
de fecha reales del esquema, así que cualquier corrección legítima sale reportada como
diferencia. Y `tecnicas` no se compara nunca, teniendo el mismo riesgo de deriva.

**16. Contrato del esquema violado en silencio.**
`"importada": true` está en 4 recetas y no lo documenta `esquema.md`; y 3 de esas 4
(arroz a la cubana, curry, ensalada de ñoquis) **no tienen `creada`**, que el esquema
declara siempre presente. Hoy no lo lee nadie, lo cual es justo por qué nadie se enteró.

**17. `SKILL.md:234` promete una comprobación que no existe.**
Dice que el validador comprueba que `tags.base` y `tags.tecnica` están puestos. Ni
`validar.py` ni `completitud.py` lo hacen. Y `tags.base` sí se usa de verdad, en
`src/shell.js:400` (filtro por categoría) y `:953` (historial): una receta con ese campo
vacío pasa en verde y luego desaparece de los filtros.

### Menores

- `data/cola.json` sigue siendo un `[]` huérfano. Nada lo lee. Dos clics en el Explorador.
- Ids fósiles: el paso del vinagre de los tacos se sigue llamando `limon-fuera-fuego`
  (`tacos-de-carne.json:340`), de cuando se encurtía en limón.
- Los conteos del bloque HISTORIAL de `verificar.py` (`:282`, `:288`) están escritos a
  mano (`== 2`, `== 1`) en vez de derivados de `slugs_cocinados()`, que ya existe en el
  mismo archivo. El día que el espejo del historial funcione, se rompen solos.
- `verificar.py:306` fija `filas == 12` a mano para los macarrones;
  `verificar-recetas.py:66` comprueba lo mismo con `len(r["pasos"])`.
- El cronómetro no tiene un solo `aria-live`: para un lector de pantalla es mudo.
- `mock-db.js` nunca rechaza una escritura ni dispara el callback de error de
  `onSnapshot`, así que toda la gestión de «sin conexión» solo se prueba en la cocina.
- `arroz blanco largo` se llama «arroz de grano largo» en el arroz a la cubana y «arroz
  blanco largo» en el curry. Es el mismo bote.
- `tomate triturado` está en `perecederos` del arroz a la cubana y no en las otras dos
  recetas que usan el mismo bote. Es una lata; no es perecedero.
- `aros-de-cebolla` y `emulsion-con-huevo` no las cita ninguna receta.
- `verificar-recetas.py` y `verificar-css.py` funcionan y no están en el README.
- El ejemplo de `datos` de `esquema.md:42` no cumple el aviso A6b de `completitud.py:253`.

### Lo que no hay que tocar

El reloj de pared por `finAt` absoluto en vez de cuenta atrás, y el barrido al volver de
segundo plano: resuelven exactamente lo que dicen resolver. El `limpia()` al principio de
cada `render()`: sin fugas entre aperturas. El doble toque de cuatro segundos para
reiniciar y para borrar, con el mismo criterio en los dos sitios. El repintado por
segundo, medido: seis layouts en seis segundos, menos de un milisegundo cada uno — no hay
jank, no toques el CSS por esto. Y `validar.py` en su parte de aritmética, dependencias y
ciclos, que hace lo que promete.

## Riesgo de fondo: no hay control de versiones

La carpeta no tiene git. El README la llama «el espejo versionable» y no está versionada:
OneDrive guarda versiones de archivo, no del proyecto, y no da diffs ni permite volver a un
estado coherente. Todo lo de arriba se arregla editando archivos que hoy no tienen red de
seguridad. Un `git init` y un commit inicial es lo más barato que le puede pasar a esto, y
de paso da la fecha de referencia que a la tabla de completitud de abajo le falta.

## Revisión de completitud

Disparador: cocinando los tacos, el ajo se perdía. Estaba en los ingredientes y en la
cronología, pero **en ningún recipiente de la checklist**, y descrito con dos cortes a la
vez («láminas finas y picado»). El validador daba verde: comprobaba que el plan cuadra, no
que se pueda cocinar.

El «antes» de esta tabla es el estado del 2026-09-16, sin commit que lo ancle porque no hay
repositorio. Los números salen de la sesión de revisión de ese día.

| | antes | después |
|---|---|---|
| `p` (el porqué de cada paso), media de caracteres | 35–395 | 242–560 |
| líneas de mise en place | 2–18 | 8–19 |
| hitos (avisos dentro de un paso) | 0–8 | 1–16 |
| tramos de fuego u horno ≥6 min sin ningún aviso | 21 | 0 |
| ingredientes sin recipiente en la checklist | 31 | 0 |
| comprobaciones de la batería | 63 | 161 |

No era solo cosa de las recetas nuevas: la peor de las nueve era `arroz-a-la-cubana`, que
es de las primeras.

### Las reglas, codificadas

`tools/completitud.py`, que corre desde `tools/validar.py`. Falla si un ingrediente no
tiene recipiente en el mise en place, si su cantidad no se dice en ninguna parte que se lea
cocinando, si no está ni en la despensa ni en la compra, si una línea describe dos cortes
para lo mismo, si un paso de fuego u horno de 6 min o más no tiene avisos, o si el último
paso no saca nada a los platos. Avisa —y con `--estricto` falla— de cortes sin medida,
cambios de potencia que el texto no nombra, `p` por debajo de 120 caracteres, tapas que
cambian a mitad de paso, y tags de técnica sin ficha.

Campo `sinHitos`: la salida para un tramo largo que debe ir a ciegas a propósito.
Documentado en `esquema.md`.

Con el agujero de substring del hallazgo 6, este listón es más bajo de lo que parece.

## Las 13 decisiones de cocina, aplicadas

| | decisión |
|---|---|
| Tacos, ajo | **Láminas de 2 mm con la cebolla**, con cantidad, ficha de corte, recipiente propio y un hito a los 2 min para vigilar que no dore |
| Tarta, glaseado | El cazo pasa del minuto 19 al **44**, para que llegue templado al pincelado |
| Tarta, fondo | **15 g de pan rallado** bajo la manzana, **mezclados con 10 de los 40 g de azúcar** para que no sepa a pan |
| Tarta, horno | Las puntas de manzana **se cubren con papel en el minuto 24** de horneado, antes de irte al cazo |
| Macarrones, ajo | Se queda donde está; fuera la frase que prometía «sesenta segundos y ni uno más», y un porqué nuevo que explica que se tuesta a propósito y acaba fundido en la salsa |
| Huevos rotos | **60 s de fritura por tanda** con hitos de sacar y cascar. 35 → 34 min |
| Huevos rotos | Dos pasos nuevos que **bajan al 5** en los huecos en que la sartén estaba al 8 vacía |
| Huevos rotos | Sin sal en escamas. Y por el mismo motivo, **fuera también del arroz a la cubana** |
| Arroz a la cubana | **12 + 4** en vez de 16 + 5. Sigue durando 66 min: lo largo es la sartén, así que la cazuela solo se enciende cinco minutos más tarde |
| Arroz a la cubana | Arroz **enjuagado**, y fuera «la grasa que quedó del bacon», que no existía |
| Salteado | **2:30 reales por tanda** y el bloque de sartén recolocado. 136 → 137 min |
| Raciones | Cada receta se queda como está, justificado en `notaEscala` |
| Pimentón | **Solo hay picante.** Despensa actualizada. Ensalada adaptada (½ → ¼ cdta, y **fuera la pimienta**, que ya no aportaba un registro distinto). Tacos de 5 g de dulce a 2 g de picante. El curry ya no descarta el pimentón «por el ahumado» sino por el pique |
| Ensalada, fondo | **No se añade nada.** El plato se queda con pique y color, sin el fondo que daba el ahumado |

## La app

- **Favorito**: estrella en la lista; la receta sube al principio mande lo que mande
  «actualizada». Vive en una colección nueva `preferencias/<slug>`, no en `estado/`, porque
  el cronómetro reescribe `estado` entero y la estrella habría durado hasta la primera vez
  que cocinaras el plato.
- **Raciones recordadas** por receta, con las cantidades escaladas de verdad.
- **Chip de cuánto hace** en lenguaje natural («hace 3 semanas», «sin estrenar»),
  destacado a partir de 60 días, con la fecha exacta en el `title`.
- **Copiar la lista de la compra** desde la ficha, en texto plano y escalada a las raciones
  puestas en ese momento. Tres escalones de respaldo. De paso arregla una incoherencia
  previa: la compra en pantalla no escalaba y los ingredientes sí.
- **Pestaña de Técnicas**: existe y funciona, con buscador y filtro por categoría.
- **Dos fallos reales de medios minutos** arreglados: la ficha escribía `2.5′` y
  `min 114.5` con punto decimal, y el cronómetro anunciaba «3 min» en un paso de 2:30.
- `esquema.md` y `README.md` al día con la colección nueva.

## El espejo y la app

La causa de la deriva de los tacos era la skill, que en su sección de entrega solo mandaba
escribir en la base de datos. **Está arreglada**: la skill guardada trae la sección 5 «La
entrega: un documento en la app, y su espejo», escribe en los dos sitios o lo dice, e
incorpora el listón de completitud, `--estricto` y `sinHitos`.

**El misterio de la URL está resuelto.** El `5b09ba82-ec92-4a93-8d1c-6a3b0c115be0` que
apuntaba la skill vieja no era otra app: es el identificador del iframe interno de esta
misma, `5b09ba82-….frame.claudeusercontent.com`, comprobado leyendo el DOM de la app viva.
Nunca hubo dos aplicaciones. `SKILL.md:12` apunta ya a `claude.ai/artifact/CF2HQA9yc7dSsK6VJh6SgX`,
que es la correcta.

**La deriva de hoy no se ha podido medir.** El volcado de la colección `recetas` necesita
ejecutarse dentro del iframe de la app, y ese iframe es de otro origen: desde el documento
de arriba no se alcanza. Lo que sí se ve desde fuera coincide (9 recetas, 15 técnicas) y el
disco ya dice vinagre en los tacos, que era la divergencia conocida. Para medirla campo a
campo hay que sacar el volcado desde la consola del propio iframe y pasárselo a
`comparar.py`.

## PWA: no es viable, y está medido

Se comprobó en Chromium reproduciendo la estructura real (iframe cross-origin dentro de
claude.ai):

- El `<link rel="manifest">` inyectado desde dentro del iframe **se ignora**: el manifest
  solo se procesa en documentos de primer nivel.
- Sin `allow-same-origin`, `navigator.serviceWorker` lanza `SecurityError`; y un SW desde
  `blob:` o `data:` se rechaza. Aunque se registrara, controlaría el origen interno, no el
  documento que el usuario instalaría.
- **Ni siquiera la mitad de iOS**: Safari construye el web-clip con el `apple-touch-icon` y
  el `<title>` del documento de primer nivel, así que saldría el icono de Claude y abriría
  claude.ai.

Para tenerlo de verdad haría falta servir la app en un origen propio (GitHub Pages, Netlify,
un VPS) con `index.html` + manifest + iconos + `sw.js`. Y entonces hay un segundo problema:
los datos vienen de `window.claude.use('db')`, que solo existe dentro del runtime del
artifact. Es otro despliegue, no una edición.

## Lo que te toca a ti

1. **Borrar `data/cola.json`**: sigue siendo un `[]` huérfano y esta sesión no tiene
   permiso de borrado en tu equipo. Dos clics en el Explorador.
2. **`git init`** en la carpeta, si te convence el argumento de arriba. Ahora más que
   antes: acaban de cambiar catorce archivos de golpe y no hay forma de ver ese diff.
3. **Guardar la skill** desde la tarjeta de revisión: apunta a `referencias/despensa.md`
   de tu carpeta en vez de a su propia copia, que es la que tenía garbanzos.
4. **Abrir la app una vez** y mirar que todo está donde debe, antes de cocinar con ella.

## Lo que sigue abierto

1. **La deriva espejo↔app sigue sin medirse.** El volcado de la colección `recetas` hay
   que sacarlo desde la consola del propio iframe de la app; desde el documento de arriba
   es otro origen y no se alcanza. Con ese volcado, `comparar.py` lo canta en un minuto, y
   ahora también compara `tecnicas` y el `historial`.
2. **El criterio de «más avanzado» entre pestañas usa la suma de índices de todos los
   carriles**, no un máximo por carril: si dos pestañas avanzan en carriles distintos, la
   suma puede enmascarar que una va por delante en uno y por detrás en otro. Para dos
   pestañas de una persona es un compromiso razonable, pero no es un merge.
3. **La escritura de `estado` a la base de datos no lleva ese mismo criterio**, solo la de
   `localStorage`. Si el pisado entre pestañas también ocurre en la base de datos, queda
   fuera de lo arreglado.
4. **El espejo del historial sigue vacío** y sigue sin llenarse solo: ahora hay
   herramienta (`comparar.py --historial`) pero el volcado lo tienes que sacar tú.
5. **Salteado — primeros `off` con medios minutos** (114:30, 116:30). Hay tests dedicados
   y pasan, pero es la primera receta así.
6. **Cambiar las raciones repinta la ficha ~600 ms después**, porque la escritura de
   `preferencias` vuelve por la suscripción. Si copias la lista de la compra justo después
   de tocar «+», el aviso «Copiado» se pierde (la lista se copia bien).
7. **`sugerido()` trunca minutos fraccionarios** hacia cero, así que la hora de reloj de un
   paso a y media cae al minuto de antes o al de después según el signo. Siempre dentro del
   minuto correcto, pero conviene saberlo.
8. **Ya no hay pimentón sin pique en casa.** Cualquier receta futura que quiera color rojo
   sin picar se queda sin opción.
9. **Ensalada — sigue sin fondo.** Decidido no añadir nada; si algún día cambias de idea,
   dos lonchas de bacon antes de los ñoquis o unas gotas de soja al final del dorado lo
   recuperarían.
10. **Datos de las recetas sin tocar, a propósito.** Siguen pendientes: `creada` falta en
    arroz a la cubana, curry y ensalada de ñoquis; `importada` sobra en cuatro; el paso del
    vinagre de los tacos se sigue llamando `limon-fuera-fuego`; «arroz de grano largo» y
    «arroz blanco largo» son el mismo bote; y el tomate triturado está en `perecederos` de
    una receta y no de las otras dos que usan la misma lata.
