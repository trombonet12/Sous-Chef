# Los entregables

Tres, y **cada dato vive en uno solo**. La duplicación fue el primer error de diseño de esta skill: el paso a paso llegó a existir tres veces y el usuario acabó sin saber cuál mirar.

| Dato | Dónde vive | Dónde NO aparece |
|---|---|---|
| Descripción del plato, cantidades, escalado de raciones | Ficha | En ningún otro sitio |
| Gráfico de tiempos y solapes | Ficha, y el mismo SVG en el chat | El cronómetro no dibuja la cronología: la ejecuta |
| Técnica, cortes, el porqué de cada decisión | Ficha | El cronómetro solo lleva la nota corta de ejecución |
| Mise en place: recipientes, orden del medidor, orden de la tabla | Ficha razonada, y la lista marcable en la portada del cronómetro | — |
| **Los pasos explicados**, con el porqué dentro de cada uno | **Ficha** | — |
| **La línea corta de cada paso**, potencia, tapa, METO/SACO, dependencias, horas de reloj | **Cronómetro** | La ficha no da horas de reloj |

**Los pasos son la excepción deliberada a la regla de no duplicar, y son la misma lista en dos niveles de detalle.** No es un descuido, es lo que pidió el usuario: la ficha se lee antes, sentado, y explica cada paso en tres o cuatro frases con su razón física dentro; el cronómetro se mira de reojo con las manos ocupadas y da una línea por paso. Reglas:

- **Mismo número de pasos y mismos títulos en los dos sitios.** Si el cronómetro tiene catorce pasos, la ficha tiene catorce entradas y catorce filas en el gráfico. Comprueba las tres cifras antes de entregar.
- **La nota del cronómetro no es un recorte de la de la ficha.** En la ficha va el porqué; en el cronómetro solo la señal que hay que mirar y el error que hay que evitar. Si una nota del cronómetro pasa de dos frases, sobra.
- **Ninguno de los dos lleva lo del otro:** la ficha no da horas de reloj, el cronómetro no argumenta.

Otra consecuencia: **la ficha habla en minutos, el cronómetro habla en horas.** La ficha vale para cualquier hora de comida; el reloj es cosa de la app.

El usuario se apoya principalmente en el cronómetro. Preséntalo primero.

## 1. En el chat

Dos a cuatro frases y **el gráfico de tiempos** como SVG en línea (`visualize:show_widget`, previa llamada silenciosa a `visualize:read_me` con `modules: ["diagram"]`). Nada más: ni la lista de cantidades, ni los pasos, ni un resumen de la receta. Todo eso está en los archivos y repetirlo en el chat es la duplicación otra vez.

Si además hay algo que explicar de los cortes, cabe un segundo SVG, con prosa entre los dos: nunca dos widgets seguidos.

**Esquema de cortes** (`viewBox="0 0 380 H"`, ilustrativo): la pieza con relleno tenue de su color real, la fibra en líneas finas del mismo color, los cortes en discontinuas oscuras. Etiquetas a la derecha con `text-anchor="start"`.

**Esquema de cronología** (`viewBox="0 0 680 H"`): una fila por paso, barras proporcionales al tiempo, eje de minutos arriba, potencia a la derecha, un color por recipiente y leyenda. Una fila propia para cada paso, que es lo que deja ver los solapes sin colisiones. Los cambios de tapa a mitad de un paso largo se parten en dos filas, porque el momento exacto es información.

## 2. Ficha — adaptar `assets/ficha.html`

Un solo archivo. Es la hoja que se lee **antes** de cocinar y se imprime: qué lleva, cuánto dura, y por qué se hace así. No es un manual de instrucciones.

**Seis secciones y ninguna más:**

