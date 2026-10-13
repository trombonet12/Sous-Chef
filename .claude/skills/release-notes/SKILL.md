---
name: release-notes
description: Redactar notas de versión desde Conventional Commits (último tag..HEAD), por tipo y en lenguaje de usuario final; no valida (release-preflight) ni publica (release).
allowed-tools: Read, Write, Edit, Bash(git log *), Bash(git show *), Bash(git describe *), Bash(git tag --list *)
---

# release-notes

Genera las notas de la próxima versión desde `git log <último-tag>..HEAD`. Destino: sección nueva en `CHANGELOG.md` o cuerpo del GitHub Release (texto para pegar); por defecto `CHANGELOG.md`.

## Cuándo usarla
- Se prepara una versión y hay que resumir qué cambió para el usuario.
- Se quiere el cuerpo de un GitHub Release o la entrada de `CHANGELOG.md`.

## Cuándo NO usarla
- Comprobar secretos, migraciones o versión antes de publicar → usa `release-preflight`.
- Publicar la versión → usa `release`.
- Documentar una decisión de arquitectura → usa `create-adr`.

## Precondiciones
- Repositorio con historial accesible; las notas se basan en commits, no en el working tree.
- Último tag: `git describe --tags --abbrev=0`. Sin tags, el rango es todo el historial y se indica.
- Versión objetivo conocida (la fija `release-preflight`); si falta, encabezado `## [Sin publicar]`.

## Pasos
1. Rango: `git log <último-tag>..HEAD --no-merges --pretty="%h|%s|%b%x1e"`.
2. Clasificar cada commit por tipo Conventional Commits:

   | Tipo | Sección de las notas |
   |---|---|
   | `!` o `BREAKING CHANGE:` | Cambios importantes (primero, con lo que debe hacer el usuario) |
   | `feat` | Novedades |
   | `fix` | Correcciones |
   | `perf` | Rendimiento |
   | `security`, o `fix` de seguridad | Seguridad |
   | `refactor`, `docs`, `test`, `chore`, `ci`, `build`, `style` | Omitir (salvo que cambien algo visible) |

3. Reescribir para el usuario: qué puede hacer ahora o qué se arregló, en una línea, sin clases, tablas ni rutas. Fusionar commits del mismo cambio.
4. Filtrar datos sensibles: ni claves, URLs privadas, project refs ni detalles explotables. Ante la duda, omitir y avisar.
5. Commits sin formato Conventional Commits: no inventar el tipo; listarlos aparte en el informe.
6. Escribir la salida:
   - `CHANGELOG.md`: insertar `## [<versión>] - <AAAA-MM-DD>` sobre la versión anterior (crear con `# Changelog` si no existe).
   - Cuerpo del release: mismas secciones, sin encabezado de versión.
7. Resumen: commits leídos, incluidos, omitidos y sin formato.

## Validaciones / tests
- `git log <último-tag>..HEAD --no-merges --oneline` tiene tantas líneas como incluidos + omitidos + sin formato.
- `git diff -- CHANGELOG.md` sin valores parecidos a claves, tokens ni URLs internas.

## Definition of Done
- [ ] Rango correcto (último tag o historial completo declarado).
- [ ] Solo secciones con contenido; cambios importantes primero.
- [ ] Lenguaje de usuario, sin jerga interna ni datos sensibles.
- [ ] Commits sin formato reportados, no inventados.

## Docs a actualizar
- `CHANGELOG.md` · docs/ESTADO.md (skill `session-handoff`)
