# Pasos manuales

> Propósito: lo que solo puede hacer el usuario. · Audiencia: Joan. · Actualizado: 2026-10-17

## 1. Secretos de GitHub para las Releases (una vez, sesión final)
En GitHub → Sous-Chef → Settings → Secrets and variables → Actions, crea los 4 secretos con los valores de
`%USERPROFILE%\.souschef\release-signing.properties` y del keystore:
- `ANDROID_KEYSTORE_BASE64`: `base64 -w0 ~/.souschef/souschef-release.jks` (ejecútalo tú; Claude no lo imprime).
- `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
Haz copia del keystore: sin él no se pueden publicar actualizaciones que se instalen encima.

## 2. Móvil (sesión final)
- Depuración USB activada y `adb devices` lo lista.
- Al abrir la cocina por primera vez: permitir notificaciones. En Android 12: permitir alarmas exactas si lo pide.
- Si el fabricante mata apps en segundo plano (Xiaomi, Samsung…), quitar Sous Chef de la optimización de batería.