1. **Portada** — título, una descripción corta de dos o tres frases que diga qué es el plato y dónde está su dificultad, y tres o cuatro cifras clave (raciones, minutos totales, reparto entre corte y fuego, número de fuegos).
2. **Ingredientes** — agrupados por recipiente, con **control de raciones** que reescala las cantidades. Las que no escalan en línea recta (sal, especias potentes, levadura) se marcan con asterisco y suben con un exponente de 0,75. Una nota recuerda que escalar no multiplica los tiempos.
3. **El tiempo** — el gráfico, que es el corazón de la hoja: una fila por paso con su nombre, el recipiente, la potencia y el estado de la tapa en la línea pequeña, y una barra proporcional sobre un eje de minutos común. Los solapes se ven porque las barras de distintos carriles caen en las mismas columnas. Debajo, dos o tres frases señalando los solapes que hay que respetar.
4. **Antes de empezar** — la mise en place razonada, en el orden en que se ejecuta: los recipientes con lo que guarda cada uno y cuándo se vacía, lo que no necesita recipiente porque va directo a la olla o la sartén en frío, el orden de las medidas con un solo vaso medidor, y el orden de los cortes con una sola tabla. Cada bloque abre con una frase de por qué ese orden y no otro. Va en el campo `mep`, con `orden:true` cuando la secuencia es obligatoria (medidor, tabla) y `orden:false` cuando es solo una lista (recipientes).
5. **Los pasos** — la lista completa, un elemento numerado por paso, con el recipiente, los minutos, la potencia y la tapa en la línea pequeña, y tres o cuatro frases explicando qué se hace **y por qué**: la señal que hay que mirar, el error que se comete y lo que pasa físicamente. Es la sección más larga de la hoja y va en el campo `pasos`.
6. **Técnica y decisiones** — los cortes con su esquema y su porqué, y una lista de decisiones explicadas: por qué esa potencia, por qué la tapa entra ahí, por qué ese ingrediente y no otro. Aquí van también los descartes, y un esquema del montaje cuando el plato se construye en capas.

**El gráfico se dibuja en HTML y CSS, no en SVG**: barras posicionadas en porcentaje sobre una pista con rejilla. Así el texto no encoge al reducir la pantalla, que es lo que pasaría con un `viewBox` fijo en un móvil, y se imprime igual de bien.

**Aspecto:** fondo papel claro, tinta oscura, y como únicos colores saturados los de los ingredientes de esa receta. Titulares y datos en **Bricolage Grotesque**, texto corrido en **Newsreader**. Los colores de las variables `:root` se ajustan a los ingredientes reales de cada plato.

**Cómo adaptarla:** cambia el bloque `var D = {...}` del `<script>` — título, descripción, raciones, datos de portada, grupos de ingredientes, filas del gráfico con sus minutos de inicio y fin, `mep`, `pasos`, cortes con su SVG en línea, y decisiones. El resto del archivo no se toca. Los minutos son relativos al arranque, con el minuto 0 en el momento de coger el cuchillo.

Imprime en A4 con `@page` y en móvil cae a una columna.

## 3. Cronómetro — adaptar `assets/cronometro.html`

Un solo archivo, **diseñado como app de móvil**: se usa con el teléfono apoyado en la encimera, de un vistazo y con una mano manchada. Esto no es un documento con scroll, es una pantalla fija de tres franjas.

**Estructura, sin scroll:** `100dvh` en columna. Barra superior fija (hora de servir, hora actual, llegada real, aviso y pestañas de carril), panel central con **un solo paso ocupando la pantalla**, y mandos fijos abajo en la zona del pulgar. El panel es la única parte que puede desplazarse, y solo si el texto no cabe.

**Lo que lo distingue: un temporizador independiente por carril.** Un carril es un recipiente sobre el fuego (sartén, olla, cazuela) o la mesa de trabajo. Cada uno tiene su cola de pasos y su propia cuenta atrás, y **lo arranca el usuario a mano**: nada arranca solo, porque necesita poder retrasar un carril sin descuadrar los demás.

**Las pestañas son el hallazgo de la versión móvil.** Una por carril, en fila arriba, cada una con su nombre, **su reloj en vivo** y su potencia actual. Así se ven todos los fuegos a la vez sin scroll y se cambia de carril con un toque. Los relojes parados salen en gris, los pasados en rojo, y la pestaña de un carril que ha llegado a cero mientras estabas en otro parpadea (con `prefers-reduced-motion` respetado).

**El panel del paso enfocado** muestra: carril, paso i de n, hora sugerida de arranque, título grande, potencia 0-9 con barra de nueve tramos, estado de la tapa, insignia de holgura, el bloque de paralelo, cuenta atrás, barra de progreso, METO, SACO, TAPA, nota corta y qué viene después en ese mismo carril.

