# Estado

| Fase | Estado |
|---|---|
| F0 Repo y gobierno | completada |
| F1 Esqueleto Android, CI, release, spike de alarmas | completada |
| MVP local-first (modo ahorro, ver plan) | **completado**: app funcional sin backend |
| F3/F4 Supabase + sync + login | **descartada** (ADR-0007) |
| F9b Revisión culinaria | completada (lotes 1-3) |
| Ronda 9 · cierre de la versión 1 | **completada**: v1.0.0 |

## Completado
- 2026-10-13 · F0 y F1 (gobierno, `.claude/`, docs, datos legados, esqueleto, tema, icono, CI, keystore, spike de alarmas).
- 2026-10-14 · MVP local-first:
  - `:receta`: modelos, JSON canónico, escalado, gantt, validador estructural (reglas 1-7, 10, 11, 16-18, 21, 23) y
    motor puro del cronómetro (`crono/`).
  - `sc validar [--estricto]` / `sc formatear [--comprobar]`: 13 recetas y 17 técnicas sin errores ni avisos.
  - App: recetas/técnicas empaquetadas en el APK desde `data/`; Room local (historial, preferencias, despensa,
    compra, estado del crono) sembrado desde `data/legado/` y `data/despensa-semilla.md` (vetos como `VETADO`).
  - Pantallas: lista de recetas, ficha, técnicas, historial, despensa + compra, cronómetro con cadena de
    `setAlarmClock`, notificaciones y deep link `souschef://crono/{slug}`.
  - APK release firmado instalado en el emulador; capturas `docs/capturas/mvp-*.png`.
- 2026-10-14 · Ronda 2 (probado en el móvil, Android 16): avisos HIGH verificados con pantalla apagada; notificación
  "En marcha" promocionada (Live Update) con cuenta atrás y acción Siguiente; avisos con id estable y caducidad;
  full-screen intent; plan plegable y hoja del porqué rediseñados; Deshacer al cerrar; letra del sistema.
- 2026-10-14 · Ronda 3 (v0.2.0): alarma estilo reloj (`AlarmaActivity` sobre el bloqueo, sonido en bucle hasta
  Parar, máx. 60 s); una sola cocinada viva a la vez (diálogo para abandonar, menú ⋮ con Abandonar/Reiniciar);
  sin "En marcha" fantasma; pulido de crono (plan en una línea, línea de tiempo, tapada/destapada en pestañas) y de
  lista/ficha/despensa/historial; CI `android.yml` solo en PR o a mano; release sin secretos de Supabase.
- 2026-10-14 · Ronda 4 (v0.3.1): `scripts/gh.mjs` y subida de secretos por script; CI en push a main y main
  protegida; validador con reglas 8, 12-15, 19, 20, 24 (13 recetas: 0 errores, 0 avisos); imprimir/guardar la ficha
  en PDF y "Compartir despensa" (probados en el móvil; releases v0.3.0 y v0.3.1 publicadas); skill `receta-cronometrada` al flujo real (sin `sc publicar/despensa`).
- 2026-10-16 · Ronda 5 (v0.4.0, v0.5.0): una notificación por carril (paso i/n, cuenta atrás, barra por pasos) y al
  vencer, la misma en canal de alarma insistente (sonido del sistema; probado en el móvil con pantalla apagada); sin
  "En marcha" (el chip lo lleva el carril más próximo); Trombito chef con consejos en el cronómetro; etiqueta pescado;
  skills sin comandos sc inexistentes; F9b lote 1 (arroz cubana, curry, emperador, ensalada de ñoquis, huevos rotos).
- 2026-10-17 · Ronda 6 (v0.6.0):
  - Backlog cerrado. Supabase descartado (ADR-0007); legado y roadmap viejo, históricos.
  - Release automática al cambiar `version.properties` y hacer push a main.
  - Despensa sin `compra`: todo ingrediente está en la semilla (R12); la ficha dice «Te falta».
  - Regla R25 «baja/sube al N».
  - Huevos rotos rehecha (47 min) y F9b lotes 2 y 3.
  - Fuera el Trombito. Cronómetro sin scroll con disposiciones FOCO y APILADA en Ajustes.
  - Notificaciones: ninguna en pantalla, una fija y una alarma agrupada (ADR-0005 actualizado).
  - Voz «Chef» con TTS (ADR-0008).
- 2026-10-17 · Ronda 7 → v0.7.0:
  - APILADA por defecto.
  - Gramática de voz ampliada: prórroga arbitraria, por qué, info, qué tengo que hacer, y carriles por número.
  - Insignia con el número en cada carril.
  - Salir de la cocina pide abandonarla.
  - Receta nueva `pollo-almendras` (136 min, 24 pasos, 2 fuegos).
- 2026-10-17 · Ronda 8 → v0.7.1:
  - Arreglado el cierre al abrir `pollo-almendras` (barra final del gantt).
  - Tarjeta compacta cuando falta alto: 4 carriles o letra grande.
  - Probado en el emulador con letra a 1,0× y 1,3×.
