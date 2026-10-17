# ADR-0003: El repo manda en las recetas; publicación unidireccional

- Estado: aceptado (publicación revisada por [ADR-0007](0007-app-solo-local-sin-supabase.md): sin `sc publicar`, el APK empaqueta `data/`)
- Fecha: 2026-10-13
- Relacionado: `docs/decisiones.md` § Recetas, [ADR-0001](0001-stack-android-y-modulos.md), [ADR-0002](0002-supabase-compartido-esquema-sous-chef.md), [herramientas](../herramientas.md)

## Contexto
- En el legado, las recetas existían en un proyecto de claude.ai, en un artifact HTML y en una base de datos intermedia (doble espejo). Cada copia derivó de las otras (campos que faltan, ids fósiles, ingredientes duplicados).
- Las recetas se diseñan y revisan con Claude Code desde este repo; la app solo las lee. Un único usuario.
- Hay que decidir qué credenciales necesita cada operación sin exponer privilegios innecesarios.

## Decisión
- `data/recetas/*.json` y `data/tecnicas/*.json` son la fuente de verdad. Pasan `sc validar --estricto` antes de publicarse.
- `sc publicar` sube a Supabase en un solo sentido, por JDBC y en una transacción: calcula un hash por documento, solo escribe lo que cambia y marca como retirado lo que ya no está en git. `sc comparar` detecta diferencias entre repo y remoto.
- Las tablas `recetas` y `tecnicas` (`doc jsonb`, hash, actualizada) tienen RLS con **solo SELECT** para la app: ningún cliente puede modificarlas.
- Credenciales (en `~/.souschef/`, nunca en el repo):
  - `SUPABASE_DB_URL` para `publicar` y `migrar`;
  - email y contraseña del usuario (sesión) para `despensa` y los datos de usuario vía PostgREST con RLS;
  - **nunca** la service role.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Supabase como fuente de verdad y edición en la app | Edición en cualquier sitio | Sin revisión en git; el validador no corre; app más compleja | Las recetas se revisan con Claude Code |
| Sincronización bidireccional repo/Supabase | Edita en ambos lados | Conflictos y deriva, justo lo que se quiere evitar | Reintroduce el doble espejo |
| Publicar vía PostgREST con service role | Sin conexión directa a la BD | Guardar un secreto muy potente en la máquina | Riesgo innecesario |
| Publicar vía PostgREST con la sesión del usuario | Sin secretos extra | Exige políticas de escritura en `recetas`, abriendo la tabla a la app | Se quiere solo SELECT para la app |
| Repo como fuente + JDBC unidireccional | Una sola verdad; RLS mínima | Necesita la DB URL en la máquina de desarrollo | Elegido |

## Consecuencias
- Positivas: se acaba la deriva; cada receta es revisable en un diff; la superficie de la app sobre las recetas es de solo lectura.
- Negativas / costes: sin acceso al repo no se puede cambiar una receta; la DB URL es un secreto que proteger (hooks, denylist).
- Seguimiento: `sc comparar` sin diferencias como comprobación por fase; publicar solo con el gate en verde.
