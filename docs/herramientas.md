# Herramientas de línea de comandos (`scripts/sc`)

> Propósito: contrato de la CLI que usan las skills y los humanos. · Audiencia: Claude Code y el usuario. · Actualizado: 2026-10-17

`scripts/sc` (bash) y `scripts/sc.cmd` (cmd/PowerShell) ejecutan `tools/build/install/tools/bin/tools`, recompilando con
`./gradlew :tools:installDist -q` solo si `tools/` o `receta/` cambiaron. Salida en español, sin colores si no hay TTY.
Código de salida: 0 bien · 1 fallos de validación · 2 error de uso o de entorno.

**Estado actual (MVP local):** la CLI solo tiene `validar` y `formatear`. No hay backend: las recetas y técnicas viajan
dentro del APK desde `data/recetas/*.json` y `data/tecnicas/*.json`. Publicar = validar en estricto + commit +
`versionCode`/`versionName` en `version.properties` + gate + push a main (`android.yml` crea la GitHub Release v<versionName> con APK, AAB y checksums.txt).

**Nunca imprime secretos.**

| Subcomando | Qué hace |
|---|---|
| `sc validar [--estricto] [--json] [slug…]` | Valida `data/recetas/*.json` y `data/tecnicas/*.json` (o los slugs dados): plan que cuadra + completitud (ver `docs/cocina/esquema-receta.md § Validación`). La comprobación de despensa (regla 12) usa `data/despensa-semilla.md`; «agua» siempre vale. |
| `sc formatear [--comprobar] [slug…]` | Reescribe los JSON con formato canónico (`indent 2`, claves ordenadas, UTF-8, `\n` final). |
