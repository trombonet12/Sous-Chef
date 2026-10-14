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
El APK sale en **Releases** (columna derecha del repo), no en "Packages" (eso es GitHub Packages y siempre está vacío).
El CI `Android` solo compila y pasa tests; no publica nada instalable.

`gh` usa el token de Git Credential Manager a través de `node scripts/gh.mjs <args>` (sin `gh auth login`).

Secretos de firma (una vez; repetir si cambia el keystore): `node scripts/subir-secretos-release.mjs` lee
`%USERPROFILE%\.souschef\release-signing.properties` y el `.jks`, y sube `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
`ANDROID_KEY_PASSWORD` y `ANDROID_KEYSTORE_BASE64` sin imprimir valores. Comprobar: `node scripts/gh.mjs secret list`.
`SUPABASE_URL` y `SUPABASE_PUBLISHABLE_KEY` son opcionales hasta F3.

Cada versión: subir `versionCode`/`versionName` en `app/build.gradle.kts`, push y
`node scripts/gh.mjs workflow run release-android.yml` (o Actions → *Release Android* → *Run workflow*). Crea el tag
`v<versionName>` con el APK y `checksums.txt` (Obtainium puede seguir el repo).

CI: `Android` y `Claude assets` corren en push a `main` (con filtro de rutas), en PR y a mano; se hace un push por
ronda. `main` está protegida: sin force-push ni borrado, historial lineal y check `Gate Android, dominio y recetas`
obligatorio en PR (el admin puede hacer push directo).

## 5. Aplazado (cuando haya presupuesto)
- Supabase: `~/.souschef/supabase.env`, exponer `sous_chef`, migraciones, sync, login (`docs/pasos-manuales.md`).
- Revisión culinaria F9b con `chef-reviewer` y validador de completitud.
- TODOs de `docs/ESTADO.md § Problemas conocidos`.
