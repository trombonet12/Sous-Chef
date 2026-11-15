# Legibilidad del cronómetro y de los textos de receta

> Propósito: auditar qué se lee y qué no en el cronómetro, la ficha y los textos de las 33 recetas, y proponer el modelo de «dos textos por paso» · Audiencia: Joan y Claude Code · Actualizado: 2026-11-15 · Relacionado: docs/cocina/esquema-receta.md, docs/cocina/reglas.md, docs/arquitectura.md, docs/ESTADO.md

## 1. Resumen
- Las cinco quejas de Joan se confirman con pruebas (39 capturas y métricas de los 407 pasos). Dos premisas del encargo no se sostienen: los pintores del diagrama son cuatro y no seis, y la `nota` no «se ve siempre» en el cronómetro.
- Cronómetro: en APILADA, la disposición por defecto, la `nota` no sale con tres carriles ni, por 2 dp, con dos; METO/SACO solo con dos y cortados. Con cuatro carriles, o con tres y letra grande, todo pasa a una línea en movimiento: unos 16 s de mirada fija para una nota mediana. Solo el reloj tiene tamaño para leerse desde la encimera.
- Arranque: el botón EMPEZAR es idéntico en el carril que toca ya y en el que toca dentro de 28 minutos. Las horas de arranque son de reloj y no se mueven cuando vas adelantado o con retraso. `junto` y `holgura` solo existen en CLÁSICA, y la ficha no enseña ni dependencias ni minuto de inicio.
- Ficha: hoy es la versión escueta (una lista con la `nota` a dos líneas). La explicación está a un toque por paso y en un solo bloque: mediana de 407 caracteres y ni un salto de línea en 407 pasos.
- Repetición: la potencia se escribe hasta en cuatro sitios y un mismo dato se pinta con hasta cinco formatos. Hay dos paneles de paso, dos vistas de la preparación y cuatro pintores del diagrama.
- Recomendación: opción B, «orden compuesta», sin campos nuevos. La tarjeta compone METE, fuego, tapa y SACA con los datos; la `nota` queda como SEÑAL (80 caracteres); `p` pasa a párrafos y listas. Un panel de detalle, un diagrama y una disposición adaptativa.
- Piloto: `huevos-rotos` reescrita entera cumple los topes propuestos y pasa el validador actual en estricto (0 errores, 0 avisos) desde una copia fuera del repo, con un 24 % menos de prosa.
- Plan: 9 lotes y unas 16 sesiones. El primero es solo código y no toca recetas: quita la marquesina, distingue los estados de arranque y pone el texto por delante del reloj.

## 2. Hallazgos por severidad
**Cómo se ha mirado.** Build debug 1.7.19 en el AVD `trombito-36` (411 × 914 dp, tema claro), con letra a 1,0× y a 1,3×, cocinando recetas de 2, 3 y 4 carriles (tortilla de patatas, huevos rotos y arroz a la cubana). Las métricas salen de un script de solo lectura sobre los 407 pasos. Límites y método, en el anexo C. Cada hallazgo lleva esfuerzo (S, M, L) y el lote del plan (§8) que lo arregla.

### Veredicto de las cinco quejas
| Queja | Veredicto | Hallazgos |
|---|---|---|
| 1. Bloques de texto ilegibles en la ficha, el cronómetro y el resto | Confirmada en la ficha, en el panel del porqué y en la impresa. En las tarjetas del cronómetro el problema es el contrario: texto oculto, cortado o en movimiento. En el resto de la app no hay bloques: solo 6 de sus 718 cadenas pasan de 120 caracteres | H3, B1 |
| 2. Lo mismo con formatos distintos; el diagrama cambia según la pantalla | Confirmada. Matiz: los pintores del diagrama son cuatro. `ui/crono/PlanTarjeta.kt` reparte el alto de la tarjeta y `ui/bienvenida/MarcoDemo.kt` es el marco de móvil de la intro; ninguno dibuja tiempos | H4, H5 |
| 3. Nada se lee de un vistazo; el texto se mueve | Confirmada en FOCO y APILADA. CLÁSICA no mueve texto, pero enseña un carril cada vez y obliga a desplazar | B1, B2, H6 |
| 4. No se ve el orden de arranque ni qué espera a qué | Confirmada, y con una causa más honda que la pantalla: las horas no siguen el avance real | B3, H1, H8 |
| 5. La ficha debe ser la rica y el cronómetro el escueto | Confirmada: en la lista de pasos hoy es al revés | H2 |

### BLOCKER
**B1 · La orden del paso no llega a la pantalla en APILADA, la disposición por defecto** · esfuerzo M · lotes 1 y 5
- En la pantalla de prueba y con letra a 1,0×, la tarjeta no enseñó la `nota` en ningún paso. Con 3 carriles caben título, reloj, próximo aviso y «Después»; con 2 se añaden METO y SACO, a una línea y cortados: «bol grande: 500 g de patata en cuartos d…» se come «+ 5 g de sal». Qué entra, cuánto y qué mirar no está en pantalla: hay que abrir la «i».
- Causa: `planTarjeta` solo acepta nota y METO/SACO si al reloj le quedan 64 sp «cómodos» (`ui/crono/PlanTarjeta.kt:16-18` y `:57-73`). Con 782 dp para las tarjetas, a 3 carriles cae todo. A 2 carriles el cálculo da 63 sp y la nota cae por 2 dp si el paso lleva METO o SACO: entraría en una pantalla algo más alta y, con más motivo, se queda fuera en las más bajas. El reloj se queda con 52-88 sp entre dos huecos. Aunque sobre sitio, METO y SACO van a 1 línea y la nota a 2 (`ui/crono/DetalleTarjeta.kt:99-101` y `:112`).
- `docs/cocina/esquema-receta.md` § «nota y p» dice lo contrario: «En el cronómetro `nota` se ve siempre».
- Capturas: [3 carriles](../capturas/auditoria-legibilidad-apilada-4-dos-en-marcha.png), [2 carriles](../capturas/auditoria-legibilidad-apilada-2carriles-2-en-marcha.png).

**B2 · El texto que sí aparece se mueve** · esfuerzo S · lote 1
- En la tarjeta compacta (4 carriles, o 3 con letra a partir de 1,1×) el título, «Después», el próximo aviso y la nota son cada uno una línea con `basicMarquee` (`ui/crono/TarjetaCompacta.kt:109`, `:154` y `:165-175`). En FOCO se mueve el título de cada fila (`ui/crono/FilaCarrilCompacta.kt:72`).
- Va a 24 dp por segundo tras 2 s de espera (`TarjetaCompacta.kt:186-187`): caben unos 58 caracteres y pasan entre 3,5 y 3,8 por segundo, medido en tres fotogramas. Una nota mediana (110 caracteres) tarda unos 16 s en pasar entera y la de «El bacon en frío» (256), unos 54 s. Quietas se leerían en 5 y 11 s (estimación a 4 palabras por segundo). Hay cuatro líneas moviéndose a la vez.
- A 1,3× ocurre ya con 3 carriles, y se mueven hasta el título y «Después».
- Alivio hoy, sin tocar código: con «Quitar animaciones» en la accesibilidad de Android, la app pinta dos líneas con «…» en vez de la marquesina (`ui/comun/Animaciones.kt`, `TarjetaCompacta.kt:177-184`).
- Capturas: [4 carriles](../capturas/auditoria-legibilidad-apilada-4carriles-2-dos-en-marcha.png), [tres fotogramas](../capturas/auditoria-legibilidad-apilada-4carriles-marquesina-3-fotogramas.png), [filas de FOCO](../capturas/auditoria-legibilidad-foco-marquesina-3-fotogramas.png), [3 carriles a 1,3×](../capturas/auditoria-legibilidad-letra13-apilada-1-antes.png).

**B3 · Nada dice qué carril arrancar ni cuándo** · esfuerzo M · lotes 1, 5 y 6
- Tras marcar «Pelar y cortar la patata» quedan tres tarjetas con tres botones negros EMPEZAR iguales. Toca la sartén grande (11:19), luego la mesa (11:25) y la sartén pequeña no hasta las 11:41. La única pista es la hora, en gris y a 14 sp, junto a «1/7».
- El dominio ya distingue «empieza ya» de «a la hora» (`ClaseEstado.YA` y `ESPERA`, con un umbral de 3 min: `receta/src/main/kotlin/app/trombito/souschef/receta/crono/ConsultasCrono.kt:9` y `:58-66`). FOCO y APILADA no lo usan: la acción es EMPEZAR en los dos casos (`ui/crono/VistaCarril.kt:224-235`, `ui/crono/AccionesCarril.kt:107-114`).
- En FOCO la tarjeta grande solo salta al carril que toca si este está a menos de 3 minutos de su hora; si no, va al que esté en marcha o al primero de la lista (`VistaCarril.kt:255-271`, `ui/crono/CronoViewModel.kt:417-431`). En la prueba, yendo 10 minutos por delante del plan, enseñaba a lo grande el paso de las 11:25 y dejaba en una fila el de las 11:19.
- Los carriles salen siempre en el orden de la receta y nada marca prioridad. El único aviso de arranque es sonoro: «Toca empezar: …» a su hora, y no suena si el carril está bloqueado (`receta/src/main/kotlin/app/trombito/souschef/receta/crono/AvisosCrono.kt:145-152`).
- Capturas: [APILADA](../capturas/auditoria-legibilidad-apilada-3-cambio-de-paso.png), [FOCO](../capturas/auditoria-legibilidad-foco-3-cambio-de-paso.png), [antes de arrancar](../capturas/auditoria-legibilidad-apilada-1-antes.png).

### HIGH
**H1 · `dep`, `junto` y `holgura` solo están enteros en CLÁSICA y no existen en la ficha** · esfuerzo M · lotes 1, 4 y 5
- «A LA VEZ QUE…» (`junto`) y «aguanta espera» o «no espera» (`holgura`) solo se pintan en `ui/crono/clasica/PanelClasica.kt:183-187` y `:237-243`.
- `dep`: la tarjeta completa dice «Antes: Pelar y cortar la patata (MESA)»; la compacta lo pierde y deja solo «Empezar igual» (`TarjetaCompacta.kt:80`); la fila de FOCO no dice nada (`FilaCarrilCompacta.kt:100-102`).
- La ficha, en pantalla y en PDF, no enseña `dep`, `junto` ni el minuto de inicio de cada paso (`ui/ficha/FichaPasos.kt:114-166`, `ui/ficha/FichaHtml.kt:258-310`).
- Alcance: 142 pasos tienen `dep` y 13 `junto`. 209 pasos (51 %) arrancan con otro carril en marcha, y en 137 de ellos el orden solo lo da `off`.
- Capturas: [CLÁSICA](../capturas/auditoria-legibilidad-clasica-3-cambio-de-paso.png), [compacta sin motivo](../capturas/auditoria-legibilidad-apilada-4carriles-1-antes.png), [fila de FOCO sin estado](../capturas/auditoria-legibilidad-foco-1-antes.png).

**H2 · La ficha es la versión escueta** · esfuerzo M · lote 4
- La pestaña Pasos lista título, chips y la `nota` cortada a 2 líneas (`FichaPasos.kt:155-162`). La explicación está en una hoja por paso (`:171-216`): 15 toques para leer huevos rotos.
- La impresa tampoco acierta: imprime `p` en un párrafo, omite la `nota` (`FichaHtml.kt:281`) y deja los avisos en 8,5 pt gris (`:85`).
- El material existe: una receta mediana lleva 2.520 palabras de prosa (unos 12 min de lectura) y huevos rotos 2.642.
- Capturas: [lista](../capturas/auditoria-legibilidad-ficha-pasos.png), [hoja del paso](../capturas/auditoria-legibilidad-ficha-paso-hoja.png), [impresa](../capturas/auditoria-legibilidad-ficha-impresa-3.png).

