# ADR-0018: Calcular las horas pendientes desde lo que ha pasado de verdad («el plan sigue al cocinero»)

- Estado: aceptado (2026-11-15)
- Fecha: 2026-11-15
- Relacionado: `docs/decisiones.md` § Ronda 19 · legibilidad del cronómetro y de los textos («El plan sigue al cocinero», «Cabecera»), [auditoría de legibilidad](../analisis/legibilidad-crono-y-textos.md) (H8, §3.3, lote 6), [ADR-0005](0005-cronometro-alarmas-sin-fgs.md) (cadena de alarmas; este ADR es su anexo), [ADR-0016](0016-temporizador-libre-y-recordatorio-de-empezar.md) (recordatorio y estado tolerante), [ADR-0017](0017-dos-textos-por-paso-orden-compuesta.md), [esquema de receta](../cocina/esquema-receta.md) (§ Dependencias, § Holgura), [spec del legado](../spec/app-legado.md)

## Contexto
- **La hora de cada paso es de reloj.** `Plano.sugeridoMs` = `servir − total + off`
  (`receta/src/main/kotlin/app/trombito/souschef/receta/crono/Plano.kt:38-42`). Solo depende de la hora de servir y
  del `off` de la receta. No cuenta nada de lo que ha pasado en la cocina.
- **El aviso «Toca empezar» sale de esa misma hora.** Es el único sitio donde se fija su instante
  (`AvisosCrono.kt:145-152`). `siguienteEvento` (`AvisosCrono.kt:18-26`) ni siquiera recibe `ahoraMs`. La cadena de
  ADR-0005 arma ese instante tal cual (`app/.../domain/crono/CadenaAlarmas.kt:110-127`).
- **Más consumidores de la misma fórmula:**
  - `ConsultasCrono.vista`, para la clase ESPERA/YA (`ConsultasCrono.kt:47-69`).
  - `paralelo` (`ConsultasCrono.kt:137`).
  - `sugerido` (`ConsultasCrono.kt:31-38`), que lee CLÁSICA (`ui/crono/clasica/PanelClasica.kt:82,110`, `PlanClasica.kt:210`).
  - `MotorCrono.hecho`, que marca `avisado` al pasar de paso (`MotorCrono.kt:95`).
  - `SesionCrono.abrir`, que hace lo mismo al abrir la cocina (`SesionCrono.kt:61`).
- **La hora de llegada no ve dependencias ni huecos.** `llegada = ahora + máx(restante por carril)`
  (`ConsultasCrono.kt:97`). Si se abre la cocina 10 min antes sin arrancar nada, «Llegas a las…» ya adelanta 10 min.
  Si un carril espera 57 min a otro (hay recetas así), la llegada sale corta. La cabecera solo enseña ese dato
  (`ui/crono/CronoCocina.kt:209`).
- **El caso real (H8).** En huevos rotos, servir a las 12:00 (origen del plan a las 11:13). La 1ª fritura arrancó a
  las 11:05, 19 min antes, y la sartén pequeña (`calentar-sarten-pequena`, `off` 28, `junto: primera-fritura`) seguía
  marcando las 11:41. Con retraso pasa lo contrario: el aviso llega antes de tiempo y la sartén se calienta vacía.
- **`junto` existe para esto y el motor no lo usa.** El esquema lo define como «paso que corre en paralelo cuando el
  momento exacto importa». El validador solo comprueba que exista y que sea de otro carril
  (`receta/.../Validador.kt:122-129`). La UI solo lo pinta (`ui/crono/VistaCarril.kt:200`).
- **Lo que el estado persistido sabe hoy** (`EstadoCrono.kt:10-57`):
  - De un paso en marcha, `finAtMs`. No guarda cuándo arrancó: `arrancar` no lo anota (`MotorCrono.kt:151`).
  - De los pasos hechos, solo `pasosReales` (duración, desde el Hecho anterior o desde abrir) y el `inicioPasoMs` del
    paso actual, que es el instante del Hecho anterior.
  - Al terminar un carril, `EstadoCarril(fin = true)` pierde ese instante (`MotorCrono.kt:91`). Así no se puede saber
    cuándo acabó el último paso de un carril, que es justo de lo que dependen otros carriles por `dep`.
