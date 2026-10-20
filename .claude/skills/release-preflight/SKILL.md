---
name: release-preflight
description: Checklist obligatorio antes de publicar: gate qa full, secret scan, versión/changelog y firma; informa GO/NO-GO, no publica (para eso release).
allowed-tools: Read, Grep, Glob, Bash(./gradlew *), Bash(scripts/sc *), Bash(node scripts/*), Bash(bash scripts/*), Bash(git status*), Bash(git log*), Bash(git diff*), Bash(gh secret list*), Bash(gh workflow list*)
---

# release-preflight

Comprueba que el repositorio está listo para publicar. Solo lee y ejecuta verificaciones; el veredicto es GO / NO-GO con huecos justificados.

## Cuándo usarla
- Antes de cualquier release Android y antes de invocar `release`.

## Cuándo NO usarla
- Publicar o disparar workflows → usa `release` (que exige este preflight).
- Generar las notas desde commits → usa `release-notes`.
- Revisión de calidad sin release → usa `qa`.

## Precondiciones
- Árbol limpio (`git status`); si no, listar cambios y parar con NO-GO.
- `gh` autenticado solo para comprobar secrets de CI; si no, el punto 6 queda "no verificable" (no es un OK).
- Nunca leer, imprimir ni pedir valores de secretos ni del keystore.

## Pasos
1. Gate qa full (skill `qa`): `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :receta:test assembleDebug`, `scripts/sc validar --estricto`, `node scripts/check-claude-assets.mjs`, `bash scripts/test-hooks.sh`, `node scripts/check-docs.mjs`. Un SKIP es un hueco que debe justificarse.
2. Secret scan: skill `security-secret-scan` sobre árbol e historial; cualquier hallazgo = NO-GO hasta rotar/limpiar.
3. Migraciones Room: si `data/db/Migrations.kt` cambió, el test de migración de `./gradlew testDebugUnitTest` debe estar en verde; si no, anotar «sin migraciones».
4. Versión: localizar `versionCode` y `versionName` en `app/build.gradle.kts` (`Grep versionCode`). `versionCode` debe superar al publicado y `versionName` ser coherente con el changelog.
5. Notas: existe `CHANGELOG.md` o notas de la versión (skill `release-notes`); si no, proponer generarlas. Las notas de Play `distribution/whatsnew/whatsnew-es-ES` deben estar al día para esta versión y medir ≤ 500 (`wc -m`).
6. Firma y CI (solo nombres): `~/.souschef/release-signing.properties` existe y define `SOUSCHEF_KEYSTORE_FILE`, `SOUSCHEF_KEYSTORE_PASSWORD`, `SOUSCHEF_KEY_ALIAS`, `SOUSCHEF_KEY_PASSWORD` (comprobar claves, no valores); el workflow `.github/workflows/android.yml` existe y genera la release con APK, AAB y `checksums.txt` al hacer push a main; sus secrets constan en `gh secret list`. Falta alguno → NO-GO.
7. Entregar informe `punto | estado (OK/HUECO/NO-GO) | evidencia | acción` y el veredicto final.

## Validaciones / tests
- Gate qa full con código 0 (o huecos documentados).
- `node scripts/check-claude-assets.mjs` sin errores si cambiaron skills o agentes.

## Definition of Done
- [ ] Informe con los 6 puntos y veredicto GO/NO-GO.
- [ ] Ningún valor de secreto en la salida.
- [ ] Todo hueco con justificación y responsable.
- [ ] Veredicto GO guardado en docs/ESTADO.md con el hash del commit evaluado.

## Docs a actualizar
- docs/ESTADO.md (skill `session-handoff`): veredicto, commit evaluado y huecos abiertos.
