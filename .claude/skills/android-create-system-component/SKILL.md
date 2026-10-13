---
name: android-create-system-component
description: Crear piezas de sistema del cronómetro de Sous Chef: canal de notificación, receiver de alarma exacta (setAlarmClock) o de arranque (BOOT_COMPLETED); sin foreground service ni micrófono.
argument-hint: "[notification|alarm-receiver|boot-receiver]"
allowed-tools: Read, Write, Edit, Grep, Glob, Bash(./gradlew *)
---

# android-create-system-component

Crea el componente `$ARGUMENTS` en `:app`. Por defecto `notification`; si el valor no es uno de los tres, pedirlo y parar. Contexto: ADR-0005 (avisos con la app cerrada mediante cadena de alarmas, sin foreground service ni micrófono).

## Cuándo usarla
- Hace falta una pieza que el sistema invoca fuera de la UI: aviso del cronómetro, alarma que dispara un paso, reprogramación tras reinicio.

## Cuándo NO usarla
- Trabajo diferible o sync → `android-create-worker`.
- Solo declarar o pedir un permiso → `android-add-permission`.
- Pantallas Compose → `android-create-feature`.
- Foreground service, micrófono o widget: fuera de alcance (ADR-0005); requiere un ADR nuevo (`create-adr`).

## Precondiciones
- Motor del cronómetro en `:receta` (puro): decide qué avisa y cuándo; el componente de sistema solo programa y muestra.
- Dependencias por Hilt (`android-create-hilt-module` si faltan). Detalles de API y límites por versión de Android se confirman con la documentación oficial, no de memoria.

## Pasos
### Comunes
1. Lógica en una clase testeable aparte (el receiver solo delega). Declarar en `AndroidManifest.xml` con `exported="false"` salvo que el sistema lo exija.
2. Textos en `strings.xml` (español). Escribir primero el test de la lógica, después el componente.

### notification
1. Canal de avisos del cronómetro con `IMPORTANCE_HIGH` y `AudioAttributes` con `USAGE_ALARM` (sonido y vibración propios), id constante, creado idempotente al arrancar la app. Canal aparte, de menor importancia, para avisos no urgentes.
2. `NotificationFactory` inyectable: icono, título, `PendingIntent` inmutable que abre la receta en curso, acción para parar.
3. `POST_NOTIFICATIONS` en runtime (API 33+) con `android-add-permission`; sin él la app sigue funcionando.

### alarm-receiver
1. Programar con `AlarmManager.setAlarmClock(...)` (exacta, exenta de Doze) mediante un `AlarmScheduler` en `data/` tras una interfaz de dominio; `PendingIntent` inmutable y `requestCode` estable por paso.
2. Permisos: `USE_EXACT_ALARM` y `SCHEDULE_EXACT_ALARM` con `android:maxSdkVersion="32"`; comprobar `canScheduleExactAlarms()` donde aplique y degradar con aviso claro.
3. `onReceive` mínimo: leer extras, publicar la notificación y programar la siguiente de la cadena. Nada pesado ni `goAsync` largo; el resto, a un Worker.

### boot-receiver
1. Receiver para `BOOT_COMPLETED` y `MY_PACKAGE_REPLACED` (permiso `RECEIVE_BOOT_COMPLETED`) que reprograma la cadena de alarmas desde el estado guardado en Room; idempotente y sin disparar avisos ya vencidos.

## Validaciones / tests
- `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug`.
- Unit/Robolectric: decisión de qué programar, construcción de notificación y canal (`ShadowNotificationManager`), `onReceive` con `ShadowAlarmManager` (`getNextScheduledAlarm`).
- Prueba manual en emulador con `android-install-and-logcat`; revisar con `android-review` (alarms, permissions, lifecycle).

## Definition of Done
- [ ] Componente declarado en el manifest, no exportado salvo necesidad, y lógica cubierta por tests.
- [ ] Canal `IMPORTANCE_HIGH` + `USAGE_ALARM`; alarmas con `setAlarmClock`; permisos exactos con `maxSdkVersion="32"`.
- [ ] Reprogramación tras `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED` probada.
- [ ] Sin datos de usuario en notificaciones ni logs.
- [ ] `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug` en verde.

## Docs a actualizar
- `docs/adr/0005-*` si cambia el enfoque de avisos · `docs/spec/app-legado.md` si cambia el comportamiento · docs/ESTADO.md (skill `session-handoff`)
