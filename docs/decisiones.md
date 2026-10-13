# Decisiones cerradas

> Propósito: fuente única de decisiones y versiones fijadas. · Audiencia: quien implemente. · Actualizado: 2026-10-13
> Las secciones fechadas posteriores mandan sobre las anteriores. Las decisiones de arquitectura llevan ADR.

## Producto (usuario, 2026-10-13)
| Tema | Decisión |
|---|---|
| Plataforma | App Android nativa; reemplaza el artifact HTML de claude.ai (`docs/legado/`) con paridad funcional (`docs/spec/app-legado.md`) |
| Backend | Supabase, mismo proyecto que Trombito, esquema `sous_chef` ([ADR-0002](adr/0002-supabase-compartido-esquema-sous-chef.md)) |
| Recetas | El repo manda: `data/recetas`, `data/tecnicas` → `sc validar --estricto` → `sc publicar` ([ADR-0003](adr/0003-el-repo-manda-en-las-recetas.md)) |
| Despensa | Editable en la app (hay/poco/falta/vetado) + lista de la compra; vive en Supabase; Claude la lee con `sc despensa` |
| Auth | Email + contraseña de Supabase (la misma cuenta que Trombito) |
| Distribución | APK firmado en GitHub Releases (`release-android.yml`, workflow_dispatch) |
| Cocina | Vitrocerámica 0-9, máx. 3 fuegos; 2 raciones; hora de servir por defecto **15:30** |
| Dispositivo | minSdk 26, compile/target 36 (como Trombito) |
| Icono | Mascota de Trombito con gorro de chef; adaptive icon vectorial + monocromo |
| Navegación | Abajo: Recetas · Despensa (con Compra dentro) · Historial. Menú superior: Técnicas, Ajustes, Cerrar sesión |
| UI | Libertad para mejorar la forma respecto a la app vieja, sin perder funciones |
| Revisión de recetas | Se aplican sin preguntar las correcciones de reglas escritas; los cambios de plato se proponen en `docs/revision-recetas.md` |
| Ejecución | Desatendida hasta la sesión final con el móvil (`docs/sesion-final.md`); push a `origin main` al cerrar cada fase con el gate en verde |
| Cola de platos | **No** (el usuario la quitó a propósito) |

## Versiones fijadas (copiadas de Trombito, verificadas allí en 2026-10)
| Pieza | Versión |
|---|---|
| Gradle | 9.7.1 (daemon y toolchain JDK 21; bytecode JVM 17) |
| AGP | 9.4.1 (Kotlin integrado; sin plugin `kotlin-android`) |
| Kotlin | 2.4.20 · KSP 2.3.12 |
| Compose BOM | 2026.06.01 |
| Room | 2.8.5 · Hilt 2.60.1 (+ hilt-navigation-compose / hilt-work 1.3.0) |
| WorkManager | 2.12.0 · Navigation Compose 2.9.8 · Lifecycle 2.10.0 · Activity Compose 1.11.0 · core-splashscreen 1.2.0 |
| DataStore | 1.2.1 |
| kotlinx-serialization | 1.11.0 · coroutines 1.11.0 |
| supabase-kt | BOM 3.8.0 · ktor 3.5.2 (OkHttp) · Tink 1.23.0 |
| ktlint | plugin 14.2.0, motor 1.8.0 · detekt 1.23.8 |
| Tests | JUnit 4.13.2, Turbine 1.2.1, MockK 1.14.11, Robolectric 4.17 |

## Decisiones tomadas durante la ejecución
<!-- Formato: - AAAA-MM-DD · tema · decisión · porqué -->
