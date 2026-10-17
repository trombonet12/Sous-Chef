# Catálogo de skills

Una línea por skill: `nombre | argumentos | contenido mínimo`. Las marcadas **[manual]** llevan
`disable-model-invocation: true` (efectos irreversibles). Fuente para `scripts/gen-claude-index.mjs`.

## 1. Cocina (dominio)
- receta-cronometrada | — | Diseña una receta cronometrada (gramos, cortes, potencia 0-9, tapa, hitos, mise en place, cronología hacia atrás) en data/recetas/<slug>.json, la valida y la publica. Úsala en recetas nuevas
- receta-revisar | — | Revisa una receta existente de data/recetas contra las reglas y el esquema
- tecnica-nueva | — | Crea una ficha de técnica culinaria en data/tecnicas/<slug>.json con el esquema de técnica y la enlaza desde tags.tecnica de las recetas que la usan
- despensa | — | Lee la despensa (pegada o semilla) y mantiene data/despensa-semilla.md alineada para el validador. Úsala para saber qué hay o qué falta

## 2. Arquitectura y diseño
- create-adr | — | Registrar una decisión de arquitectura como ADR numerado en docs/adr/ y enlazarlo en los docs
- design-feature | — | Diseñar una feature antes de implementarla (contratos, flujo de datos, impacto, plan de lotes)
- architecture-review | [boundaries|data-flow|domain|concurrency] | Revisar arquitectura sin tocar código, por foco [boundaries|data-flow|domain|concurrency], con findings por severidad

## 3. Android — crear
- android-create-feature | [nombre_feature] | Crear una feature Android de UI en Sous Chef (screen Compose, ViewModel con UiState/StateFlow, ruta en AppNavHost, tests JVM)
- android-create-data-layer | [nombre_entidad] | Crear la capa de datos Android de una entidad de Sous Chef (entity Room, DAO, repository, mapper y tests JVM)
- android-create-room-migration | — | Crear una migración Room manual (versión N a N+1) en data/db/Migrations.kt con schema JSON versionado y test Robolectric JVM
- android-create-hilt-module | — | Crear un módulo Hilt en data/di/*Module.kt con scopes y bindings de interfaces de dominio, y su reemplazo con fakes en tests
- android-add-permission | — | Añadir un permiso Android a Sous Chef (manifest, petición runtime, rationale, denegación permanente) con test y mínimo privilegio
- android-create-system-component | [notification|alarm-receiver|boot-receiver] | Crear piezas de sistema del cronómetro de Sous Chef: canal de notificación, receiver de alarma exacta (setAlarmClock) o de arranque (BOOT_COMPLETED)

## 4. Android — operar y revisar
- android-build | [debug|release|bundle|cli] | Compilar Sous Chef con Gradle y verificar el artefacto: debug (APK), release (APK firmado), bundle (AAB) o cli (:tools)
- android-run-tests | [app|receta|all] | Ejecutar tests de Sous Chef y leer reportes: app (JVM con Robolectric/Compose), receta (dominio puro) o all
- android-static-analysis | — | Pasar ktlintCheck, detekt y Android Lint (lintDebug) sobre Sous Chef y resumir hallazgos
- android-install-and-logcat | — | Arrancar el emulador trombito-36, instalar el APK debug de Sous Chef, lanzar la app, leer logcat filtrado sin datos sensibles y capturar pantalla
- android-debug-failure | [build|crash] | Diagnosticar un fallo Android de Sous Chef, build (Gradle) o crash (logcat): análisis de log, hipótesis y reproducción mínima
- android-profile | [cpu|memory|battery] | Medir CPU, memoria o batería de Sous Chef (p. ej. cronómetro con varios recipientes, alarmas) antes de optimizar y registrar métricas antes/después
- android-review | [lifecycle|background|permissions|alarms] | Revisar Android de Sous Chef sin modificar (lifecycle, background, permissions, alarms) con findings por severidad

## 5. Calidad y tests
- test-write | [unit|domain|integration|android-ui|db|cli] | Escribir tests por tipo (unit, domain, integration, android-ui, db, cli) con TDD en lógica crítica
- test-create-double | [fixture|mock|fake] | Crear dobles deterministas reutilizables (fixture, mock o fake) sin azar ni tiempo real
- test-debug-flaky | — | Diagnosticar y corregir un test intermitente (aislar, repetir, hallar causa raíz)
- qa | [fast|full] | Ejecutar la puerta de calidad en modo fast o full e interpretar el resultado
- qa-scenario | [process-death|apk-update|reboot|doze|permissions|timezone|rotation|dark-theme] | Guion manual en Android por escenario (process-death, apk-update, reboot, doze, permissions, timezone, rotation, dark-theme)
- bugfix-flow | [descripción-del-bug] | Corregir un bug: reproducir, test que falla, arreglo, regresión y causa raíz documentada

## 6. Seguridad
- security-review | [secrets|android-permissions|components|alarms|privacy|logging|ci] | Revisar seguridad de un área (secrets|android-permissions|components|alarms|privacy|logging|ci) con findings por severidad, sin editar
- security-secret-scan | — | Buscar secretos (claves, JWT, service_role, keystores, contraseñas) en árbol e historial sin mostrar valores

## 7. Git, release y dependencias
- git-check | [working-tree|generated|large-files] | Auditar el árbol git (solo lectura) [working-tree|generated|large-files]: cambios sin commitear, generados rastreados, archivos grandes. Para commitear usa git-commit
- git-commit | — | Revisar y crear un commit local Conventional Commits tras el gate qa fast y secret scan **[manual]**
- git-prepare-pr | — | Redactar la descripción de un PR (resumen, tipo, fase, DoD, pruebas) para pegar o revisar
- release | [local|github] | Publicar una release Android: build firmado local con assembleRelease y/o GitHub Release vía release-android.yml (workflow_dispatch) **[manual]**
- release-preflight | — | Checklist obligatorio antes de publicar: gate qa full, secret scan, versión/changelog y firma
- release-notes | — | Redactar notas de versión desde Conventional Commits (último tag..HEAD), por tipo y en lenguaje de usuario final
- dependency-add | — | Evaluar y añadir una dependencia nueva (Gradle/Version Catalog) con versión exacta
- dependency-maintenance | [upgrade|remove|unused|license] | Mantener dependencias existentes según [upgrade|remove|unused|license]: actualizar, quitar, detectar sin uso o inventariar licencias

## 8. Sesión, fases y documentación
- session-start | — | Al empezar una sesión o retomar trabajo: leer ESTADO.md, fase activa y ADRs, ver git y verificar el entorno
- session-handoff | [normal|compact] | Actualizar docs/ESTADO.md al cerrar tarea o sesión [normal|compact]
- phase | [start|plan|split|complete] [NN] | Gestionar docs/fases/NN-*.md (start = arranque | plan = lotes | split = subfases | complete = gates y DoD)
- delegate-task | — | Generar un brief para un subagente con BRIEF.template.md y comprobar límites de tamaño, validación y paralelismo
- docs-update | [architecture|data-model|testing|ci|herramientas|runbook] | Actualizar el doc afectado (architecture|data-model|testing|ci|herramientas|runbook) según DOC.template.md
- docs-check | [links|consistency] | Revisar docs/** y CLAUDE.md (links = enlaces/anclas rotos | consistency = versiones y datos vs decisiones.md, VERIFICAR)
