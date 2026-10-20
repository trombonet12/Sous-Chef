# Arquitectura

> Propósito: describir la arquitectura de Sous Chef, sus componentes y fronteras · Audiencia: desarrolladores y agentes · Actualizado: 2026-10-20 · Relacionado: docs/adr/, docs/herramientas.md, docs/spec/app-legado.md

## Resumen
Sous Chef es una app Android personal de recetas cronometradas que funciona solo en local ([ADR-0007](adr/0007-app-solo-local-sin-supabase.md)). Las recetas se escriben y validan en este repo con Claude Code, y viajan empaquetadas en el APK desde `data/`. La app lee recetas y técnicas, guarda historial, preferencias y ajustes en Room, y ejecuta el cronómetro con alarmas del sistema. No hay backend, sync ni login, y la app no usa la red: no pide permisos de Internet ([ADR-0011](adr/0011-sin-aviso-de-version-nueva.md)). **Estado:** descripción de la arquitectura actual; se implementa por fases (`docs/fases/`).

## Principios
| Principio | Consecuencia arquitectónica |
|---|---|
| El repo manda en las recetas | `data/*.json` es la fuente de verdad. El APK empaqueta `data/` y se re-siembra por hash al arrancar con datos nuevos ([ADR-0007](adr/0007-app-solo-local-sin-supabase.md)); la app nunca edita recetas ([ADR-0003](adr/0003-el-repo-manda-en-las-recetas.md)). |
| Una sola implementación de las reglas | Validador y motor del crono en `:receta`, compartidos por la app y la CLI ([ADR-0001](adr/0001-stack-android-y-modulos.md)). |
| Solo local: Room es la única persistencia | Room es la fuente de la UI y guarda todo lo que escribe el usuario (historial, preferencias, ajustes, estado del crono). Sin sync ni outbox. |
| Avisos fiables sin servicio permanente | Cadena de una alarma `setAlarmClock` con estado en Room ([ADR-0005](adr/0005-cronometro-alarmas-sin-fgs.md)). |
| Secretos fuera del repo | Firma de release en `~/.souschef/` y `local.properties`; nunca en el repo. |

## Diagrama de contexto
```mermaid
flowchart LR
  subgraph Repo["Repo + Claude Code"]
    Data[data/*.json] --> CLI["CLI sc: validar, formatear"]
  end
  Data -->|release del APK| Rel[Releases de GitHub]
  Rel -->|Obtainium| APK
  subgraph Movil["Móvil Android (solo local)"]
    APK[APK con recetas y técnicas] -->|siembra al cambiar el hash| Room[(Room)]
    UI[UI Compose] --> Room
    Crono[Motor del crono + AlarmManager] --> Room
  end
```

## Flujos
### Publicar una receta
```mermaid
sequenceDiagram
  actor J as Joan
  participant C as Claude Code
  participant R as Repo (data/recetas)
  participant S as sc
  participant G as Releases de GitHub
  participant M as Móvil
  J->>C: pide una receta
  C->>R: escribe data/recetas/slug.json
  C->>S: sc validar --estricto
  S-->>C: informe
  J->>G: release nueva del APK (push de version.properties a main, workflow android.yml)
  G->>M: Obtainium instala el APK
  Note over M: al arrancar con un hash de datos nuevo, resiembra recetas y técnicas
```

### Historial y preferencias
1. La UI escribe directamente en Room. No hay red, cola ni sincronización.
2. Si se pierde el móvil sin copia de seguridad, se pierde el historial. Las copias salen por el backup de Android ([ADR-0007](adr/0007-app-solo-local-sin-supabase.md)).