**H3 · Bloques de texto sin estructura** · esfuerzo M · lotes 4 y 8
- `p`: mediana 407 caracteres, máximo 1.275; el 86 % pasa de 300 y ninguno de los 407 lleva salto de línea ni `<b>`. En pantalla son unas 10 líneas seguidas de mediana y 30 la mayor; la de «1ª fritura» ocupa 16.
- `lectura`: mediana 587 y máximo 1.285, en cursiva y en un párrafo (`ui/ficha/FichaResumen.kt:119-123`). La de huevos rotos no cabe en una pantalla.
- `descripcion`: mediana 363; 6 líneas en cursiva en el Resumen y 8 a 1,3× (`FichaResumen.kt:45-52`). `cortes.p` (mediana 311) va en una columna estrecha junto al dibujo (`ui/ficha/FichaPorque.kt:46-56`). `porque` llega a 741.
- No hay con qué pintar párrafos: todo es un `Text(textoRico(…))`, y `textoRico` solo entiende `<b>` (`ui/comun/Papel.kt:99-108`).
- El validador empuja hacia arriba: exige `p` de 120 o más (regla 21, `receta/src/main/kotlin/app/trombito/souschef/receta/Validador.kt:226-231`) y no pone ningún máximo. La norma «una nota de más de dos frases sobra» no se comprueba: unas 42 notas la incumplen y el 78 % pasa de 80 caracteres.
- La voz ya impone el tope que el texto no cumple: «por qué» corta a 300 caracteres e «info» a unos 160 (`ui/crono/voz/RespuestasVoz.kt:53-69` y `:134-136`). El 86 % de los porqués se oyen a medias.
- Capturas: [lectura](../capturas/auditoria-legibilidad-ficha-lectura.png), [descripción](../capturas/auditoria-legibilidad-ficha-resumen.png), [descripción a 1,3×](../capturas/auditoria-legibilidad-letra13-ficha-resumen.png), [corte](../capturas/auditoria-legibilidad-ficha-porque-abierto.png).

**H4 · La misma cosa, varias veces y con formatos distintos** · esfuerzo M · lotes 4, 5 y 8
- Dentro de un paso: «si cogen color, baja del 4 al 3» está en la `nota`, en el aviso de 15:00 y contada otra vez en `p`, y la pantalla las enseña seguidas. En las 33 recetas, el 36 % de los pasos tiene una frase de la nota casi literal en `p`; la potencia está en `f` y además en `t` o `nota` en 159 de los 213 pasos con fuego; la medida del corte de huevos rotos aparece 8 veces.
- Un mismo dato, hasta cinco formatos:

| Dato | Formatos en pantalla |
|---|---|
| Duración | `18′` (lista y diagrama), `18 MIN` (hoja del paso), `18 min` (panel del porqué y CLÁSICA), `18:00` («Después» de la tarjeta), `min 11-29` (diagrama) |
| Potencia | llama y número (chip), `fuego 4` (chip de papel, diagrama, «Después»), `FUEGO 4` (hoja), `Potencia 4` (panel), número grande con barra (CLÁSICA) |
| Tapa | 4 valores en los datos (`sin tapa`, `tapa fuera`, `TAPA PUESTA`, `TAPA A MEDIAS`), 3 chips (`destapada`, `tapada`, `tapa a medias`), `SIN TAPA` (hoja, diagrama y CLÁSICA) |
| Aviso | `+2:00 texto` (ficha), `a 02:00 · texto` (tarjeta), `02:00 · texto` (panel y CLÁSICA) |
| Entra y sale | `METO` y `SACO` (cocina), `Entra:` y `Sale:` (ficha), `Meto:` y `Saco:` (impresa) |

- Componentes duplicados: la hoja del paso (`FichaPasos.kt:171-216`) y el panel del porqué (`ui/crono/CronoPorque.kt:36-99`) enseñan lo mismo en distinto orden. La preparación sale como plegable en la ficha (`ui/ficha/FichaPreparacion.kt`) y como checklist en la portada (`ui/crono/portada/ChecklistAntes.kt`). Hay dos barras de progreso (`DetalleTarjeta.kt:70-94`, `ui/crono/clasica/RelojClasica.kt:88-109`). La impresa repite los ingredientes, juntos y por zona (`FichaHtml.kt:152-157`).
- Capturas: [aviso y nota repetidos](../capturas/auditoria-legibilidad-clasica-4b-sarten-abajo.png), [panel del porqué](../capturas/auditoria-legibilidad-crono-porque.png), [impresa](../capturas/auditoria-legibilidad-ficha-impresa-1.png).

**H5 · El diagrama de tiempos tiene cuatro pintores y dos listas, y no coinciden** · esfuerzo L · lote 7
- MESA es beige en la ficha y azul en la cocina (`ui/comun/Componentes.kt:32` frente a `ui/crono/DatosCrono.kt:56-72`). En el Resumen una fila es un carril y en «Ver diagrama completo» es un paso. El completo pide desplazamiento lateral ya con 47 minutos y sigue el orden del array. La línea «ahora» sale del reloj, no del avance. Inventario y propuesta, en §5.
- Capturas: [por carril](../capturas/auditoria-legibilidad-ficha-resumen-tiempo.png), [por paso en la ficha](../capturas/auditoria-legibilidad-ficha-diagrama-completo.png), [por paso en la cocina](../capturas/auditoria-legibilidad-crono-diagrama.png).

**H6 · Solo el reloj tiene tamaño para leerse desde la encimera** · esfuerzo S · lotes 1 y 5
- Reloj, 52-88 sp; título, 16-22 sp; nota, 14 sp en cursiva con serifa (`DetalleTarjeta.kt:105-115`); aviso y hora prevista, 14 sp; «Después», 12 sp gris (`ui/crono/TarjetaCarril.kt:283-290`); chips, 12 sp (`AccionesCarril.kt:176-178`).
- Estimación: 14 sp son unos 1,5 mm de alto de mayúscula, que a 70 cm subtienden unos 8 minutos de arco. Las normas de puestos con pantalla piden entre 18 y 22 (`VERIFICAR` la cifra exacta; fuentes en el anexo C). Con el móvil apoyado solo se lee el reloj.

**H7 · El panel de detalle pone la explicación antes que lo ejecutivo** · esfuerzo S · lote 1
- Orden actual: `p` (16 líneas en el ejemplo), nota, METO, SACO, TAPA, potencia y avisos (`CronoPorque.kt:63-88`). Quien abre la «i» porque la tarjeta no dice qué entra tiene que pasar todo el porqué para encontrarlo.
- Captura: [panel](../capturas/auditoria-legibilidad-crono-porque.png).

**H8 · Las horas de arranque son de reloj y no siguen al cocinero** · esfuerzo L · lote 6
- La hora de cada paso es «servir − total + `off`» (`receta/src/main/kotlin/app/trombito/souschef/receta/crono/Plano.kt:38-42`) y el aviso «Toca empezar» se programa a esa hora (`AvisosCrono.kt:145-152`). La hora de servir solo se mueve a mano y de 15 en 15 minutos (`ui/crono/CronoCocina.kt:43` y `:198-204`).
- Si vas adelantado o con retraso, las horas dejan de corresponder a los fuegos. En la prueba, con la 1ª fritura arrancada a las 11:05 (acaba a las 11:23), la sartén pequeña seguía marcando las 11:41: su aviso habría sonado 18 minutos después de sacar la patata. Con retraso pasa lo contrario: el aviso llega antes de tiempo y la sartén se calienta vacía.
- `junto` existe para esto («cuando el momento exacto importa», dice el esquema) y el motor no lo usa: solo lo lee el validador y lo pinta CLÁSICA.
- Captura: [dos en marcha y la tercera a las 11:41](../capturas/auditoria-legibilidad-apilada-4-dos-en-marcha.png).

### MEDIUM
**M1 · El botón principal invita al error** · esfuerzo S · lote 1. En APILADA y FOCO, HECHO es negro y grande desde el primer segundo de un paso de 18 minutos, igual que EMPEZAR (`AccionesCarril.kt:107-114` y `:141-146`). En CLÁSICA el primario es «Hecho · siguiente» aunque el paso no haya arrancado, y «Empezar» va de contorno (`ui/crono/clasica/MandosClasica.kt:70` y `:78-84`). Existe deshacer, pero el gesto por defecto es el equivocado. [Captura](../capturas/auditoria-legibilidad-clasica-1-antes.png).

**M2 · FOCO desperdicia el sitio que le falta al texto** · esfuerzo S · lote 9. Cerca de la mitad de la tarjeta grande está en blanco mientras METO y SACO se cortan a una línea y la nota a dos. Ya consta en `docs/ESTADO.md` § Problemas conocidos. [Captura](../capturas/auditoria-legibilidad-foco-4-dos-en-marcha.png).

**M3 · Los nombres de carril se cortan justo donde se parecen** · esfuerzo S · lote 1. «SARTÉN GR…», «SARTÉN M…», «SARTÉN G…» y «SARTÉN P…», «SART…»: en la tarjeta al aparecer Pausa y +30 s (`TarjetaCarril.kt:105-131`), en el plan de CLÁSICA (84 dp, `ui/crono/clasica/PlanClasica.kt:222-235`), en el diagrama compacto (72 dp, `ui/ficha/FichaDiagramaCompacto.kt:48` y `:98-105`) y en las filas de FOCO a 1,3×. En la notificación, un título largo se come la línea («MESA · Pelar y cortar la patata en cuartos…»). Capturas: [plan](../capturas/auditoria-legibilidad-clasica-3c-plan-desplegado.png), [FOCO a 1,3×](../capturas/auditoria-legibilidad-letra13-foco-1-antes.png), [notificación](../capturas/auditoria-legibilidad-notificacion.png).

**M4 · Cortes y ruido en la tarjeta** · esfuerzo S · lotes 1, 5 y 8. El aviso próximo va a una línea con elipsis (`DetalleTarjeta.kt:54-64`) y el 63 % de los 270 avisos pasa de 40 caracteres: «a 02:00 · Mueve con la espumadera: despega la que se ha…». «Después: Subir al 5 mientras la patata reposa · fuego 5 · 02:00» repite la potencia (`VistaCarril.kt:245-249`). «11:25 −19» no dice qué mide ni en qué unidad (`ui/crono/HoraPrevista.kt:20-58`). El chip «destapada» sale en el 78 % de los pasos que declaran tapa (168 de 215) y no aporta nada.

**M5 · La cabecera fija de la ficha ocupa el 44 % de la pantalla** · esfuerzo S · lote 4. Título, raciones, servir, «Empieza a las», «Avísame», botón y pestañas no se desplazan (`ui/ficha/FichaScreen.kt:119-128`); a 1,3× es el 46 %.

**M6 · La lista de pasos y el PDF no van por orden de reloj** · esfuerzo S · lotes 2 y 3. Siguen el array, que tiene 7 saltos atrás en 5 recetas. En huevos rotos «Calentar la sartén pequeña» (minuto 28) va detrás de «Subir al 8» (minuto 31) (`FichaPasos.kt:67`, `FichaHtml.kt:265`, `ui/ficha/FichaGantt.kt:117`).

