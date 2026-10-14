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

Una vez, en GitHub → Settings → Secrets and variables → Actions → *New repository secret*:

| Secreto | Valor |
|---|---|
| `ANDROID_KEYSTORE_PASSWORD` | `SOUSCHEF_KEYSTORE_PASSWORD` de `%USERPROFILE%\.souschef\release-signing.properties` |
| `ANDROID_KEY_ALIAS` | `SOUSCHEF_KEY_ALIAS` del mismo archivo |
| `ANDROID_KEY_PASSWORD` | `SOUSCHEF_KEY_PASSWORD` del mismo archivo |
| `ANDROID_KEYSTORE_BASE64` | el `.jks` de `SOUSCHEF_KEYSTORE_FILE` en base64 (ver abajo) |

```powershell
# Copia al portapapeles el keystore en base64 (pon la ruta de SOUSCHEF_KEYSTORE_FILE)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\ruta\al\keystore.jks")) | Set-Clipboard
```
`SUPABASE_URL` y `SUPABASE_PUBLISHABLE_KEY` son opcionales hasta F3.

Cada versión: subir `versionCode`/`versionName` en `app/build.gradle.kts`, push, y Actions → *Release Android* →
*Run workflow*. Crea el tag `v<versionName>` con el APK y `checksums.txt` (Obtainium puede seguir el repo).
El CI `Android` ya no corre en cada push (solo en PR o a mano); el gate se pasa en local.

## 5. Aplazado (cuando haya presupuesto)
- Supabase: `~/.souschef/supabase.env`, exponer `sous_chef`, migraciones, sync, login (`docs/pasos-manuales.md`).
- Imprimir/PDF de la ficha.
- Revisión culinaria F9b con `chef-reviewer` y validador de completitud.
- TODOs de `docs/ESTADO.md § Problemas conocidos`.
