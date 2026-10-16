# Estado

| Fase | Estado |
|---|---|
| F0 Repo y gobierno | completada |
| F1 Esqueleto Android, CI, release, spike de alarmas | completada |
| MVP local-first (modo ahorro, ver plan) | **completado**: app funcional sin backend |
| F3/F4 Supabase + sync + login | aplazada |
| F9b Revisión culinaria | en curso: lote 1 de 3 aplicado |

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

## Entorno
| Herramienta | Estado |
|---|---|
| JDK 21 (Gradle) | OK vía `~/.gradle/gradle.properties` |
| Android SDK 36 / build-tools 36.0.0 | OK |
| Emulador | AVD `trombito-36` |
| Keystore release | `~/.souschef/` (`node scripts/android-release.mjs`) |
| `~/.souschef/supabase.env` | falta (no hace falta para el MVP) |

## Pendiente del usuario
- Actualizar a la v0.5.0 por Obtainium. En el móvil ya hay una compilación equivalente.
- Cocinar con la v0.5.0: alarma insistente + pantalla completa (permiso concedido por adb), notificaciones por carril y Trombito.

## Problemas conocidos / TODO
- Sin probar en el móvil: AlarmaActivity a pantalla completa (el permiso estaba denegado; ya concedido), Parar → el carril
  vuelve a su estado; si el aviso caduca a los 60 s sin tocar nada, el carril desaparece hasta el siguiente repintado.
- `shortCriticalText` del chip es estático entre repintados (la cuenta atrás nativa sí corre).
- Aplazado: Supabase/sync/login, `sc publicar/despensa/migrar`, F9b lotes 2 (macarrones, pastel de patata, pizza,
  salmón) y 3 (salteado, tacos de carne, tacos de fiesta, tarta de manzana): chef-reviewer opus propone, sonnet aplica.

## Próxima acción exacta
F9b lote 2 (cuando Joan lo pida); después F3 Supabase cuando haya presupuesto.
