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
- 2026-10-14 · Ronda 2 (probado en el móvil, Android 16): avisos HIGH verificados con pantalla apagada; notificación
  "En marcha" promocionada (Live Update) con cuenta atrás y acción Siguiente; avisos con id estable y caducidad;
  full-screen intent; plan plegable y hoja del porqué rediseñados; Deshacer al cerrar; letra del sistema.
- 2026-10-14 · Ronda 3 (v0.2.0): alarma estilo reloj (`AlarmaActivity` sobre el bloqueo, sonido en bucle hasta
  Parar, máx. 60 s); una sola cocinada viva a la vez (diálogo para abandonar, menú ⋮ con Abandonar/Reiniciar);
  sin "En marcha" fantasma; pulido de crono (plan en una línea, línea de tiempo, tapada/destapada en pestañas) y de
  lista/ficha/despensa/historial; CI `android.yml` solo en PR o a mano; release sin secretos de Supabase.

## Entorno
| Herramienta | Estado |
|---|---|
| JDK 21 (Gradle) | OK vía `~/.gradle/gradle.properties` |
| Android SDK 36 / build-tools 36.0.0 | OK |
| Emulador | AVD `trombito-36` |
| Keystore release | `~/.souschef/` (`node scripts/android-release.mjs`) |
| `~/.souschef/supabase.env` | falta (no hace falta para el MVP) |

## Pendiente del usuario
- Subir los 4 secretos de firma y lanzar *Release Android* (`docs/sesion-final.md` §4).
- Probar la ronda 3 en el móvil (pantalla apagada con un aviso).

## Problemas conocidos / TODO
- Sin probar en el móvil: AlarmaActivity con pantalla apagada (encender, Parar, Hecho → siguiente), diálogo de
  abandonar, deslizar "En marcha", hoja del porqué, Deshacer. Fuente al 1.3 sin revisar.
- `shortCriticalText` del chip es estático entre repintados (la cuenta atrás nativa sí corre).
- Aplazado: Supabase/sync/login, `sc publicar/despensa/migrar`, imprimir/PDF, completitud heurística del validador,
  revisión culinaria F9b.

## Próxima acción exacta
Sesión con el móvil (`docs/sesion-final.md`); después, F3 Supabase cuando haya presupuesto.
