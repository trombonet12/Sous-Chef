# ADR-0001: Adoptar el stack de Trombito con tres módulos (:app, :receta, :tools)

- Estado: aceptado
- Fecha: 2026-10-13
- Relacionado: `docs/decisiones.md` § Stack, [arquitectura](../arquitectura.md), [ADR-0003](0003-el-repo-manda-en-las-recetas.md), [herramientas](../herramientas.md)

## Contexto
- Sous Chef es una app Android personal que sustituye a un artifact HTML. Comparte proyecto Supabase, cuenta y convenciones con Trombito (mismo autor, mismo dispositivo).
- El validador de recetas lo necesitan la CLI (`sc validar --estricto`, antes de publicar) y la app (modelos, escalado, gantt y motor del cronómetro). Duplicarlo en dos lenguajes reproduciría la deriva del doble espejo del legado.
- El motor del cronómetro es lógica crítica (TDD): debe poder probarse en JVM puro, sin emulador y con un `Clock` inyectado.

## Decisión
- Stack: Kotlin 2.4.20, Compose/Material 3 (BOM 2026.06.01), Hilt 2.60.1, Room 2.8.5, KSP 2.3.12, WorkManager, supabase-kt 3.8.0 y ktor 3.5.2; AGP 9.4.1 (Kotlin integrado) y Gradle 9.7.1. Las versiones se copian del `libs.versions.toml` de Trombito. minSdk 26, target/compile 36.
- Tres módulos:
  - `:app`: Android, capas `domain/ data/ ui/`, regla de capas vigilada por `DomainDependencyRuleTest`.
  - `:receta`: Kotlin/JVM puro (modelos kotlinx-serialization, escalado, formato, gantt, validador, motor del crono). Sin disco ni variables de entorno.
  - `:tools`: CLI JVM (`scripts/sc`), depende de `:receta`.
- `:receta` y `:tools` compilan con toolchain JDK 21 y bytecode JVM 17 (mismo criterio que el ADR-0008 de Trombito); `kotlin-jvm` y `serialization` se declaran `apply false` en la raíz.
- Validador y motor del crono viven en el mismo módulo compartido para que la CLI y la app ejecuten exactamente las mismas reglas.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Un solo módulo `:app` | Menos configuración | La CLI no podría reutilizar el validador; tests del motor atados a Android | Impide compartir validador y motor |
| Validador en Node/Python y motor en Kotlin | Scripts rápidos de escribir | Dos implementaciones de las mismas reglas; deriva | Repite el problema del doble espejo |
| Kotlin Multiplatform | Un módulo para todo | Complejidad y toolchain extra para un uso solo JVM/Android | Sin beneficio sobre un módulo JVM puro |
| Módulos separados para validador y motor | Aislamiento fino | Más módulos y wiring sin ganancia a este tamaño | Un `:receta` basta; se divide si crece |
| `:receta` JVM puro compartido | Una fuente de verdad; tests rápidos | Sin APIs Android; hay que inyectar reloj y E/S | Elegido |

## Consecuencias
- Positivas: una sola implementación de las reglas; tests de dominio en segundos; versiones coherentes con Trombito.
- Negativas / costes: hay que disponer de JDK 21; `jvmTarget` 17 renuncia a bytecode más moderno; `:receta` no puede usar APIs Android.
- Seguimiento: revisar al subir AGP/Kotlin; si `:receta` crece mucho, valorar separar el motor del crono.
