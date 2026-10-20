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

## 2026-10-16 · Ronda 5
- Sonido del aviso: lo reproduce el sistema desde el canal `crono_alarma_v3` (HIGH, tono de alarma, `USAGE_ALARM`) con
  `FLAG_INSISTENT` y tope de 60 s. El `MediaPlayer` en el receptor moría con el proceso (sin FGS) y no sonaba en el móvil.
- Una notificación por carril (`crono_carriles`, DEFAULT sin sonido, grupo `carriles:<slug>`), con paso i/n, cuenta atrás y
  `ProgressStyle` por pasos. Al acabar un paso se repinta el mismo id en el canal de alarma; Parar/Siguiente lo devuelven.
- Se elimina la "En marcha" (duplicaba los carriles). El chip de Live Update lo lleva el carril con el `finAt` más próximo.
  Deslizar una notificación ya no abandona la cocinada (solo el menú ⋮).
- Android 16 deniega por defecto `USE_FULL_SCREEN_INTENT`: el cronómetro avisa y enlaza al ajuste.
- Recetas de pescado: `tags.base = "pescado"`.
- Trombito chef: mascota Compose dentro del cronómetro (no overlay del sistema). Consejos de `Consejero` (receta/crono),
  solo con datos existentes: "en nada toca" (<60 s), siguiente evento de otro carril (<5 min), "ve adelantando" (mep sin
  marcar). No repite la nota del paso, que ya está en pantalla. Bocadillo de 3 líneas como máximo.

## 2026-10-17 · Ronda 6 (revisión de backlog con Joan)
- Supabase, sync, login y `sc publicar/comparar/despensa/migrar` (F3/F4): **descartados**. App solo local
  ([ADR-0007](adr/0007-app-solo-local-sin-supabase.md), que sustituye a ADR-0002 y ADR-0004). Fuera dependencias,
  BuildConfig, `localDebug`, skills `supabase-*` y el agente `database-engineer`. Los hooks siguen bloqueando comandos de
  Supabase porque protegen los secretos de Trombito en la misma máquina.
- Roadmap legado (`docs/legado/auditoria-y-roadmap.md`, puntos 1-7: PWA, pestañas, espejo): cerrado por obsoleto.
- F9b lotes 2 y 3: se hacen en esta ronda junto con la corrección de huevos rotos.
- Release: solo en push a main, con la versión en `version.properties`. Si la release `v<versionName>` ya existe no se
  publica nada.
- Despensa: desaparece `compra` por receta y todo ingrediente tiene que estar en la despensa. La ficha muestra «Te falta»
  según el estado de la despensa en la app.
- Cronómetro: sin Trombito y sin scroll, con dos disposiciones elegibles en Ajustes (carriles apilados / foco + compactos).
- Notificaciones: ninguna con la cocina en pantalla. Fuera, una sola fija con todos los carriles y una de alarma que
  agrupa lo que coincide. Sin aviso de 10 s. Pantalla completa solo con el móvil bloqueado.
- Voz: palabra clave «Chef», escucha solo con el cronómetro en primer plano y responde con TTS (ADR-0008).
- Implementación (2026-10-17):
  - `PresenciaCrono` es una clase simple dentro de `CadenaAlarmas` (@Singleton), no inyectable, porque `domain/` no
    admite `javax.inject`.
  - La fija usa BigTextStyle, no InboxStyle, para que Android 16 la promocione a Live Update.
  - El tic del ViewModel solo consume avisos con la cocina a la vista. Fuera de la pantalla los consume la cadena.
  - «Bloqueado» para la pantalla completa también incluye la pantalla apagada (`!isInteractive`).
  - Tras la muerte del proceso, la alarma agrupada no se reconstruye: la siguiente sustituye a la de la bandeja.
    Se acepta.
  - Causa raíz del «+30 s lento»: `masTreinta` pasaba por el cerrojo de acciones, que descartaba pulsaciones seguidas.
    Ahora va por `mutar`.

