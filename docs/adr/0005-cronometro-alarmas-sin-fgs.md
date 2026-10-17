# ADR-0005: Cronómetro con una cadena de alarmas, sin foreground service

- Estado: aceptado
- Fecha: 2026-10-13
- Relacionado: `docs/decisiones.md` § Cronómetro, [ADR-0001](0001-stack-android-y-modulos.md), [ADR-0008](0008-voz-en-el-cronometro.md) (voz solo en primer plano), [spec del legado](../spec/app-legado.md)

## Contexto
- La app vieja avisaba con la pestaña abierta. En Android hay que avisar de fin de paso, hitos y "toca empezar" con la pantalla apagada y bajo Doze, con varios recipientes a la vez.
- Un foreground service mantiene el proceso vivo, pero exige notificación permanente y tipo de servicio declarado, y es más frágil frente a restricciones de segundo plano y fabricantes.
- `setExactAndAllowWhileIdle` tiene límite de frecuencia en Doze; `setAlarmClock` es la alarma más fiable y está exenta de ese límite.

## Decisión
- El estado del crono vive en Room: `finAt` en reloj de pared y los flags de deduplicación (`hechos`, `aFin`, `avisado`; `a10` solo para leer estados viejos) también persistidos.
- Una **única cadena de alarmas**: `AlarmManager.setAlarmClock` programa solo el próximo evento. El receiver recarga el estado de Room, emite todo lo vencido (los hitos con más de 90 s de retraso se marcan sin sonar) y programa el siguiente.
- Permisos: `USE_EXACT_ALARM` (API 33+) y `SCHEDULE_EXACT_ALARM` con `maxSdk=32`.
- Se reprograma en `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` y al cambiar el permiso de alarmas exactas.
- Notificaciones (actualizado en la ronda 6, 2026-10-17, ver decisiones.md):
  - Con la cocina en pantalla (`PresenciaCrono.visible`), **ninguna**. Avisan la app y la voz.
  - Fuera de la pantalla hay **una fija** (`ID_COCINA`, canal `crono_cocina`, LOW) con todos los carriles, la cuenta
    atrás al fin más próximo y el chip de Live Update.
  - Hay **una alarma** (`ID_ALARMA`, `crono_alarma_v3`, `USAGE_ALARM`). Suena con `FLAG_INSISTENT` solo en fin, hito y
    toca empezar, y agrupa lo que coincide en 60 s (`AgrupadorAlarmas`) sin volver a sonar. El sonido lo pone el
    sistema, no el proceso.
  - Sin aviso de 10 s (`DIEZ_S` queda obsoleto).
  - Pantalla completa solo con el móvil bloqueado.
  - `POST_NOTIFICATIONS` se pide al abrir la cocina.
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