**M7 · La ficha impresa de una receta de 47 minutos son 7 páginas** · esfuerzo M · lote 4. Ingredientes dos veces, el diagrama por pasos partido entre páginas y los pasos sin minuto de inicio. [Captura](../capturas/auditoria-legibilidad-ficha-impresa-1.png).

### LOW
**L1 · El chip «min N» de la preparación toma el primer «minuto N» de la línea** · lote 4. El jamón marca «min 12», que es cuando se rasga, y entra en la sartén en el 30 (`receta/src/main/kotlin/app/trombito/souschef/receta/LineaMep.kt:16` y `:25-30`). [Captura](../capturas/auditoria-legibilidad-ficha-preparacion.png).

**L2 · Detalles** · lote 4. `sinHitos` no se enseña en ninguna pantalla: 29 pasos lo llevan (mediana de 120 caracteres) y solo lo lee el validador. «Entra: la fuente: 500 g…» lleva dos puntos dobles. La pantalla de técnica apila hasta cinco chips «Receta: …» antes del contenido y repite el título (`ui/tecnicas/TecnicaScreen.kt:66-67` y `:107`). [Captura](../capturas/auditoria-legibilidad-tecnica-ficha.png).

### Lo que funciona
- El estado vencido: tarjeta roja, «+00:13» y botón rojo. Es lo único que se entiende sin leer ([captura](../capturas/auditoria-legibilidad-apilada-5-vencido.png)).
- La pestaña Preparación y la checklist de la portada: cabecera en negrita, minuto y detalle al tocar ([portada](../capturas/auditoria-legibilidad-crono-portada.png)).
- Ingredientes, novedades, Ajustes e historial: líneas cortas y tabulares ([ingredientes](../capturas/auditoria-legibilidad-ficha-ingredientes.png), [novedades](../capturas/auditoria-legibilidad-novedades.png), [selector](../capturas/auditoria-legibilidad-ajustes-crono.png)). Las técnicas tienen párrafos de 155 caracteres de mediana.
- CLÁSICA aguanta la letra a 1,3× sin mover nada ([captura](../capturas/auditoria-legibilidad-letra13-clasica-1-antes.png)).

## 3. Propuesta de cronómetro
### 3.1 Reglas
1. **Nada se mueve.** Si un texto no cabe, se corta en palabra con «…» y el resto está en el detalle. Los topes de §6 hacen que quepa.
2. **La tarjeta dice una cosa según el momento.** Antes de empezar, qué meto y a qué fuego. En marcha, cuánto queda, el próximo aviso y la señal. Al vencer, qué saco.
3. **Lo que es dato se pinta, no se escribe.** Potencia, tapa, duración y quién espera a quién salen de `f`, `tapa`, `s`, `dep` y `junto`.
4. **Una sola invitación a tocar.** El botón lleno aparece solo en el carril que toca ya o que ha vencido.
5. **Los carriles no cambian de sitio.** Se resalta el que toca; el orden fijo conserva la memoria del sitio y los números de la voz («del 2»).
6. **Dos tamaños.** De encimera, 18 sp o más: reloj, orden, aviso próximo y señal. De mano, 14 sp: hora, paso i/n y «Luego».

Presupuesto de mirada: en 1-2 s se leen de 4 a 8 palabras (estimación). Hoy, con las medianas de las recetas, la tarjeta en foco pide unas 60 (título, METO, SACO, nota y «Después») y la de APILADA unas 25, ninguna de ellas la instrucción. Con esta propuesta, cada momento pone arriba una línea de 6 palabras o menos y, debajo, una señal de 14 o menos.

### 3.2 La tarjeta en sus tres momentos
```
POR EMPEZAR (toca ya): borde grueso del color del carril
┌──────────────────────────────────────────┐
│ ② SARTÉN GRANDE · 3/7              11:24 │  de mano
│ 1ª fritura (blanqueo)                    │  título
│ METE   500 g de patata · la fuente       │  orden, de encimera
│ FUEGO  4 · sin tapa · 18 min             │  datos pintados
│ ( i )                      [ EMPEZAR ]   │  único botón lleno
└──────────────────────────────────────────┘

EN MARCHA
┌──────────────────────────────────────────┐
│ ② SARTÉN GRANDE · 3/7           fuego 4  │
│ 1ª fritura (blanqueo)                    │
│                 12:07                    │  reloj, lo más grande
│ ▓▓▓▓▓▓▓▓▓░░│░░░░░░░░░░░░│░░░░░░░░░░░░░░░░ │
│ En 3:07  Dales la vuelta                 │  próximo aviso
│ SEÑAL  Tiernas y pálidas, sin color.     │  la nota, quieta
│        Si se doran, baja al 3.           │
│ ( i )   Pausa   +30 s          Hecho     │  Hecho de contorno
└──────────────────────────────────────────┘

VENCIDO: fondo y borde de alerta, como hoy
┌──────────────────────────────────────────┐
│ ② SARTÉN GRANDE · 3/7             +00:13 │
│ 1ª fritura (blanqueo)                    │
│ SACA   la patata, al plato con papel     │  orden de salida
│ Luego  Subir al 5 · 2 min                │
│ ( i )   +30 s               [ HECHO ]    │  botón lleno, rojo
└──────────────────────────────────────────┘

NO TOCA TODAVÍA: una línea, sin botón lleno
│ ③ SARTÉN PEQUEÑA  Calentar la sartén   11:41 · en 22 min │
│ ② SARTÉN GRANDE   Calentar el aceite   espera a ① MESA   │
```
Para Joan cambia que la tarjeta deja de enseñar siempre lo mismo y pasa a contestar la pregunta de ese momento: qué meto, qué miro o qué saco.

### 3.3 El arranque de carriles
Los cinco estados ya existen en el dominio (`ClaseEstado`, `receta/src/main/kotlin/app/trombito/souschef/receta/crono/EstadoCrono.kt:94-106`); falta pintarlos distintos.
```
 dep sin hacer       faltan > 3 min       llega la hora       EMPEZAR          0:00
 ESPERA A ①  ─────►  A LAS 11:41  ─────►  EMPIEZA YA  ─────►  EN MARCHA  ───►  LISTO
 (BLOQ)              (ESPERA)             (YA)                (MARCHA)         (ATENDER)
 gris, una línea     gris, una línea      borde grueso        reloj grande     rojo
 «espera a ① …»      «en 22 min»          botón lleno         Hecho de         «SACA …»
 «Empezar igual»     «Empezar antes»      aviso sonoro        contorno         HECHO lleno
 en texto            en texto
```
Encima de los carriles, una franja fija con lo que toca y lo que viene, en orden de reloj:
```
┌──────────────────────────────────────────────────┐
│ AHORA  ② Calentar el aceite          [ EMPEZAR ] │
│ LUEGO  11:25  ① Rasgar el jamón y lavar el padrón│
│               a la vez que ② 1ª fritura          │
│        11:41  ③ Calentar la sartén pequeña       │
└──────────────────────────────────────────────────┘
```
- AHORA sale de `ConsultasCrono.siguienteAccionable` (`ConsultasCrono.kt:72-83`) y LUEGO de `ConsultasCrono.paralelo` (`:111-123`), que hoy solo usa CLÁSICA.
- `dep` se dice «cuando acabe ①»; `junto`, «a la vez que ②»; `holgura` crítica, «no espera».
- Con una sola línea visible cuando no hay nada pendiente y tres como mucho, ocupa entre 40 y 100 dp. Tocarla abre el diagrama (§5).

**Las horas siguen al cocinero (H8).** Para que «en 22 min» signifique algo, el plan tiene que moverse con el avance real:
- Las horas de los pasos pendientes se desplazan lo que lleve de adelanto o de retraso el paso en marcha, un dato que la app ya calcula por carril (`VistaCarril.kt:181-184` y `:201-219`). Los avisos «Toca empezar» se mueven con ellas.
- `junto` pasa a ser el ancla: un paso «a la vez que X» se calcula desde el arranque real de X, no desde el reloj.
- La cabecera deja de pedir cuentas: «Llegas 15:21, 9 min antes» en lugar de SERVIR, LLEGAS y un «−19» por carril.
- Es un cambio del motor del cronómetro: pide tests primero, revisión de arquitectura y un ADR propio. Mientras llega, la franja decide AHORA por dependencias cumplidas y, si no hay nada en marcha, por el primero de la cola; así no esconde un carril que ya puede arrancar solo porque su hora de reloj no ha llegado.

Para Joan cambia que hay un sitio fijo donde mirar qué arrancar, que el botón negro deja de aparecer en carriles a los que les faltan 20 minutos y que las horas se mueven con él.

### 3.4 Las tres disposiciones
| | FOCO | APILADA | CLÁSICA |
|---|---|---|---|
| Carriles a la vista | todos: 1 grande y filas | todos | 1; los demás, en pestañas |
| `nota` | solo en el carril en foco, 2 líneas | no se vio a 1,0× (ni con 2 ni con 3 carriles); en movimiento con 4 o con letra grande | entera, a veces bajo el pliegue |
| METO y SACO | solo en foco, 1 línea cortada | solo con 2 carriles, 1 línea cortada | enteros |
| Avisos | el próximo del foco | el próximo de cada carril, cortado | todos, enteros |
| `dep` | en foco sí; en las filas no | sí; en compacta solo «Empezar igual» | sí, con salto al carril |
| `junto` y `holgura` | no | no | sí |
| Qué toca entre carriles | solo el salto del foco, y solo a menos de 3 min de la hora | no | «AHORA TOCA» (plegado y sin ordenar por hora) y «MIENTRAS ESTO CORRE» |
| Texto en movimiento | títulos de las filas | toda la tarjeta compacta | ninguno |
| Letra a 1,3× | filas ilegibles («SART…») | 3 carriles ya en movimiento | aguanta |
| Botón principal | EMPEZAR o HECHO | EMPEZAR o HECHO | «Hecho · siguiente» siempre |
| Desplazamiento | no | no | sí |
| Código propio | 126 líneas | 26 líneas | 7 archivos y 1.217 líneas |

**Recomendación: una sola disposición, «Carriles».** Toma de APILADA el esqueleto (todos a la vista, sitio fijo), de FOCO el reparto (el carril que te necesita crece y los que esperan quedan en una línea) y de CLÁSICA la información (franja «Ahora / Luego», `junto`, `holgura`). FOCO y APILADA dejan de ser opciones porque pasan a ser los dos extremos del mismo reparto. CLÁSICA sobra cuando el panel de detalle (§3.5) enseñe lo que hoy enseña su panel; propongo mantenerla una versión más y retirarla cuando Joan haya cocinado dos recetas con la nueva. Tres disposiciones triplican cada arreglo: hoy la nota se pinta de tres maneras (`DetalleTarjeta.kt:105`, `TarjetaCompacta.kt:141`, `RelojClasica.kt:76`).

Reparto del alto, con 782 dp útiles a 1,0× (estimación a validar con un prototipo):

| Caso | Reparto |
|---|---|
| 2 carriles en marcha | 2 tarjetas de unos 388 dp: cabe todo |
| 3 carriles, 2 en marcha y 1 esperando | 2 tarjetas de unos 355 dp y 1 fila de 56 dp |
| 3 carriles en marcha | 3 tarjetas de unos 256 dp: cae «Luego» y el reloj baja a 36 sp |
| 4 carriles, 2 en marcha y 2 esperando | 2 tarjetas de unos 325 dp y 2 filas |
| Letra a 1,3× | como mucho 2 tarjetas expandidas, las de aviso más próximo; el resto, filas |

