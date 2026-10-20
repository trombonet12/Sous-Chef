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
   2. Subir el `sous-chef-<versión>.aab` de la release de GitHub.
   3. Añadir a Joan como tester (lista de correos) y abrir el enlace de participación en el móvil.
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
6. **Ficha de Play Store:** descripción corta y larga, icono de 512 px, gráfico de 1024×500 y al menos 2 capturas
   (sirven las de `docs/capturas/`).

## Subida automática
1. En Google Cloud:
   1. crear una **cuenta de servicio** en un proyecto;
   2. activar la *Google Play Android Developer API*;
   3. descargar la clave JSON. No va al repo: se guarda en `~/.souschef/`.
2. En Play Console > Usuarios y permisos, invitar el correo de la cuenta de servicio, solo para esta app, con permiso
   para **publicar en pistas de prueba**.
3. En GitHub > Settings > Secrets and variables > Actions:
   - secreto `PLAY_SERVICE_ACCOUNT_JSON`, con el contenido del JSON;
   - variable `PLAY_ESTADO`:
     - `draft` mientras la app esté en borrador en Play;
     - `completed` cuando ya haya salido una versión, para que cada release llegue sola a los testers.
4. A partir de ahí, cada push a main con un `version.properties` nuevo publica en GitHub **y** en la pista interna.
   Para pasar de interna a producción, en Play Console: «Promocionar versión».

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
- `APK specifies a version code that has already been used`: hay que subir `versionCode` en `version.properties`.
- `The caller does not have permission`: la cuenta de servicio no está invitada en Play Console o le falta el
  permiso.
