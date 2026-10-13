---
name: dependency-add
description: Evaluar y añadir una dependencia nueva (Gradle/Version Catalog) con versión exacta; no para actualizar o quitar (dependency-maintenance).
allowed-tools: Read, Grep, Glob, Edit, Bash(./gradlew *)
---

# dependency-add

Decide si una dependencia merece entrar y, solo entonces, la añade con versión exacta y decisión registrada. Sin esta skill no se añade ninguna dependencia (CLAUDE.md § Reglas de trabajo).

## Cuándo usarla
- Se propone una librería o plugin nuevo para `app/`, `receta/` o `tools/`.

## Cuándo NO usarla
- Actualizar, quitar o inventariar licencias de lo existente → usa `dependency-maintenance`.
- Herramienta ya fijada en `docs/decisiones.md` (versiones): usar esa versión, sin repetir la evaluación completa.

## Precondiciones
- Nombre de la librería y módulo destino claros (`app`, `receta`, `tools`). Si faltan, pedirlos y parar.
- `gradle/libs.versions.toml` es reservado del agente principal: un subagente propone el cambio en su respuesta.
- Fase activa compatible con la dependencia (no implementar fuera de fase).

## Pasos
1. **Necesidad**: ¿lo resuelve la plataforma, la librería estándar de Kotlin o algo ya presente (`Grep` en `gradle/libs.versions.toml` y `*.gradle.kts`)? Si sí, no añadir.
2. **Licencia**: compatible con un proyecto privado (MIT, Apache-2.0, BSD, ISC). Copyleft o sin licencia → parar y consultar.
3. **Mantenimiento**: último release, frecuencia, issues críticos. Datos no verificables → marcar `VERIFICAR`, no afirmarlos.
4. **Tamaño**: impacto en el APK y en el arranque; evitar transitivas pesadas. `receta/` debe seguir siendo Kotlin/JVM puro (sin dependencias Android).
5. **Seguridad**: sin servicios con backend propio ni telemetría; permisos nuevos en el manifest justificados.
6. **Versión exacta publicada hace ≥ 2 semanas**; compatible con Kotlin, AGP y las versiones fijadas en `docs/decisiones.md`.
7. **Instalar**: versión y alias en `gradle/libs.versions.toml` (`[versions]` y `[libraries]`/`[plugins]`) y usarlo vía `libs.*`; nunca coordenadas con versión en `build.gradle.kts`. Sin rangos ni `+`.
8. **Registrar la decisión**: ADR con `create-adr` si es estructural (persistencia, DI, red, seguridad); si no, una línea en `docs/decisiones.md` vía agente principal.

## Validaciones / tests
- `./gradlew assembleDebug testDebugUnitTest :receta:test` (la build resuelve la dependencia).
- `./gradlew ktlintCheck detekt lintDebug`.

## Definition of Done
- [ ] Necesidad, licencia, mantenimiento, tamaño y seguridad evaluados y anotados.
- [ ] Versión exacta con ≥ 2 semanas de antigüedad y compatible con las versiones fijadas.
- [ ] Declarada en el Version Catalog y consumida por `libs.*`.
- [ ] Decisión registrada (ADR si es estructural).
- [ ] Gate qa fast en verde.

## Docs a actualizar
- `docs/adr/` (si estructural) · `docs/decisiones.md` (vía agente principal, reservado) · docs/ESTADO.md (skill `session-handoff`)
