---
name: video-promo
description: Rehacer el vídeo promocional vertical de Sous Chef para la ficha de Google Play (clips del emulador trombito-36 y montaje en build/promo); no sube nada a YouTube ni toca la ficha de Play.
disable-model-invocation: true
allowed-tools: Read, Bash(bash scripts/video/*), Bash(python *), Bash(ffmpeg *), Bash(ffprobe *), Bash(adb *)
---

# video-promo

Graba 5 clips reales de la app en el emulador y los monta en `build/promo/sous-chef-promo.mp4` (1080×1920, 30 fps, H.264, sin audio, ~30 s). Sin dependencias del proyecto: ffmpeg, Python con Pillow y Chrome.

Requisitos de Play (https://support.google.com/googleplay/android-developer/answer/9866151): URL de YouTube pública u oculta, sin anuncios, sin restricción de edad y embebible. Solo los primeros 30 s se reproducen solos y en silencio, así que la app debe verse en los primeros 10 s y ocupar ≥ 80 % del vídeo. Poco texto; nada de «mejor», «#1» ni «descarga ya».

## Cuándo usarla
- Tras cambios visibles grandes en la app, o antes de una versión de producción.
- Para rehacer uno o varios clips (`c1`…`c5`) sin regrabar todo.

## Cuándo NO usarla
- Solo instalar la app o sacar una captura → `android-install-and-logcat`.
- Capturas y gráficos de la ficha (`graphics/`, `phone/`) → `node scripts/ficha-play.mjs` (ver `docs/play.md`).
- Publicar la versión → `release`.

## Precondiciones
- Emulador `trombito-36` arrancado (`adb devices` muestra `device`) con la app debug instalada.
- Datos reales en la app: receta «Arroz a la cubana con bacon», cuadraditos Arroz/Salsa, técnica «Bechamel» e historial con una cocinada.
- Permisos de micrófono y notificaciones ya concedidos (ensayar el cronómetro antes).
- `ffmpeg`, `ffprobe` y Python con Pillow en el PATH. Chrome: variable `CHROME` si no está en una ruta estándar.
- Opcional: `ADB=<ruta>` si `adb` no está en `%LOCALAPPDATA%\Android\Sdk\platform-tools`.

## Pasos
1. Instalar la build actual: `./gradlew assembleDebug` y `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Grabar: `bash scripts/video/grabar-clips.sh todos` o solo los que cambien (p. ej. `bash scripts/video/grabar-clips.sh c3`). Clips: `c1-recetas`, `c2-ficha`, `c3-cronometro`, `c4-tecnicas`, `c5-historial`. El script fija 1080×1920 y 420 dpi, zona Europe/Madrid, y espera a que «Empieza a las» cuadre con la hora real (puede tardar hasta ~15 min). Al terminar restaura tamaño, densidad, zona horaria y para la app.
3. Revisar `build/promo/clips/` y que cada clip tenga el contenido correcto. Si falta un clip, el montaje usa la captura de `distribution/listing/graphics/phone/` con zoom suave.
4. Montar: `python -I scripts/video/montar-promo.py`. Textos, duraciones y transiciones están al principio del script (`TRAMOS`, `INTRO_S`, `CIERRE_S`, `SEG_S`).
5. Revisar fotogramas de control `build/promo/control-01s.png`, `control-08s.png`, `control-18s.png` y `control-29s.png`. Comprobar que la app sale en los primeros 10 s.
6. Subida (manual, la hace Joan): subir el MP4 a YouTube como «Oculto» y pegar la URL en Play Console > Ficha > Vídeo. Ver `docs/play.md § Vídeo promocional`.

## Validaciones / tests
- `ffprobe -v error -select_streams v:0 -show_entries stream=width,height,r_frame_rate -of csv=p=0 build/promo/sous-chef-promo.mp4` devuelve `1080,1920,30/1`.
- `ffprobe -v error -select_streams a -show_entries stream=codec_type -of csv=p=0 build/promo/sous-chef-promo.mp4` no devuelve nada (sin audio).
- `ffprobe -v error -show_entries format=duration -of csv=p=0 build/promo/sous-chef-promo.mp4` está entre 29 y 31 s.
- `build/` está ignorado por git: no añadir el MP4 al repo.

## Definition of Done
- [ ] Clips grabados en `build/promo/clips/` con la app real (no pantalla de error ni diálogos de permisos).
- [ ] `build/promo/sous-chef-promo.mp4` generado y validado con ffprobe (1080×1920, 30 fps, sin audio, ~30 s).
- [ ] Fotogramas de control revisados; texto breve y sin frases prohibidas.
- [ ] Wm size, densidad y zona horaria del emulador restaurados.
- [ ] Subida a YouTube y URL en Play: pendiente de Joan, anotada en `docs/ESTADO.md` si no se hace en la sesión.

## Docs a actualizar
- `docs/play.md` (sección «Vídeo promocional») · docs/ESTADO.md (skill session-handoff)