- **Lo que ya no existe.** El desvío por carril («−19») se calculaba en `VistaCarril` y salió en `ca7d2c2`
  (v1.8.0, hito 1). Hoy esas líneas (`VistaCarril.kt:181-200`) solo pasan `sugeridoMs` y `faltanMin`
  (`VistaCarril.kt:184` y `:194`).
- **Datos (33 recetas, 407 pasos):**
  - Los grafos de colas, `dep` y `junto` no tienen ciclos en ninguna receta.
  - 113 de las 157 aristas `dep` tienen hueco 0. Las demás esperan entre 1 y 61 min (reposos, enfriados).
  - Hay 13 `junto`, con desfase entre −1 y +17 min respecto al arranque del compañero.
  - 28 pasos solo se sitúan por el reloj, sin `dep` ni `junto`: 12 primeros pasos de carril con `off` > 0 (casi todos
    «precalentar» o «calentar aceite») y 16 con hueco en su propia cola.
- **El estado es JSON en Room** (`AlmacenCrono.kt:50-56`, `ignoreUnknownKeys = true`). Un campo nuevo con valor por
  defecto no cambia el esquema de Room.

## Decisión
La hora prevista de cada paso pendiente se calcula, en cada consulta, a partir de lo que ha pasado de verdad. Ya no
sale del reloj de servir. Se propaga por las colas, `dep` y `junto`, conservando los huecos de la receta. El aviso
«Toca empezar» suena a esa hora prevista, pero solo cuando no depende de `ahora` («firme»). La cabecera enseña el
fin previsto más tardío. Es una función pura de `(estado, receta, ahoraMs)` en `:receta`, sin nada nuevo en el
reloj ni en la cadena de alarmas.

### Hechos que se persisten
Dos campos nuevos en `EstadoCrono`, opcionales y con valor por defecto:
- `inicioRealMs: Map<String, Long> = emptyMap()`: paso → instante de su **primer** arranque (EMPEZAR, «Empezar
  igual», «Empezar antes» o «Hecho y siguiente»). `reanudar` no lo pisa.
- `finRealMs: Map<String, Long> = emptyMap()`: paso → instante de su Hecho. `deshacer` lo quita, igual que hoy quita
  `pasosReales`.

Ninguno de los dos entra en el historial. `reiniciar` los vacía, porque vuelve a `inicial`. Se decodifican con
tolerancia, como `libres` (ADR-0016): si un mapa viene mal formado se pierde ese mapa y se usan los respaldos de
abajo, nunca la cocinada.

### Fórmula
Notación, toda en ms:
- `O_plan = servirMs − totalMs`.
- `off(p) = medioMinutoMs(p.off)`, `dur(p) = p.segundos · 1000` y `finPlan(p) = off(p) + dur(p)`.

**Aristas de un paso `p`** (cada una da un término):

| Arista | Término |
|---|---|
| Cola: `q`, el paso anterior de su carril | `finEf(q) + (off(p) − finPlan(q))` |
| `dep`: cada `d` | `finEf(d) + (off(p) − finPlan(d))` |
| `junto`: `j` | `inicioEf(j) + (off(p) − off(j))` (ancla al **arranque** de `j`, no a su fin) |
| Raíz (sin cola, `dep` ni `junto`) | `origen(p)` (ver abajo) |

- **`previsto(p) = máx(términos)`.** Si un paso no tiene ninguna arista, solo cuenta el término de raíz. El hueco
  `off(p) − finPlan(·)` puede ser negativo hasta 30 s (`s` frente a `min`, regla 17) y no se recorta. Así se cumple
  el invariante principal: **sin desvíos, `previsto(p) = O_plan + off(p)`, que es la hora de hoy**.
- **`origen(p)`.** El arranque de la cocina `A` es el mínimo `inicioEf` de los pasos con `off(p) = 0` que ya han
  arrancado o están hechos. La regla 1 garantiza que alguno existe.

  | Situación | `origen(p)` | Firme |
  |---|---|---|
  | Existe `A` | `A + off(p)` | sí |
  | Sin `A` y `off(p) = 0` | `O_plan` | sí (es el paso que va tarde) |
  | Sin `A` y `off(p) > 0` | `máx(O_plan, ahora) + off(p)` | solo si `ahora ≤ O_plan` |

  Abrir la cocina no es arrancar. Que el primer arranque sea «Empezar antes» en un paso con `off` > 0 tampoco mueve
  el origen.

