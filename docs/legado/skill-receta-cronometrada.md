---
name: receta-cronometrada
description: "Convierte cualquier petición de receta en un plan de cocina ejecutable: cantidades exactas en gramos y mililitros, cortes con dirección de fibra, potencia del fuego del 0 al 9 por recipiente, estado de la tapa, avisos cronometrados dentro de un paso, mise en place y una cronología calculada hacia atrás desde la hora de servir aprovechando el paralelismo. Escribe la receta en la app Sous Chef, que la sirve como ficha imprimible y como cronómetro con un temporizador por recipiente. Úsala SIEMPRE que alguien pida una receta con hora de servir, tiempos, potencias de vitro o inducción, cronómetro de cocina, ficha imprimible, cuadrar varios fuegos, escalar raciones, cómo organizar la mesa, o diga qué ingredientes tiene y pregunte qué hacer con ellos."
---

# Receta cronometrada

Una receta normal dice qué lleva. Esto dice **qué haces, cuándo, en qué recipiente, a qué potencia y con la tapa dónde**, con los tiempos cuadrados hacia atrás desde la hora de comer y el paralelismo aprovechado. El usuario cocina con el móvil en la encimera.

Las recetas no son archivos: son **datos** en la app **Sous Chef**, que las sirve como ficha y como cronómetro. Escribir una receta es escribir un documento en su base de datos.

**App:** `https://claude.ai/artifact/CF2HQA9yc7dSsK6VJh6SgX`

## 1. Antes de escribir nada

Pregunta solo lo que cambia el plan de verdad, con botones, no prosa. Como máximo tres preguntas, y ninguna cuya respuesta ya esté en la conversación:

- **Hora de servir** — no la preguntes para el dato en sí: la app recalcula el reloj entero desde la hora que el usuario ponga al abrir el cronómetro. Pregúntala solo si cambia el plan (una comida a las 14:00 y una cena a las 21:30 pueden pedir platos distintos).
- **Raciones** — si no lo dice, asume 2 y avísalo.
- **Equipo, solo si el plan depende de ello** — número de fuegos libres, diámetro de la sartén cuando la cantidad de carne o verdura puede no caber, horno sí o no.
- **Restricciones** que cambien ingredientes.

**No preguntes nunca qué ingredientes hay.** Están en `referencias/despensa.md` **de la carpeta Sous Chef del usuario**, que es la única despensa. Léela antes de diseñar cualquier receta.

Nunca preguntes por preferencias que puedes decidir tú y justificar. Decidir y explicar el porqué vale más que consultar.

**Antes de diseñar, mira lo que ya hay en la app.** Lee la colección `recetas` (¿existe ya este plato, o uno muy parecido que conviene corregir en vez de duplicar?) y la colección `historial` (¿qué se cocinó los últimos días?). No repitas base ni técnica dos platos seguidos.

## 1 bis. La despensa manda

`referencias/despensa.md` de la carpeta Sous Chef es la lista de lo que hay siempre en casa, con las consecuencias prácticas de lo que falta (sin cítricos, sin mantequilla, el arroz es largo y no sirve para risotto...).

**Una sola despensa, y vive en la carpeta del usuario.** Esta skill tuvo su propia copia en `references/despensa.md` y derivó en silencio: llegó a listar garbanzos, que están vetados de por vida, y a decir «pimentón» cuando en casa solo hay picante. **Ignora esa copia**; la de la carpeta manda siempre. Si la carpeta no está conectada, dilo y pide al usuario que pegue su despensa: diseñar a ciegas es cómo se cuela un ingrediente vetado.

Reglas:

- **Diseña solo con lo que está en esa lista.** Una receta que necesita algo que no hay es una receta inservible, no una receta ambiciosa.
- Si el usuario menciona algo que no está en la lista, cuenta como disponible en esa conversación.
- Los frescos perecederos pueden faltar en el momento. Si el plato se apoya entero en uno, dilo en una línea.
- **Cuando un plato saldría claramente mejor con algo que no hay**, no lo metas en la receta: dilo al final, bajo **Para la próxima compra**, en dos o tres líneas como máximo, siguiendo `references/sugerencias-compra.md`. Nada de ese archivo está en casa; se usa solo para sugerir, nunca para cocinar. Si no hay nada que aportar, la sección no aparece, que es lo normal.
- Lo que sí haga falta comprar va en el campo `compra` de la receta, y la app lo marca en el índice.