**El bloque "MIENTRAS ESTO CORRE" es lo que cierra el agujero del paralelismo.** No sirve de nada calcular que el horno se enciende a mitad de la carne si luego el usuario tiene que acordarse de memoria: la app se lo dice. Encima de la cuenta atrás, una lista de los **otros** carriles cuyo siguiente paso está a doce minutos o menos de su hora sugerida, ordenados por proximidad, cada uno con "ahora", "en 4 min" o "ya · Hecho" si su temporizador ya sonó, más su potencia, su duración y su hora sugerida. Cada línea lleva a ese carril de un toque. Se calcula desde los `off` del plan, así que **no hace falta escribir el aviso en ninguna nota**: quita de las notas las frases del tipo "cuando lleve diez minutos, enciende el horno" y deja que lo diga la app. Se excluyen los carriles que ya están en marcha y los bloqueados por una dependencia pendiente.

Además **pita y vibra una vez** cuando a otro carril le llega su hora, aunque estés mirando otra pestaña, con un flag por carril para no repetir. Al abrir la app y al cambiar la hora de servir hay que marcar como ya avisados los carriles que estén pasados de hora, o suenan todos de golpe.

**La portada es la mise en place.** Antes de "Abrir la cocina", la lista de `previo`: los recipientes que hay que sacar, lo que va directo a la olla en frío, el orden de las medidas y el orden de los cortes. Cada línea es una casilla que se marca al tocarla, porque es una lista de comprobación y se ejecuta una vez. Es la misma información que la sección de la ficha, sin los porqués. **El título, la intro y la lista `previo` viven en `.portada-scroll`, con su propio `overflow-y:auto`; la hora de servir y el botón "Abrir la cocina" viven en `.portada-pie`, fuera de esa zona de scroll.** `.portada-scroll` no lleva `justify-content:flex-end` bajo ningún concepto: esa combinación con `overflow-y:auto` es la causa raíz del bug (el contenido que se sale por arriba queda fuera de alcance del scroll en varios navegadores móviles). El contenido va en flujo normal, de arriba abajo, aunque eso signifique que una lista corta no quede pegada al fondo.

**Avisos:** pitido a falta de 10 segundos, tres al llegar a cero, y vibración (`navigator.vibrate`) porque en la cocina hay ruido. **No avanza solo**: al llegar a cero sigue contando en positivo (`+01:20`) hasta que el usuario pulsa. Si el carril que suena no es el que tienes delante, la pestaña se marca en rojo y la cabecera del plan lo dice.

**Dependencias, porque el usuario puede pulsar cualquier cosa.** Con un cronómetro por carril nada impide encender la sartén sin haber cortado, y desde la app eso no se ve. Cada paso declara:

- `dep`: los pasos de **otros** carriles que tienen que estar terminados antes. El orden dentro de un mismo carril ya es obligatorio por la cola, así que no se declara.
- `junto`: el paso con el que debe correr en paralelo, cuando el momento exacto importa (destapar la sartén justo cuando la pasta entra al agua).

Un paso con requisitos pendientes queda **bloqueado con salida**: el botón principal pasa a `Antes: <paso> · ir a <CARRIL>` y lleva de un toque al carril que hay que resolver, y el secundario se convierte en `Empezar igual`. Se avisa, no se prohíbe: la decisión sigue siendo del usuario. En el panel, una línea marca el requisito en rojo si falta y en verde si ya está cumplido, y otra el paso con el que va en paralelo.

**La lista de "ahora toca"**, plegable y abierta por defecto, es lo que convierte la app en un plan y no en tres relojes: una fila por carril, ordenadas por lo que hay que hacer primero — atender un aviso que ya sonó, empezar algo que toca ya, lo que está en marcha, lo que espera su hora, y al final lo bloqueado con el nombre de lo que hay que terminar antes. Cada fila lleva al carril de un toque. La cabecera de ese bloque es el desvío, así que el estado del plan y la siguiente acción ocupan un solo sitio en pantalla.

Al abrir la app el foco arranca en el primer carril accionable, nunca en uno bloqueado.

**Al construir el grafo:** que cada `dep` apunte a un id que exista, que no haya ciclos, y que las dependencias reflejen la cronología real. Si un paso arranca a mitad de otro (el agua durante el chup-chup), su requisito es el paso anterior al largo, no el largo mismo, o quedaría bloqueado hasta que el otro termine.

