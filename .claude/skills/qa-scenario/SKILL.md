---
name: qa-scenario
description: Guion manual en Android por escenario (process-death, apk-update, reboot, doze, permissions, offline-despensa, timezone, rotation, dark-theme); no tests automáticos.
argument-hint: "[process-death|apk-update|reboot|doze|permissions|offline-despensa|timezone|rotation|dark-theme]"
allowed-tools: Read, Grep, Glob, Bash(adb *), Bash(./gradlew *)
---

# qa-scenario

Genera y ejecuta el guion del escenario `$ARGUMENTS` con preparación, pasos, resultado esperado y evidencia. Comandos y expectativas por escenario: [scenarios.md](scenarios.md).

## Cuándo usarla
- Validar lo que los tests unitarios no cubren: cronómetro y alarmas ante muerte del proceso, reinicio, actualización del APK, Doze, permisos, modo avión, zona horaria, rotación y tema oscuro.
- Cierre de una fase que toca el cronómetro, las alarmas, Room o la despensa.

## Cuándo NO usarla
- Ejecutar la puerta de calidad automática → usa `qa`.
- Convertir un hallazgo en test automático → usa `test-write` (tipo `android-ui` o `integration`).
- Corregir un fallo encontrado → usa `bugfix-flow`.

## Precondiciones
- `$ARGUMENTS` es uno de los valores del hint; si falta, preguntar cuál (no asumir uno).
- `adb devices` lista un emulador o dispositivo; APK debug construido (`./gradlew assembleDebug`). Para `apk-update`, el APK de la versión anterior.
- Probar en Android 14 o superior (versiones objetivo del proyecto); `doze` y `reboot` mejor en dispositivo real. Sin datos personales reales.

## Pasos
1. Abrir [scenarios.md](scenarios.md) y localizar la sección `$ARGUMENTS`.
2. Preparación: ejecutar solo los comandos de esa sección; anotar versión de Android, dispositivo y versión de la app.
3. Ejecutar los pasos en orden, capturando evidencia (`adb exec-out screencap -p`, `adb logcat -d`).
4. Comparar con el resultado esperado; cada desviación es un hallazgo con pasos de reproducción.
5. Restaurar el estado del dispositivo (red, Doze, `always_finish_activities`, permisos, zona horaria, rotación, tema).
6. Con hallazgos, abrir tarea con `bugfix-flow` y proponer el test automático con `test-write`.
7. Entregar el informe: `escenario | Android | dispositivo | PASS/FAIL | evidencia | hallazgos`.

## Validaciones / tests
- `adb devices` con al menos un dispositivo en estado `device`.
- `adb logcat -d` sin `FATAL EXCEPTION` ni ANR del paquete de la app durante el escenario.
- La alarma del cronómetro suena a la hora esperada (margen de segundos) en todos los escenarios que la tocan.

## Definition of Done
- [ ] Escenario ejecutado en al menos una versión objetivo, con la versión indicada en el informe.
- [ ] Evidencia (capturas, logcat) guardada fuera del repo o adjunta, sin secretos ni datos personales.
- [ ] Dispositivo restaurado a su estado normal.
- [ ] Cada hallazgo tiene reproducción y test automático propuesto o registro en ESTADO.md.

## Docs a actualizar
- `docs/testing.md` si existe (casos nuevos o resultados) · docs/ESTADO.md (skill `session-handoff`)