**Valores efectivos según el estado de cada paso:**

| Estado | `inicioEf` | `finEf` | Flotante si… |
|---|---|---|---|
| Hecho | `inicioRealMs[p] ?: finReal(p) − dur(p)` | `finReal(p)` | nunca |
| En marcha | `inicioRealMs[p]` | `máx(finAtMs, ahora)` | `finAtMs < ahora` (vencido sin Hecho) |
| En pausa | `inicioRealMs[p]` | `ahora + restanteS · 1000` | siempre |
| Pendiente (paso actual sin arrancar, o futuro) | `máx(previsto, ahora)` | `inicioEf + duración` | algún término flotante, o `previsto < ahora` |

- **Duración de un pendiente.** En el paso actual es `(restanteS ?: segundos) · 1000`, para que cuente la prórroga
  hecha antes de arrancar. En un paso futuro, `dur(p)`.
- **Firmeza.** `previsto(p)` es **firme** si todos sus términos lo son. «Flotante» quiere decir que el valor se mueve
  con `ahora` mientras nadie toque nada.
- **Orden de cálculo.** El grafo (colas, `dep` y `junto`) se ordena topológicamente una vez por `Plano`. Por si acaso
  apareciera un ciclo (el validador lo prohíbe, ver abajo), se ignoran las aristas `junto` que lo cierran: el motor
  nunca entra en bucle.
- **Respaldos** para estados sin los mapas (deterministas y sin excepciones; en la práctica no se dan, porque la
  actualización de Play espera a que no haya cocinada viva, ADR-0016 § Contexto):
  - `finReal` de un paso hecho: el `inicioPasoMs` del paso actual si es su anterior en la cola; si no, `O_plan + finPlan(p)`.
  - `inicioReal` de un paso en marcha: `finAtMs − dur(p)`.
  - `inicioReal` de un paso en pausa: `inicioPasoMs`.
- **Hora de llegada.** `llegadaMs` es el máximo `finEf` de los pasos no hechos. «Llegas a las HH:mm» es solo eso: sin
  diferencia y sin color (`docs/decisiones.md` § Ronda 19). Sin nada arrancado y antes de `O_plan`, coincide con la
  hora de servir, salvo el redondeo de `s`.
- **Holgura no entra en la fórmula.** Dice cuánto aguanta un paso esperando, no cuándo empieza. Meterla haría saltar
  las horas de forma imprevisible. Se sigue usando para «aguantan» y «no esperan» (`ConsultasCrono.plan`).

### Qué pasa con cada mando
- **Empezar, «Empezar igual» y «Empezar antes»** anotan `inicioRealMs[p] = ahora` si no estaba. Desde ahí, los
  `junto` que apuntan a `p` cuentan desde ese arranque, y sus `dep` y su cola desde `finAtMs`. Si `p` tiene
  `off = 0`, puede fijar `A`.
- **Pausa.** El paso deja de ser firme: su fin crece con `ahora`, y con él sus dependientes y la llegada. Los `junto`
  que apuntan a él no se mueven, porque anclan al arranque.
- **Reanudar.** Vuelve a ser firme con el nuevo `finAtMs`. No toca `inicioRealMs`.
- **Prórroga (+30 s).** En marcha suma a `finAtMs`. Parado suma a `restanteS`, que es la duración del pendiente. Los
  dependientes por cola y por `dep`, y la llegada, se mueven lo mismo. Los `junto`, no.
- **Hecho.** Anota `finRealMs[p] = ahora`. El `avisado` del siguiente paso del carril pasa a ser `previsto(siguiente,
  estado nuevo) ≤ ahora`. Es la misma regla de hoy: el cocinero está delante y no hay que avisarle del paso que tiene
  en la mano. Con hueco 0 no suena. Con hueco, suena al acabar el hueco.
- **Deshacer.** Quita `finRealMs` del paso deshecho. Como todo es derivado, las horas vuelven a ser exactamente las
  de antes del Hecho. El `avisado` de otros carriles no se restaura. `rearmaToca` ya lo rearma si el carril vuelve a
  quedar bloqueado.
