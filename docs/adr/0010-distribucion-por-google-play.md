# ADR-0010: Distribuir también por Google Play, pista interna, desde el mismo CI

- Estado: aceptado
- Fecha: 2026-10-18
- Relacionado: `docs/decisiones.md` § 2026-10-18 · Ronda 9, [ADR-0009](0009-aviso-de-version-nueva.md), [Google Play](../play.md)

## Contexto
- Con la v1.0 Joan quiere poder publicar en Google Play, además de la release de GitHub que instala con Obtainium.
- La release ya sale sola del CI al cambiar `version.properties` y hacer push a main, con el APK firmado con la clave
  de `~/.souschef/` (secretos `ANDROID_*`).
- Play solo acepta AAB y exige crear la ficha y subir la primera versión a mano desde Play Console. Las siguientes se
  pueden subir por la API de Google Play Developer con una cuenta de servicio.

## Decisión
- El job `release` compila `assembleRelease bundleRelease` y adjunta el APK y el AAB a la release de GitHub, con sus
  sha256.
- Un job aparte, `play`, con `permissions: {}`, sin checkout ni keystore, recibe el AAB y el mapping como artefacto.
  Así la acción de terceros no ve la clave ni un token con escritura (revisión de seguridad de la ronda 9).
- Si existe el secreto `PLAY_SERVICE_ACCOUNT_JSON`, ese job sube el AAB a la pista **internal** con
  `r0adkll/upload-google-play` fijada por SHA (v1.1.5), junto con el `mapping.txt` de R8. El estado de la release
  sale de la variable `PLAY_ESTADO`: `draft` por defecto y `completed` cuando la app salga de borrador en Play.
  - Sin el secreto, el paso se omite con un aviso: la release de GitHub no depende de Play.
- La clave actual pasa a ser la **clave de subida**, y Play App Signing guarda la de firma. El keystore no cambia.
- Instalada desde Play (`installingPackageName == com.android.vending`), la app no consulta GitHub ni avisa de versión
  nueva: actualiza la tienda. Con Obtainium o `adb` todo sigue como en ADR-0009.
- Política de privacidad pública en `docs/privacidad.md`, porque la app pide el micrófono.
- Nota (2026-10-20): el aviso de versión nueva de GitHub ya no existe; ver [ADR-0011](0011-sin-aviso-de-version-nueva.md).

## Consecuencias
- Hay una sola fuente de versiones (`version.properties`) para GitHub y para Play, así que `versionCode` tiene que
  subir siempre.
- Pasar de la pista interna a producción es manual, en Play Console. Las cuentas personales nuevas necesitan antes
  una prueba cerrada de 12 testers durante 14 días; ver [Google Play](../play.md).
- La credencial de Play la recibe una acción de terceros. Está fijada por SHA y solo corre en el job de release de
  main.
- `USE_EXACT_ALARM` y `USE_FULL_SCREEN_INTENT` se declaran en Play Console como app de temporizador.

## Alternativas consideradas
- **Gradle Play Publisher:** añade una dependencia al build para algo que es un paso de CI.
- **Subir siempre a mano:** sigue siendo posible, porque el AAB va en la release, pero es fácil olvidarlo.
- **fastlane supply:** necesita Ruby en el runner y más configuración para el mismo resultado.