- 2026-10-17 · v0.7.2:
  - La tarjeta compacta muestra además el próximo hito, «Después» y la nota, si caben.
  - Repo hecho público.
- 2026-10-17 · v0.8.0: aviso de versión nueva (ADR-0009), al abrir y una vez al día; al tocar abre Obtainium.
- 2026-10-17 · v0.8.1: el texto largo se desplaza; «Después» lleva la duración del siguiente paso.
- 2026-10-18 · Ronda 9 → **v1.0.0** (probada en el móvil por Joan, OK):
  - Ficha: «Empieza a las HH:mm» según la hora de servir.
  - Receta nueva `aros-de-cebolla` (47 min, huevo espesado con harina; OK de Joan) y fuera la técnica del mismo nombre.
  - Fuera la despensa de la app (Room 1 → 2); Técnicas es la pestaña del medio.
  - Cronómetro: «Diagrama de tiempos» en el ⋮, el de la ficha a pantalla completa con scroll y la línea de «ahora».
  - Google Play preparado (ADR-0010): el CI genera el AAB y lo sube a la pista interna si hay credenciales; sin aviso de GitHub si se instala desde Play.
  - Interfaz CLÁSICA del cronómetro (la de antes de la v0.6) como tercera disposición; selector con miniaturas en Ajustes y en el ⋮ de la cocina («Interfaz»).
  - Voz: el carril por número se entiende también como «info dos», «de 2», «en el dos» o «de los», y entre alternativas gana la que nombra carril; orden nueva «próximos pasos» (general o de un carril).
  - Deshacer el último Hecho de un carril: minibotón ↶ en las tarjetas, filas y clásica, y por voz («deshaz», «atrás», «paso anterior» [+ carril]).
  - Atrás en el cronómetro vuelve a la ficha con la cocinada viva («Cocinando · N recipientes», botón «Volver a la cocina»); salir de la ficha pide abandonarla. Abandonar desde la cocina, solo en el ⋮.
  - Revisión de código de la ronda aplicada (voz: alternativas y número suelto más prudentes, «deshaz» detrás de las preguntas; deshacer caduca al tocar el carril y encadena por orden; horas de inicio coherentes).
  - Copia del historial (y favoritos/raciones) en un archivo del móvil: Ajustes → Historial → «Guardar copia» / «Cargar copia» (añade sin pisar).
  - Cronómetro: hora prevista de cada paso en las tarjetas, con «+N» (tarde) o «−N» (adelantado) en minutos.
  - Ficha: ingredientes «Todo junto» (suma los repetidos entre zonas) o «Por zona»; la impresa lleva las dos.

## Entorno
| Herramienta | Estado |
|---|---|
| JDK 21 (Gradle) | OK vía `~/.gradle/gradle.properties` |
| Android SDK 36 / build-tools 36.0.0 | OK |
| Emulador | AVD `trombito-36` |
| Keystore release | `~/.souschef/` (`node scripts/android-release.mjs`) |

## Pendiente del usuario
- Google Play: los pasos manuales de `docs/play.md`:
  - cuenta y ficha;
  - primera subida del AAB adjunto a la release v1.0.0;
  - declaración de permisos y seguridad de datos;
  - cuenta de servicio, secreto `PLAY_SERVICE_ACCOUNT_JSON` y variable `PLAY_ESTADO`.
- Guardar una copia del historial de vez en cuando (Ajustes → Historial → «Guardar copia»).
- Revisar la receta `pollo-almendras` antes de cocinarla, sobre todo las cantidades de la salsa y el tostado de las
  almendras al 5.
- Spike de voz (ADR-0008, `VERIFICAR`):
  - reconocimiento en español sin red;
  - latencia;
  - si el bucle de escucha pita;
  - convivencia con el TTS.

## Problemas conocidos / TODO
- La ficha calcula «cocinando» al abrirse y cuando cambia el estado guardado: si la cocinada caduca con la ficha
  abierta horas sin tocar nada, la banda sigue hasta salir y volver (riesgo bajo, revisión de la ronda 9).
- Sin probar en el móvil: el chip de Live Update de la fija y la pantalla completa de la alarma agrupada.
- La alarma agrupada no se reconstruye tras la muerte del proceso (aceptado, ver decisiones.md).
- Disposición FOCO: mucho hueco vacío en la tarjeta con pasos sin nota. Sin probar con 2 carriles.
- Sin probar en el móvil: el diálogo al salir (ronda 7), la tarjeta compacta (ronda 8, solo emulador) y el aviso de
  versión nueva (v0.8.0, probado en el emulador).
- Worktrees en Windows: si `git worktree remove` falla con «Filename too long», se borra con
  `cmd //c rmdir /s /q "\\?\<ruta>"` y después `git worktree prune`.

## Próxima acción exacta
v1.0.0 publicada al hacer push. Comprobar que la release lleva APK y AAB y que el aviso de versión llega a la 0.8.1 de
Obtainium; después, cuando Joan tenga la cuenta, dar de alta la app en Play con `docs/play.md`.
