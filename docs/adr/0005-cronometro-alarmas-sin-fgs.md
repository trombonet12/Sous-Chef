# ADR-0005: Cronómetro con una cadena de alarmas, sin foreground service

- Estado: aceptado
- Fecha: 2026-10-13
- Relacionado: `docs/decisiones.md` § Cronómetro, [ADR-0001](0001-stack-android-y-modulos.md), [ADR-0004](0004-sync-outbox-lww.md), [spec del legado](../spec/app-legado.md)

## Contexto
- La app vieja avisaba con la pestaña abierta. En Android hay que avisar de fin de paso, hitos y "toca empezar" con la pantalla apagada y bajo Doze, con varios recipientes a la vez.
- Un foreground service mantiene el proceso vivo, pero exige notificación permanente y tipo de servicio declarado, y es más frágil frente a restricciones de segundo plano y fabricantes.
- `setExactAndAllowWhileIdle` tiene límite de frecuencia en Doze; `setAlarmClock` es la alarma más fiable y está exenta de ese límite.

## Decisión
- El estado del crono vive en Room: `finAt` en reloj de pared y los flags de deduplicación (`hechos`, `a10`, `aFin`, `avisado`) también persistidos.
- Una **única cadena de alarmas**: `AlarmManager.setAlarmClock` programa solo el próximo evento. El receiver recarga el estado de Room, emite todo lo vencido (los hitos con más de 90 s de retraso se marcan sin sonar) y programa el siguiente.
- Permisos: `USE_EXACT_ALARM` (API 33+) y `SCHEDULE_EXACT_ALARM` con `maxSdk=32`.
- Se reprograma en `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` y al cambiar el permiso de alarmas exactas.
- Notificaciones (actualizado en la ronda 5, ver decisiones.md): una por carril en `crono_carriles` y, al vencer un paso, la misma en `crono_alarma_v3` (`USAGE_ALARM`, `FLAG_INSISTENT`): el sonido lo pone el sistema, no el proceso. `POST_NOTIFICATIONS` se pide al abrir la cocina.
- Con el crono abierto, `FLAG_KEEP_SCREEN_ON`.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Foreground service con temporizadores en memoria | Reloj preciso mientras vive | Notificación permanente; la muerte del proceso o restricciones lo cortan; más permisos | Menos fiable y más intrusivo |
| `setExactAndAllowWhileIdle` por recipiente | API estándar | Límite de frecuencia en Doze; alarmas solapadas pueden retrasarse | Poco fiable con avisos solapados |
| WorkManager con retraso | Sencillo | Sin precisión de segundos | No cumple la puntualidad |
| Una alarma por evento | Sin lógica de cadena | Muchas alarmas que gestionar y cancelar | La cadena de una alarma es más simple y robusta |
| Cadena con `setAlarmClock` + estado en Room | Fiable en Doze y tras muerte del proceso | Icono de alarma en la barra de estado | Elegido |

## Consecuencias
- Positivas: sobrevive a la muerte del proceso y al reinicio; sin servicio permanente.
- Negativas / costes: `setAlarmClock` muestra el icono de alarma del sistema; algunos fabricantes pueden pedir exención de batería (enlace único); el comportamiento depende del OEM.
- Seguimiento: spike en el emulador (`dumpsys deviceidle force-idle`, 3 alarmas solapadas a 2-7 min) en F1 y repetición en el móvil real; escenarios de QA de muerte del proceso, upgrade y permisos.