## 2026-10-17 · Ronda 7 (tras probar la v0.6.0)
- Disposición por defecto: carriles apilados (APILADA). Si se guardó FOCO en Ajustes, se respeta.
- Voz, gramática ampliada (`InterpreteVoz`):
  - prórroga con cualquier cantidad: «2 minutos más», «medio minuto», «un minuto y medio», o la cantidad sola. Tope de
    30 min. `MotorCrono.prorrogar`, y `masTreinta` delega en él;
  - «por qué» lee el porqué del paso;
  - «info» o «dame info» dice título, nota, fuego, tapa y cuánto queda;
  - «qué tengo que hacer» y sus variantes;
  - más sinónimos de hecho, seguir y pausa;
  - activadores chef, jef, shef, jefe, chefs y sheff.
- Carriles numerados desde 1, en el orden de la receta, con una insignia en pantalla. Por voz se nombran por número
  («del 2», «al 1», «el 3») o por nombre. «para + carril» pausa el carril; «para» solo silencia.
  - Si el número del carril no existe, la orden va al carril en foco.
  - «N más» sin unidad solo cuenta como segundos si N ≥ 10.
- Salir de una cocina en marcha (gesto atrás o flecha) pide confirmación y la abandona: no se quedan cocinas vivas por
  detrás. Salir al escritorio o apagar la pantalla no la abandona, porque la alarma tiene que seguir sonando.
- Receta nueva `pollo-almendras` en vez de añadir almendras al salteado: es otro plato, con dados, maicena, salsa
  espesada y arroz.
  - Joan cerró el diseño: zanahoria y calabacín; salsa de soja, caldo y maicena sin dulce; arroz en la olla; marinada
    larga de 1 h; almendra cruda pelada entera tostada en seco durante la nevera.
  - Opus fijó los detalles técnicos. La despensa añade «Frutos secos · almendras crudas peladas».

## 2026-10-17 · Ronda 8 (tras probar la v0.7.0)
- Arreglado el cierre al abrir `pollo-almendras`. `Gantt.barra` ensanchaba la barra al 1 % mínimo, pero el último
  paso (1 min, que acaba justo en el 136) no tenía sitio y `coerceIn` recibía un rango vacío. Ahora una barra
  ensanchada que no cabe se corre a la izquierda.
  - `DatosRealesTest` comprueba las barras de todas las recetas reales.
- Tarjeta compacta, elegida por Joan. Cuando la tarjeta no da para la parte fija, una línea de título y el reloj
  mínimo, se pinta en dos filas:
  - arriba, número, nombre, paso, fuego y la «i»;
  - abajo, el reloj con su barra, Pausa/+30 y el botón principal.
  - Ocurre con 4 carriles o con la letra del sistema grande: `tarjetaCompacta` escala el umbral con `fontScale`.
  - Se quitan «Después», METO/SACO y la nota, y el chip «destapada» porque es lo normal.
  - Un carril bloqueado muestra «Empezar igual» en lugar del botón deshabilitado.
- Tras probar la v0.7.1, Joan pide más información en la tarjeta compacta. Debajo del reloj se añaden, por prioridad y
  según el alto que quede (`lineasExtraCompacta`, escalado con la letra):
  - el próximo hito;
  - «Después: …»;
  - la nota del paso (una línea cada uno).
- Repo de GitHub hecho público, por decisión de Joan, para que la app pueda consultar las releases sin token.
  - Antes se revisó el historial entero: sin secretos.
  - Quedan visibles el Gmail de los commits y `data/legado/`; Joan lo aceptó.

## 2026-10-17 · Aviso de versión nueva (v0.8.0, ADR-0009)
- La app consulta `releases/latest` de la API pública de GitHub, sin token ni datos del usuario:
  - al abrir, como mucho cada 6 h;
  - una vez al día con `JobScheduler` (persistente y con red).
- Si el tag es mayor que la versión instalada y no se avisó ya, notifica en el canal «Actualizaciones». Al tocar abre
  Obtainium (`dev.imranr.obtainium` o `.fdroid`) o, si no está, la página de la release.
- Permisos nuevos: `INTERNET` y `ACCESS_NETWORK_STATE`. La build debug no comprueba.
- Probado en el emulador con una build temporal de 0.7.0 sin el filtro de debug: aparece «Sous Chef 0.7.2
  disponible», el trabajo queda programado y la versión avisada se guarda.
- v0.8.1, a petición de Joan:
  - El texto que no cabe en una línea (título, hito, después, nota) se desplaza despacio, en bucle
    (`desplazarSiNoCabe`, `basicMarquee` a 24 dp/s y 2 s de pausa).
  - «Después» muestra la duración del siguiente paso (`VistaCarril.despuesS`), solo en pantalla: la voz no la dice.