### 3.5 El detalle a un toque
Un solo componente para la ficha y la cocina, con el orden invertido respecto al panel de hoy: primero lo ejecutivo y después el porqué. Se abre con la «i» (48 dp) o tocando el título; con la voz, «Chef, por qué» lee el primer párrafo.
```
SARTÉN GRANDE · paso 3 de 7 · min 11-29 (14:54-15:12)
1ª fritura (blanqueo)

METE    500 g de patata · la fuente
FUEGO   4 · sin tapa · 18 min
AVISOS  2:00   Despega del fondo las que se hayan pegado
        9:00   Dales la vuelta: el aceite no las cubre
        15:00  Pincha un bastón: debe ceder sin resistencia
SEÑAL   Tiernas y pálidas, sin color. Si se doran, baja al 3.
SACA    la patata, al plato con papel
        ajustado · después, «Subir al 5»

POR QUÉ
Aquí la patata se cuece, no se dora: el almidón de la
superficie gelatiniza y el centro se hace del todo. La
costra llega en la segunda fritura.
 · Despegar a los 2 minutos: la patata cruda se pega, y la
   que se pega se dora ahí antes de tiempo.
 · Pinchar a los 15 minutos: es la única señal fiable.
Frita una sola vez sale blanda y sin costra, o dorada por
fuera y cruda por dentro.

Técnicas: Punto del aceite para freír · Corte en bastones
```

## 4. Propuesta de ficha
La ficha pasa a ser el sitio donde se lee todo, sentado, sin abrir hojas.

**Pasos como lectura.** Lista por orden de reloj, con cada paso desplegado:
```
min 11 · 14:54 · SARTÉN GRANDE · 18 min · fuego 4 · sin tapa
3  1ª fritura (blanqueo)                  tras 2 · ajustado
   METE    500 g de patata · la fuente
   AVISOS  2:00 Despega del fondo las que se hayan pegado
           9:00 Dales la vuelta: el aceite no las cubre
           15:00 Pincha un bastón: debe ceder sin resistencia
   SEÑAL   Tiernas y pálidas, sin color. Si se doran, baja al 3.
   SACA    la patata, al plato con papel
   Por qué
   Aquí la patata se cuece, no se dora: …
```
- Se conservan los filtros por carril y se añade «Solo órdenes», que pliega los porqués para un repaso rápido antes de encender.
- Cada paso lleva minuto y hora de inicio (la ficha ya sabe la hora de servir), «tras N» por cada `dep`, «a la vez que N» por `junto` y «no espera» si la holgura es crítica.
- La hoja por paso desaparece: es el mismo componente de §3.5, ya abierto.

**Estructura de la prosa.** Un mini formato dentro de la cadena, sin cambiar el tipo de ningún campo:
- línea en blanco: párrafo nuevo;
- línea que empieza por `- `: elemento de lista;
- `<b>` sigue valiendo, y nada más (ni Markdown ni HTML).

Lo lee un analizador puro en `:receta` (`Bloques`, con párrafos y listas) y lo pintan dos piezas: una en Compose y otra en HTML (`<p>`, `<ul>`), cada bloque con `break-inside: avoid` para el PDF. Sirve para `p`, `lectura`, `notaEscala`, `porque`, `mep.porq`, `cortes.p` y las técnicas. No rompe nada de lo que ya hay: la voz une los espacios al limpiar (`ui/crono/voz/TextoHablado.kt:23`), el validador ya corta frases por salto de línea (`Validador.kt:513`) y `sc formatear` conserva los `\n` (comprobado con el piloto).

**Contenido de `p`.** El primer párrafo es el porqué en una o dos frases y se sostiene solo: es lo que lee la voz. Después, si hace falta, el porqué de los avisos en lista y qué pasa si no se hace. `p` no repite cantidades, potencias ni el texto de los avisos.

**Resumen.** La descripción baja a 200 caracteres y va en redonda. «Cómo se reparte el tiempo» deja de ser un bloque y pasa a tres piezas: la cuenta (una línea), los solapes (una lista; las ventanas con dos o tres fuegos las puede calcular la app con `Gantt.fuegos`) y lo que gobierna el plan (dos frases). La cabecera se desplaza con el contenido y solo queda fijo el botón «Empezar a cocinar».

**Mini diagramas que no hay que escribir.**
- Perfil de fuego por carril: los escalones de `f` sobre el eje del tiempo. En huevos rotos, 4 → 5 → 8 → 7 cuenta de un vistazo lo que hoy cuentan 1.102 caracteres de `lectura`, cuatro `p` y una entrada de `porque`.
- Barra de avisos del paso: la que ya pinta la cocina, también en la ficha.
- En HTML se pintan con `<div>` posicionados, como el diagrama de hoy: sin imágenes ni dependencias, y sin tocar el ADR-0006.

**Impresa.** Dos salidas del mismo HTML: una «hoja de cocina» de 1 o 2 páginas (ingredientes una vez, diagrama por carriles con los pasos numerados y la cronología con órdenes y señales) y la «ficha completa», que añade porqués, preparación razonada y cortes. Hoy solo existe la segunda y son 7 páginas.

## 5. Diagrama unificado
### Inventario
| Pintor | Dónde se ve | Una fila es | Rótulos | Color | Orden | Particularidad |
|---|---|---|---|---|---|---|
| `DiagramaTiempos` (`ui/ficha/FichaGantt.kt:106`) | ficha → «Ver diagrama completo» (`FichaDiagramaCompacto.kt:136`) | un paso | título, duración y «CARRIL · min a-b · fuego N · TAPA» | el del JSON, con borde | el del array | 10 dp por minuto: con 47 minutos ya hay que desplazar a lo ancho |
| el mismo con otra paleta (`ui/crono/DiagramaCrono.kt:54`) | cocina → ⋮ → Diagrama de tiempos | un paso | igual | `colorDeCarril`: los neutros cambian y MESA pasa a azul; sin borde | el del array | línea «ahora» según el reloj y la hora de servir (`DiagramaCrono.kt:42-50`) |
| `DiagramaCompacto` (`FichaDiagramaCompacto.kt:57`) | ficha → Resumen | un carril | nombre del carril en 72 dp | el del JSON | carriles | cabe entero |
| `Huella` (`ui/recetas/Huella.kt:36`) | tarjeta «Clásica» de la lista (`ui/recetas/TarjetasLista.kt:209`) | un carril, 5 dp | leyenda debajo | el del JSON | carriles | sin eje; calcula sus barras aparte, con mínimo 0,02 en vez de 0,01 (`Huella.kt:31` y `:64-65`) |
| HTML (`FichaHtml.kt:179-238`) | impresa y PDF | un paso | título y sublínea a la izquierda, duración a la derecha | el del JSON, algo transparente | el del array | se parte entre páginas |
| `PlanPlegable` (`PlanClasica.kt:71`) | CLÁSICA | un carril vivo | hora · carril · paso | chip del carril | por estado, no por hora | es una lista |
| `Paralelos` (`PanelClasica.kt:248`) | CLÁSICA | un carril que pide atención en 12 minutos o menos | carril · paso · «en N min» | texto del carril | por minutos | es una lista |

### Diferencias que se ven
1. Color: MESA beige en la ficha y azul en la cocina, en la misma receta.
2. Unidad: una fila es un carril en el Resumen y un paso en el completo. Son dos dibujos, no dos zooms del mismo.
3. El completo es una lista de 15 filas de unos 60 dp con una barrita cada una: el paralelismo no se ve, porque las barras que coinciden quedan lejos unas de otras.
4. Orden del array en vez de orden de reloj (M6).
5. La línea «ahora» no marca pasos hechos ni en marcha, y desaparece si se cocina antes de la hora prevista.
6. Nombres cortados («SARTÉN GR…») y dos grafías en la misma línea («sin fuego» y «SIN TAPA»).

### Propuesta: un componente, `DiagramaPlan`, y un solo lenguaje
- **Siempre carriles en filas.** Lo que hay que ver de un vistazo es quién trabaja a la vez. Tres densidades del mismo dibujo:
  - `huella`, 5-8 dp por carril y sin texto: tarjeta de la lista y franja de la cocina;
  - `resumen`, 20-24 dp por carril, con el nombre entero y el eje: Resumen de la ficha, portada de la cocina y cabecera de la impresa;
  - `detalle`, 36-44 dp por carril, cada barra con el número de su paso y la lista numerada debajo: «completo» de la ficha, ⋮ de la cocina e impresa.
- **Reglas comunes.** Una sola función de color, la de la cocina, que distingue los neutros. `Gantt.barra` para todos. Eje en minutos con la hora real debajo cuando hay hora de servir. Orden por `off`. Cabe a lo ancho hasta 90 minutos y solo entonces se desplaza.
- **Con estado, si se le pasa.** Hecho, barra apagada. En marcha, barra llena y línea «ahora» según el avance real. Pendiente, contorno. El diagrama de la cocina es el de la ficha, vivo.
- **Relaciones.** En la lista numerada, en palabras: «tras 2», «a la vez que 3». En el dibujo, una muesca al inicio de la barra que espera.
- **Perfil de fuego** bajo cada carril de fuego (§4).

Huevos rotos en densidad `detalle` (un carácter por minuto; los números son los pasos de §7):
```
min         0         10        20        30        40     47
hora        14:43     14:53     15:03     15:13     15:23  15:30
MESA        ██████      ▒▒▒▒▒            ████       ▒▒   ██
  paso      1           4                6          12   15
S. GRANDE         █████▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒██▒▒███████▒▒███
  paso            2    3                 7 9 11     13,14
  fuego           4    4                 5 8 8      7 7
S. PEQUEÑA                              ██▒▒████
  paso                                  5 8 10
  fuego                                 6 6 8
                                ▲ ahora
```
Para Joan cambia que el plan es el mismo dibujo en la lista, en la ficha, en la cocina y en el papel, y que el tramo difícil (dos fuegos del 28 al 36) se ve sin leer.

## 6. Modelo de texto recomendado y cambios de esquema y validador
### 6.1 Lo que dicen los datos
Caracteres por campo en las 33 recetas (`{N}` cuenta 1 y las etiquetas 0):

| Campo | Mediana | P90 | Máximo | Observación |
|---|---|---|---|---|
| `t` | 27 | 49 | 76 | el 24 % pasa de 36; 45 llevan la potencia y 26 un tiempo |
| `nota` | 110 | 197 | 449 | el 78 % pasa de 80; 20 llevan avisos de paralelismo o minutos de reloj, contra el esquema |
| `meto` | 61 | 125 | 304 | el 72 % pasa de 45; 42 repiten la medida del corte; 102 ya separan con « + » y 91 anteponen «recipiente:» |
| `saco` | 44 | 87 | 130 | |
| `hitos[].t` | 50 | 77 | 113 | el 63 % pasa de 40 |
| `p` | 407 | 582 | 1.275 | el 86 % pasa de 300; ninguno con saltos |
| `lectura` | 587 | 945 | 1.285 | |
| `descripcion` | 363 | 467 | 519 | |
| `porque` (texto) | 185 | 397 | 741 | |

