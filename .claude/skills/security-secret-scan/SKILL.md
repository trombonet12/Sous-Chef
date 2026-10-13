---
name: security-secret-scan
description: Buscar secretos (claves, JWT, service_role, keystores, contraseñas) en árbol e historial sin mostrar valores; no es security-review (área) ni revisión de dependencias.
allowed-tools: Read, Grep, Glob, Bash(gitleaks *), Bash(git ls-files *), Bash(git check-ignore *), Bash(git log *)
---

# security-secret-scan

Detecta secretos expuestos y los informa como archivo, línea, tipo y acción. **Nunca imprime, copia ni resume el valor.** Solo lectura: no modifica ni purga nada.

## Cuándo usarla
- Antes de un commit relevante, de un release o tras añadir configuración, scripts o fixtures con credenciales posibles.
- Sospecha de filtración (clave en logs, en un commit, `local.properties` o un keystore rastreado).

## Cuándo NO usarla
- Revisión de RLS, permisos, componentes o alarmas → usa `security-review`.
- Licencias o actualizaciones de dependencias → usa `dependency-maintenance`.

## Precondiciones
- `gitleaks` disponible: `gitleaks version`. En Windows puede no estar en el PATH: buscar `gitleaks.exe` bajo `%LOCALAPPDATA%/Microsoft/WinGet/Packages` y llamarlo por ruta completa. Si no existe, avisar y usar solo los pasos 3-4 (no instalar).
- Los hooks de `.claude/hooks/` y los `deny` de `.claude/settings.json` impiden leer `.env*`, `*.pem`, `*.key`, `*.jks`, `*.keystore` y `~/.souschef/*`: no intentar saltárselos.

## Pasos
1. Working tree: `gitleaks detect --redact --no-git`. Opcional: `--report-format json --report-path <ruta fuera del repo>`; con `--redact` el informe no contiene valores.
2. Historial: `gitleaks git --redact` (en versiones antiguas, `gitleaks detect --redact`). Cubre commits ya borrados del árbol.
3. Higiene de archivos sensibles sin leer su contenido:
   - `git ls-files` filtrado por `.env`, `*.pem`, `*.key`, `*.jks`, `*.keystore`, `google-services.json`, `local.properties`, `release-signing.properties`, `supabase.env` → no debe haber ninguno rastreado (salvo ejemplos con valores vacíos).
   - `git check-ignore -v <ruta>` confirma que están en `.gitignore`.
4. Patrones propios con `Grep` en modo **`files_with_matches`** (jamás `content` ni `-o`, mostrarían el valor): `service_role`, `eyJ` (JWT), `BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY`, `SOUS_CHEF_PASSWORD\s*=`, `SUPABASE_DB_URL\s*=`, `storePassword`, `keyPassword`, `password\s*=`. Indicar solo el archivo ("línea no determinada") salvo que gitleaks dé la línea.
5. Regla del proyecto: los secretos viven solo en `~/.souschef/` o en secrets de GitHub; la app solo lleva la clave anon/publicable. `service_role` o `SUPABASE_DB_URL` en `app/`, `receta/`, `tools/` (código), docs o logs es hallazgo.
6. Descartar falsos positivos (placeholders, hashes, fixtures) justificándolos; no añadir `.gitleaksignore` sin confirmación del agente principal.
7. Emitir findings `SEVERIDAD | archivo:línea | tipo de secreto | acción`, con acción en {rotar, purgar historial, mover a ~/.souschef, añadir a .gitignore}. Para un secreto real: **rotar primero**, luego purgar; la purga reescribe historial y exige decisión explícita del usuario. Nunca incluir el valor ni su prefijo en informe, chat ni commits.
8. Hallazgo confirmado: recomendar análisis de impacto por un revisor de seguridad [opus].

## Validaciones / tests
- `gitleaks detect --redact --no-git` y `gitleaks git --redact` sin fugas nuevas (exit 0) o cada hallazgo clasificado.
- Salida revisada: ningún valor, token ni fragmento de secreto en el informe.

## Definition of Done
- [ ] Working tree e historial escaneados (o indicado por qué no).
- [ ] Todos los hallazgos con severidad, `archivo:línea`, tipo y acción; cero valores expuestos.
- [ ] `.gitignore` cubre claves, keystores, `local.properties` y `.env*`.
- [ ] Secretos reales: rotación recomendada antes que purga.

## Docs a actualizar
- `docs/herramientas.md` o el doc de secretos si cambia el procedimiento · docs/ESTADO.md (skill `session-handoff`)