- **Carril bloqueado** (`dep` sin hacer y sin `salto`). Tiene `previsto`, porque las horas de la franja y del
  diagrama lo necesitan, pero no avisa «Toca empezar», como hoy. Cuando se desbloquea, su hora se vuelve firme. Si
  ya ha pasado, avisa en el acto (hoy pasa lo mismo cuando se va tarde).
- **Hora de servir.** Solo mueve `O_plan`, así que después del primer arranque no cambia ninguna hora. Ver «Dudas
  para Joan».
- **Abrir la cocina.** `avisado = previsto ≤ ahora` en los pasos actuales, igual que hoy pero con la hora nueva.

### Cómo se reprograman los avisos «Toca empezar»
- **Regla del aviso.** El paso actual de un carril avisa «Toca empezar» si:
  - no corre, no está en pausa y no está `avisado`;
  - tiene `salto` o no está bloqueado;
  - **su `previsto` es firme**.

  El evento es `Evento(previsto, TOCA_EMPEZAR, carril, t)`. `siguienteEvento`, `siguienteEventoFuera` y `vencidos`
  aplican la misma regla, y los tres reciben `ahoraMs`. Los hitos y el fin no cambian: siguen saliendo de `finAtMs`.
- **Cuándo se recalcula.** Nunca por temporizador propio. La hora es derivada, así que basta con lo que ya hay:
  - tras cada mando (`CronoViewModel.ejecutar` → `cadena.reprogramar`, `CronoViewModel.kt:640-652`);
  - en cada disparo de la alarma (`procesar`);
  - al pasar a segundo plano;
  - en `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` y al cambiar el permiso;
  - con la cocina en pantalla, en el tic de 1 s, que llama a `vencidos` con `ahora`.
- **Por qué no hacen falta más recálculos.** Un `previsto` firme solo cambia cuando cambia el estado, y eso solo pasa
  con un mando. Una hora firme solo pasa a flotante en tres casos:
  - un paso en marcha llega a cero sin Hecho, y en ese instante suena su FIN;
  - un pendiente pasa su hora sin arrancar, y en ese instante suena su «Toca empezar»;
  - una pausa, que es un mando.

  En los tres casos ya hay un disparo, y `procesar` reprograma.
- **Sin despertares en falso.** Las horas flotantes no generan eventos. Si los generaran, la alarma sonaría a
  `ahora + hueco`, al despertar la hora se habría movido y se volvería a armar sin avisar nada, en bucle.
- **Idempotencia y ids estables.**
  - Sigue habiendo **una** alarma del sistema, con el mismo `PendingIntent` (requestCode 4100, `FLAG_UPDATE_CURRENT`,
    `AlarmaSistemaAndroid.kt:24-53`). Adelantar o retrasar un aviso es sustituirla.
  - La deduplicación sigue en el flag persistido `EstadoCarril.avisado`, uno por paso actual de cada carril. Se pone a
    cero al cambiar de paso y se escribe en la misma transacción `almacen.modificar` que consume el aviso.
  - Con el mismo estado y el mismo `ahora` se obtienen el mismo evento y los mismos avisos.
  - Un aviso que ya sonó no vuelve a sonar aunque su hora se mueva después.
  - Los `junto` con desfase 0 suenan en el mismo instante que su compañero y `AgrupadorAlarmas` los junta en una sola
    alarma.
- **Pantalla apagada y Doze.** Nada nuevo: `setAlarmClock` al `ms` del evento, como en ADR-0005.
- **Muerte del proceso.** Los hechos (`inicioRealMs`, `finRealMs`, `finAtMs` y los flags) están en Room. `procesar`
  recalcula desde el estado al despertar. Lo único en memoria sigue siendo `activas`, como hoy.

### Qué no cambia
- La cadena de una sola alarma con `setAlarmClock`, los permisos, los canales, la notificación fija y la alarma
  agrupada (ADR-0005).
- Los avisos de hito y de fin, el silencio pasados 90 s y la regla `SesionCrono.vivo`.
- El recordatorio «Avísame para empezar» de la ficha (ADR-0016). Usa `O_plan`, que coincide con la regla de una
  cocina sin arrancar.
