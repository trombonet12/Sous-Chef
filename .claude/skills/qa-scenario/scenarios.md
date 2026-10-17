# Tabla de escenarios (qa-scenario)

`<pkg>` = applicationId de la app (comprobar en `app/build.gradle.kts`). Comandos `adb` estándar; con varios dispositivos añadir `-s <serial>`. Lo que no se pueda verificar en la versión de Android probada se anota como "no verificado". Receta de referencia: una con al menos dos recipientes y un temporizador de 2-3 minutos para no esperar.

## process-death
- Preparación: iniciar una receta en el cronómetro con varios recipientes y uno en marcha. Alternativa: `adb shell settings put global always_finish_activities 1` (restaurar a 0).
- Pasos: enviar a segundo plano (`adb shell input keyevent KEYCODE_HOME`), matar el proceso (`adb shell am kill <pkg>`), esperar a que venza el temporizador, volver desde recientes.
- Esperado: la alarma suena a su hora aunque el proceso estuviera muerto; al volver el estado del cronómetro (tiempo restante, recipientes, paso) se restaura sin saltos; sin crash.
- Evidencia: capturas antes/después; logcat sin `FATAL EXCEPTION`.

## apk-update
- Preparación: instalar la versión N-1, programar alarmas del cronómetro (receta en marcha con temporizador largo).
- Pasos: actualizar sin desinstalar (`adb install -r <apk-N>`) con alarmas pendientes; no abrir la app y esperar el vencimiento.
- Esperado: el receiver de `MY_PACKAGE_REPLACED` reprograma las alarmas; la alarma suena a su hora; migración de Room aplicada y datos (historial, despensa, lista de la compra) intactos.
- Evidencia: `adb shell dumpsys alarm | grep <pkg>` antes y después; recuento de filas; logcat de la migración.

## reboot
- Preparación: cronómetro en marcha con alarma programada; despensa con cambios.
- Pasos: `adb reboot`; tras arrancar, no abrir la app y esperar el vencimiento. Repetir con el dispositivo bloqueado.
- Esperado: `BOOT_COMPLETED` reprograma las alarmas todavía vigentes; las vencidas durante el apagado se notifican como vencidas, sin duplicados; la app arranca limpia.
- Evidencia: `adb shell dumpsys alarm | grep <pkg>`, capturas de la notificación, logcat.

## doze
- Preparación: cronómetro en marcha; pantalla apagada (`adb shell input keyevent KEYCODE_POWER`); forzar Doze: `adb shell dumpsys deviceidle force-idle` (salir con `adb shell dumpsys deviceidle unforce`).
- Pasos: esperar el vencimiento con la pantalla apagada y la app en segundo plano; repetir con el ahorro de batería activado.
- Esperado: la alarma exacta suena a su hora (con tolerancia de segundos) y la notificación se muestra; sin retrasos de minutos.
- Evidencia: `adb shell dumpsys deviceidle`, `adb shell dumpsys alarm | grep <pkg>`, logcat.

## permissions
- Preparación: `adb shell pm revoke <pkg> android.permission.POST_NOTIFICATIONS`; denegar alarmas exactas: `adb shell cmd appops set <pkg> SCHEDULE_EXACT_ALARM deny`.
- Pasos: iniciar una receta con temporizador; denegar con "no volver a preguntar"; concederlo desde Ajustes y volver; revocar con la app en segundo plano. Restaurar con `adb shell cmd appops set <pkg> SCHEDULE_EXACT_ALARM allow`.
- Esperado: la app explica qué falta y ofrece ir a Ajustes, sin crash; sin permiso de alarmas exactas hay un aviso claro o un respaldo documentado (no fallo silencioso); al concederlo, las alarmas se programan sin reiniciar.
- Evidencia: capturas de cada estado y logcat.

## timezone
- Preparación: cronómetro en marcha con alarma programada; zona horaria inicial conocida.
- Pasos: cambiar la zona desde Ajustes (Fecha y hora); comprobar la hora de vencimiento y la hora de servir de la ficha; cruzar un cambio de hora (DST) si es posible.
- Esperado: los temporizadores relativos no cambian; la hora de servir y el historial se muestran en la zona nueva sin corromper fechas; las alarmas se reprograman si dependen de hora de pared.
- Evidencia: capturas y `adb shell dumpsys alarm | grep <pkg>`. Restaurar la zona.

## rotation
- Preparación: cronómetro en marcha, ficha de receta abierta.
- Pasos: desactivar auto-rotación y forzar `adb shell settings put system user_rotation 1` (volver a 0); alternar con la app en primer plano y con un diálogo abierto.
- Esperado: sin reinicio del cronómetro ni pérdida del paso actual, scroll ni diálogos; layout usable en horizontal.
- Evidencia: capturas en ambas orientaciones; logcat sin excepciones.

## dark-theme
- Preparación: `adb shell cmd uimode night yes` (restaurar con `night no` o `auto`).
- Pasos: recorrer cronómetro, ficha, despensa, lista de la compra e historial; alternar el tema con la app abierta.
- Esperado: contraste legible, sin texto sobre fondo del mismo tono, la alarma y las notificaciones legibles; cambio de tema sin perder estado.
- Evidencia: capturas de cada pantalla en oscuro.
