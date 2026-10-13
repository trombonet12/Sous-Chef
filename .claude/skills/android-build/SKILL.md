---
name: android-build
description: Compilar Sous Chef con Gradle y verificar el artefacto: debug (APK), release (APK firmado), bundle (AAB) o cli (:tools); no ejecuta tests (android-run-tests) ni lint (android-static-analysis).
argument-hint: "[debug|release|bundle|cli]"
allowed-tools: Read, Grep, Glob, Bash(./gradlew *), Bash(ls *), Bash(test *)
---

# android-build

Compila según `$ARGUMENTS` (por defecto `debug`) desde la raíz del repo y comprueba que el artefacto existe y es el esperado.

## Cuándo usarla
- Generar un APK debug o release, un AAB, o recompilar la CLI `:tools`.
- Confirmar que el proyecto compila tras un cambio en `:app`, `:receta` o `:tools`.

## Cuándo NO usarla
- Ejecutar tests → `android-run-tests`. ktlint, detekt, Lint → `android-static-analysis`.
- Publicar o distribuir una release → skill `release` **[manual]**.
- El build falla y hay que diagnosticar → `android-debug-failure`.

## Precondiciones
- Gradle desde la raíz (`./gradlew`; no hay `apps/android`, pnpm ni `scripts/gradle.mjs`). Módulos: `:app`, `:receta` (JVM puro), `:tools`.
- JDK 21 vía `~/.gradle/gradle.properties` y Android SDK (`ANDROID_HOME` o `local.properties`); ver CLAUDE.md § Comandos.
- `release` y `bundle`: credenciales de firma tomadas de `~/.souschef/` o variables de entorno según `app/build.gradle.kts`. Solo comprobar que existen (`test -n "$VAR"`), **nunca imprimir valores**.
- Nunca añadir un keystore al repo ni contraseñas a `gradle.properties`, scripts o docs.

## Pasos
1. Resolver `$ARGUMENTS`; si falta, `debug`; si no es uno de los cuatro valores, pedir aclaración y parar.
2. Ejecutar:

| Valor | Comando | Artefacto esperado |
|-------|---------|--------------------|
| `debug` | `./gradlew assembleDebug` | `app/build/outputs/apk/debug/*.apk` |
| `release` | `./gradlew assembleRelease` | `app/build/outputs/apk/release/*.apk` |
| `bundle` | `./gradlew bundleRelease` | `app/build/outputs/bundle/release/*.aab` |
| `cli` | `./gradlew :receta:test :tools:installDist` | `tools/build/install/tools/bin/tools` |

3. `release`/`bundle`: comprobar antes la firma (ver Precondiciones). Si falta, parar: el artefacto no sería releasable.
4. Si Gradle falla, no reintentar a ciegas: pasar el log a un subagente haiku de análisis o seguir `android-debug-failure`.
5. Verificar con `ls -l`: existe, tamaño > 0, fecha reciente, nombre acorde al tipo (un APK release sin firmar no se distribuye).
6. Informar: tarea ejecutada, ruta, tamaño y si hubo firma (sí/no, sin valores).

## Validaciones / tests
- `BUILD SUCCESSFUL` y código de salida 0.
- `ls -l` sobre la ruta del artefacto del valor pedido.
- `git status` sin keystores ni credenciales nuevas.

## Definition of Done
- [ ] La tarea Gradle del valor pedido termina en `BUILD SUCCESSFUL`.
- [ ] Artefacto localizado y reportado con ruta y tamaño.
- [ ] Firma release solo vía credenciales externas al repo; ningún secreto impreso ni versionado.
- [ ] `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug` en verde si el build cierra una tarea.

## Docs a actualizar
- Si cambia la forma de firmar o los nombres de artefacto: `docs/herramientas.md` · docs/ESTADO.md (skill `session-handoff`)
