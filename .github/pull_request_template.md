## Resumen

<!-- Que cambia y por que, en 1-3 frases. -->

## Tipo (Conventional Commits)

- [ ] feat
- [ ] fix
- [ ] refactor
- [ ] perf
- [ ] test
- [ ] docs
- [ ] build / ci
- [ ] chore

## Fase / subfase

<!-- Ej.: Fase 2 / 2.3 (ver docs/fases/ y docs/ESTADO.md). No implementar fuera de la fase activa. -->

## Checklist (Definition of Done)

- [ ] Gate Gradle en verde: `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :receta:test :tools:test assembleDebug`
- [ ] `scripts/sc validar --estricto` en verde (si toca `data/`, `receta/` o `tools/`)
- [ ] Tests anadidos o actualizados (TDD en validador, escalado, motor del cronometro, sync, migraciones/RLS)
- [ ] Sin secretos, `.env*` ni keystores en el diff
- [ ] `docs/ESTADO.md` actualizado
- [ ] ADR creado si cambia la arquitectura (`docs/adr/`)
- [ ] Tablas de usuario nuevas con `user_id uuid not null default auth.uid()` + RLS
- [ ] Sin tests desactivados ni TODOs sin registro en ESTADO.md

## Como se probo

<!-- Comandos ejecutados y resultado, pruebas manuales, capturas si hay UI. -->
