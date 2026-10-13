# Sous Chef

## Producto
App Android personal de cocina: recetas cronometradas (ficha imprimible + cronómetro de varios recipientes), historial de
lo cocinado, técnicas, **despensa editable y lista de la compra**. Un usuario (Joan). Las recetas se diseñan y revisan
**desde este repo con Claude Code** (skill `receta-cronometrada`) y se publican a Supabase; la app nunca edita recetas.

## Stack
- Android: Kotlin, Compose/Material 3, Hilt, Room, WorkManager, supabase-kt (`app/`, Gradle KTS + Version Catalog).
- `receta/`: Kotlin/JVM puro compartido (modelos, escalado, gantt, validador, motor del cronómetro).
- `tools/`: CLI JVM `scripts/sc` (validar, publicar, comparar, despensa, migrar, importar-legado). Ver `docs/herramientas.md`.
- Backend: Supabase **compartido con Trombito**, esquema `sous_chef`. Migraciones en `db/migraciones/` con runner propio
  (`sc migrar`); **nunca** `supabase db push` ni `apply_migration` del MCP (ADR-0002).
- Decisiones y versiones: `docs/decisiones.md`. Arquitectura: `docs/arquitectura.md`, `docs/adr/`.

## Comandos
```
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug   # gate Android
./gradlew :receta:test :tools:installDist                                # dominio + CLI
scripts/sc validar --estricto            # valida data/recetas y data/tecnicas (scripts/sc.cmd en cmd/PowerShell)
scripts/sc publicar | comparar | despensa | migrar [--local]
npx supabase start > /dev/null 2>&1 ; npx supabase test db              # stack local (Docker), pgTAP
node scripts/check-claude-assets.mjs ; bash scripts/test-hooks.sh ; node scripts/check-docs.mjs
```
JDK 21 vía `~/.gradle/gradle.properties` (`org.gradle.java.installations.paths`). Secretos en `~/.souschef/` (nunca en el repo).

## Reglas de cocina (resumen; completas en `docs/cocina/reglas.md`)
- 2 raciones por defecto; vitrocerámica 0-9, máx. 3 fuegos a la vez; hora de servir por defecto 15:30.
- Vetos: garbanzos, guisantes, champiñones, coliflor, brócoli, casquería, anchoas, alcaparras, marisco. Pescado solo sin espinas.
- Solo ingredientes de la despensa (`scripts/sc despensa`) o declarados en `compra`. Gramos/ml + equivalencia de andar por casa.
- Toda receta pasa `scripts/sc validar --estricto` antes de publicarse. El repo manda; `sc publicar` sube.

## Reglas de trabajo
- Antes de trabajar: `docs/ESTADO.md` y la fase activa (`docs/fases/`). Al terminar: actualizar ESTADO.md (skill session-handoff).
- TDD en lógica crítica: validador, escalado, motor del cronómetro, sync, migraciones/RLS.
- Commits pequeños, Conventional Commits. Push a `origin main` solo con el gate en verde; nunca force-push.
- No añadir dependencias sin la skill dependency-add. No cambiar arquitectura sin ADR (skill create-adr).
- No tocar ni imprimir secretos (`~/.souschef/*`, `local.properties`, `.env*`, keystores). Consultar nombres, nunca valores.
- Toda tabla de usuario: `user_id uuid not null default auth.uid()` + RLS. `recetas`/`tecnicas`: solo SELECT para la app.
- No desactivar tests para pasar el gate. No ignorar errores. TODOs solo con registro en ESTADO.md.
- Ejecución desatendida: ante una duda que no bloquea, decidir, registrar en `docs/decisiones.md` y seguir; si bloquea,
  rodear y apuntar en `docs/ESTADO.md § Pendiente del usuario`.

## Delegación
- Subagentes pequeños: 1 objetivo, brief ≤ 40 líneas, ≤ 5 archivos (o un directorio propio), 1 comando de validación,
  respuesta ≤ 15 líneas. Plantilla `docs/_templates/BRIEF.template.md` (skill delegate-task).
- Modelo siempre explícito: haiku = mecánico · sonnet = ingeniería · opus = seguridad/arquitectura/revisión culinaria.
- Paralelo solo con archivos disjuntos (máx. 5). Solo el agente principal hace commits y push.
- Reservados al agente principal: CLAUDE.md, .claude/settings.json, .claude/hooks/**, docs/_templates/**, docs/ESTADO.md,
  docs/decisiones.md, gradle/libs.versions.toml, settings.gradle.kts.
- Procedimientos en `.claude/skills/` (índice `docs/skills.md`); especialistas en `.claude/agents/` (índice `docs/agentes.md`).
- Si un agente no está cargado: `general-purpose` + "Actúa según .claude/agents/<nombre>.md" con el modelo de su frontmatter.