- La ficha, el gantt y la impresión: siguen en minutos de receta.
- `pasosReales`, el resumen, los desvíos del cierre y el historial (entidad Room sin cambios).
- Los temporizadores libres.
- Los nombres públicos `VistaEstado.sugeridoMs` y `VistaCarril.sugeridoMs` se mantienen para no tocar la UI, pero ahora
  significan «hora prevista que sigue al cocinero».

### Validador
- La regla 2 amplía la comprobación de ciclos: **sin ciclos en el grafo de colas, `dep` y `junto`** (ERROR). Las 33
  recetas actuales la pasan.
- Para los 28 pasos que solo siguen al reloj se propone, sin regla nueva, una tanda del `chef-reviewer` que añada
  `junto` o `dep` donde el acople con otro carril es real (los «precalentar» y «calentar aceite» primero). La guarda
  `scripts/guarda-recetas.mjs` marcará esos cambios y se justifican con este ADR.

### Archivos que cambian
- `:receta` (TDD primero):
  - `crono/Plano.kt`: orden topológico y huecos; fuera `sugeridoMs`.
  - `crono/Prevision.kt` (nuevo): `PasoPrevisto(inicioMs, finMs, firme)` y `llegadaMs`.
  - `crono/AvisosCrono.kt`, `crono/ConsultasCrono.kt`: `previstos(e, receta, ahoraMs)`, más `ahoraMs` en `sugerido`
    y en `siguienteEvento*`.
  - `crono/EstadoCrono.kt`: los dos mapas, con decodificación tolerante.
  - `crono/MotorCrono.kt`, `crono/SesionCrono.kt`.
  - `Validador.kt`: regla 2.
- `:app`:
  - `domain/crono/CadenaAlarmas.kt`: pasar `ahora` a `siguienteEvento`.
  - `ui/crono/clasica/PanelClasica.kt` y `PlanClasica.kt`: firma de `sugerido`.
  - `ui/crono/CronoCocina.kt`: el ajuste de servir tras el arranque.
  - `ui/crono/DatosCrono.kt`: calcular `previstos` una vez por tic.
- Docs: `docs/cocina/esquema-receta.md` (§ Dependencias, `junto` como ancla, regla 2), `docs/arquitectura.md`,
  `docs/spec/app-legado.md` (nota en la fórmula de `sugerido`) y el enlace a este ADR en ADR-0005 § Relacionado.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Desplazamiento global único: sumar a todas las horas el desvío del paso en marcha (lo que proponía §3.3) | Muy simple; un solo número | Con dos carriles en marcha no hay un desvío único; ignora `junto` y `dep`; mueve carriles que no dependen del que va tarde | No cumple «`junto` ancla al arranque real» y falla con varios carriles |
| Suelo de reloj para todos los pasos: `máx(aristas, origen + off)` | Las horas nunca adelantan de golpe; tapa los huecos sin `dep` | Solo se propaga el retraso: acabar antes no adelanta nada | Contradice la decisión de Joan (adelanto y retraso) |
| Replanificar con un optimizador (3 fuegos, holgura, reordenar) | Plan «óptimo» en cada momento | Complejo, difícil de probar y puede cambiar el orden que el cocinero tiene en la cabeza | Desproporcionado; rompe el sitio fijo de los carriles |
| Mover solo la pantalla y dejar las alarmas en hora de reloj | No toca la cadena | La pantalla y el aviso no coinciden; H8 sigue con el móvil bloqueado | Inconsistente; el problema es justo el aviso |
| Guardar las horas previstas en el estado en cada mando | Lectura barata | Duplica datos derivables; se desincroniza con pausas y vencidos; más campos que migrar | Derivar es puro, determinista y sin desfase |
| Usar la holgura para decidir quién se mueve | Más «culinario» | Reglas opacas; horas que saltan sin un mando | Holgura dice cuánto aguanta, no cuándo empieza |
| **Propagar por colas, `dep` y `junto` con hechos reales persistidos** | Cumple adelanto, retraso y `junto`; puro; sin migración; la cadena no cambia | Depende de que las recetas declaren sus acoples | **Elegida** |

## Consecuencias
- Positivas:
  - Las horas, los «en N min» y los avisos «Toca empezar» coinciden con los fuegos reales, también con la pantalla
    apagada.
  - `junto` por fin tiene efecto.
  - «Llegas a las HH:mm» cuenta dependencias y huecos, y no se adelanta por abrir la cocina antes.
  - Sin migración de Room y sin cambios en ADR-0005: la cadena sigue armando «el próximo evento», solo que calculado
    de otra forma.