### Cocinar con alarmas
1. "Abrir la cocina" pide `POST_NOTIFICATIONS` y crea el estado del crono en Room (`finAt` en reloj de pared).
2. El motor calcula el próximo evento y se programa **una** alarma `setAlarmClock`.
3. Al saltar, el receiver recarga Room, emite lo vencido (hitos con más de 90 s de retraso se marcan sin sonar) y programa la siguiente.
4. Si el proceso muere o el móvil reinicia, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET` o `TIMEZONE_CHANGED` reprograman la cadena ([ADR-0005](adr/0005-cronometro-alarmas-sin-fgs.md)).

### Cocinado a historial
1. Al terminar, el motor produce el resumen con `pasosReales`; la persona añade una nota.
2. Se escribe una fila de `historial` en Room con id `slug-fecha-HHMMSS`.
3. Se muestra "Deshacer" durante 8 s. El estado del crono se borra (solo local).

## Límites de componentes
| Módulo | Paquete | Responsabilidad | No puede |
|---|---|---|---|
| `:app` | `domain/` | Casos de uso, interfaces de repositorio | Depender de `data/` ni `ui/` ni Android (`DomainDependencyRuleTest`) |
| `:app` | `data/` | Room (entidades, DAOs, siembra de recetas desde el APK), sesión cifrada | Contener lógica de UI |
| `:app` | `ui/` | Pantallas Compose, navegación, tema | Acceder a Room o red directamente |
| `:app` | alarmas y notificaciones | `AlarmManager`, receivers, canales | Mantener estado fuera de Room |
| `:receta` | modelos | Receta, técnica, despensa (kotlinx-serialization, campos opcionales tolerantes) | Acceder a disco o entorno |
| `:receta` | escalado y formato | `esc:"sub"` (f^0,75), números ("2,5") | Depender de Android |
| `:receta` | gantt | Plan y huella de fuegos | Depender de Android |
| `:receta` | validador | 12 comprobaciones, completitud, plurales, `--estricto`, `mep` solo `<b>` | Escribir ficheros |
| `:receta` | motor del crono | Máquina de estados con `Clock` inyectado | Usar el reloj real |
| `:tools` | CLI `sc` | validar, formatear | Contener reglas de dominio (viven en `:receta`) |

## Estructura del repo
```
app/  receta/  tools/  gradle/  settings.gradle.kts  build.gradle.kts
data/recetas/*.json  data/tecnicas/*.json  data/legado/{historial,preferencias}.json
docs/ ESTADO.md decisiones.md arquitectura.md pasos-manuales.md testing.md ci-cd.md
      adr/ fases/ _templates/ cocina/{reglas.md,perfil.md,esquema-receta.md,entregables.md,sugerencias-compra.md}
      spec/app-legado.md (inventario funcional completo de la app vieja)  legado/sous-chef-publicada.html
.claude/ settings.json hooks/ skills/ agents/      scripts/ sc.ps1 sc.cmd check-claude-assets.mjs test-hooks.sh
.github/ workflows/{android.yml,claude-assets.yml} dependabot.yml pull_request_template.md
CLAUDE.md README.md .editorconfig .gitattributes .gitignore .env.example
```

## Decisiones y fuentes
- [ADR-0001 Stack y módulos](adr/0001-stack-android-y-modulos.md)
- [ADR-0002 Supabase compartido, esquema `sous_chef`](adr/0002-supabase-compartido-esquema-sous-chef.md) (sustituido por ADR-0007)
- [ADR-0003 El repo manda en las recetas](adr/0003-el-repo-manda-en-las-recetas.md) (revisado por ADR-0007)
- [ADR-0004 Sync outbox y LWW](adr/0004-sync-outbox-lww.md) (sustituido por ADR-0007)
- [ADR-0005 Cronómetro con alarmas, sin FGS](adr/0005-cronometro-alarmas-sin-fgs.md)
- [ADR-0006 Ficha nativa e impresión HTML](adr/0006-ficha-nativa-e-impresion-html.md)
- [ADR-0007 Solo local, sin Supabase](adr/0007-app-solo-local-sin-supabase.md) (matizado por ADR-0009 y retirado por ADR-0011)
- [ADR-0008 Voz en el cronómetro](adr/0008-voz-en-el-cronometro.md)
- [ADR-0009 Aviso de versión nueva](adr/0009-aviso-de-version-nueva.md) (sustituido por ADR-0011)
- [ADR-0010 Distribución por Google Play](adr/0010-distribucion-por-google-play.md)
- [ADR-0011 Sin aviso de versión nueva](adr/0011-sin-aviso-de-version-nueva.md)
- [ADR-0012 Vídeo promocional con Remotion](adr/0012-video-promocional-con-remotion.md)
- CLI y herramientas: [herramientas](herramientas.md). Inventario funcional del legado: [spec](spec/app-legado.md).

## Pendiente
- Spike de alarmas en emulador (F1) y repetición en el móvil real en la sesión final.
