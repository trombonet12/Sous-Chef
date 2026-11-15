# Textos del paso: dos textos por paso (orden compuesta)

Modelo de la Ronda 19 (`docs/decisiones.md § Ronda 19`; razonado en `docs/analisis/legibilidad-crono-y-textos.md` §6.3). Sin campos nuevos: la app compone la tarjeta con los datos. Cada dato vive en **un solo sitio**. Los caracteres se cuentan como los ve el usuario: `{N}` vale 1, `<b>` vale 0.

## Plantilla de paso

| Campo | Qué escribes | Tope |
|---|---|---|
| `t` | Etiqueta en infinitivo o nombre. Sin duración («N min», «N s») ni tapa. Lleva `{N}` solo si el paso consiste en cambiar la potencia | 36 |
| `meto` | Lo que entra: `origen: cantidad y qué`, con « + » entre elementos. Sin medida de corte (va en `mep` y en `cortes`) | 36 por elemento |
| `saco` | Lo que sale y adónde, mismo separador | 36 por elemento |
| `nota` | **La señal**: cómo sabes que está y la única corrección. Puede ir vacía. Sin porqués, sin la potencia de `f`, sin tapa, sin minutos de reloj ni paralelismo | 80 y 2 frases |
| `hitos[].t` (avisos) | Una acción, en imperativo | 44 |
| `p` | El porqué, en párrafos (línea en blanco) y listas («- »); el primer párrafo se sostiene solo. Sin repetir cantidades ni potencias de `meto` y `f` | 600 total, 240 por párrafo, mínimo 120 |

- `f`, `tapa`, `s`, `dep`, `junto` y `holgura` son datos: no se redactan en prosa.
- El array `pasos` va en orden de `off` (regla 35, ERROR). Una cola de carril se coloca por su `off`, no al final.
- `porque` de receta: solo decisiones de receta (qué se ha elegido y descartado). El porqué de cada paso vive en su `p`.

## Lo que no se escribe porque lo pinta la app
- Potencia de `f` («fuego 4», o «4 → 5» si cambia) y tapa («sin tapa», «tapada», «tapa a medias»): chips de la tarjeta.
- Duración del paso, minuto y hora de reloj, «tras N», «a la vez que N», holgura.
- Avisos de solape («Mientras esto corre») y recuperación de retrasos.
- Etiquetas METE, SEÑAL y SACA, y el origen de `meto` al final de la línea.
- Medida de corte en `meto` (ya en `mep` y `cortes`) y recipiente en `t` (`carril`).

## Ejemplos buenos

**1. Paso completo** (huevos rotos, `docs/analisis/legibilidad-crono-y-textos.md` §7.3; no cambia ninguna cantidad, tiempo ni potencia):

```json
"f": 4, "tapa": "sin tapa", "min": 18, "s": 1080,
"t": "1ª fritura (blanqueo)",
"meto": "la fuente: 500 g de patata",
"nota": "Tiernas y pálidas, sin color. Si se doran, baja al {3}.",
"hitos": [ { "en": 120, "t": "Despega del fondo las que se hayan pegado" },
           { "en": 900, "t": "Pincha un bastón: debe ceder sin resistencia" } ],
"saco": "la patata, al plato con papel",
"p": "Aquí la patata se cuece, no se dora: el almidón de la superficie gelatiniza y el centro se hace del todo. La costra llega en la segunda fritura.\n\n- Despegar a los 2 minutos: la patata cruda se pega, y la que se pega se dora ahí antes de tiempo.\n- Pinchar a los 15 minutos: es la única señal fiable, porque por fuera todas se ven igual de pálidas.\n\nFrita una sola vez sale blanda y sin costra, o dorada por fuera y cruda por dentro."
```
Por qué vale: `t` 21; `nota` 54 con la señal y una corrección (el «{3}» es otra potencia, no la de `f`); sin tapa ni minutos en la `nota`; `p` con primer párrafo autónomo y lista; los minutos 2 y 15 viven en los avisos y en `p` como razón, no como orden.

**2. «El bacon en frío»** (arroz a la cubana; ejemplo sin revisión culinaria):
- `meto`: «el plato llano: 200 g de bacon» (30; sin «tacos de 8 mm» ni «a la sartén fría»).
- `nota`: «Bacon dentro y entonces enciendes. Punto: grasa clara y chirrido, no silbido.» (77, 2 frases).

## Ejemplos malos

**3. Nota que repite lo que ya está en otro campo** (256 caracteres, la `nota` actual de «El bacon en frío»):
> «Sartén de 28 cm, el bacon dentro en frío y entonces enciendes al {6}: así la grasa sale despacio. En sartén caliente el exterior se sella y la grasa se queda dentro. Remueve cada 2 minutos. Punto: el fondo con grasa clara y los dados chirriando, no silbando.»

Falla: el recipiente es `mep`; «al {6}» es la potencia de `f`; «así la grasa…» y «en sartén caliente…» son porqué (van en `p`); «cada 2 minutos» ya son avisos. Solo la señal («Punto: …») es nota. Arreglo: el ejemplo 2.

**4. `meto` con corte y tapa, `t` con minutos, aviso largo:**
- `t`: «Dorar el jamón, 1 min por lado» (la duración es `s` o un aviso).
- `meto`: «el plato llano: 200 g de bacon en tacos de 8 mm, a la sartén fría y sin nada de aceite» (86; medida de corte, tapa y aceite que no entra).
- `nota`: «Tapada al {4} durante 5 minutos; mientras, ve rallando el queso.» (tapa, potencia de `f`, minutos y paralelismo: los cuatro los pinta la app).

## Antes de dar el paso por bueno
- `scripts/sc validar --legibilidad <slug>` sin avisos y `scripts/sc validar --estricto <slug>` a 0/0.
- Lee `t` + METE + SEÑAL + SACA seguidos, como los verá Joan: tienen que sonar a una orden.
- Si la `nota` queda vacía, es correcto: no hay nada que vigilar.

## Trampa de `meto` y `saco`: el origen
La app toma como recipiente de origen lo que va delante del primer «: » si no empieza por cifra («la fuente: 500 g de
patata» → «500 g de patata · la fuente»). No pongas ahí condiciones ni repartos: «si no espesa: 3 g de maicena» o
«por bolsa: 22 ml de soja» se leen mal en la notificación y en la voz. Escribe «3 g de maicena, solo si no espesa» o
«22 ml de salsa de soja por bolsa». Lo que haya que hacer antes de empezar el paso va en `meto` o en el paso anterior,
no en la `nota` (la señal se ve con el paso en marcha).
