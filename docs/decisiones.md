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
- 2026-10-13 · tema · `primary` claro pasa de #B07408 a #8C5A04: el original daba 3,35:1 sobre papel (ContrastTest). El ámbar vivo queda para acentos grandes.
- 2026-10-13 · alarmas · Spike en el emulador (API 36, pantalla apagada, reposo INACTIVE): cadena `setAlarmClock` a +1/+2/+3 min llegó con 5-11 ms de retraso. El emulador no entra en Doze profundo: se repite en el móvil en la sesión final.
- 2026-10-13 · hooks · `block-secret-commands.sh` solo considera sensibles los directorios `/.souschef` y `/.trombito` (antes cualquier `.trombito`, que bloqueaba el paquete `app.trombito` al portar código).
- 2026-10-13 · build · `debug` apunta al Supabase remoto (local.properties) y `localDebug` al stack local (supabase-local.properties). Release firmado con keystore propia en `~/.souschef/` (`scripts/crear-keystore.py`).
- 2026-10-13 · CLI · `sc despensa` y el resto de herramientas usan JDBC con `SUPABASE_DB_URL` y resuelven el `user_id` por `SOUS_CHEF_EMAIL` (una sola vía de acceso; la contraseña solo la usa el login de la app en el emulador).

## 2026-10-14 · Ronda 2 tras probar en el móvil
- Letra del sistema (`FontFamily.Default`) en toda la app; se retiran Bricolage/Newsreader (petición de Joan). ADR-0006
  y la spec legada siguen citándolas como historia.
- Alarmas en hardware real (Android 16, LineageOS): `setAlarmClock` dispara a su hora con pantalla apagada.
- Notificación "En marcha" sin FGS: ongoing LOW con `android.requestPromotedOngoing` (extra a mano: la API 36.0 no
  expone el setter), cuenta atrás nativa y `shortCriticalText`; "Siguiente" = Hecho del paso más urgente + arrancar
  el siguiente del carril.
- Avisos: tag = slug, id = carril+tipo, `setTimeoutAfter` 2/10 min, se cancelan al avanzar el carril o cerrar.

## 2026-10-14 · Ronda 3
- Avisos: el sonido lo pone la app (`SonidoAlarma`, MediaPlayer USAGE_ALARM en bucle, tope 60 s), no el canal; canal
  `crono_avisos_v2` solo vibra (los ajustes de canal son inmutables, se borra el viejo). El full-screen intent abre
  `AlarmaActivity` con `showWhenLocked`/`turnScreenOn` en el manifest: activarlos desde Compose llegaba tarde.
- Una sola cocinada viva a la vez (decisión de Joan); "En marcha" solo para estados vivos; deslizarla abandona.
- CI `android.yml` y `claude-assets.yml` solo en PR o `workflow_dispatch`; el gate se pasa en local y se hace push
  una vez por ronda. Release: secretos de Supabase opcionales hasta F3.

## 2026-10-14 · Ronda 4
- `gh` sin `gh auth login`: `scripts/gh.mjs` le pasa el token de Git Credential Manager por `GH_TOKEN` (solo en
  memoria). `scripts/subir-secretos-release.mjs` sube los 4 secretos de firma por stdin sin imprimir valores.
- CI vuelve a correr en push a `main` (con filtro de rutas). `main` protegida: sin force-push ni borrado, historial
  lineal, check `Gate Android, dominio y recetas` obligatorio en PR; `enforce_admins` desactivado para el push directo.
- Validador: reglas 8, 12-15, 19, 20 y 24 automáticas; 9 y 22 se quedan fuera (no detectables con fiabilidad).
  R8 casa por palabra completa (núcleo no genérico: «salsa», «semillas», «hoja»… no cuentan). R12 contra
  `data/despensa-semilla.md`, con «agua» siempre permitida. R20 busca la potencia en `t`, `nota` y `p`; potencia 0
  vale con «apaga/retira/fuera del fuego/reposo». R19 acepta «por la mitad».
- Imprimir/PDF: HTML A4 generado en la app + WebView fuera de pantalla + `PrintManager`; el PDF lo da el diálogo del
  sistema (sin dependencias nuevas).
- Revisión culinaria opus de las 13 recetas aplazada por presupuesto; esta ronda solo corrige lo que marca el validador.