Repetición entre campos: en el 54 % de los pasos, la mitad o más de las palabras de contenido de la `nota` ya están en `p`; el 36 % tiene una frase casi literal. 80 notas hablan de la tapa y 59 repiten tiempos. El 19 % de las cantidades de `meto` vuelve a salir en `p`.

Muletillas de `p`: «en vez de» (72 veces), «es lo que» (55), «que es cuando, donde o lo que» (49), «así que» (48) y «de golpe» (29). La construcción dominante es la frase partida por dos puntos: 594 en los 407 porqués y 132 en las notas. Las frases de `p` tienen 23 palabras de mediana y llegan a 73.

### 6.2 Las dos opciones
| Criterio | A · campo nuevo escrito a mano (`orden`) | B · orden compuesta |
|---|---|---|
| Qué se escribe | una frase entera por paso: «Mete 300 g de cebolla · {5} · sin tapa · hasta que esté transparente» | nada nuevo: se acortan `meto`, `nota` y avisos, y la app compone |
| Duplicación | la potencia pasa a estar en 4 sitios (`f`, `t` o `nota`, `p` y `orden`), la tapa en 3 y las cantidades en 3 | cada dato en un sitio |
| Coherencia | pide reglas nuevas que crucen la frase con `f`, `tapa` y `meto`, como hoy las reglas 20, 25 y 27 | la da el dato |
| Cocina del usuario | `{N}` dentro de la frase | el chip ya se traduce (ADR-0014) |
| Momento del paso | una frase fija para antes, durante y después | cada momento enseña su parte: METE, SEÑAL, SACA |
| Voz, notificación y alarma | hay que limpiar la frase para cada canal | una función da la pantalla, la línea y la frase hablada |
| Lectura | natural, con el verbo que toque | más seca, con etiquetas fijas |
| Casos raros (tandas, «la mitad ahora») | se escriben sin problema | caben en `meto` y en los avisos, pero obligan a ser breve |
| Trabajo en las 33 recetas | 407 frases nuevas, y además hay que adelgazar la `nota` | reescribir 407 `nota`, 324 `meto` y los avisos largos |
| Esquema y código | campo nuevo en `Modelo.kt`, validador, voz y HTML | sin cambios de esquema |

**Recomiendo B.** Tres razones: quita copias en vez de añadir una, el ejemplo de Joan sale tal cual de los datos (METE `meto` · fuego `f` · `tapa` · SEÑAL `nota`) y solo B permite que la tarjeta cambie con el momento. La salida si falla: si tras cocinar el piloto más de 1 de cada 10 pasos se lee mal compuesto, se añade un `orden` opcional que sustituye la línea METE solo en esos pasos.

### 6.3 El modelo
| Campo | Qué es | Tope | Dónde se pinta |
|---|---|---|---|
| `t` | etiqueta del paso, en infinitivo o nombre. Lleva `{N}` solo si el paso consiste en cambiar la potencia; sin tiempos ni tapa | 36 | título, listas, diagrama, avisos |
| `f`, `tapa`, `s` | datos, como hoy | — | «fuego 4», o «fuego 4 → 5» si cambia respecto al paso anterior del carril; «sin tapa», «tapada» o «tapa a medias»; «18 min» |
| `meto` | lo que entra: `origen: cantidad y qué`, con « + » entre elementos; sin medida de corte | 36 por elemento | «METE …» antes de empezar |
| `saco` | lo que sale y adónde, con el mismo separador | 36 por elemento | «SACA …» al vencer |
| `nota` | la señal: cómo sabes que está y la única corrección. Sin porqués, sin la potencia de `f`, sin tapa y sin minutos de reloj | 80 y 2 frases | «SEÑAL …» en marcha |
| `hitos[].t` | una acción, en imperativo | 44 | «En 3:07 …», alarma y voz |
| `p` | el porqué, en párrafos y listas; el primero se sostiene solo | 600 en total y 240 por párrafo; mínimo 120 (regla 21) | ficha y detalle |
| `dep`, `junto`, `holgura` | sin cambios | — | «tras N», «a la vez que N», «no espera» |

Sin campos nuevos ni tipos cambiados: `receta/src/main/kotlin/app/trombito/souschef/receta/Modelo.kt` no se toca. Los topes son de partida y se calibran con el piloto: a 18 sp caben unos 36 caracteres por línea (medido en las capturas: unos 9 dp por carácter a 16 sp).

Una función pura en `:receta` (`OrdenPaso`) da las tres salidas de la misma orden:
- pantalla: «METE 500 g de patata · la fuente» y «fuego 4 · sin tapa · 18 min»;
- una línea, para la notificación y «Toca empezar»: «1ª fritura · METE 500 g de patata · fuego 4»;
- voz: «Sartén grande: primera fritura. Mete quinientos gramos de patata. Fuego cuatro, sin tapa. Dieciocho minutos».

**Un caso difícil.** La `nota` de «El bacon en frío» (arroz a la cubana, 256 caracteres) ya tiene cada frase en otro campo. Es un ejemplo sin revisión culinaria:

| Frase de la nota actual | Dónde vive en el modelo |
|---|---|
| «Sartén de 28 cm,» | preparación (`mep`) |
| «el bacon dentro en frío y entonces enciendes al {6}» | `meto` y chip de fuego; la nota conserva la orden |
| «así la grasa sale despacio. En sartén caliente el exterior se sella y la grasa se queda dentro.» | `p`, que ya lo explica |
| «Remueve cada 2 minutos.» | los tres avisos, que ya existen |
| «Punto: el fondo con grasa clara y los dados chirriando, no silbando.» | la señal |

Queda `meto` «el plato llano: 200 g de bacon» (30) y `nota` «Bacon dentro y entonces enciendes. Punto: grasa clara y chirrido, no silbido.» (77).

### 6.4 Validador
| Regla | Nivel | Comprueba |
|---|---|---|
| 30 | AVISO de legibilidad | `t` de 36 caracteres o menos y sin duración escrita («N min», «N s») |
| 31 | AVISO de legibilidad | `nota` de 80 o menos y 2 frases como mucho; sin «minuto N»; sin `{N}` igual a `f` |
| 32 | AVISO de legibilidad | cada elemento de `meto` y `saco` de 36 o menos; sin mm ni cm |
| 33 | AVISO de legibilidad | `hitos[].t` de 44 o menos |
| 34 | AVISO de legibilidad | `p` de 600 o menos, con párrafos de 240 o menos y 4 como mucho; `descripcion` de 200 o menos; `lectura` de 800 o menos y con algún salto; `porque` y `mep.porq` de 280 o menos |
| 35 | ERROR | los pasos del array van en orden de `off` |
| 36 | AVISO de legibilidad | frase de la `nota` cuyas palabras de contenido están en `p` en un 80 % o más |

- **Migración sin romper el gate.** Las reglas 30-34 y 36 solo corren con un indicador nuevo, `sc validar --legibilidad`. Cuando las 33 recetas pasen, se funden en `--estricto`. Hasta entonces `scripts/sc validar --estricto` sigue dando 0 errores y 0 avisos.
- La regla 35 exige antes reordenar 5 recetas. Es mecánico y seguro: la cola de cada carril se saca filtrando el array, así que su orden interno no cambia.
- La regla 20 (nombrar la potencia nueva en el texto) deja de hacer falta cuando el chip enseñe «4 → 5»; hasta entonces se cumple desde `t` o `p`. La 21 y la 25 no cambian.
- Los caracteres se cuentan como los ve el usuario: `{N}` vale 1 y `<b>` vale 0.

### 6.5 Impacto
| Pieza | Qué cambia |
|---|---|
| Esquema (`docs/cocina/esquema-receta.md`) | § «nota y p» reescrita (se quita «`nota` se ve siempre»); apartado nuevo «Orden compuesta»; mini formato de la prosa; reglas 30-36 |
| Reglas (`docs/cocina/reglas.md`) | una línea: la potencia y la tapa las pinta la app y no se repiten en la `nota` |
| Validador (`Validador.kt` y sus tests) | reglas nuevas con TDD; indicador `--legibilidad` en `tools` |
| Voz (`ui/crono/voz/`) | `info` dice la orden compuesta y deja de cortarse (hoy título, nota, fuego y tapa: `RespuestasVoz.kt:59-69`); `porQue` lee el primer párrafo. Ojo: `IngredientesVoz.kt:90` busca los ingredientes en `t`, `meto`, `saco` y `nota`, así que al acortar hay que conservar el nombre del ingrediente; un test lo fija |
| Notificaciones (`domain/crono/CarrilesNotificacion.kt:47-54`, `data/alarma/NotificadorAndroid.kt:186-187`) | la línea del carril pasa de `t · fuego N` a la orden de una línea; con `t` de 36 deja de cortarse |
| Alarma (`AvisosCrono.kt:145-176`) | «Toca empezar: {t}» añade lo que se mete; «{t}: hecho» añade lo que se saca; los avisos de 44 caben en una línea |
| Ficha imprimible (`FichaHtml.kt`) | bloques `<p>` y `<ul>`; la señal impresa; cronología; ingredientes una vez. El ADR-0006 sigue valiendo |
| Skill `receta-cronometrada` | plantilla de paso con los topes, dos ejemplos buenos y dos malos, y «lo que no se escribe porque lo pinta la app» |
| Skill `receta-revisar` y agente `chef-reviewer` | añadir `--legibilidad` y comprobar que acortar no cambia la cocina |
| Historial | `pasosReales` guarda `t`: los títulos nuevos solo afectan a las cocinadas nuevas |

## 7. Receta piloto: huevos rotos
**Cómo leerlo.** Lo que va entre comillas angulares es literal del JSON, con la potencia como `{N}`. «fuego», tapa y minutos salen de `f`, `tapa` y `s`; «tras N» de `dep`; «a la vez que N» de `junto`; lo último de la cabecera es la holgura. En pantalla el origen de `meto` va detrás («500 g de patata · la fuente») y `{3}` se ve como 3. No cambia ninguna cantidad, tiempo, potencia ni dependencia.

### 7.1 Paso a paso
**1 · min 0-6 · MESA** · `mise-en-place` · aguanta espera
- Por empezar: «Pelar y cortar la patata» · **METE** «500 g de patata (3 medianas)» · 6 min
- En marcha: **SEÑAL** «Bastones de 1,5×1,5×6 cm, todos del mismo grosor.»
- Al acabar: **SACA** «a la fuente, tapados con un paño»
- Ficha (`p`):
  > Con doble fritura el grosor manda: si varía, la primera fritura cuece unos bastones antes que otros y la segunda los dora con esa diferencia ya hecha.

**2 · min 6-11 · SARTÉN GRANDE** · `calentar-aceite` · tras 1 · aguanta espera
- Por empezar: «Calentar el aceite» · fuego 4 · sin tapa · 5 min
- En marcha: **SEÑAL** «El aceite ya está dentro: solo enciendes. No lo subas más.»
- Ficha (`p`):
  > La primera fritura cuece la patata por dentro sin colorearla por fuera. Por eso el aceite se queda bajo, lejos del punto en el que empezaría a dorar.

