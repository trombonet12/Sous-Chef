# Herramientas de línea de comandos (`scripts/sc`)

> Propósito: contrato de la CLI que usan las skills y los humanos. · Audiencia: Claude Code y el usuario. · Actualizado: 2026-10-13

`scripts/sc` (bash) y `scripts/sc.cmd` (cmd/PowerShell) ejecutan `tools/build/install/tools/bin/tools`, recompilando con
`./gradlew :tools:installDist -q` solo si `tools/` o `receta/` cambiaron. Salida en español, sin colores si no hay TTY.
Código de salida: 0 bien · 1 fallos de validación/diferencias · 2 error de uso o de entorno.

**Nunca imprime secretos.** Lee `~/.souschef/supabase.env` (`SUPABASE_DB_URL`, `SOUS_CHEF_EMAIL`, `SOUS_CHEF_PASSWORD`) y,
para `--local`, las credenciales del stack local de `npx supabase` (puertos 553xx).

| Subcomando | Qué hace |
|---|---|
| `sc validar [--estricto] [--json] [slug…]` | Valida `data/recetas/*.json` y `data/tecnicas/*.json` (o los slugs dados): plan que cuadra + completitud (ver `docs/cocina/esquema-receta.md § Validación`). La comprobación de despensa usa la despensa de Supabase si hay credenciales, si no `data/despensa-semilla.md`, y lo dice. |
| `sc formatear [slug…]` | Reescribe los JSON con formato canónico (`indent 2`, claves ordenadas, UTF-8, `\n` final). |
| `sc publicar [--local] [--seco]` | Valida en estricto y sube recetas y técnicas a `sous_chef.recetas/tecnicas` por JDBC en una transacción; salta las que no cambiaron (hash); marca `deleted_at` en las que ya no están en git. `--seco` solo informa. |
| `sc comparar [--local]` | Compara repo ↔ Supabase (recetas y técnicas, ignorando `creada`/`actualizada`). Sale con 1 si hay deriva. |
| `sc despensa [--local] [--json]` | Imprime la despensa del usuario (por categoría y estado: hay/poco/falta/vetado) y la lista de la compra pendiente. Es lo que lee la skill `receta-cronometrada` antes de diseñar. |
| `sc despensa poner <estado> <nombre…>` | Cambia el estado de uno o varios productos (crea el producto si no existe, pidiendo `--categoria`). |
| `sc compra añadir <nombre> [--cantidad X] [--receta slug]` | Añade a la lista de la compra. |
| `sc historial [--json] [-n 20]` | Lista lo cocinado (para no repetir base ni técnica dos días seguidos). |
| `sc migrar [--local] [--estado]` | Aplica `db/migraciones/*.sql` en orden con su tabla `sous_chef._migraciones` (checksum, advisory lock, una transacción por archivo, `notify pgrst`). `--estado` solo lista. |
| `sc importar-legado [--local]` | Una sola vez: historial y preferencias de `data/legado/` y la semilla de despensa de `data/despensa-semilla.md` (con los vetos de `docs/cocina/reglas.md` como `vetado`). Idempotente. |
| `sc comprobar-entorno` | Comprobación previa sin imprimir valores: archivo de secretos, conexión, esquema expuesto. |
