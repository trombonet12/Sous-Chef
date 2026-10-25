# ADR-0016: Añadir al cronómetro el temporizador libre y el recordatorio «Avísame para empezar»

- Estado: propuesto
- Fecha: 2026-10-25
- Relacionado: anexo de [ADR-0005](0005-cronometro-alarmas-sin-fgs.md), [ADR-0008](0008-voz-en-el-cronometro.md) (orden de voz nueva), `docs/decisiones.md` § Ronda 18 · v1.7, `docs/analisis/v1.7.md` (F9, F11, A8), `docs/play.md` (declaración de permisos), [arquitectura](../arquitectura.md)

## Contexto
- **Temporizador libre (F11).** Cocinando, a veces hace falta contar un tiempo que la receta no tiene: un reposo, el
  horno de otra cosa o «déjalo 5 minutos más». Ahora solo hay carriles de la receta (`Plano`), con sus fuegos, sus
  pasos y sus `pasosReales`, que acaban en el historial.
- La regla de «cocinada viva» está en un solo sitio desde A8: `SesionCrono.vivo` (abierta, sin caducar y con algún
  carril sin terminar). La usan `CocinadasVivas` (una sola cocinada a la vez, actualización de Play, banda «Cocinando»
  de la ficha) y `CadenaAlarmas`, que solo arma alarmas de las cocinadas vivas.
- `EstadoCrono` se guarda como JSON en Room. `AlmacenCrono` lo decodifica con `ignoreUnknownKeys = true`, y si falla la
  decodificación, devuelve null y la cocinada se pierde.
- **«Avísame para empezar» (F9).** La ficha dice «Empieza a las HH:mm» según la hora de servir. Joan quiere que el móvil
  le avise a esa hora sin abrir la cocina antes, porque una cocinada abierta cuenta como viva y bloquea otras cosas.
- **Permiso de alarma exacta (manifest):** `USE_EXACT_ALARM` (API 33+) y `SCHEDULE_EXACT_ALARM` con
  `maxSdkVersion="32"`, los de ADR-0005. Play restringe `USE_EXACT_ALARM` a las apps cuya función principal es una
  alarma, un temporizador o un calendario. Sous Chef lo declara como temporizador de cocina (`docs/play.md`). ADR-0005
  ya cuenta «toca empezar» entre los avisos del cronómetro. El recordatorio es ese mismo aviso del primer paso, pero
  antes de abrir la cocina.
- `AlarmaSistemaAndroid` usa los requestCode 4100 (disparo) y 4101 (`AlarmClockInfo`). Las notificaciones abren
  `MainActivity` con intents explícitos que llevan un `data` propio (`souschef://crono/{slug}`, `souschef://alarma/{slug}`),
  sin intent-filter en el manifest.

## Decisión
### Temporizador libre
- `EstadoCrono.libres: List<TemporizadorLibre> = emptyList()`. Cada uno lleva id, etiqueta, duración, `finAtMs` en
  reloj de pared (o lo que le queda, si está en pausa) y un flag `avisado`, como un `EstadoCarril`. Los nombres
  exactos los fija la implementación.
- **Decodificación tolerante:** los estados viejos no traen el campo y entran con la lista vacía. Si `libres` viene
  mal formado, `AlmacenCrono` vuelve a decodificar sin ese campo: se pierden los libres, nunca la cocinada. Una versión
  anterior de la app lo ignora por `ignoreUnknownKeys`.
- **Fuera de la receta:** no entra en `Plano` ni en el gantt, no cuenta para los 3 fuegos, no escribe `pasosReales` y
  no sale en el resumen, ni en los desvíos ni en «Tu ritmo». Tampoco entra en `deshacer` ni en `ultimoHecho`.
- **Cómo se crea:** con «+ Temporizador» en la cocina o con la voz «Chef, pon N minutos» (orden nueva de
  `InterpreteVoz`, TDD, ADR-0008). Duración de 1 a 180 min y como mucho 3 a la vez. Solo existe dentro de una cocinada
  viva: con la cocina cerrada no se ofrece.
- **Cómo avisa:** igual que un carril. El motor de `:receta` lo incluye en `AvisosCrono.siguienteEvento` y `vencidos`
  con un `TipoAviso` propio. `CadenaAlarmas` lo arma en la misma cadena `setAlarmClock` y lo publica como una línea más
  de la alarma agrupada (`AgrupadorAlarmas`), sin «Hecho y siguiente». La notificación fija le da una línea. Con la
  cocina en pantalla avisan la app y la voz, como siempre.
- **Mantiene viva la cocinada: sí, mientras no ha avisado.** `vivo` pasa a ser: abierta, sin caducar y con algún carril
  sin terminar **o algún libre sin avisar**. Motivos:
  - La cadena solo arma alarmas de cocinadas vivas. Si el libre no contara, un «pon 5 minutos» puesto al terminar el
    último carril (para el reposo, por ejemplo) no sonaría con el móvil bloqueado. Fallaría en silencio, que es lo
    que ADR-0005 no admite.
  - La regla sigue en un solo sitio (`SesionCrono.vivo`), así que la cadena, la actualización de Play, «una sola
    cocinada a la vez» y la banda de la ficha la cumplen a la vez sin código nuevo.
  - No alarga la cocinada sin límite: un libre que ya avisó no cuenta, y la caducidad de 12 h se mantiene.
  - La pantalla de cierre sigue saliendo cuando terminan todos los carriles (`todoTerminado`), y enseña los libres
    pendientes. Cerrar o abandonar la cocinada los quita.

