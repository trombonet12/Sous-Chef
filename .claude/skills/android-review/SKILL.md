---
name: android-review
description: Revisar Android de Sous Chef sin modificar (lifecycle, background, permissions, alarms) con findings por severidad; no arquitectura (architecture-review) ni implementar.
argument-hint: "[lifecycle|background|permissions|alarms]"
allowed-tools: Read, Grep, Glob, Bash(git diff *), Bash(git log *), Bash(git show *), Bash(./gradlew *)
---

# android-review

Revisión de solo lectura de `:app` según `$ARGUMENTS`. Devuelve findings; no modifica código. Sin argumento, recorrer los cuatro ámbitos en orden: lifecycle, background, permissions, alarms.

## Cuándo usarla
- El diff toca ViewModels, pantallas Compose, Workers, receivers, `AndroidManifest.xml` o el motor del cronómetro en `:app`.
- Antes de cerrar una tarea Android.

## Cuándo NO usarla
- Fronteras de módulos, capas o flujo de datos → `architecture-review`.
- Backend → `supabase-review`.
- Implementar o corregir lo hallado → agente de ingeniería correspondiente o `bugfix-flow`.

## Precondiciones
- Rama con cambios respecto a `main`; sin diff, revisar el estado actual de `:app`.
- Decisiones aplicables: `docs/decisiones.md` y ADR-0004 (sync) y ADR-0005 (avisos por alarmas, sin foreground service ni micrófono).
- Concurrencia o lifecycle complejos (Flow compartidos, scopes manuales) → escalar a revisión de arquitectura.

## Pasos
1. Delimitar: `git diff main...HEAD --stat` sobre `app/`; leer los archivos del ámbito.
2. Aplicar la lista del valor de `$ARGUMENTS`:

| Valor | Comprobar |
|---|---|
| `lifecycle` | Estado en ViewModel (`StateFlow` con `stateIn(WhileSubscribed(5000))`), sin `Context`/`Activity` retenidos; `collectAsStateWithLifecycle`; eventos por `Channel` sin pérdida; `LaunchedEffect`/`DisposableEffect` con claves y `onDispose`; sin `GlobalScope`; dispatchers inyectados; el cronómetro sobrevive a rotación y muerte de proceso (estado en Room/`SavedStateHandle`, no solo en memoria) |
| `background` | Sync en WorkManager (constraints, backoff, `uniqueWork`, idempotente, `retry` solo en transitorios); nada de foreground service ni micrófono (ADR-0005); sin hilos/`Timer` ad hoc; `onReceive` solo lee extras, notifica y reprograma; escritura y outbox en la misma transacción |
| `permissions` | Mínimo en el manifest, sin huérfanos; `POST_NOTIFICATIONS` pedido en contexto con rationale y denegación permanente; `USE_EXACT_ALARM`/`SCHEDULE_EXACT_ALARM` con `maxSdkVersion="32"`; componentes `exported` justificados; sin datos sensibles en logs; sin secretos en el cliente (solo anon key, nunca service_role) |
| `alarms` | `setAlarmClock` para avisos exactos del cronómetro; `PendingIntent` inmutable con `requestCode` estable; cadena reprogramada en `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED`; canal `IMPORTANCE_HIGH` con `USAGE_ALARM`; alarmas canceladas al parar o terminar; `canScheduleExactAlarms()` donde aplique; sin avisos duplicados ni vencidos tras reinicio |

3. Cada hallazgo con `archivo:línea` exacto y una corrección concreta. Lo no verificable en el código se pide confirmar, no se afirma.
4. Contrastar con las herramientas: ejecutar la validación y atribuir fallos al cambio.
5. Emitir findings por severidad (CRITICA, ALTA, MEDIA, BAJA) y veredicto: limpio / con avisos / bloquea. Aviso que no suena con la app cerrada o secretos en cliente: mínimo ALTA.

## Validaciones / tests
- `./gradlew lintDebug testDebugUnitTest` y `./gradlew ktlintCheck detekt`.
- Comportamiento revisado sin test: finding MEDIA.

## Definition of Done
- [ ] Ámbito de `$ARGUMENTS` (o los cuatro) recorrido por completo.
- [ ] Findings `SEVERIDAD | archivo:línea | problema | corrección propuesta`, ordenados por severidad.
- [ ] Ningún archivo modificado; veredicto emitido.
- [ ] Escalado a revisión de arquitectura si hay concurrencia/lifecycle complejo.
- [ ] Gate `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest` ejecutado (o fallo atribuido).

## Docs a actualizar
- Ninguno si el veredicto es limpio; ADR (`create-adr`) si se acuerda un cambio de enfoque · docs/ESTADO.md (skill `session-handoff`)
