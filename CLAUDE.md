# Sous Chef

## Producto
App Android personal de cocina: recetas cronometradas (ficha imprimible + cronómetro de varios recipientes), historial de
lo cocinado, técnicas, **despensa editable y lista de la compra**. Un usuario (Joan). Las recetas se diseñan y revisan
**desde este repo con Claude Code** (skill `receta-cronometrada`) y van empaquetadas en el APK (`data/`); la app nunca edita recetas. App **solo local**, sin backend (ADR-0007).

## Stack
- Android: Kotlin, Compose/Material 3, Hilt, Room (`app/`, Gradle KTS + Version Catalog).
- `receta/`: Kotlin/JVM puro compartido (modelos, escalado, gantt, validador, motor del cronómetro).
- `tools/`: CLI JVM `scripts/sc` (validar, formatear). Ver `docs/herramientas.md`.
- Decisiones y versiones: `docs/decisiones.md`. Arquitectura: `docs/arquitectura.md`, `docs/adr/`.

## Comandos
```
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug   # gate Android
./gradlew :receta:test :tools:installDist                                # dominio + CLI
scripts/sc validar --estricto            # valida data/recetas y data/tecnicas (scripts/sc.cmd en cmd/PowerShell)
node scripts/check-claude-assets.mjs ; bash scripts/test-hooks.sh ; node scripts/check-docs.mjs
```
JDK 21 vía `~/.gradle/gradle.properties` (`org.gradle.java.installations.paths`). Secretos en `~/.souschef/` (nunca en el repo).

## Reglas de cocina (resumen; completas en `docs/cocina/reglas.md`)
- 2 raciones por defecto; vitrocerámica 0-9, máx. 3 fuegos a la vez; hora de servir por defecto 15:30.
- Vetos: garbanzos, guisantes, champiñones, coliflor, brócoli, casquería, anchoas, alcaparras, marisco. Pescado solo sin espinas.
- Todo ingrediente de una receta está en la despensa (`data/despensa-semilla.md`); no hay `compra` por receta. Gramos/ml + equivalencia de andar por casa.
- Toda receta pasa `scripts/sc validar --estricto` antes de publicarse. El repo manda; una versión nueva en `version.properties` + push a main publica el APK.

## Reglas de trabajo
- Antes de trabajar: `docs/ESTADO.md` y la fase activa (`docs/fases/`). Al terminar: actualizar ESTADO.md (skill session-handoff).
- TDD en lógica crítica: validador, escalado, motor del cronómetro, avisos, intérprete de voz, migraciones Room.
- Commits pequeños, Conventional Commits. Push a `origin main` solo con el gate en verde; nunca force-push.
- No añadir dependencias sin la skill dependency-add. No cambiar arquitectura sin ADR (skill create-adr).
- No tocar ni imprimir secretos (`~/.souschef/*`, `local.properties`, `.env*`, keystores). Consultar nombres, nunca valores.
- No desactivar tests para pasar el gate. No ignorar errores. TODOs solo con registro en ESTADO.md.
- Ejecución desatendida: ante una duda que no bloquea, decidir, registrar en `docs/decisiones.md` y seguir; si bloquea,
  rodear y apuntar en `docs/ESTADO.md § Pendiente del usuario`.

## Delegación
- Subagentes pequeños: 1 objetivo, brief ≤ 40 líneas, ≤ 5 archivos (o un directorio propio), 1 comando de validación,
  respuesta ≤ 15 líneas. Plantilla `docs/_templates/BRIEF.template.md` (skill delegate-task).
- Modelo siempre explícito, también en Explore/Plan (nunca omitir `model`, que heredaría Opus): haiku = mecánico · sonnet = ingeniería · opus = seguridad/arquitectura/revisión culinaria.
- Paralelo solo con archivos disjuntos (máx. 5). Solo el agente principal hace commits y push.
- Reservados al agente principal: CLAUDE.md, .claude/settings.json, .claude/hooks/**, docs/_templates/**, docs/ESTADO.md,
  docs/decisiones.md, gradle/libs.versions.toml, settings.gradle.kts.
- Procedimientos en `.claude/skills/` (índice `docs/skills.md`); especialistas en `.claude/agents/` (índice `docs/agentes.md`).
- Si un agente no está cargado: `general-purpose` + "Actúa según .claude/agents/<nombre>.md" con el modelo de su frontmatter.
