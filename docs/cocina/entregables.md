# Entregables: ficha y cronómetro

> Propósito: qué contenido lleva cada pantalla de la app y qué no se duplica · Audiencia: Claude Code y quien diseñe la app · Actualizado: 2026-10-13 · Relacionado: docs/cocina/esquema-receta.md

## Resumen
Ya no hay HTML por receta. La **ficha** y el **cronómetro** son pantallas de la app Android y se derivan del mismo JSON de receta (`pasos` es una sola lista). Cada dato vive en una sola pantalla, salvo los pasos, que son la excepción deliberada: la misma lista en dos niveles de detalle. El usuario se apoya sobre todo en el cronómetro.

| Dato | Dónde vive | Dónde NO aparece |
|---|---|---|
| Descripción, cantidades, escalado de raciones | Ficha | Cronómetro |
| Gráfico de tiempos y solapes | Ficha | El cronómetro no dibuja la cronología: la ejecuta |
| Técnica, cortes, el porqué de cada decisión | Ficha | El cronómetro solo lleva la `nota` corta |
| Mise en place (recipientes, orden del medidor, orden de la tabla) | Ficha razonada (`mep`) y lista marcable en la portada del cronómetro | — |
| Pasos explicados con el porqué (`p`) | Ficha | — |
| Línea corta de cada paso: potencia, tapa, METO/SACO, dependencias, horas de reloj | Cronómetro | La ficha no da horas de reloj |

- Mismo número de pasos y mismos títulos en ambas pantallas (viene de la lista única).
- La `nota` no es un recorte de `p`: `p` argumenta, `nota` dicta (señal que mirar y error que evitar; más de dos frases sobra).
- **La ficha habla en minutos, el cronómetro en horas.** La ficha vale para cualquier hora de comida.
- En el chat solo un resumen breve de tiempos (ya no hay gráfico SVG de chat). Ni cantidades ni pasos.

## Ficha (se lee antes, sentado; imprimible)
Seis secciones y ninguna más:
1. **Portada**: título, descripción de dos o tres frases (qué es y dónde está su dificultad) y tres o cuatro cifras clave (`datos`).
2. **Ingredientes** agrupados por recipiente, con control de raciones que reescala. Los `esc:"sub"` se marcan con asterisco y suben con exponente 0,75. Nota: escalar no multiplica los tiempos (`notaEscala`).
3. **El tiempo**: gráfico con una fila por paso (nombre, recipiente, potencia y tapa en línea pequeña, barra proporcional sobre un eje de minutos común). Debajo, `lectura`: los solapes que hay que respetar.
4. **Antes de empezar**: `mep` razonado, en el orden en que se ejecuta; cada bloque abre con su `porq`.
5. **Los pasos**: uno por paso, con recipiente, minutos, potencia y tapa en la línea pequeña y su `p` (tres o cuatro frases con el porqué físico). Es la sección más larga.
6. **Técnica y decisiones**: `cortes` con su SVG y `porque` (incluye los descartes).

Aspecto: fondo papel claro, tinta oscura, como únicos colores saturados los de los ingredientes de la receta. Titulares en Bricolage Grotesque y texto corrido en Newsreader. El gráfico se dibuja con barras posicionadas en porcentaje, no con un SVG de tamaño fijo. Imprime en A4.

## Cronómetro (móvil apoyado en la encimera, manos manchadas)
Tres franjas sin scroll: barra superior fija, **un solo paso ocupando la pantalla** y mandos abajo, en la zona del pulgar.

- **Un temporizador independiente por carril**, que arranca el usuario a mano: nada arranca solo.
- **Pestañas por carril** con nombre, reloj en vivo y potencia actual. Paradas en gris, pasadas en rojo, parpadeo (respetando reducir movimiento) si un carril llega a cero mientras miras otro.
- **Panel del paso**: carril, paso i de n, hora sugerida, título, potencia 0-9 con barra de nueve tramos, tapa, holgura, bloque paralelo, cuenta atrás, progreso, METO, SACO, `nota` y qué viene después en ese carril. El `p` queda a un toque («ver el porqué»).
- **«Mientras esto corre»**: otros carriles cuyo siguiente paso está a 12 minutos o menos de su hora sugerida, ordenados por proximidad, calculado desde los `off`. Por eso las notas no llevan avisos de paralelismo. Aviso sonoro y vibración una vez cuando le llega la hora a otro carril.
- **Portada = mise en place**: lista marcable de `mep` (sin los porqués) antes de «Abrir la cocina», con hora de servir editable.
- **Avisos**: pitido a 10 s, tres al llegar a cero y vibración. No avanza solo: sigue en positivo (`+01:20`) hasta que el usuario pulsa. Los hitos se marcan en la barra, cuentan atrás y avisan.
- **Dependencias**: un paso con `dep` pendientes queda bloqueado con salida («Antes: <paso> · ir a <CARRIL>» y «Empezar igual»). Se avisa, no se prohíbe. El foco arranca en el primer carril accionable.
- **Lista «ahora toca»**: una fila por carril, ordenada por urgencia (aviso sonado, toca ya, en marcha, espera, bloqueado).
- **Desvío**: hora de servir editable, hora actual y hora de llegada real (máximo del trabajo pendiente por carril); al tocar, qué pasos aguantan la espera según `holgura`.
- **Tiempos por reloj real**: se guarda el instante en que acaba cada paso, no una cuenta atrás; aguanta bloqueo de pantalla y recargas. Pantalla siempre encendida mientras cocina.
- **Color**: el de cada carril es acento (borde de pestaña, número de potencia, relleno de progreso), nunca texto. Texto en tres grises fijos con contraste mínimo 4,5:1. Botones de 44 dp o más y reloj grande que deje METO, tapa y `nota` visibles sin desplazarse.
- Lo cocinado se anota en el historial al terminar, con los minutos reales; favorito y raciones elegidas los guarda la app, no la receta.
