---
name: release
description: Publicar una release Android: build firmado local con assembleRelease y/o GitHub Release vía release-android.yml (workflow_dispatch); no para preparar (release-preflight) ni notas (release-notes).
argument-hint: "[local|github]"
allowed-tools: Read, Grep, Bash(./gradlew assembleRelease*), Bash(gh workflow *), Bash(gh run *), Bash(git status*), Bash(git log*), Bash(git rev-parse*), Bash(sha256sum *)
disable-model-invocation: true
---

# release

Publica la release `$ARGUMENTS`. Sin valor, preguntar `local|github` y parar. Acción con efectos externos: solo bajo petición explícita del usuario.

## Cuándo usarla
- El usuario pide generar el APK firmado (`local`) o publicarlo como GitHub Release (`github`).

## Cuándo NO usarla
- Sin `release-preflight` en GO para el commit actual → ejecutar antes `release-preflight`.
- Redactar las notas → usa `release-notes`.
- Solo verificar el gate → usa `qa`.

## Precondiciones
- `release-preflight` en GO sobre el commit que se publica (comprobar `docs/ESTADO.md`); si falta o es de otro commit, parar.
- `local`: variables `SOUSCHEF_KEYSTORE_FILE`, `SOUSCHEF_KEYSTORE_PASSWORD`, `SOUSCHEF_KEY_ALIAS`, `SOUSCHEF_KEY_PASSWORD`, cargadas desde `~/.souschef/release-signing.properties`. Comprobar solo que están definidas, nunca leer ni mostrar valores.
- `github`: el commit ya está en `origin main`; `gh` autenticado y los secrets del workflow configurados (solo nombres). Nunca `git push` ni tags salvo petición explícita en este turno, y jamás force-push.

## Pasos
1. `git status` limpio y `git rev-parse HEAD`; mostrar el commit y confirmar con el usuario antes de actuar.
2. **local**: `./gradlew assembleRelease` con las variables de firma en el entorno. El APK queda en `app/build/outputs/apk/release/`. Calcular su SHA-256 (`sha256sum` o `Get-FileHash`).
3. **github**: `gh workflow run release-android.yml` (workflow_dispatch; pasar `-f version_name=<versionName>` si el workflow lo define). Seguir con `gh run list --workflow release-android.yml --limit 1` y `gh run watch <id>`.
4. Si falla por secrets (`Falta el secret …`) o por el gate, detener y reportar; sin reintentos a ciegas.
5. **github**: descargar el artefacto (`gh run download <id> -D <dir-nuevo-vacío>`), verificar que contiene el APK firmado y comparar su SHA-256 con el publicado.
6. Informar `modo | commit | resultado | artefacto | SHA-256`. No pegar logs con valores sensibles.

## Validaciones / tests
- `local`: `assembleRelease` termina con éxito y el APK existe; instalarlo en un dispositivo con `adb install -r` si el usuario lo pide.
- `github`: `gh run view <id>` concluye `success` y el hash coincide.

## Definition of Done
- [ ] `release-preflight` en GO para el commit publicado.
- [ ] APK firmado generado (o run en `success`) con commit y SHA-256 registrados.
- [ ] Ningún push, tag ni valor de secreto fuera de lo pedido por el usuario.
- [ ] El keystore y `release-signing.properties` siguen fuera del repo y no impresos.

## Docs a actualizar
- Registro de la release (commit, versión, SHA-256) en docs/ESTADO.md (skill `session-handoff`) · `CHANGELOG.md` (skill `release-notes`)