## 2. La receta

- **Solo lo necesario.** Que un ingrediente esté en la despensa no es motivo para meterlo, y la despensa es grande: casi siempre habrá cosas que encajarían y sobran igual. Por cada cosa que descartas y el usuario podría esperar, **una línea con el motivo** ("curry: el comino y la cúrcuma se comen el tomate"). Esa lista de descartes es contenido valioso, no un apéndice: va en `porque`.
- **Cantidades exactas** en gramos, mililitros, cucharadas y cucharaditas. Nada de "un chorrito" ni "al gusto". Si un ingrediente no escala linealmente (sal, especias potentes, levadura, gelificantes), márcalo con `esc:"sub"`.
- **Cortes con dirección y tamaño**, y el porqué físico: hacia dónde va la fibra, si el corte la cruza o la sigue, y qué le pasa al ingrediente en el tiempo de cocción que tiene por delante. Un dado de 3 mm y otro de 8 mm no son la misma receta. **Un ingrediente, una forma de corte y una medida**: «láminas finas y picado» no es un corte, son dos, y cocinando hay que elegir sin información.
- **Cada paso declara cinco cosas**: recipiente (`carril`), potencia (`f`), qué entra (`meto`), qué sale (`saco`) y dónde está la tapa (`tapa`).
- **La tapa es información, no un detalle.** Puesta retiene vapor; a medias evita salpicaduras y deja reducir; fuera es para evaporar y espesar. Di siempre cuándo entra y cuándo sale, y por qué en ese momento. Si la tapa cambia a mitad de un paso largo, **son dos pasos**: el momento exacto es información.
- **El porqué de cada decisión**, en una frase anclada en física o química de la cocina: evaporación, Maillard, dirección de la fibra, emulsión del almidón, punto de humo, ácido que frena el ablandamiento. Sin esto la receta es una lista más.

## 2 bis. Lo que pasa DENTRO de un paso: los hitos

Un paso de 11 minutos en el que hay que remover cada 3 no es un paso mudo. Eso son **hitos**: avisos cronometrados dentro del paso, en segundos desde que ese paso arranca.

```jsonc
"hitos": [ { "en": 180, "t": "Destapa, remueve y vuelve a tapar" },
           { "en": 360, "t": "Destapa, remueve y vuelve a tapar" } ]
```

**La frontera con partir el paso en dos, que es la decisión que importa:**

- **Cambia el montaje** —la potencia, la tapa, lo que hay dentro del recipiente— → **dos pasos**. Cada mitad tiene ajustes distintos y el cronómetro tiene que enseñarlos por separado.
- **No cambia nada, solo hay que hacer algo** —remover, dar la vuelta, rascar el fondo, echar un puñado que no altera el fuego— → **un hito**. Partirlo obligaría al usuario a pulsar *Hecho* de más y llenaría la ficha de pasos de un minuto.

Reglas: `en` en segundos, en orden, siempre menor que la duración del paso (uno que cae en el final no es un hito, es el final). El texto, un imperativo corto. No los repitas en `nota`: la app los enseña marcados en la barra de progreso, los cuenta atrás y los avisa sola.

**Ningún tramo largo a ciegas.** Un paso de fuego u horno de 6 minutos o más sin un solo hito deja el cronómetro mudo todo ese rato, y eso casi siempre es un descuido: hay algo que mirar, remover o voltear. Cuando el silencio es lo correcto —el arroz tapado, donde abrir es justo el error— se declara en el campo `sinHitos` con la frase que lo explica. Sin hitos y sin esa frase, la receta no vale.

## 3. La planificación es el núcleo

Este es el trabajo que nadie más hace bien. Hazlo así:

**a) Hacia atrás desde la hora de servir.** Coloca el último paso pegado a la hora de comer y ve retrocediendo. Pero **escribe los minutos relativos al arranque** (`off`, minuto 0 = coges el cuchillo): las horas de reloj las calcula la app. Si la receta necesita cortar algo, el corte es un paso de la MESA en el minuto 0, no una nota previa: sin él, el minuto 0 significa otra cosa en esa receta que en las demás.

**b) Busca el paralelismo activamente.** Identifica el camino crítico (la cadena más larga, casi siempre la salsa o el asado) y mete todo lo demás en sus huecos muertos. El agua no se pone al principio "por si acaso": se pone en el minuto exacto en el que, tardando lo que tarda en hervir, rompe justo cuando la necesitas. **No escribas el aviso del paralelismo en ninguna nota**: la app lo calcula desde los `off` y lo dice sola en el bloque "MIENTRAS ESTO CORRE".

**c) Cuenta los tiempos muertos como recurso.** Cortar, pesar, rallar, poner la mesa y limpiar caben dentro de un chup-chup, y también dentro de un precalentado de horno. Si un carril está esperando, algo debería estar pasando en otro.

**d) Marca la holgura de cada paso**, que la app usa para decir dónde se recupera un retraso:
- **flexible** — aguanta esperar sin estropearse (una reducción a fuego bajo, un sofrito tapado, un reposo).
- **ajustado** — se puede estirar unos minutos con coste de textura.
- **crítico** — no espera (pasta escurrida, ajo, emplatado, masas montadas).

**e) Comprueba la aritmética.** El paso que acaba más tarde tiene que terminar exactamente en `totalMin`, y algún paso tiene que arrancar en el minuto 0. Si no cuadra, corrígelo y dilo.

**f) Escalar no es multiplicar el tiempo.** Al cambiar raciones, ajusta y explica:

| Qué | Cómo se comporta al doblar |
|---|---|
| Sudar verdura | +30-40 % de tiempo: más masa en la misma superficie |
| Dorar carne | Por tandas, o sube tiempo un 60-70 %; amontonada se cuece en su vapor |
| Reducir salsa | Lo que peor escala: la superficie de evaporación no cambia. Puede ser +40 % o más |
| Hervir agua | Proporcional al volumen; tapada, un 30 % menos |
| Cocer pasta o arroz | Casi igual, si hay agua de sobra |
| Horno | Casi igual con una bandeja; dos bandejas piden rotación y algún minuto más |
| Sal y especias | Sublineales: empieza por 1,5× y corrige al final |

Si el total crece, dilo en la primera línea de la respuesta con la cifra concreta.

**g) Máximo 3 fuegos a la vez**, y avisa de las ventanas en que coinciden, en minutos del plan.

## 3 bis. La mise en place se planifica igual que los tiempos

Una cocina real tiene **una tabla de cortar, un vaso medidor y pocos boles**. Da eso por supuesto y planifica en consecuencia, sin preguntar.

**Recipientes: el mínimo, y razonado.** Un recipiente solo se justifica cuando lo que guarda entra en la olla en un **momento distinto** al del resto:

- **Lo que entra a la vez comparte recipiente.** Un bol por ingrediente es un error de planificación.
- **Lo que entra desde el principio no necesita ninguno: la olla y la sartén en frío son los mejores recipientes de preparación.** Lo que dejas ahí ya está en su destino.
- **Lo que ya viene en un recipiente se queda en él.** La carne en su bandeja, la pasta pesada en su paquete.

Cuenta los recipientes en voz alta y di cuál se reutiliza.

**Ningún ingrediente sin recipiente y sin cantidad.** Todo lo que está en `grupos` tiene que aparecer en alguna línea de `mep`, con sus gramos o mililitros y con el minuto en que entra. La portada del cronómetro se genera desde `mep`: lo que no está ahí no existe mientras cocinas, aunque figure en los ingredientes y en la cronología. Este es el error que más veces ha pasado desapercibido.