## 2026-10-18 · Ronda 9 → v1.0.0
- Fuera la despensa de la app, a petición de Joan: pestaña, lista de la compra, «Te falta» en la ficha y «Compartir
  despensa». La despensa queda como `data/despensa-semilla.md`, interna, para diseñar recetas y para la regla 12 del
  validador; ya no se empaqueta en el APK.
  - Room 1 → 2 (`MIGRATION_1_2`): borra `despensa_item`, `compra_item` y la meta `despensa_hash`; conserva historial,
    preferencias, ajustes y estado del crono. Test con el SQL del schema 1 en lugar de `MigrationTestHelper`, porque
    Robolectric solo ve los assets de `debug` y los schemas no deben ir al APK.
  - Técnicas pasa a ser la pestaña del medio (icono libreta); el menú ⋮ deja solo Ajustes.
- Ficha: «Empieza a las HH:mm» = servir − total redondeado hacia arriba al minuto. Ingredientes «Todo junto» (por
  defecto; suma los repetidos por nombre normalizado, unidad y modo de escala) o «Por zona»; la impresa lleva los dos.
- Cronómetro: cada tarjeta muestra la hora prevista del paso (`sugeridoMs`) y el desvío en minutos frente al plan
  (fin previsto − (sugerido + duración)): esperar antes de la hora no cuenta como adelanto; pausas y prórrogas suman.
- Aros de cebolla pasan de técnica a receta (47 min). Revisión culinaria (opus): 250 g en vez de 400 (solo aros de
  fuera, 4 tandas de 6), 10 min de nevera, sartén de 22-24 cm y 30 s entre tandas.
- Google Play (ADR-0010):
  - El job `release` añade `bundleRelease` y adjunta el AAB a la release de GitHub.
  - Un job `play` aparte, con `permissions: {}`, sin keystore ni checkout, recibe AAB y mapping como artefacto y los
    sube a la pista interna con `r0adkll/upload-google-play` v1.1.5 (por SHA). `download-artifact` va en v8.0.1
    porque la v8.0.2 tiene menos de dos semanas.
  - Solo sube si existe `PLAY_SERVICE_ACCOUNT_JSON`. `PLAY_ESTADO` (draft | completed) se valida antes.
  - Instalada desde Play (`com.android.vending`), la app no avisa de versiones de GitHub.
  - Política de privacidad en `docs/privacidad.md` y pasos manuales en `docs/play.md`.
- Disposición CLÁSICA (`DisposicionCrono.CLASICA`): el cronómetro anterior a la v0.6 (borrado en `903ad72`)
  recuperado en `ui/crono/clasica/`, con plan plegable, pestañas, un panel por recipiente con scroll y mandos abajo.
  - Sus mandos usan `AccionesCarril`, como las otras disposiciones, en vez del ViewModel; así se prueba en JVM.
  - La cabecera del panel añade el desvío del lote de hora prevista. Sus textos van en `strings_clasica.xml`.
  - Se elige en Ajustes o desde el ⋮ de la cocina («Interfaz»), con el mismo `SelectorDisposicion`: tres tarjetas
    con una miniatura dibujada (esquema en la paleta del crono, no captura), en fila si caben y en lista si no.
    Cambiarla en plena cocina no la reinicia.
- Voz, «info del dos» respondía de la MESA (foco). La numeración de pantalla y de voz es la misma
  (`indiceCarril + 1`): fallaba que el número no se reconocía como referencia y la orden caía al foco. Ahora valen
  «de», «en» y un número suelto al final («info 2»); «de los» al final se lee como «de dos»; y entre las alternativas
  del reconocedor gana la primera que nombra carril. Orden nueva `ProximosPasos(carril?)`: sin carril, qué toca y qué
  sigue en cada carril vivo por hora prevista; con carril, sus hasta 3 pasos siguientes con duración
  (`VistaCarril.proximos`).
- Deshacer un Hecho (toque sin querer): `MotorCrono.deshacer` restaura el `EstadoCarril` de antes del último Hecho de
  ese carril (reloj, pausa e hitos avisados incluidos) y borra su tiempo real. `EstadoCrono.deshacer` (un nivel por
  carril) y `ultimoHecho` son campos opcionales, así que los estados guardados siguen leyéndose. Las alarmas se
  rearman solas porque pasa por `mutar`. Minibotón ↶ de 40 dp solo cuando hay algo que deshacer; por voz, «deshaz»
  sin carril deshace el último Hecho de la cocina.
