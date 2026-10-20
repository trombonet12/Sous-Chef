---
name: release
description: Publicar una versión: subir version.properties, gate, commit y push a main, vigilar el CI (Release APK+AAB) y subir el AAB a Google Play; no preparar (release-preflight) ni notas (release-notes).
argument-hint: "[local|github]"
allowed-tools: Read, Edit, Grep, Bash(./gradlew *), Bash(git status*), Bash(git log*), Bash(git rev-parse*), Bash(git add version.properties*), Bash(git add distribution/*), Bash(git add CHANGELOG.md*), Bash(git commit *), Bash(git push origin main), Bash(gh run *), Bash(gh release view*), Bash(node scripts/gh.mjs *), Bash(wc *), Bash(sha256sum *)
disable-model-invocation: true
---

# release

Publica la versión `$ARGUMENTS`. Sin valor, preguntar `local|github` y parar. Acción con efectos externos: solo bajo petición explícita del usuario.

## Cuándo usarla
- `github`: el usuario pide publicar una versión nueva (release de GitHub con APK y AAB, y subida a Play).
- `local`: el usuario pide solo el APK firmado en su máquina, sin publicar nada.

## Cuándo NO usarla
- Sin `release-preflight` en GO para el commit actual → ejecutar antes `release-preflight`.
- Redactar las notas (CHANGELOG, GitHub Release, Play) → usa `release-notes`.
- Solo verificar el gate → usa `qa`.

## Precondiciones
- `release-preflight` en GO sobre el commit a publicar (ver `docs/ESTADO.md`); si falta o es de otro commit, parar.
- `release-notes` con destino Play hecho: `distribution/whatsnew/whatsnew-es-ES` listo para esta versión.
- `local`: variables `SOUSCHEF_KEYSTORE_FILE`, `SOUSCHEF_KEYSTORE_PASSWORD`, `SOUSCHEF_KEY_ALIAS`, `SOUSCHEF_KEY_PASSWORD` cargadas desde `~/.souschef/release-signing.properties`. Comprobar solo que están definidas, nunca leer ni mostrar valores.
- `github`: `gh` autenticado (o `node scripts/gh.mjs <args>`, que usa el token de Git Credential Manager). Jamás force-push.

## Pasos
1. `git status` y `git rev-parse HEAD`; mostrar commit y versión actual de `version.properties` y confirmar con el usuario antes de actuar.
2. **local**: `./gradlew assembleRelease` con las variables de firma en el entorno. El APK queda en `app/build/outputs/apk/release/`. Calcular su SHA-256 (`sha256sum`). Terminar con el paso 11.
3. **github**: subir `versionName` y `versionCode` en `version.properties` (`versionCode` mayor que el publicado; `versionName` coherente con el changelog).
4. Gate: `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug`. Si falla, parar y reportar; no se publica con el gate en rojo.
5. Comprobar `wc -m distribution/whatsnew/whatsnew-es-ES` ≤ 500.
6. Commit sin trailer `Co-Authored-By`: `chore(release): v<versionName>` (incluye `version.properties`, notas de Play y `CHANGELOG.md` si cambió).
7. `git push origin main` (publicar es el push; un push sin cambio de versión no publica nada).
8. Vigilar el CI: `gh run list --limit 3`, luego `gh run view <id> --json conclusion` hasta `success`. Si falla, reportar el job y no reintentar a ciegas.
9. Comprobar la release: `gh release view v<versionName> --json assets` debe listar `sous-chef-<v>.apk`, `sous-chef-<v>.aab` y `checksums.txt`.
10. Play: el job `play` solo sube si existe el secreto `PLAY_SERVICE_ACCOUNT_JSON` (hoy no existe). Mientras no exista, subida manual en Play Console: Prueba interna > Crear versión > subir el `.aab` de la release > en «Novedades» pegar las notas envueltas en `<es-ES>…</es-ES>`. Ver `docs/play.md`.
11. Informar `modo | commit | versión | resultado | artefactos | SHA-256`. No pegar logs con valores sensibles.

## Validaciones / tests
- `local`: `assembleRelease` termina con éxito y el APK existe; `adb install -r` solo si el usuario lo pide.
- `github`: gate en verde antes del push; run del CI en `success`; la release tiene los 3 assets.
- Aviso de Play «código nativo sin símbolos de depuración»: se ignora; viene de `libandroidx.graphics.path.so` (Compose, ya stripped). Ver `docs/play.md` § Avisos conocidos.

## Definition of Done
- [ ] `release-preflight` en GO para el commit publicado.
- [ ] `github`: run en `success`, release `v<versionName>` con APK, AAB y `checksums.txt`.
- [ ] `github`: AAB subido a la pista interna (CI o manual) con las notas de `whatsnew-es-ES`.
- [ ] `local`: APK firmado generado con commit y SHA-256 registrados.
- [ ] Ningún force-push, tag ni valor de secreto fuera de lo pedido; keystore y `release-signing.properties` fuera del repo y sin imprimir.

## Docs a actualizar
- Versión, commit y estado de Play en docs/ESTADO.md (skill `session-handoff`) · `CHANGELOG.md` (skill `release-notes`)