- Negativas y costes:
  - **El modelo es tan bueno como los acoples declarados.** Un paso sin `dep` ni `junto` sigue al arranque de la
    cocina o a su cola, no al carril con el que se acopla de verdad (28 pasos hoy).
  - **Más avisos «en el acto».** Con hueco 0 (113 aristas `dep`), al pulsar Hecho en un carril el que dependía de él
    avisa «Toca empezar» en ese momento. Antes eso solo pasaba yendo tarde.
  - **La hora de servir deja de mandar tras el primer arranque.**
  - **Tests que cambian a propósito.** En `MotorCronoTest` los de «Plan y sugerido» y «Plan, retraso, paralelo»
    (llegada en hora `servir − 7 min` y `sugeridoNoTruncaMediosMinutos` tras un Hecho) se reescriben con la regla
    nueva. Ninguno se desactiva.
  - CLÁSICA cambia de firma hasta que se retire.
  - Coste de cálculo: O(pasos + aristas), menos de 30 pasos, una vez por tic. Despreciable.
- Choques: **ninguno con ADR-0005** en el mecanismo (una alarma, `setAlarmClock`, flags en Room, reprogramar tras cada
  cambio). Su texto («toca empezar» a `servir − total + off`, en la spec) queda superado y hay que enlazarlo. **Ningún
  dato persistido necesita migración**: `EstadoCrono` es JSON y los campos nuevos tienen valor por defecto. Una
  versión anterior de la app los ignora (`ignoreUnknownKeys`).
- Seguimiento:
  - TDD en `:receta` con los escenarios de abajo antes de tocar código.
  - `architecture-reviewer` sobre el diff.
  - Escenarios `qa-scenario` en el móvil antes de publicar: pantalla apagada, Doze, muerte del proceso y una cocinada
    real de huevos rotos (riesgo 5 del plan).
  - Tanda del `chef-reviewer` sobre los 28 pasos.

### Respuestas de Joan (2026-11-15)
1. **Aviso en el acto en otro carril tras un Hecho:** suena, como hoy cuando vas tarde.
2. **Hora de servir tras el primer arranque:** tocar «Llegas a las…» solo abre el ajuste antes de arrancar; después
   solo informa.
3. **Los 28 pasos que solo siguen al reloj:** el `chef-reviewer` les añade `junto` o `dep` donde corresponde, sin cambiar
   tiempos, potencias ni cantidades.

## Escenarios de test (para el test-engineer)
**Receta sintética R** (total 37 min; carriles `mesa`, `sarten` y `olla`, de fuego, y `horno`):

| Paso | Carril | `off` | `s` | `dep` / `junto` | Arista y hueco |
|---|---|---|---|---|---|
| m1 | mesa | 0 | 600 | – | raíz con `off` 0 |
| s1 | sarten | 10 | 300 | `dep` m1 | `dep`, hueco 0 |
| s2 | sarten | 15 | 900 | – | cola tras s1, hueco 0 |
| o1 | olla | 20 | 300 | `junto` s2 | +5 min desde el arranque de s2 |
| o2 | olla | 25 | 600 | – | cola tras o1, hueco 0 |
| h1 | horno | 12 | 600 | – | raíz con `off` 12 |
| h2 | horno | 30 | 300 | – | cola tras h1, hueco 8 |
| m2 | mesa | 35 | 120 | `dep` s2, o2 | cola tras m1 (hueco 25); `dep` s2 (hueco 5) y o2 (hueco 0) |

`S` = servir; `T0 = S − 37 min`. «Previsto» es `ConsultasCrono.previstos`. «Evento» es
`AvisosCrono.siguienteEvento(e, R, ahora)`.

**Estado base B** (ahora `T0+23`):
- m1 empezado a `T0` y hecho a `T0+10`.
- s1 empezado a `T0+10` y hecho a `T0+15`.
- s2 empezado a `T0+15` (`finAt` `T0+30`).
- h1 empezado a `T0+12` y hecho a `T0+22`.
- o1 empezado a `T0+20` (`finAt` `T0+25`).