- Cambia la regla de la ronda 7 («salir de la cocina pide abandonarla»), a petición de Joan: atrás y la flecha del
  cronómetro vuelven a la ficha sin abandonar (la cocinada sigue en segundo plano, como al bloquear el móvil). La
  ficha lo sabe por `CadenaAlarmas.viva(slug)` (mismo criterio `vivo` que «una sola cocinada viva»): banda
  «Cocinando», botón «Volver a la cocina» (el crono reanuda sin pisar el estado) y, al salir de la ficha, diálogo
  «¿Abandonar esta cocinada?». Abrir una técnica desde la ficha no abandona. Si se entró por la notificación y no hay
  ficha debajo, atrás abre la ficha en lugar del crono.
- Historial fuera de la app, por decisión de Joan: exportar/importar a mano (no carpeta automática ni copia de Google,
  que está desactivada con `allowBackup=false` y en LineageOS sin servicios de Google puede no existir). Ajustes →
  «Guardar copia» crea `sous-chef-historial-AAAA-MM-DD.json` donde elija con el selector del sistema (SAF, sin
  permisos nuevos); «Cargar copia» añade las cocinadas que falten por id, suma favoritos y solo pone raciones donde no
  las hay: nunca pisa ni borra. Formato `sous-chef-historial` versión 1 (`CopiaHistorial`, puro y con tests). Borrar
  la caché no borra el historial; lo borran «borrar datos» y desinstalar.

## 2026-10-19 · Ronda 10 → v1.1.0 (lista de recetas y lasaña)
- Lista de recetas rediseñada a petición de Joan, que encontraba la anterior saturada. Ahora tiene tres diseños y se
  elige en Ajustes → «Lista de recetas» (`DisposicionLista`):
  - **TINTE** (por defecto): tarjeta teñida con el color de la base y el tiempo grande a la izquierda.
  - **FRANJA**: tarjeta clara con una franja de color.
  - **CLÁSICA**: la de antes, con el mini-gantt.

  Los tres llevan una sola línea de descripción, «empieza HH:mm» para servir a las 15:30
  (`Servir.inicio(Servir.DEFECTO, r)`), base y proteína, fuegos y horno, y «hace…».
- Color por **familia de base**, no por base: siete familias apagadas más «Otras»:
  - Pasta;
  - Arroz;
  - Patata;
  - Pescado;
  - Verdura (verdura y ensalada);
  - Masas (masa de pizza, hojaldre y taco);
  - Salteado.

  Están en `ui/recetas/ColoresBase.kt`, con variante clara y oscura. Una base nueva sin familia cae en «Otras» hasta
  añadirla a `FamiliaBase`. El color nunca va solo: la base siempre aparece escrita.
- **Filtros** en una hoja inferior (botón junto al buscador). Secciones:
  - Mostrar: favoritas, sin estrenar, hace +2 meses;
  - Tiempo: < 45, 45-90 y > 90 min;
  - Base;
  - Proteína: pollo, carne, cerdo, pescado, huevo y vegetariana, sacadas de `tags.proteina` por palabras;
  - Equipo: sin horno, con horno, 1 fuego.

  Cómo se combinan y se cuentan:
  - dentro de una sección se suman (O) y entre secciones se restringen (Y);
  - cada chip cuenta las recetas que quedarían con el resto de filtros puestos;
  - solo salen las opciones que existen entre las recetas del buscador.

  Los filtros activos se ven como chips con ✕ bajo el buscador. Sustituyen a la fila de chips de base.
- El **orden** se elige en la misma hoja y se guarda en Ajustes (`ajustes.recetas.orden`):
  - favoritas y recientes (por defecto);
  - hace más que no la hago (sin estrenar primero);
  - alfabético;
  - más rápida.

  Los **filtros no se guardan**: viven en el ViewModel y se limpian al cerrar la app, para que la lista no aparezca
  filtrada sin que Joan lo recuerde. Diseño y orden van en la tabla clave-valor `meta`, así que no hace falta migración.