**Un solo vaso medidor: el orden lo decide el residuo.** Primero lo que no se puede rectificar, luego lo que comparte destino, y **la grasa siempre al final**, porque es la única que deja un film que obliga a lavar.

**Una sola tabla: manda la humedad, no el olor.** Lo duro y lo seco primero, porque una tabla mojada convierte el corte fino en un problema de seguridad. Lo que suelta agua después, y el ajo al final. Si hay carne cruda, va primero y la tabla se lava una vez.

**Y una regla de sentido común:** no salpimentes la carne en la mise en place. La sal saca agua y luego hay que evaporarla otra vez.

Todo esto va en `mep`, con `orden:true` cuando la secuencia es obligatoria (medidor, tabla) y `orden:false` cuando es solo una lista (recipientes). La app la usa dos veces: razonada en la ficha, y como lista marcable en la portada del cronómetro.

## 4. Potencias

Escala 0-9 de vitrocerámica por defecto. Si el usuario cocina con gas o inducción, mantén el número y añade una vez la equivalencia (gas: llama baja / media / viva; inducción responde antes y hay que bajar un punto). Referencia:

- **1-2** mantener caliente, fundir, reposos
- **3-4** chup-chup, reducciones largas, guisos
- **5-6** sofritos, sudar verdura, arrancar
- **7-8** dorar, sellar, desglasar, hervir mantenido
- **9** llevar agua a ebullición, golpe de calor

La vitro guarda calor: al bajar de potencia, la inercia dura un par de minutos. Cuando importe, dilo.

Cada cambio de potencia se dice también en el texto del paso, en esta escala. Si un paso hereda la potencia del anterior, no hace falta repetirla.

## 5. La entrega: un documento en la app, y su espejo

Tres cosas, y nada más:

**1. En el chat** — dos a cuatro frases y el gráfico de tiempos en SVG (`visualize:show_widget`, previa llamada silenciosa a `visualize:read_me` con `modules: ["diagram"]`). Ni cantidades ni pasos: están en la app. Una fila por paso, barras proporcionales, eje de minutos arriba, potencia a la derecha, un color por carril y leyenda; los cambios de tapa ya son pasos separados, así que cada uno tiene su fila.

**2. En la app** — escribe el documento `recetas/<slug>` en la base de datos del artifact, con la acción `write_db` de la herramienta de artifacts (`db_op: "set"`, `collection: "recetas"`, `doc_id: <slug>`), sobre la URL de arriba. Aparece en el móvil sin republicar nada.

**3. En el espejo, si la carpeta del proyecto está conectada** — escribe **el mismo JSON, byte a byte**, en `data/recetas/<slug>.json` de la carpeta Sous Chef, con `json.dumps(d, ensure_ascii=False, indent=2, sort_keys=True) + "\n"`.

**Los dos sitios o ninguno.** Esto no es un respaldo opcional: es la mitad del entregable. La carpeta es el espejo versionable —de ahí salen los diffs, el validador y la reconstrucción de la app— y la base de datos es lo que se cocina. Escribir solo en uno los separa en silencio, y la diferencia no se nota hasta que alguien cocina una receta que en disco dice otra cosa. Ya ha pasado: una corrección se escribió solo en la base de datos y durante días el espejo tuvo el ingrediente equivocado. Si la carpeta no está conectada, dilo en una línea al terminar, para que el espejo se ponga al día luego.

Al terminar, una línea: qué plato has escrito y dónde ha quedado.

Si la herramienta de base de datos no está disponible en la superficie donde estás, entrega el JSON como archivo y dilo claramente en una línea, para que se suba luego. No vuelvas a generar los dos HTML de antes: la ficha y el cronómetro son vistas de la app, no archivos por receta.

`references/entregables.md` describe el formato antiguo de dos HTML. Ya no se usa como mecánica de entrega, pero **sigue siendo la especificación de qué contenido lleva cada vista** (qué va en la ficha, qué va en el cronómetro, qué no se duplica). Léela con ese criterio.

### El esquema del documento