1. **Invariante sin desvíos.** Para cada una de las 33 recetas de `data/recetas`: `inicial` con
   `ahora = servir − total − 60 min` → `previsto(p) = servir − totalMs + medioMinutoMs(off)` en todos los pasos, todos
   firmes. `llegadaMs = servir − totalMs + máx(off(p) + s·1000)`.
2. **Abrir antes sin arrancar.** R, `abrir` a `T0−10` → previstos como en 1. Evento = TOCA m1 a `T0`. Llegada = `S`
   (hoy daría `S − 10`).
3. **Arranque adelantado.** Empezar m1 a `T0−10` → `A = T0−10`.
   - h1 = `T0+2`; s1 = `T0` (bloqueado, sin TOCA); s2 = `T0+5`; o1 = `T0+10` firme; h2 = `T0+20`; m2 = `T0+25`.
   - Llegada `S − 10`. Evento = FIN m1 a `T0`.
4. **Hecho adelantado.** m1 empezado a `T0` y hecho a `T0+5`.
   - s1 = `T0+5`, firme y no bloqueado: `vencidos(T0+5)` da TOCA s1, no silencioso.
   - o1 = `T0+15`; m2 = `T0+30`; h1 = `T0+12`; h2 = `T0+30`.
   - Llegada = `T0+35` (manda h2).
5. **Prórroga.** B + 2 × `masTreinta(sarten)` → s2 `finAt` `T0+31`. m2 = `T0+36` y llegada = `S+1`. o1, o2 y h2 sin
   cambios.
6. **Pausa.** B + o1 hecho a `T0+25` + o2 empezado a `T0+25` + s2 en pausa a `T0+23` (le quedan 420 s).
   - A `T0+26`: m2 = `T0+38`, no firme; llegada `T0+40`.
   - A `T0+27`: m2 = `T0+39`; llegada `T0+41`.
   - o2 y h2 no cambian.
7. **`junto` ancla al arranque (H8 sintético).** m1 empezado a `T0`, hecho a `T0+5`; s1 empezado a `T0+5`, hecho a
   `T0+8`; s2 empezado a `T0+8`.
   - o1 = `T0+13` firme (hoy, `T0+20`). `siguienteEventoFuera(excepto = "horno")` = TOCA o1 a `T0+13`.
   - En `:app`, `CadenaAlarmas` con `AlarmaSistema` falso y la cocina fuera de pantalla: tras el mando,
     `programar(T0+12)` (TOCA h1). Tras empezar h1 a `T0+12`, `programar(T0+13)`.
8. **`junto` a un compañero aún sin arrancar.** m1 empezado a `T0`, ahora `T0+1` → o1 = `T0+20` firme. Tras
   `masTreinta(mesa)`: s2 = `T0+15:30` y o1 = `T0+20:30`.
9. **Compañero atrasado: hora no firme.** m1 empezado a `T0` y hecho a `T0+10`; s1 empezado a `T0+10` y hecho a
   `T0+15`; h1 empezado a `T0+12`; s2 y o1 sin empezar; ahora `T0+18`.
   - s2 es YA (previsto `T0+15`). o1 = `T0+23`, no firme. `siguienteEventoFuera(excepto = "horno")` = null.
   - Empezar s2 a `T0+19` → o1 = `T0+24` firme. `siguienteEventoFuera(excepto = "horno")` = TOCA o1 a `T0+24`.
10. **Raíz con `off` > 0 sin arrancar la cocina.** Nada empezado; ahora `T0+3`.
    - m1 = `T0` (YA). h1 = `T0+15`, no firme, sin evento.
    - Empezar m1 a `T0+4` → h1 = `T0+16` firme. Evento = FIN m1 a `T0+14`.
11. **Hecho sin empezar en un paso de `off` 0.** m1 hecho a `T0+12` sin EMPEZAR → `A = T0+2`. h1 = `T0+14`. s1 =
    `T0+12` y su TOCA vence en el acto.
12. **«Empezar igual».** B + `empezarIgual(mesa)` a `T0+23` → `salto`, `inicioRealMs[m2] = T0+23`, `finAt`
    `T0+25`. Llegada = `T0+35`.