**3 · min 11-29 · SARTÉN GRANDE** · `primera-fritura` · ajustado
- Por empezar: «1ª fritura (blanqueo)» · **METE** «la fuente: 500 g de patata» · fuego 4 · sin tapa · 18 min
- En marcha: avisos 2:00 «Despega del fondo las que se hayan pegado» · 9:00 «Dales la vuelta: el aceite no las cubre» · 15:00 «Pincha un bastón: debe ceder sin resistencia» · **SEÑAL** «Tiernas y pálidas, sin color. Si se doran, baja al {3}.»
- Al acabar: **SACA** «la patata, al plato con papel»
- Ficha (`p`):
  > Aquí la patata se cuece, no se dora: el almidón de la superficie gelatiniza y el centro se hace del todo. La costra llega en la segunda fritura.
  >
  > - Despegar a los 2 minutos: la patata cruda se pega, y la que se pega se dora ahí antes de tiempo.
  > - Pinchar a los 15 minutos: es la única señal fiable, porque por fuera todas se ven igual de pálidas.
  >
  > Frita una sola vez sale blanda y sin costra, o dorada por fuera y cruda por dentro.

**4 · min 12-17 · MESA** · `rasgar-jamon-lavar-padron` · a la vez que 3 · aguanta espera
- Por empezar: «Rasgar el jamón y lavar el padrón» · **METE** «80 g de jamón serrano + 200 g de pimientos de padrón» · 5 min
- En marcha: **SEÑAL** «Jamón en trozos de 3 cm, a mano. Padrón seco del todo: mojado salpica.»
- Al acabar: **SACA** «el jamón, al platito hondo + el padrón, seco, al bol»
- Ficha (`p`):
  > El jamón se rasga a lo largo de la loncha: el borde roto toca más sartén que un corte limpio de cuchillo y dora en manchas, no en una lámina pareja.
  >
  > El padrón se lava entero y se seca uno a uno con un paño: cualquier gota de agua en aceite muy caliente salpica.

**5 · min 28-30 · SARTÉN PEQUEÑA** · `calentar-sarten-pequena` · a la vez que 3 · aguanta espera
- Por empezar: «Calentar la sartén pequeña» · fuego 6 · sin tapa · 2 min
- En marcha: **SEÑAL** «Seca y vacía: ni aceite ni jamón todavía.»
- Ficha (`p`):
  > La vitro tarda un par de minutos en calentar una sartén fría, y el jamón solo pasa dos en ella: si entra en frío suelta la grasa y se cuece antes de tostar el borde. Va seca porque la grasa la pone el jamón.

**6 · min 29-33 · MESA** · `reposo-patata` · tras 3 · aguanta espera
- Por empezar: «Reposo de la patata en papel» · **METE** «sartén grande: la patata blanqueada» · 4 min
- En marcha: **SEÑAL** «Déjalas quietas: tienen que soltar el vapor.»
- Ficha (`p`):
  > El vapor que aún llevan dentro tiene que salir ahora. Si entraran directas a la segunda fritura, ese vapor mantendría la superficie húmeda y el aceite las cocería otra vez en lugar de formar costra.

**7 · min 29-31 · SARTÉN GRANDE** · `subir-al-5` · a la vez que 6 · aguanta espera
- Por empezar: «Subir al {5}» · fuego 4 → 5 · sin tapa · 2 min
- En marcha: **SEÑAL** «Sartén sola, solo aceite. Aún no al {8}: la patata tarda en volver.»
- Ficha (`p`):
  > La patata reposa cuatro minutos y la sartén se queda vacía: al {5} el aceite gana temperatura sin pasarse.
  >
  > El girasol humea sobre los 225 °C, y 500 ml al {8} sin nada dentro llegan ahí en un par de minutos. El aceite se degrada, coge sabor y ya no hay vuelta atrás para los huevos que se fríen luego en él.
  >
  > Desde el {5} llega al {8} en los dos minutos del paso siguiente.

**8 · min 30-32 · SARTÉN PEQUEÑA** · `dorar-jamon` · tras 4 · a la vez que 6 · ajustado
- Por empezar: «Dorar el jamón» · **METE** «el platito hondo: 80 g de jamón» · fuego 6 · sin tapa · 2 min
- En marcha: avisos 1:00 «Dale la vuelta al jamón» · **SEÑAL** «Sin aceite. Fuera al acabar: si se pasa, queda gomoso.»
- Al acabar: **SACA** «el jamón, a un plato»
- Ficha (`p`):
  > Va sin aceite porque el jamón suelta su propia grasa. Con un minuto por lado los bordes quedan crujientes; pasado ese punto suelta demasiada grasa y se pone gomoso.

**9 · min 31-33 · SARTÉN GRANDE** · `subir-al-8` · ajustado
- Por empezar: «Subir al {8}» · fuego 5 → 8 · sin tapa · 2 min
- En marcha: **SEÑAL** «Al acabar entra la patata; no lo subas antes de tiempo.»
- Ficha (`p`):
  > La segunda fritura pide el aceite muy caliente para que la costra se forme en minutos. Es la misma grasa: tras el blanqueo sigue limpia de almidón y no hace falta cambiarla.
  >
  > Se sube ahora y no antes a propósito: 500 ml de girasol al {8} sin nada dentro se degradan y acaban humeando, y ese humo se queda en el sabor de la patata.

**10 · min 32-36 · SARTÉN PEQUEÑA** · `freir-padron` · tras 4 · ajustado
- Por empezar: «Freír los pimientos de padrón» · **METE** «el bol: 200 g de pimientos de padrón + 10 ml de aceite de oliva» · fuego 6 → 8 · sin tapa · 4 min
- En marcha: **SEÑAL** «Mueve sin parar: fuera hinchados y arrugados, no blandos. La sal, al sacarlos.»
- Al acabar: **SACA** «los pimientos, a un plato»
- Ficha (`p`):
  > La misma sartén, ya caliente por el jamón, sube del {6} al {8} con el aceite de oliva. El calor fuerte convierte de golpe en vapor el agua del pimiento: lo hincha y le arruga la piel, que es la textura que se busca.
  >
  > La sal entra al servir y no antes: echada mientras se fríen sacaría agua por ósmosis y el pimiento se ablandaría en vez de quedar tostado.

**11 · min 33-40 · SARTÉN GRANDE** · `segunda-fritura` · tras 6 · ajustado
- Por empezar: «2ª fritura (crocante)» · **METE** «plato con papel: la patata reposada» · fuego 8 · sin tapa · 7 min
- En marcha: avisos 3:00 «Dales la vuelta» · 6:00 «Si ya están doradas parejas, fuera» · **SEÑAL** «No bajes el fuego. Fuera cuando estén doradas parejas.»
- Al acabar: **SACA** «la patata, al plato con papel nuevo»
- Ficha (`p`):
  > Entran 500 g de golpe y enfrían el aceite: por eso la costra tarda en formarse y el mando se queda en el {8} hasta que doren.
  >
  > La superficie, seca por el reposo, forma la costra crujiente. El interior no necesita más cocción: ya está hecho desde el blanqueo.

**12 · min 40-42 · MESA** · `escurrir-salar-patata` · tras 11 · no espera
- Por empezar: «Escurrir y salar la patata» · **METE** «la patata recién frita + el platito: 2 g de sal» · 2 min
- En marcha: **SEÑAL** «Sala nada más sacarla: en caliente la sal se agarra.»
- Al acabar: **SACA** «la patata, a los platos»
- Ficha (`p`):
  > El papel se lleva el aceite que sobra. La sal va enseguida, con la costra aún caliente y porosa, que es cuando mejor se agarra; en frío rebota en la superficie ya sellada.

**13 · min 40-42 · SARTÉN GRANDE** · `bajar-al-7` · a la vez que 12 · aguanta espera
- Por empezar: «Bajar al {7}» · fuego 8 → 7 · sin tapa · 2 min
- En marcha: **SEÑAL** «En cuanto salga la patata: los huevos van a este fuego, no al {8}.»
- Ficha (`p`):
  > Dos minutos de escurrir y salar con la sartén vacía al {8} son dos minutos de aceite degradándose.
  >
  > Además los huevos se fríen al {7} y no al {8}: a temperatura de patata la clara se tuesta antes de que la yema se caliente, y salen con la puntilla quemada.
  >
  > Mientras escurres y cascas el primer huevo, el aceite se asienta en su punto.

**14 · min 42-45 · SARTÉN GRANDE** · `freir-huevos` · tras 12 · no espera
- Por empezar: «Freír los huevos en dos tandas» · **METE** «la huevera: la mitad de los huevos» · fuego 7 · sin tapa · 3 min
- En marcha: avisos 1:00 «Fuera la primera tanda, escurriendo» · 1:30 «Entra la segunda tanda» · 2:30 «Fuera la segunda tanda» · **SEÑAL** «Baña la clara con aceite, a cucharadas. Fuera con puntilla y yema líquida.»
- Al acabar: **SACA** «cada tanda, directa al plato»
- Ficha (`p`):
  > De dos en dos, porque cuatro juntos no dejan sitio para que la clara suba con puntilla. Sesenta segundos por tanda: lo que tarda la clara en cuajar y el borde en encresparse, con la yema aún entera y líquida.
  >
  > Eso los hace rotos y no fritos: al romperlos la yema tiene que correr entre la patata. Con dos minutos en el aceite la yema cuaja y ya no hay salsa.
  >
  > - Los 30 segundos entre tandas son para sacar y cascar, no para freír.
  > - El aceite ya sabe a patata: estos huevos no salen neutros.

**15 · min 45-47 · MESA** · `montar` · tras 14 y 10 · no espera
- Por empezar: «Montar y sal final» · **METE** «los dos platos: jamón y padrón + 1 g de sal para el padrón + 1 g de sal para el resto» · 2 min
- En marcha: **SEÑAL** «Jamón y padrón encima de todo, lo último.»
- Al acabar: **SACA** «todo, a los platos»
- Ficha (`p`):
  > El jamón lleva trece minutos esperando y el padrón nueve, pero ninguno suelta líquido, así que llegan al plato como salieron de la sartén.
  >
  > Van encima y al final: cualquier cosa sobre el jamón lo aplastaría contra el plato y perdería el crujiente de los bordes.

### 7.2 Textos de receta
- `descripcion` (271 → 189): «Patata en bastón con doble fritura, jamón serrano y pimientos de padrón dorados aparte, y huevos con puntilla al final. Lo difícil está del minuto 28 al 36, con las dos sartenes encendidas.»
- `lectura` (1.102 → 775, en tres bloques):
  > La cuenta, por el camino largo: 6 de mise en place + 5 de calentar el aceite + 18 de primera fritura + 4 de reposo + 7 de segunda + 2 de escurrir + 3 de huevos + 2 de montar = 47 minutos.
  >
  > - La sartén pequeña (8 minutos: calentar, jamón y padrón) cabe entera dentro de los tiempos de la grande.
  > - Rasgar el jamón y lavar el padrón (del 12 al 17) ocurre durante el blanqueo: no suma.
  > - Dos fuegos a la vez solo del 28 al 36; nunca tres.
  >
  > Lo que gobierna el plan: la sartén grande nunca se queda sola al {8}. Cuando la patata sale y tarda en volver, el mando se mueve: sube por escalones (del {4} al {5} y del {5} al {8}) justo antes de que entre, y baja del {8} al {7} para los huevos. Esos pasos que parecen no hacer nada son los que salvan el aceite, y con ese aceite se fríen los huevos.
- `notaEscala` (un párrafo → lista):
  > - De 1 a 2 raciones, con las mismas tandas. El aceite no cambia: es la profundidad de la sartén, no depende de la patata.
  > - A 1 ración la segunda fritura dora antes, porque la patata enfría menos el aceite: sácala por el color, no por el reloj.
  > - Por encima de 2, no: la patata enfría el aceite y no cruje, y los huevos ya no caben en sus 3 minutos.