### «Avísame para empezar»
- Un chip debajo de «Empieza a las HH:mm» en la ficha arma **un recordatorio**, de uno en uno: armar otro sustituye al
  anterior. Se guarda en `meta` (slug, hora de empezar y hora de servir, en reloj de pared). No es un `EstadoCrono`,
  no está en `AlmacenCrono` y **no cuenta como cocinada viva**.
- No se ofrece con una cocinada viva ni si la hora de empezar ya pasó. **Abrir la cocina de cualquier receta lo
  cancela**, porque solo se cocina una a la vez.
- **Alarma propia:** `setAlarmClock`, como la cadena, con un `PendingIntent` de broadcast propio: acción
  `RECORDATORIO`, requestCode 4102 y 4103 para el `AlarmClockInfo`. No toca la alarma de la cadena (4100). El disparo
  lo recibe `AlarmaReceiver`.
- **Reprogramación:** las acciones que `AlarmaReceiver` ya recibe (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`,
  `TIMEZONE_CHANGED` y el cambio del permiso de alarmas exactas) releen `meta` y lo vuelven a armar. Si la hora pasó
  hace 10 min o menos, avisa en el acto; si pasó hace más, o la receta ya no está en el catálogo, se borra sin avisar.
- **Notificación normal:** canal propio `crono_recordatorio` con importancia por defecto: un sonido, sin
  `FLAG_INSISTENT`, sin `USAGE_ALARM` y **sin pantalla completa**. Id propio en `IdsNotificacion`. Se cierra al
  tocarla. El `contentIntent` es inmutable (`FLAG_IMMUTABLE`) y explícito a `MainActivity`, con
  `data = souschef://ficha/{slug}`. Ese `data` distinto evita que se confunda con los `PendingIntent` de la cocina, y
  la ruta solo la lee Navigation, sin intent-filter. Si falta `POST_NOTIFICATIONS`, se pide al tocar el chip.
- **Permiso:** se usa el `USE_EXACT_ALARM` que ya está, porque el recordatorio es el «toca empezar» del temporizador de
  cocina. Si la revisión de Play no lo admite para este uso, la alternativa ya está decidida:
  `setAndAllowWhileIdle` inexacto, sin permiso de alarma exacta, armado **10 min antes**, con el texto «Empieza hacia
  las HH:mm». Así el margen cae antes de la hora y no después. Es la misma caída que ya tiene `AlarmaSistemaAndroid`
  en Android 12-12L sin el permiso.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Libre como carril más de `Plano` | Reutiliza todo el motor | Contaría en fuegos, `pasosReales`, gantt e historial; ensucia «Tu ritmo» | Un tiempo que no es de la receta no debe entrar en sus datos |
| Libre que no mantiene viva la cocinada | `vivo` no cambia | Un libre puesto con todo terminado no suena fuera de la app | Fallo silencioso de un aviso pedido |
| Libre fuera de la cocinada (temporizador global) | Sirve sin receta | Otra cadena, otro estado y otra notificación | Fuera del alcance: Sous Chef no es un reloj genérico |
| Recordatorio como cocinada abierta en la portada | Sin estado nuevo | Bloquea la actualización de Play y otras cocinadas durante horas | Contradice «no cuenta como viva» |
| Recordatorio con la notificación de alarma (`crono_alarma_v3`) | Ya existe | Insistente y a pantalla completa por algo que puede esperar | Demasiado intrusivo para un recordatorio |
| Recordatorio con `setAndAllowWhileIdle` desde el principio | No depende de la política de alarma exacta | En Doze puede llegar con minutos de retraso | Queda como alternativa si Play no admite el uso |
| WorkManager programado | Persistente | Dependencia nueva y sin precisión de minutos | No cumple la puntualidad (ADR-0005) |

## Consecuencias
- Positivas:
  - Un temporizador extra sin tocar la receta ni el historial, y fiable con el móvil bloqueado.
  - Un aviso para empezar sin dejar abierta una cocinada.
  - Las dos cosas usan la cadena, los receivers y los canales que ya existen: no hay servicio ni dependencias nuevas.
- Negativas / costes:
  - `vivo` cambia. Hay que revisar los tests de `SesionCrono`, `CocinadasVivas` y `CadenaAlarmas`, y añadir los de los
    libres: TDD en el motor, en la decodificación y en el intérprete de voz.
  - `setAlarmClock` enseña el icono de alarma y la «próxima alarma» del sistema durante horas mientras el recordatorio
    está armado.
  - Un canal de notificación más, que el usuario ve en los ajustes del sistema.
- Seguimiento:
  - VERIFICAR en la política de Play de `USE_EXACT_ALARM` (y en la declaración de permisos de Play Console,
    `docs/play.md`) que un recordatorio que programa el usuario dentro de un temporizador de cocina entra en el uso
    admitido. Si no, aplicar la alternativa de `setAndAllowWhileIdle` 10 min antes.
  - VERIFICAR el retraso real de `setAndAllowWhileIdle` en Doze en el móvil (Android 16), si se llega a usar.
  - VERIFICAR que `setAlarmClock` con dos `PendingIntent` distintos (cadena y recordatorio) deja armados los dos en
    el móvil.
  - Probar en el móvil: el libre suena con la pantalla apagada y todos los carriles terminados; el recordatorio
    sobrevive a un reinicio.