Los pasos son **una sola lista**. Antes vivían tres veces —la prosa de la ficha, la fila del gráfico y la cola del carril— y había que comprobar a mano que las tres cuadraran; ahora las tres vistas se derivan de `pasos` y el error no puede ocurrir.

```jsonc
{
  "slug": "macarrones-bolonesa",           // minúsculas, sin acentos, con guiones
  "titulo": "Macarrones con boloñesa",     // el nombre del plato, sin subtítulos
  "descripcion": "Dos o tres frases: qué es el plato y dónde está su dificultad.",
  "raciones": 4,
  "totalMin": 82,                          // el paso más tardío acaba aquí
  "datos": [["82 min", "de principio a fin"], ["12 + 70", "cortar + fuego"], ["2", "fuegos"], ["12", "pasos"]],

  "tags": { "base": "pasta", "tecnica": ["sofrito", "reduccion"], "proteina": "carne picada" },
  "perecederos": ["carne picada mixta"],   // frescos que gasta
  "compra": [],                            // [] si sale entera de la despensa

  "carriles": [                            // un recipiente o zona de trabajo
    { "id": "sarten", "nombre": "SARTÉN", "color": "#F0AE4A", "tipo": "fuego" }
    // tipo: "fuego" | "mesa" | "horno". Colores de la casa: mesa #C0B7A6,
    // sartén #F0AE4A, olla #6FBEE8, cazuela #E8836B, horno #B58CD0
  ],

  "grupos": [                              // ingredientes agrupados por recipiente
    { "n": "SARTÉN", "items": [
      { "a": 500, "u": "g", "n": "carne picada mixta" },
      { "txt": "½ cdta", "n": "pimienta negra", "esc": "sub" }
    ]}
  ],
  "notaEscala": "Qué no escala en línea recta y por qué.",

  "pasos": [
    {
      "id": "sofrito",          // único en la receta; lo usan las dependencias
      "t": "Sudar la verdura",
      "carril": "sarten",
      "off": 13,                // minuto de inicio desde el arranque
      "min": 11,                // duración en minutos
      "s": 660,                 // duración exacta en segundos (el cronómetro usa esto)
      "f": 5,                   // potencia 0-9; null si el carril no es fuego
      "tapa": "TAPA PUESTA",    // o "TAPA A MEDIAS", "tapa fuera", "sin tapa", null
      "holgura": "flexible",    // flexible | ajustado | critico
      "dep": ["cortar"],        // pasos de OTROS carriles que deben estar hechos.
                                // Nunca del mismo carril: la cola ya los ordena.
                                // Si un paso arranca a mitad de otro largo, su
                                // requisito es el paso ANTERIOR al largo.
      "junto": "macarrones",    // paso que corre en paralelo cuando el momento exacto importa
      "meto": "el bol: 160 g de cebolla + 120 g de zanahoria",  // qué entra, cuánto y de qué recipiente sale
      "saco": "",               // qué sale y adónde; el último paso siempre lo lleva
      "nota": "Una o dos frases: la señal que mirar y el error que evitar.",
      "hitos": [{ "en": 180, "t": "Destapa, remueve y vuelve a tapar" }],
      "sinHitos": "",           // solo en pasos de fuego u horno de 6 min o más SIN hitos:
                                // la frase que explica por qué el silencio es lo correcto
      "p": "Tres o cuatro frases con el porqué físico dentro. Esto es la ficha."
    }
  ],

  "lectura": "Los solapes que hay que respetar, y la suma de los tramos, que tiene que cuadrar con totalMin.",
  "mep": [{ "t": "CUATRO RECIPIENTES", "orden": false, "porq": "Por qué este reparto.",
            "l": ["<b>Bol mediano</b> · 160 g de cebolla en dados de 4 mm. Entra en el minuto 13"] }],
  "cortes": [{ "t": "Cebolla · dados de 4 mm", "p": "El porqué.", "svg": "<svg …/>" }],
  "porque": [["Título de la decisión", "El razonamiento, y aquí también los descartes"]],
  "pie": "Minutos contados desde que coges el cuchillo. Las horas de reloj están en el cronómetro.",
  "creada": "2026-09-10T00:00:00Z",
  "actualizada": "2026-09-10T00:00:00Z"
}
```