- `mep`: las 12 líneas no cambian (ya tienen cabecera en negrita, detalle y minuto). Se recortan los tres `porq` (249, 363 y 309 → 211, 224 y 270):
  - Tabla y fregadero: «El padrón va el último porque es lo único que se moja: así la tabla y las manos llegan secas a los 500 ml de aceite, de donde salen las salpicaduras. La patata va primero: es el corte largo y marca el minuto 11.»
  - Cinco sitios: «Un recipiente se justifica cuando lo que guarda entra en un minuto distinto al del resto. El jamón y el padrón no comparten plato aunque vayan a la misma sartén: los separan dos minutos y uno sale antes de que entre el otro.»
  - Sin recipiente: «Girasol en la grande y oliva en la pequeña, no al revés: el girasol tiene que aguantar el {8} de la segunda fritura sin humear, y su punto de humo está unos cuarenta grados por encima. Los 10 ml de oliva solo acompañan al padrón cuatro minutos, y ahí sí interesa su sabor.»
- `porque` (9 entradas y 2.084 caracteres → 5 y 961). Se quedan las decisiones de receta; el porqué de cada paso vive en su paso («Jamón rasgado», «La sal del padrón», «El jamón, lo último» y «Sesenta segundos de huevo» ya están en los pasos 4, 10, 15 y 14):
  - **Doble fritura.** «Una sola fritura obliga a elegir entre patata cocida por dentro o costra por fuera. Blanqueando primero a temperatura baja y subiendo después se consiguen las dos cosas sin quemar nada.»
  - **El aceite se reutiliza, no se cambia.** «El mismo aceite blanquea, dora después de subir de potencia y, ya con sabor a patata, fríe los huevos al final: nada se desperdicia y el sabor se acumula.»
  - **Con la sartén grande vacía, nunca al {8}.** «El girasol humea sobre los 225 °C, y 500 ml al {8} sin nada dentro llegan ahí en un par de minutos. Ese aceite coge un sabor rancio que acaba en la segunda fritura y en los huevos: por eso sube por escalones y baja del {8} al {7} mientras escurres.»
  - **Sal en dos momentos.** «2 g sobre la patata recién escurrida, cuando la costra caliente y porosa la retiene mejor; 1 g al final, sobre el conjunto ya montado, para que también se note en el huevo y el jamón, que no han pasado por la sal de la patata.»
  - **El padrón, en el hueco de la sartén pequeña.** «La sartén pequeña queda libre desde que sale el jamón, en el minuto 32, hasta el montaje del 45: los pimientos entran ahí sin sumar ni un minuto al total.»
- `cortes`, `grupos`, `carriles`, `datos`, `escala`, `tags` y `pie`: sin cambios.

### 7.3 Un paso en el JSON
```json
{
  "carril": "sarten",
  "dep": [],
  "f": 4,
  "hitos": [
    { "en": 120, "t": "Despega del fondo las que se hayan pegado" },
    { "en": 540, "t": "Dales la vuelta: el aceite no las cubre" },
    { "en": 900, "t": "Pincha un bastón: debe ceder sin resistencia" }
  ],
  "holgura": "ajustado",
  "id": "primera-fritura",
  "junto": null,
  "meto": "la fuente: 500 g de patata",
  "min": 18,
  "nota": "Tiernas y pálidas, sin color. Si se doran, baja al {3}.",
  "off": 11,
  "p": "Aquí la patata se cuece, no se dora: el almidón de la superficie gelatiniza y el centro se hace del todo. La costra llega en la segunda fritura.\n\n- Despegar a los 2 minutos: la patata cruda se pega, y la que se pega se dora ahí antes de tiempo.\n- Pinchar a los 15 minutos: es la única señal fiable, porque por fuera todas se ven igual de pálidas.\n\nFrita una sola vez sale blanda y sin costra, o dorada por fuera y cruda por dentro.",
  "s": 1080,
  "saco": "la patata, al plato con papel",
  "t": "1ª fritura (blanqueo)",
  "tapa": "sin tapa"
}
```

### 7.4 Cuentas y comprobaciones
| Campo | Antes (mediana / máximo) | Después (mediana / máximo) |
|---|---|---|
| `t` | 26 / 36 | 21 / 33 |
| `nota` | 72 / 128 | 54 / 78 |
| `meto`, por elemento | 52 / 125 (campo entero) | todos de 36 o menos |
| avisos | 35 / 81 | 34 / 44 |
| `p` | 322 / 678, en 15 bloques | 261 / 491, en 28 párrafos (148 / 211) |
| prosa total | 2.642 palabras | 2.011 palabras (−24 %) |

- **Validador actual:** la copia completa pasa `validar --estricto` con 0 errores y 0 avisos y `potencias` con 0 candidatos y 0 dudosos, ejecutados con el binario de `tools` sobre una copia fuera del repo. `sc formatear` conserva los `\n`.
- **Topes propuestos:** ningún paso los incumple (comprobado con un script).
- **Guardas mecánicas:** `carril`, `off`, `min`, `s`, `f`, `tapa`, `holgura`, `dep` y `junto` son idénticos en los 15 pasos. Cuatro pasos pierden una cantidad de `meto` porque en ellos no entra nada: el aceite ya está en la sartén (pasos 2 y 13), la patata vuelve entera (paso 6) y los platos llegan completos (paso 15).
- **Cambios de estructura a confirmar:** el array va por `off` (un salto menos); «Dorar el jamón» cambia «1 min por lado» del título por un aviso a 1:00; «1ª fritura» gana un `saco` y «Pelar y cortar» un `meto` y un `saco`, tomados de la preparación.
- **Duda de cocina que el piloto no toca:** la sal del padrón. La preparación dice «al sacarlo de la sartén», `p` dice «al servir» y «Montar» la echa en el minuto 45.
- **Aviso sobre el piloto:** huevos rotos es la segunda receta con las notas más cortas (mediana 72). En arroz a la cubana (177) o pechugas empanadas (181) el recorte será mucho mayor.

## 8. Plan por lotes
| Lote | Qué | Dónde | Quién | Estimación | Riesgo |
|---|---|---|---|---|---|
| 1 · Alivio sin tocar recetas | Sin marquesina (2 líneas y «…»). EMPEZAR lleno solo en «empieza ya»; «a las HH:mm · en N min» y «espera a…» en tarjeta, compacta y fila; `junto` visible. METO, SACO y nota antes que el reloj y con las líneas que quepan. HECHO de contorno hasta el cero; en CLÁSICA, «Empezar» primario. Panel con lo ejecutivo arriba. Nombres de carril enteros | `ui/crono/` (`TarjetaCompacta`, `FilaCarrilCompacta`, `PlanTarjeta`, `DetalleTarjeta`, `TarjetaCarril`, `VistaCarril`, `AccionesCarril`, `CronoPorque`, `clasica/MandosClasica`) y cadenas | 2 × `android-ui-engineer` (sonnet) | 1 sesión | tests de Compose que fijan textos; alto de la tarjeta a 1,3× |
| 2 · Reglas, documentos y ADR | Reglas 30-36 con `--legibilidad` (TDD); esquema, reglas y skills; ADR-0017; guarda `scripts/comparar-textos.mjs` y el script de métricas | `receta/`, `tools/`, `docs/cocina/`, `.claude/skills/`, `docs/adr/` | `test-engineer`, `docs-writer`, `skill-author` (sonnet); el agente principal, los reservados | 1 sesión | bajo |
| 3 · Piloto en datos | `huevos-rotos.json` de §7; reordenar por `off` las otras 4 recetas con saltos; publicar; Joan la cocina y se ajustan los topes | `data/recetas/` | `chef-reviewer` (opus) | media sesión y una cocinada | topes mal calibrados: se corrigen aquí, antes de migrar |
| 4 · Ficha explicativa | `Bloques` en `:receta` (TDD) y sus dos pintores; pestaña Pasos desplegada, con minuto y relaciones; detalle único que sustituye a la hoja y al panel; impresa con bloques, señal, cronología e ingredientes una vez; cabecera que se desplaza; `sinHitos` visible | `receta/`, `ui/comun/`, `ui/ficha/`, `ui/crono/CronoPorque.kt` | `android-ui-engineer` y `test-engineer` (sonnet) | 2 sesiones | saltos de página del PDF |
| 5 · Orden compuesta | `OrdenPaso` en `:receta` (TDD); tarjeta por momentos; franja «Ahora / Luego»; voz, notificación y textos de alarma | `receta/`, `ui/crono/`, `domain/crono/`, `data/alarma/` | `android-engineer` (sonnet) y `architecture-reviewer` (opus) | 2 sesiones | toca avisos y voz, lógica crítica: tests primero |
| 6 · El plan sigue al cocinero | Horas pendientes y avisos «Toca empezar» desplazados con el desvío real; `junto` como ancla; cabecera con una sola cifra | `receta/` (`crono/Plano`, `AvisosCrono`, `ConsultasCrono`) y `ui/crono/` | `android-engineer` (sonnet), `test-engineer` y `architecture-reviewer` (opus) | 2 sesiones | motor del cronómetro y cadena de alarmas: TDD y ADR antes de tocar |
| 7 · Diagrama unificado | `DiagramaPlan` con tres densidades y estado; sustituye a los cuatro pintores; una función de color; perfil de fuego | `ui/comun/`, `ui/ficha/`, `ui/crono/`, `ui/recetas/` | `android-ui-engineer` (sonnet); `performance-reviewer` si el estado repinta cada segundo | 2 sesiones | `DiagramaCronoTest`; coste de repintado |
| 8 · Las otras 32 recetas | 4 tandas de 8, por familia. Cada tanda: reescritura (3 recetas como mucho por subagente), guardas mecánicas, `chef-reviewer` y repaso de Joan a 2 recetas | `data/recetas/` | subagentes sonnet y `chef-reviewer` (opus) | 4 sesiones y 4 repasos de media hora | cambiar la cocina al acortar: es el mayor riesgo del plan |
| 9 · Una disposición | «Carriles» adaptativa; CLÁSICA una versión más y fuera; Ajustes, intro y guía; mapear la preferencia guardada | `ui/crono/`, `ui/ajustes/`, `ui/bienvenida/`, `domain/Modelos.kt` | `android-ui-engineer` (sonnet) y `architecture-reviewer` (opus) | 2 sesiones | preferencia de Joan; 1.217 líneas borradas y sus tests |

- **Orden.** 1 → 2 → 3, y después en paralelo el código (4, 5, 6 y 7) y los datos (8), que no comparten archivos. El 9, al final. En total, unas 16 sesiones, una cocinada de Joan y cuatro repasos.
- **Validación de cada lote.** El gate de `CLAUDE.md`, `scripts/sc validar --estricto` (y `--legibilidad` desde el lote 2), `node scripts/check-docs.mjs` y capturas a 1,0× y 1,3× con 2, 3 y 4 carriles.
- **ADR.** Hacen falta dos. ADR-0017, «Dos textos por paso: orden compuesta y explicación estructurada»: fija un contrato que cruza esquema, validador, interfaz, voz y avisos, y que seguirán todas las recetas nuevas. ADR-0018, «El plan sigue al cocinero»: cambia cómo el motor calcula las horas y programa las alarmas (toca el ADR-0005). La retirada de disposiciones se anota en `docs/decisiones.md`.
- **Riesgos de conjunto.** (1) Acortar puede cambiar la cocina sin querer: lo cubren las guardas mecánicas, el `chef-reviewer` y el repaso de Joan. (2) Los topes salen de un emulador y de una receta fácil: el lote 3 existe para corregirlos. (3) A 1,3× cada línea ocupa más: menos tarjetas expandidas, nunca texto en movimiento. (4) Hay más de 500 tests en `app`; los de Compose fijan textos y nodos. (5) El lote 6 toca avisos que hoy funcionan con la pantalla apagada: se prueba en el móvil antes de publicar.