**Control del desvío**, siempre visible arriba: hora de servir editable, hora actual y **hora de llegada real**, recalculada como el máximo del trabajo pendiente de cada carril. Al tocar el aviso se despliega el detalle: hora prevista contra realista, **qué pasos aguantan la espera y cuáles no**, sacados de las etiquetas de holgura.

**El color, con una sola regla:** el color de cada carril es **acento, nunca texto**. Se usa para el borde superior de su pestaña, el nombre de la pestaña, el número de potencia y el relleno de la barra de progreso. Todo el texto sale de tres grises fijos (principal, apagado, tenue) sobre los fondos oscuros. En la primera versión el color del carril se aplicaba con `currentColor` al panel entero y se lo comía también el cuerpo del texto; si un color de carril es oscuro, eso deja letras casi negras sobre fondo negro. Comprueba el contraste de cada par texto/fondo antes de dar por bueno el archivo: mínimo 4,5:1 para todo el texto pequeño, incluidas las etiquetas de 10-11 px, que es donde se cuela el fallo.

**Los controles nativos también se pintan.** Un `input type=time` hereda el tema del sistema, no tu CSS, y en un fondo oscuro sale con las cifras en negro. Hace falta `color-scheme: dark` en `:root` más el `<meta name="color-scheme" content="dark">`, `appearance: none`, y color explícito en los pseudoelementos `::-webkit-datetime-edit`, `-fields-wrapper`, `-hour-field`, `-minute-field`, `-text` y `::-webkit-date-and-time-value`, con `filter: invert(1)` en `::-webkit-calendar-picker-indicator`. Dale además fondo y borde propios: así se ve que es un campo editable.

**Navegación entre carriles, tres caminos al mismo sitio:** tocar la pestaña, deslizar el panel a izquierda o derecha, y las flechas del teclado en escritorio. La pestaña activa se centra sola con `scrollIntoView`. Una línea de pista bajo el menú explica el gesto y desaparece en cuanto el usuario cambia de carril por primera vez.

**Detalles de móvil que no son opcionales:** `viewport-fit=cover` con `env(safe-area-inset-*)` arriba y abajo; `100dvh` en vez de `100vh` para que la barra del navegador no corte los mandos; inputs a 16px o más para que iOS no haga zoom al enfocarlos; `touch-action: manipulation` y `user-select: none` en todo lo pulsable; **los tres mandos en una sola fila** de unos 38 px de alto, no en dos filas altas, porque cada fila de botones que añades es una fila de contenido que el usuario deja de ver y tiene que ir a buscar con scroll; el reloj de la cuenta atrás en `clamp(40px,13vw,64px)` y no más, por el mismo motivo — con los mandos y el reloj comidos, el METO, la tapa y la nota caben en pantalla sin desplazarse, que es lo único que importa cuando tienes las manos sucias. Si un botón puede llevar texto largo (el de bloqueado), acórtalo hasta que quepa en su columna; `overscroll-behavior: none` para que un gesto de más no recargue la página y borre el estado; media query por altura (`max-height: 680px`) que reduce el reloj en pantallas cortas. No bloquees el zoom con `maximum-scale`: `touch-action` ya evita el zoom por doble toque y quitarlo del todo rompe la accesibilidad.

**Wake Lock** para mantener la pantalla encendida, y volver a pedirlo en `visibilitychange`, porque al minimizar se pierde. **Nada de localStorage.**

**Cómo adaptarlo:** cambia solo el bloque `var PLAN = {...}` del `<script>`: título, intro, hora de servir, total en minutos, `previo` con la mise en place, y un objeto por carril con sus pasos (`t`, `s` en segundos, `off` en minutos desde el arranque, `f`, `tapa`, `holgura`, `meto`, `saco`, `nota`). Da a cada carril un color propio. El resto del archivo funciona con cualquier número de carriles.

## Entrega

Escribe los dos HTML en el directorio de salida y llámalos con `present_files`. **Primero el que lleva el peso de lo que has cambiado**: el cronómetro cuando el trabajo está en la ejecución o el paralelismo, la ficha cuando está en los pasos, las cantidades o la técnica. Un archivo que no se presenta no existe para el usuario. Después, dos o tres frases de cierre: a qué hora hay que empezar, lo que hayas cambiado respecto a lo pedido, y nada más. No resumas la receta en el chat.
