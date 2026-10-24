# Google Play

> Propósito: pasos para publicar Sous Chef en Google Play y dejar la subida automática · Audiencia: Joan · Actualizado: 2026-10-20

## Resumen
El CI ya genera el AAB firmado en cada release y, si tiene credenciales, lo sube solo a la pista interna de Play
([ADR-0010](adr/0010-distribucion-por-google-play.md)). Dar de alta la app no se puede automatizar: se hace una vez,
a mano, en Play Console.

## Una sola vez, en Play Console
1. **Cuenta de desarrollador** en <https://play.google.com/console>. Cuesta 25 USD (pago único) y pide verificar la
   identidad.
   - Las cuentas personales nuevas tienen que hacer una **prueba cerrada con al menos 12 testers durante 14 días**
     antes de poder pedir producción.
   - Para uso propio basta la prueba interna, que no tiene ese requisito.
2. **Crear la app:** nombre «Sous Chef», idioma español (España) y gratuita. El ID de paquete, `app.trombito.souschef`, queda
   fijado con la primera subida.
3. **Play App Signing:** aceptarlo. La clave de `~/.souschef/` queda como **clave de subida**, y Google guarda la de
   firma.
4. **Primera subida, a mano:**
   1. Prueba > Prueba interna > Crear versión.
   2. Subir el `sous-chef-<versión>.aab` de la release de GitHub. No hace falta subir archivo de desofuscación: el
      mapping de R8 va dentro del AAB.
   3. Pegar las notas de versión en «Novedades» (ver «Notas de versión», más abajo).
   4. Añadir a Joan como tester (lista de correos) y abrir el enlace de participación en el móvil.
5. **Contenido de la app** (menú Política):
   - Política de privacidad: `https://github.com/trombonet12/Sous-Chef/blob/main/docs/privacidad.md`.
   - Seguridad de los datos: no se recoge ni se comparte ningún dato. La app no usa Internet: no pide permisos de red
     y no envía nada. El micrófono se usa en el dispositivo y no guarda nada.
   - Permisos:
     - `USE_EXACT_ALARM`: es un temporizador de cocina y la alarma tiene que sonar al segundo.
     - `USE_FULL_SCREEN_INTENT`: la alarma del paso sale a pantalla completa con el móvil bloqueado.
     - Ningún permiso de red: la app no pide `INTERNET` ni `ACCESS_NETWORK_STATE` ([ADR-0011](adr/0011-sin-aviso-de-version-nueva.md)).
   - Datos de contacto (para los usuarios): correo `trombonet12@trombonet12.es`, web `https://www.trombonet12.es/`.
     Desarrollador: «Trombonet12© and Co. Interactive Systems Solutions».
   - Público objetivo: adultos. Sin anuncios.
   - Clasificación de contenido: rellenar el cuestionario; no hay contenido sensible.
6. **Ficha de Play Store**: todo está en `distribution/listing/`.
   - Textos: `es-ES/short-description.txt` (≤ 80) y `es-ES/full-description.txt` (≤ 4000).
   - Icono `graphics/icon-512.png` y gráfico `graphics/feature-1024x500.png`. Se regeneran con
     `node scripts/ficha-play.mjs`, que usa Chrome headless y Pillow.
   - Capturas `graphics/phone/*.png` en 1080×1920. Las de `docs/capturas/` no valen: son 9:20, y Play no admite
     que el lado largo pase del doble del corto.

## Subida automática
1. En Google Cloud:
   1. crear una **cuenta de servicio** en un proyecto;
   2. activar la *Google Play Android Developer API*;
   3. descargar la clave JSON. No va al repo: se guarda en `~/.souschef/`.
2. En Play Console > Usuarios y permisos, invitar el correo de la cuenta de servicio, solo para esta app, con permiso
   para **publicar en pistas de prueba**.
3. En GitHub > Settings > Secrets and variables > Actions:
   - secreto `PLAY_SERVICE_ACCOUNT_JSON`, con el contenido del JSON. Lo sube `node scripts/subir-secreto-play.mjs`,
     que comprueba la forma de la clave y no imprime valores;
   - variable `PLAY_ESTADO`:
     - `draft` mientras la app esté en borrador en Play;
     - `completed` cuando ya haya salido una versión, para que cada release llegue sola a los testers.
4. A partir de ahí, cada push a main que cambie `versionMinor` o `versionMajor` en `version.properties` publica en GitHub **y** en la pista interna.
   Para pasar de interna a producción, en Play Console: «Promocionar versión».

## Actualización obligatoria
- La app fuerza la actualización con la pantalla de Play (In-App Updates, [ADR-0013](adr/0013-actualizacion-obligatoria-desde-play.md))
  cuando la versión nueva tiene prioridad 4 o 5. Nunca salta con una cocinada en marcha ni en instalaciones de
  GitHub u Obtainium.
