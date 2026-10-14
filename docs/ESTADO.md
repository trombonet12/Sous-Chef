# Estado

| Fase | Estado |
|---|---|
| F0 Repo y gobierno | completada |
| F1 Esqueleto Android, CI, release, spike de alarmas | completada |
| MVP local-first (modo ahorro, ver plan) | **completado**: app funcional sin backend |
| F3/F4 Supabase + sync + login | aplazada |
| F9b Revisión culinaria | aplazada |

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

## Entorno
| Herramienta | Estado |
|---|---|
| JDK 21 (Gradle) | OK vía `~/.gradle/gradle.properties` |
| Android SDK 36 / build-tools 36.0.0 | OK |
| Emulador | AVD `trombito-36` |
| Keystore release | `~/.souschef/` (`node scripts/android-release.mjs`) |
| `~/.souschef/supabase.env` | falta (no hace falta para el MVP) |

## Pendiente del usuario
- Ver `docs/sesion-final.md`: probar en el móvil, secretos de GitHub, decisiones aplazadas.

## Problemas conocidos / TODO
- Ficha: si sales antes de 600 ms tras cambiar raciones, no se guarda (debounce en `viewModelScope`).
- Crono: sin enlaces "Técnica: X →" en "Ver el porqué", sin notificación en curso por carril (canal LOW), sin línea
  tapada/destapada en pestañas; el estado se borra al cerrar (sin Deshacer de 8 s).
- Barra de estado clara sobre el crono oscuro (cosmético).
- Menú: "Ajustes" y "Cerrar sesión" aún no hacen nada (no hay login en el MVP).
- Alarmas con Doze profundo sin probar en hardware real (el emulador no entra en Doze).
- Aplazado: Supabase/sync/login, `sc publicar/despensa/migrar`, imprimir/PDF, completitud heurística del validador.

## Próxima acción exacta
Sesión con el móvil (`docs/sesion-final.md`); después, F3 Supabase cuando haya presupuesto.
