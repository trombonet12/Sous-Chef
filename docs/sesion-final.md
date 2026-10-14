# Sesión final con el móvil

## 1. Instalar
```
node scripts/android-release.mjs                       # APK release firmado
adb install -r app/build/outputs/apk/release/app-release.apk
```
Al primer arranque se cargan las 13 recetas, 17 técnicas, el historial, las favoritas y la despensa.

## 2. Permisos
- Notificaciones: se piden al pulsar "Abrir la cocina".
- Alarmas exactas: concedidas por `USE_EXACT_ALARM` (Android 13+).
- Si el fabricante mata procesos (Xiaomi, Samsung…): Ajustes → Batería → Sous Chef → sin restricciones.

## 3. Recorrido de QA
1. Lista: búsqueda, chips, estrella de favorita, "hace X".
2. Ficha: cambiar raciones (tarta a 6), copiar la compra, "Añadir a la compra".
3. Despensa: ciclar estados, "Pasar a la compra lo que falta", compartir la lista.
4. Cronómetro (ensalada de ñoquis, 19 min): abrir la cocina, arrancar carriles, **apagar la pantalla** y comprobar
   que suenan los avisos (10 s, fin, hitos) a su hora; tocar la notificación abre el crono; cerrar y ver la entrada
   en Historial.
5. Reiniciar el móvil con un crono en marcha: las alarmas deben reprogramarse.

## 4. GitHub Release
Subir los secretos `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`
(valores en `~/.souschef/`) y lanzar el workflow `release-android` (`workflow_dispatch`).

## 5. Aplazado (cuando haya presupuesto)
- Supabase: `~/.souschef/supabase.env`, exponer `sous_chef`, migraciones, sync, login (`docs/pasos-manuales.md`).
- Imprimir/PDF de la ficha.
- Revisión culinaria F9b con `chef-reviewer` y validador de completitud.
- TODOs de `docs/ESTADO.md § Problemas conocidos`.