- El CI sube cada versión con `inAppUpdatePriority: 5` (`.github/workflows/android.yml`). Para publicar una versión
  que no obligue a actualizar, bajarlo a 3 en ese commit.
- La prioridad no se puede cambiar en Play Console después de subir la versión.

## Vídeo promocional
- Play lo pide como URL de YouTube (pública u oculta, sin anuncios, embebible). Solo se reproducen solos los primeros
  30 s, en silencio, así que la app tiene que verse pronto.
- Es **horizontal** (1920×1080). El vertical sale como Short en YouTube y no sirve para la ficha de Play. El vídeo
  vertical anterior (<https://www.youtube.com/watch?v=ZcVUNMAC-Uc>) se sustituye por el nuevo.
- Se hace con Remotion en `video/` ([ADR-0012](adr/0012-video-promocional-con-remotion.md)). Un solo comando:
  ```
  node scripts/video/promo.mjs              # renderiza build/promo/sous-chef-promo-1920x1080.mp4
  node scripts/video/promo.mjs --grabar     # graba antes los clips en el emulador (scripts/video/grabar-clips.sh)
  node scripts/video/promo.mjs --control    # opción --control del mismo comando (VERIFICAR qué hace)
  ```
  Las dependencias de `video/` (`node_modules`) viven solo en local. La música y el guion están en
  `video/public/musica/` y `video/src/guion.ts`.
- Se rehace tras cambios visibles grandes o antes de una versión de producción. Procedimiento completo: skill
  `video-promo` (`.claude/skills/video-promo/SKILL.md`).
- El MP4 queda en `build/promo/` y no se versiona. La subida es a mano:
  1. Subir el MP4 a YouTube como **Oculto**.
  2. Copiar la URL `https://www.youtube.com/watch?v=<id>`.
  3. Pegarla en Play Console → Ficha → Vídeo.

## Notas de versión
- Están en `distribution/whatsnew/whatsnew-es-ES`. Son texto para el usuario: máximo 500 caracteres, viñetas «• ».
- Se actualizan en cada versión (skill `release-notes`, destino `play`).
- Con el secreto `PLAY_SERVICE_ACCOUNT_JSON`, el CI las sube junto con el AAB.
- Subida manual: copiar el texto en «Novedades» envuelto en `<es-ES>` y `</es-ES>`.

## Qué cambia en la app instalada desde Play
- No tiene red ni avisa de versiones: las actualizaciones las da Play. Instalada con Obtainium o `adb`, Obtainium
  avisa de las releases de GitHub (ADR-0011).
- En el mismo móvil no se pueden mezclar Play y Obtainium, porque Play firma con su clave y las firmas no coinciden.
  Para pasar de una a otra hay que desinstalar, y el historial se pierde salvo que haya copia de seguridad de
  Android.

## Problemas típicos
- Si falla la subida a Play, la release de GitHub ya está hecha. En la ejecución de Actions, «Re-run failed jobs»
  repite solo el job `play` (el AAB se guarda 7 días como artefacto). Si no, sube a mano el AAB adjunto a la release.
- `Only releases with status draft may be created on draft app`: falta poner `PLAY_ESTADO=draft`, o la app sigue en
  borrador en Play.
- `APK specifies a version code that has already been used`: el clon era superficial (el versionCode es `git rev-list --count HEAD` y necesita `fetch-depth: 0`) o esa versión ya estaba subida. No hay que subir nada a mano.
- `The caller does not have permission`: la cuenta de servicio no está invitada en Play Console o le falta el
  permiso.

## Avisos conocidos

### «Este App Bundle contiene código nativo, pero no has subido símbolos de depuración»
- Es informativo: no bloquea la subida ni la publicación.
- Lo provoca `libandroidx.graphics.path.so`, de Compose UI. Google lo distribuye sin símbolos, así que no se puede
  simbolizar.
- Activar `ndk.debugSymbolLevel` no lo quita, y obligaría a fijar un NDK en el CI.
- Desde la v1.3.3, el CI saca esas `.so` del AAB y las mete en `native-debug-symbols-<v>.zip`, con carpetas por
  ABI. Las adjunta a la release y las sube a Play con `debugSymbols`; conservan la tabla de símbolos exportados. Si
  Play sigue avisando, se ignora: no bloquea nada.
- Desde la v1.3.2 aparece menos: se quitó la dependencia sin uso datastore-preferences, que añadía
  `libdatastore_shared_counter.so`.
- Más información: <https://developer.android.com/build/include-native-symbols>