## 9. Preguntas abiertas para Joan
> **Respondidas el 2026-11-15.** Las decisiones están en `docs/decisiones.md` § Ronda 19. Cambios respecto a lo que
> proponía este informe: la cabecera queda solo en «Llegas a las HH:mm», sin diferencia ni rojo; el desvío por carril
> desaparece; Joan no repasa las recetas reescritas (guardas mecánicas y `chef-reviewer`); el momento de salar el
> padrón lo decide el `chef-reviewer`.

1. **Letra y distancia.** ¿Usas la letra del móvil a 1,0× o más grande? ¿Lo dejas apoyado o lo coges? De esto dependen los tamaños mínimos y cuántas tarjetas caben expandidas.
2. **Disposición.** ¿Cuál usas hoy, y qué te hizo volver a pedir la CLÁSICA? ¿Aceptas una sola disposición adaptativa, con la CLÁSICA una versión más y luego fuera?
3. **Adelanto y retraso.** Si vas por delante del plan, ¿quieres que las horas se adelanten contigo o que la app te haga esperar a la hora de servir?
4. **Modelo.** ¿Te vale la orden compuesta (B) o prefieres una frase escrita a mano por paso (A)?
5. **Etiquetas y tapa.** ¿METE, SACA y SEÑAL, o ENTRA, SALE y PUNTO? ¿Quieres ver «sin tapa» siempre, o solo cuando hay tapa o cambia?
6. **Nota vacía.** ¿Puede quedar sin señal un paso donde no hay nada que vigilar? Hoy las 407 la llevan.
7. **Topes.** Título 36, elemento de METE o SACA 36, señal 80, aviso 44 y porqué 600 en párrafos de 240. ¿Los ves bien al leer el piloto?
8. **`porque` de receta.** ¿Se quedan solo las decisiones de receta (de 9 a 5 en el piloto) y el porqué de cada paso vive en el paso?
9. **Papel.** ¿Imprimes la ficha? ¿Quieres la hoja de cocina de 1 o 2 páginas?
10. **Dudas del piloto.** El aviso nuevo de dar la vuelta al jamón al minuto, y cuándo se sala el padrón: al sacarlo o al montar.

## Anexo A. Dónde aparece cada texto
| Campo | Dónde sale y con qué forma | Observación |
|---|---|---|
| `t` | lista de pasos, hoja, diagramas, impresa (dos veces), tarjeta, compacta y fila (en movimiento), CLÁSICA (pestaña, panel, plan y paralelos), panel, «Después», notificación, alarma, voz e historial | es el único texto que está en todas partes: conviene corto |
| `nota` | lista de pasos (2 líneas), hoja (cursiva gris), impresa solo si no hay `p`, tarjeta (2 líneas si cabe, y en APILADA no cupo en la prueba), compacta (en movimiento), CLÁSICA (entera), panel (gris) y voz | 6 maneras de pintarla; en la disposición por defecto casi nunca se ve |
| `p` | hoja, impresa, panel y voz (300 caracteres) | siempre un bloque |
| `meto` y `saco` | hoja («Entra», «Sale»), impresa («Meto», «Saco»), tarjeta (1 línea, con 2 carriles o en foco), CLÁSICA y panel | 3 rótulos |
| avisos | hoja (`+m:ss`), impresa (8,5 pt), tarjeta (el próximo), barra (marcas), CLÁSICA (todos), panel, alarma y voz | 3 formatos de hora |
| `sinHitos` | ninguna pantalla | 29 pasos lo llevan |
| `f` y `tapa` | chips, sublíneas, meta, panel, CLÁSICA, «Después», notificación y voz; y además dentro de `t`, `nota` y `p` | 5 y 4 formatos |
| `dep` | tarjeta completa («Antes: …»), CLÁSICA y voz | no en compacta, fila ni ficha |
| `junto` y `holgura` | solo CLÁSICA | no en la ficha |
| `mep` | Preparación (plegable), portada de la cocina (checklist) e impresa | dos componentes para lo mismo |
| `porque` | pestaña ¿Por qué? (plegado) e impresa | repite porqués de pasos: 4 de 9 en huevos rotos |
| `cortes` | pestaña ¿Por qué? (dibujo y prosa en columna estrecha) e impresa | el dibujo, bien |
| `lectura` | Resumen (plegada, cursiva) e impresa | bloque |
| `descripcion` | lista (1-3 líneas), Resumen (entera, cursiva), portada (1 línea plegable) e impresa | 6-8 líneas en el Resumen |
| `notaEscala` | Ingredientes (tras un chip) e impresa | es una lista escrita como párrafo |

## Anexo B. Capturas
Las 39 están en `docs/capturas/auditoria-legibilidad-*.png`. Receta: huevos rotos, salvo donde se dice otra.
- **Ficha:** [resumen](../capturas/auditoria-legibilidad-ficha-resumen.png), [el tiempo](../capturas/auditoria-legibilidad-ficha-resumen-tiempo.png), [lectura](../capturas/auditoria-legibilidad-ficha-lectura.png), [diagrama completo](../capturas/auditoria-legibilidad-ficha-diagrama-completo.png), [ingredientes](../capturas/auditoria-legibilidad-ficha-ingredientes.png), [preparación](../capturas/auditoria-legibilidad-ficha-preparacion.png), [pasos](../capturas/auditoria-legibilidad-ficha-pasos.png), [hoja del paso](../capturas/auditoria-legibilidad-ficha-paso-hoja.png), [¿por qué?](../capturas/auditoria-legibilidad-ficha-porque-abierto.png), [impresa, página 1](../capturas/auditoria-legibilidad-ficha-impresa-1.png), [impresa, pasos](../capturas/auditoria-legibilidad-ficha-impresa-3.png), [resumen a 1,3×](../capturas/auditoria-legibilidad-letra13-ficha-resumen.png).
- **Cocina, común:** [portada](../capturas/auditoria-legibilidad-crono-portada.png), [panel del porqué](../capturas/auditoria-legibilidad-crono-porque.png), [diagrama](../capturas/auditoria-legibilidad-crono-diagrama.png), [notificación (tortilla)](../capturas/auditoria-legibilidad-notificacion.png).
- **APILADA:** [antes de arrancar](../capturas/auditoria-legibilidad-apilada-1-antes.png), [cambio de paso](../capturas/auditoria-legibilidad-apilada-3-cambio-de-paso.png), [dos en marcha](../capturas/auditoria-legibilidad-apilada-4-dos-en-marcha.png), [vencido](../capturas/auditoria-legibilidad-apilada-5-vencido.png), [2 carriles (tortilla)](../capturas/auditoria-legibilidad-apilada-2carriles-2-en-marcha.png), [4 carriles antes (arroz)](../capturas/auditoria-legibilidad-apilada-4carriles-1-antes.png), [4 carriles en marcha (arroz)](../capturas/auditoria-legibilidad-apilada-4carriles-2-dos-en-marcha.png), [marquesina en tres fotogramas (arroz)](../capturas/auditoria-legibilidad-apilada-4carriles-marquesina-3-fotogramas.png), [3 carriles a 1,3×](../capturas/auditoria-legibilidad-letra13-apilada-1-antes.png).
- **FOCO:** [antes de arrancar](../capturas/auditoria-legibilidad-foco-1-antes.png), [cambio de paso](../capturas/auditoria-legibilidad-foco-3-cambio-de-paso.png), [dos en marcha](../capturas/auditoria-legibilidad-foco-4-dos-en-marcha.png), [marquesina en tres fotogramas](../capturas/auditoria-legibilidad-foco-marquesina-3-fotogramas.png), [a 1,3×](../capturas/auditoria-legibilidad-letra13-foco-1-antes.png).
- **CLÁSICA:** [antes de arrancar](../capturas/auditoria-legibilidad-clasica-1-antes.png), [cambio de paso](../capturas/auditoria-legibilidad-clasica-3-cambio-de-paso.png), [plan desplegado](../capturas/auditoria-legibilidad-clasica-3c-plan-desplegado.png), [dos en marcha](../capturas/auditoria-legibilidad-clasica-4-dos-en-marcha-sarten.png), [el mismo panel, más abajo](../capturas/auditoria-legibilidad-clasica-4b-sarten-abajo.png), [a 1,3×](../capturas/auditoria-legibilidad-letra13-clasica-1-antes.png).
- **Resto:** [selector de disposición](../capturas/auditoria-legibilidad-ajustes-crono.png), [novedades](../capturas/auditoria-legibilidad-novedades.png), [técnica](../capturas/auditoria-legibilidad-tecnica-ficha.png).

## Anexo C. Método y límites
- **Hecho.** `./gradlew assembleDebug` en verde; instalación en el AVD `trombito-36`; huevos rotos cocinada a mano en las tres disposiciones (antes de arrancar, cambio de paso, dos carriles en marcha y un paso vencido), y arroz a la cubana y tortilla de patatas en APILADA; ficha en sus cinco pestañas, panel del porqué, diagrama, notificación y vista previa de impresión. Lectura del código de `ui/crono`, `ui/ficha`, `receta/` y las cadenas.
- **Ojo con las horas de las capturas.** En huevos rotos la cocina se abrió unos 10 minutos antes de la hora prevista (servir a las 12:00): los desvíos de «−19» son el caso real de H8. En las otras dos recetas, en las capturas a 1,3× y en la primera de APILADA se dejó la hora de servir por defecto (15:30) con el reloj del emulador en las 11: sus «14:43» y «−221» son un artefacto de la prueba y no cuentan como hallazgo.
- **Emulador.** Se arrancó con `-no-snapshot-save` y se cerró al acabar. Al volver a abrirlo está como antes de la sesión (con la 1.4.0 de su instantánea): ni la instalación ni los ajustes cambiados persisten.
- **Métricas.** Script Node de solo lectura sobre `data/recetas/*.json`, guardado fuera del repo como pedía el encargo. Las frases y las repeticiones se cuentan con heurísticas sencillas: valen como orden de magnitud. El lote 2 propone dejarlo en `scripts/`.
- **No comprobado.** El móvil de Joan y el tema oscuro. TalkBack. La intro y el buzón, vistos solo en el código y las cadenas. Los tiempos de lectura (4 palabras por segundo, 4-8 palabras por mirada) son estimaciones, no medidas. Las alturas de §3.4 son cálculo, no prototipo. La regla de «las horas siguen al cocinero» de §3.3 es un esbozo: su diseño es trabajo del lote 6.
- **Cifra de ergonomía.** Los 18-22 minutos de arco proceden de referencias secundarias a ISO 9241-303 y a una norma alemana de puestos con pantalla, no de la norma leída: [estudio que cita la ISO](https://link.springer.com/10.1111/opo.13170), [resumen de la norma alemana](https://people.freebsd.org/~debdrup/German%20Workplace%20Standards.pdf). `VERIFICAR` antes de usarla como requisito.
- **No se ha modificado** código, `data/`, skills ni documentos reservados. Los únicos archivos nuevos son este informe y las capturas.