**`nota` y `p` son los dos niveles del mismo paso, y es la única duplicación admitida**: `p` argumenta (se lee antes, sentado), `nota` dicta (se mira de reojo con las manos ocupadas). Si una `nota` pasa de dos frases, sobra. Y quita de las notas los avisos de paralelismo: los da la app. En el cronómetro, `nota` se ve siempre y `p` está a un toque, en «ver el porqué»: no hace falta acortar `p` para que quepa. Un `p` de menos de 120 caracteres es un paso sin escribir.

**El esquema de cortes** (`svg` en línea, `viewBox="0 0 120 88"`, ilustrativo): la pieza con relleno tenue de su color real, la fibra en líneas finas del mismo color, los cortes en discontinuas oscuras. Si la receta corta algo, `cortes` no se queda vacío.

### Antes de escribir, comprueba

Que el plan cuadre:

1. El paso más tardío acaba exactamente en `totalMin`, y alguno arranca en el minuto 0.
2. Todo `dep` y todo `junto` apuntan a un `id` que existe, de otro carril, y que termina antes de que empiece el paso que lo declara. Sin ciclos.
3. Nunca más de 3 carriles de `tipo:"fuego"` solapados. Y dentro de un mismo carril, dos pasos no pueden solaparse nunca: un carril es un recipiente físico, y no se puede sofreír y dorar en la misma sartén a la vez.
4. `f` está entre 0 y 9, y es `null` si el carril no es fuego.
5. Todo `carril` usado por un paso está declarado, y al revés.
6. `tags.base` y `tags.tecnica` están puestos: el historial los usa para avisar de repeticiones, y las técnicas que existen como ficha se enlazan solas.
7. Los `hitos` de cada paso van en orden y ninguno llega a la duración del paso.

Y que además se pueda cocinar sin adivinar nada:

8. Todo ingrediente de `grupos` aparece en alguna línea de `mep`, con su cantidad y su minuto de entrada.
9. Ninguna línea describe dos formas de corte para el mismo ingrediente, y los cortes llevan su medida en mm o cm.
10. Ningún paso de fuego u horno de 6 minutos o más se queda sin `hitos` ni sin `sinHitos`.
11. El último paso declara `saco`: algo tiene que llegar a los platos.
12. Nada que no esté en la despensa se usa sin estar en `compra`.

Si el usuario tiene el proyecto local, `python3 tools/validar.py --estricto` hace las doce comprobaciones solo. Pásalo siempre antes de escribir, y en estricto: los avisos que ignora el modo normal (un corte sin medida, un cambio de potencia que el texto no nombra, un `p` vacío) son exactamente lo que separa una receta que valida de una receta que se puede cocinar.

## 6. Lo que la app hace sola (y por tanto no escribes)

Para no duplicar en el texto lo que ya resuelve la app:

- **Los tiempos van por reloj real.** El cronómetro guarda el instante en que acaba cada paso, no una cuenta atrás, así que aguanta el móvil bloqueado, otra app por encima o una recarga; al volver avisa de lo que venció. No escribas advertencias sobre no salir de la app.
- **Las horas de reloj las calcula ella** desde la hora de servir. La receta habla siempre en minutos relativos.
- **Los solapes los anuncia ella** desde los `off`, en el bloque "MIENTRAS ESTO CORRE".
- **El retraso y cómo recuperarlo** salen de `holgura`.
- **Los hitos** los marca en la barra, los cuenta atrás y los pita.
- **Lo cocinado se anota solo** en el historial al terminar, con los minutos reales.
- **La estrella de favorito y las raciones que el usuario elige** las guarda ella en `preferencias/<slug>`. No las escribas en la receta.

## 7. Tono

Español, de tú, directo y sin relleno. Nada de "¡delicioso plato!" ni presentaciones. Si algo puede salir mal, dilo con la señal concreta que hay que mirar ("cuando el fondo esté casi seco", "si burbujea fuerte, baja al 2"), no con un aviso genérico.