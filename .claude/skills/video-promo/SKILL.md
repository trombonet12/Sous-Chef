---
name: video-promo
description: Regenerar el vídeo promocional horizontal de Sous Chef (Remotion, 1920×1080, ~32 s) con clips reales del emulador y video/src/guion.ts; no sube a YouTube ni toca la ficha de Play.
allowed-tools: Read, Bash(node scripts/video/promo.mjs*), Bash(bash scripts/video/grabar-clips.sh*), Bash(adb *), Bash(ffprobe *)
disable-model-invocation: true
---

# video-promo

Compone `build/promo/sous-chef-promo-1920x1080.mp4` (1920×1080, 30 fps, ~32 s, con música CC0 de `video/public/musica/`). Proyecto Remotion en `video/` (ADR-0012: `docs/adr/0012-video-promocional-con-remotion.md`). Los datos del vídeo están en `video/src/guion.ts`: tramos (clip, inicio, duración, título, subtítulo, tono), entrada, cierre y música.

Requisitos de Play (https://support.google.com/googleplay/android-developer/answer/9866151): la app real en los primeros 10 s y ocupando ≥ 80 % del vídeo, poco texto, nada de «mejor», «#1» ni «descarga ya». Horizontal: el vertical saldría como Short y no sirve para la ficha.

## Cuándo usarla
- Tras cambios visibles en la app, o antes de una versión de producción.
- Para cambiar textos, tiempos o animaciones del vídeo, o para rehacer los clips `c1`…`c5`.

## Cuándo NO usarla
- Solo instalar la app o sacar una captura → `android-install-and-logcat`.
- Capturas y gráficos de la ficha (`graphics/`, `phone/`) → `node scripts/ficha-play.mjs` (ver `docs/play.md`).
- Publicar la versión → `release`.
- Dudas de cómo animar o componer en Remotion → `remotion-best-practices`; de exportar → `remotion-render`.

## Precondiciones
- Para regrabar: emulador `trombito-36` arrancado (`adb devices` muestra `device`) con la app debug instalada y datos reales: receta «Arroz a la cubana con bacon», cuadraditos Arroz/Salsa, técnica «Bechamel» e historial con una cocinada. Permisos de micrófono y notificaciones ya concedidos.
- Bash de Git for Windows (el script lo busca en `C:\Program Files\Git\bin\bash.exe`) y `adb` en `%LOCALAPPDATA%\Android\Sdk\platform-tools` (u `ADB=<ruta>`).
- Sin regrabar: basta con `build/promo/clips/` o `video/public/clips/` con `c1`…`c5`.
- `video/node_modules` (si falta, `promo.mjs` ejecuta `npm ci`). La primera vez Remotion descarga Chrome Headless Shell.
- Opcional: `ffprobe` para validar; Python con Pillow para la hoja de contacto.

## Pasos
1. Si cambió la app: `./gradlew assembleDebug` y `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Si cambió la app o faltan clips: `node scripts/video/promo.mjs --grabar` (graba todo y renderiza). Con `--solo-grabar` solo graba. La grabación espera a que «Empieza a las» cuadre con la hora real y puede tardar ~15 min. Clips en `build/promo/clips/cN-*.mp4`.
3. Solo re-renderizar: `node scripts/video/promo.mjs`. Copia los clips a `video/public/clips/` (ignorado por git) y falla si falta `c1`…`c5`.
4. Ajustar contenido: editar `video/src/guion.ts` y los componentes de `video/src/`. Previsualizar con `cd video && npm run studio` (interactivo: lo arranca Joan).
5. Control: `node scripts/video/promo.mjs --control` genera `build/promo/control-01.png`…`control-06.png` y `build/promo/control-hoja.png`. Revisar la hoja: texto breve, sin frases prohibidas.
6. Comprobar que la app sale en los primeros 10 s con un still de un frame concreto: `cd video && npx remotion still src/index.ts Promo ../build/promo/inicio.png --frame=150`.
7. Subida (manual, la hace Joan): YouTube como «No listado», no para niños, sin restricción de edad. Pegar `https://www.youtube.com/watch?v=<id>` en Play Console > Ficha > Vídeo (ver `docs/play.md § Vídeo promocional`).

## Validaciones / tests
- `node scripts/video/promo.mjs --sin-render` comprueba clips y dependencias sin renderizar.
- `cd video && npm run typecheck` (tsc --noEmit).
- `ffprobe -v error -select_streams v:0 -show_entries stream=width,height,r_frame_rate -of csv=p=0 build/promo/sous-chef-promo-1920x1080.mp4` devuelve `1920,1080,30/1`.
- `ffprobe -v error -show_entries format=duration -of csv=p=0 build/promo/sous-chef-promo-1920x1080.mp4` entre 30 y 34 s.
- `ffprobe -v error -select_streams a -show_entries stream=codec_type -of csv=p=0 build/promo/sous-chef-promo-1920x1080.mp4` devuelve `audio` (la música sí va incluida).
- `build/` está ignorado por git: no añadir el MP4 al repo.

## Definition of Done
- [ ] Clips `c1`…`c5` presentes y con la app real (sin diálogos de permisos ni pantallas de error).
- [ ] `build/promo/sous-chef-promo-1920x1080.mp4` generado y validado con ffprobe (1920×1080, 30 fps, audio, ~32 s).
- [ ] Control visual revisado y la app aparece en los primeros 10 s.
- [ ] Tamaño, densidad y zona horaria del emulador restaurados (lo hace `grabar-clips.sh` al terminar).
- [ ] Subida a YouTube y URL en Play: pendiente de Joan, anotada en `docs/ESTADO.md` si no se hace en la sesión.

## Docs a actualizar
- `docs/play.md` (sección «Vídeo promocional») · `docs/adr/0012-video-promocional-con-remotion.md` si cambia el flujo · docs/ESTADO.md (skill session-handoff)