- Receta `lasana-carne-berenjena` (4 raciones, fuente de 28×20, 136 min). Las placas precocidas El Pavo se remojan
  10 min en agua caliente, sin hervir, como dice el envase. El remojo acaba justo al montar, y entonces se sacan todas
  a un paño para que no sigan bebiendo agua. La bechamel es fluida (60 g/l), porque la placa termina de hidratarse en
  el horno, y se hace en la sartén de la berenjena. El reposo es de 15 min. Técnica nueva `bechamel`.

## 2026-10-19 · Ronda 11 → v1.2.0 (Ajustes, tema y hora de servir)
- **Hora de servir por defecto** en Ajustes → Horario (`ajustes.servir`, «HH:mm»; por defecto 15:30). Es el punto de
  partida:
  - la ficha arranca con ella, y su ± cambia solo esa vez, sin tocar el ajuste;
  - el cronómetro la usa cuando la ruta no trae hora (p. ej. al abrirlo desde la notificación). Una cocinada
    guardada conserva su hora.
- **Hora de las tarjetas** de la lista, en Ajustes → Horario (`ajustes.recetas.hora`):
  - «Cuándo empezar»: hora de servir − duración;
  - «Si empiezas ya»: ahora + duración, en la zona del móvil. Solo en este modo la lista se refresca cada minuto.
- **Tema** automático, claro u oscuro (`ajustes.tema`). La app mantiene el splash hasta leerlo, para no parpadear, y
  las barras del sistema siguen al tema elegido.
- **El cronómetro sigue al tema** (antes era oscuro siempre). `CronoPalette` pasa a ser `ColoresCrono` con versión
  oscura y clara, servida por `LocalColoresCrono` desde `SousChefTheme`. La alarma a pantalla completa también lo
  sigue.

  En claro, los textos pintados con el color del carril se oscurecen hasta 4,5:1 (`colorCarrilTexto`); barras,
  franjas y bordes conservan el color de la receta. Las miniaturas de los selectores se dibujan con los colores del
  tema en uso.
- **Ajustes es un menú** con cuatro apartados; cada fila resume su valor y abre su propia pantalla:
  - Apariencia: tema y diseño de la lista;
  - Horario: hora de servir y hora de las tarjetas;
  - Cronómetro: interfaz y voz;
  - Historial: copia.

  Al pie, la versión.

## 2026-10-20 · Ronda 12 → v1.3.0 (paquete, listas coherentes, sin aviso de versión)
- **Id de paquete `app.trombito.souschef`** (antes `app.souschef`; debug, `app.trombito.souschef.dev`). Todas las apps
  de Joan empezarán por `app.trombito.`. Cambian a la vez el `applicationId`, el `namespace` y el paquete del código
  de `:app`, `:receta` y `:tools`, y también la carpeta de esquemas de Room.

  Para Android es **otra app**:
  - antes de instalarla, guardar una copia del historial en la vieja (Ajustes → Historial);
  - instalar la nueva, cargar la copia y desinstalar la vieja. El formato de la copia no guarda nombres de clase,
    así que sirve;
  - en Obtainium, quitar la app y volver a añadirla.

  En Play no afecta, porque la primera subida sigue pendiente. El esquema `souschef://` se queda igual.
- **Sin aviso de versión nueva** (ADR-0011, sustituye a ADR-0009). Se quitan el comprobador, el trabajo diario y la
  notificación «Actualizaciones». El manifest pierde `INTERNET`, `ACCESS_NETWORK_STATE` y los `<queries>` de
  Obtainium, así que la app queda **sin red**. Las versiones nuevas las avisa Obtainium, que sigue las Releases de
  GitHub, o Play. `docs/privacidad.md` y `docs/play.md` cambian en consecuencia.
- **Orden de las listas** (a petición de Joan): primero el buscador con el orden y los filtros, después los
  cuadraditos y una raya antes de la lista. Los cuadraditos se ocultan al buscar o filtrar, para ir directo a los
  resultados.
- **Contacto** para usuarios (en Ajustes → Acerca de, en Play y en la política de privacidad). Joan descartó los
  enlaces sueltos en el pie del menú por feos:
  - desarrollador «Trombonet12© and Co. Interactive Systems Solutions»;
  - correo trombonet12@trombonet12.es;
  - web https://www.trombonet12.es/.