13. **Deshacer.** Desde 4, `deshacer(mesa)` → `finRealMs` sin m1, `pasosReales` sin m1, m1 vuelve a estar en marcha.
    Los previstos son idénticos a los de antes del Hecho.
14. **Idempotencia.** `vencidos` dos veces con el mismo estado y el mismo `ahora` → la segunda no da avisos. Con el
    mismo estado y el mismo `ahora`, el mismo `Evento`.
15. **Sin despertares en falso.** En el estado de 6, `siguienteEvento` a `T0+26`, `T0+27` y `T0+28` → siempre TOCA h2
    a `T0+30` (el `ms` no se mueve con `ahora`). Propiedad: ningún TOCA devuelto tiene `firme = false`.
16. **Misma cola, con hueco y sin hueco.**
    - h1 empezado a `T0+12` y hecho a `T0+20` → h2 = `T0+28`, `avisado = false`; evento TOCA h2 a `T0+28`.
    - s1 hecho a `T0+16` → s2 = `T0+16`, `avisado = true`; sin TOCA s2.
17. **Llegada sin arrancar.** Nada empezado, ahora `T0−60` → llegada = `S`. Hoy daría `T0−60` + el restante máximo.
18. **Hora de servir.**
    - Sin arrancar, `servir(S + 15 min)` → todos los previstos y el TOCA m1 se mueven +15.
    - Con m1 empezado a `T0`, el mismo cambio no mueve ningún previsto ni la llegada.
19. **Estado viejo.**
    - JSON sin `inicioRealMs` ni `finRealMs`, con B → se decodifica y se aplican los respaldos: s2 `inicio` =
      `finAt − dur`; s1 `finReal` = `inicioPasoMs` de s2. Sin excepciones.
    - JSON con un mapa mal formado → se decodifica la cocinada sin ese mapa.
20. **Reanudar no pisa el arranque.** B + pausa de s2 a `T0+23` + reanudar a `T0+24` → `inicioRealMs[s2] = T0+15` y
    o1 sin cambios.
21. **Ciclo, por defensa.** Receta sintética con a `junto` b y b `junto` a → `previstos` termina, sin bucle, ignorando
    esos `junto`. El validador da ERROR de regla 2 («ciclo»).
22. **Huevos rotos real (H8).** Servir a las 12:00.
    - mise-en-place: empezado 10:54, hecho 11:00. calentar-aceite: empezado 11:00, hecho 11:05. primera-fritura:
      empezado 11:05.
    - A las 11:05: calentar-sarten-pequena = 11:22, firme (hoy, 11:41); rasgar-jamon-lavar-padron = 11:06.
23. **Validador.** Las 33 recetas pasan la regla 2 ampliada sin hallazgos.
24. **Clases de vista.** En 3, a `T0−10`: `vista(horno)` = ESPERA con `sugeridoMs` `T0+2` y `vista(sarten)` = BLOQ.
    En 9, a `T0+18`: `vista(sarten)` = YA.
25. **Temporizadores libres.** Un libre puesto en B sale en `siguienteEvento` con el mismo `ms` que hoy y no altera
    ningún previsto ni la llegada.

## Anexo (auditoría final): hueco de cola
- **Problema (H1, `docs/analisis/auditoria-final-ronda19.md`).** El término de cola conservaba el hueco de la receta y hacía de suelo: si los `dep` acababan antes, el paso seguía esperando a «fin real del anterior del carril + hueco» (arroz a la cubana: «Aliñar y montar» a 51 min con todo hecho).
- **Regla.** Si el paso tiene algún `dep` o `junto` aceptado en el grafo, su cola aporta solo el carril libre: `finEf(q) + mín(0, off(p) − finPlan(q))`. Son los enlaces los que lo sitúan. Sin enlaces, la cola conserva su hueco (escenario 16, h2).
- **Invariante.** Sin desvíos cada `dep` da `O_plan + off(p)` (conserva su propio hueco) y cada `junto` también, así que `previsto = O_plan + off` se mantiene en las 33 recetas sin regla nueva de validador. El `mín(0, ·)` respeta el solape de hasta 30 s de la regla 17.
- **Tests.** `PrevisionAdelantoTest`: arroz a la cubana, `huevos-rotos:reposo-patata` 5 min antes y el invariante cocinando a su hora todas las recetas (además de `PrevisionTest.e01`).
