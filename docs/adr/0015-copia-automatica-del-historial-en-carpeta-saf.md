# ADR-0015: Copiar el historial en una carpeta elegida (SAF) al cerrar cada cocinada

- Estado: aceptado (2026-10-25)
- Fecha: 2026-10-25
- Relacionado: `docs/decisiones.md` § 2026-10-18 · Ronda 9 → v1.0.0 (copia manual), `docs/decisiones.md` § Ronda 18 · v1.7, `docs/analisis/v1.7.md` (F5, A2), amplía [ADR-0007](0007-app-solo-local-sin-supabase.md), [arquitectura](../arquitectura.md)

## Contexto
- El historial (cocinadas, notas, valoraciones, favoritos, raciones y «Quiero hacerla») solo vive en Room. La app tiene
  `allowBackup="false"` (`app/src/main/AndroidManifest.xml`), así que el backup de Android no lo guarda. La única salida
  es la copia manual de Ajustes → Historial («Guardar copia» / «Cargar copia», `GestorCopia`, formato
  `sous-chef-historial` v2 en `CopiaHistorial`).
- Para pasar de la versión de Play a la de Obtainium (o al revés) hay que desinstalar, porque cambia la firma. Al
  desinstalar se borra el historial si nadie se acordó de guardar la copia (`docs/analisis/v1.7.md`, F5).
- `ArchivoCopiaAndroid` abre el destino en modo `"wt"`, y ese modo vacía la copia buena antes de escribir. Por eso
  `EscrituraSegura` valida el contenido entero en memoria antes de abrir el destino (A2). Aun así, un fallo a media
  escritura deja un archivo truncado en el lugar de la copia buena.
- En la ronda 9, Joan decidió que la copia fuera manual y sin carpeta automática (`docs/decisiones.md` § 2026-10-18 ·
  Ronda 9). En la ronda 18 la cambia por una carpeta elegida que guarda los 5 últimos archivos (`docs/analisis/v1.7.md`
  § Alcance elegido).
- ADR-0007 deja Room como la única persistencia, sin red. Una carpeta del almacenamiento del móvil no es un servidor:
  no hace falta red ni permisos nuevos.

## Decisión
- **Carpeta elegida una vez** con el selector del sistema: `ActivityResultContracts.OpenDocumentTree` y
  `takePersistableUriPermission` (lectura y escritura). La app no pide ningún permiso de almacenamiento, ni
  `READ/WRITE_EXTERNAL_STORAGE` ni `MANAGE_EXTERNAL_STORAGE`. La URI del árbol se guarda en `meta`. Si se elige otra
  carpeta, se suelta la anterior con `releasePersistableUriPermission`.
- **Un archivo nuevo en cada cierre.** Cuando una cocinada queda cerrada en firme (después del «Deshacer» de 12 s), se
  escribe `souschef-historial-AAAAMMDD-HHmm.json` con la copia v2 completa, leída del historial en ese momento.
  Orden:
  1. Se construyen los bytes y se validan con el mismo lector de «Cargar copia» (`validaCopia`).
  2. Se crea el documento dentro del árbol (`DocumentsContract.createDocument`) y se escribe con `EscrituraSegura`.
     Nunca se abre en `"wt"` un archivo que ya existe.
  3. `flush` y, si el proveedor da un descriptor de archivo, `FileDescriptor.sync()`. Si no lo da o falla, se sigue.
  4. Se relee el archivo nuevo y tiene que pasar `CopiaHistorial.leer`.
  5. Solo entonces se borran los más viejos y se dejan los **5 últimos**. La rotación solo toca nombres con el patrón
     automático: no toca las copias manuales (`sous-chef-historial-AAAA-MM-DD.json`) ni otros archivos.
  6. Si falla la relectura, se borra el archivo nuevo (si se puede) y no se rota: quedan las anteriores.
- **Dónde corre:** una corrutina en `@ApplicationScope` sobre IO, con un `Mutex` para que no se solapen dos copias. Sin
  WorkManager, que sería una dependencia nueva. Si el proceso muere antes de escribir, la copia de esa cocinada se
  pierde y la siguiente la recoge, porque cada archivo lleva el historial entero.
- **Si la URI deja de valer** (permiso retirado, carpeta borrada, proveedor desinstalado): se guarda el fallo en
  `meta` y la app enseña un aviso visible que lleva a elegir otra carpeta. En Ajustes → Historial se ve la carpeta y
  «Última copia: hace N días», con el instante de la última copia releída bien, también en `meta`.
- **Privacidad en el log:** la URI y el nombre de la carpeta no se escriben en el log. Solo el resultado tipado
  (`ResultadoEscritura`) y la clase de la excepción.
- **Restaurar sigue siendo manual:** con «Cargar copia», que añade sin pisar (`GestorCopia.importar`). La app no lee
  la carpeta sola al arrancar.
- **Relación con ADR-0007:** lo amplía sin cambiar su decisión. La app sigue sin red, sin backend y con Room como
  única persistencia. Esta copia sustituye a la frase de ADR-0007 sobre el backup de Android, que ya estaba desactivado.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Seguir solo con la copia manual | Nada que mantener | Depende de que Joan se acuerde; al cambiar de Play a Obtainium se pierde el historial | Es el problema que se quiere resolver (F5) |
| Backup de Android (`allowBackup=true`, Auto Backup) | Sin UI | Depende de Google: en LineageOS sin servicios de Google puede no existir; no se controla cuándo ni dónde | Descartado en la ronda 9 (`docs/decisiones.md` § 2026-10-18 · Ronda 9) |
| Un solo archivo fijo que se reescribe con `"wt"` | Un archivo que encontrar | Un fallo a media escritura deja la única copia truncada (A2) | Puede destruir la copia buena |
| Carpeta pública con permisos de almacenamiento | Ruta conocida | Permisos que Play restringe; más superficie de seguridad | SAF lo resuelve sin permisos |
| WorkManager con reintentos | Sobrevive a la muerte del proceso | Dependencia nueva para una tarea de segundos | La siguiente cocinada repone la copia perdida |
| Carpeta SAF, archivo nuevo, relectura y rotación de 5 | Sin permisos ni red; nunca toca la copia buena antes de tener otra | Hay que elegir la carpeta otra vez tras reinstalar | Elegido |

## Consecuencias
- Positivas:
  - El historial sobrevive a desinstalar, a «borrar datos» y al cambio de firma, porque la carpeta queda fuera de la
    app.
  - En ningún momento hay menos de una copia buena, y quedan 5 puntos a los que volver.
  - Sin permisos nuevos ni red: el manifest no cambia.
- Negativas / costes:
  - **La carpeta puede sincronizarse con la nube del usuario** (Drive, Nextcloud, Syncthing...), y las notas y las
    valoraciones van dentro de cada archivo. La app no lo decide ni lo sabe. El texto de Ajustes tiene que decirlo al
    elegir la carpeta.
  - Tras reinstalar se pierde el permiso persistente. Hay que cargar la copia a mano y volver a elegir la carpeta.
  - Más estado en `meta` (URI, última copia, fallo) y una pantalla de aviso más.
  - Una cocinada deshecha dentro de los 12 s no llega a la copia. Una que muere con el proceso en esos 12 s llega con la
    siguiente.
- Seguimiento:
  - VERIFICAR qué hace `DocumentsContract.createDocument` si ya existe el nombre (dos cierres en el mismo minuto): si
    lo renombra el proveedor (« (1)») o falla. La rotación tiene que contar esos nombres.
  - VERIFICAR si `openFileDescriptor` da un descriptor sincronizable con el proveedor local (`ExternalStorageProvider`)
    y qué pasa con los proveedores de nube.
  - VERIFICAR que el permiso persistente sobrevive a una actualización de la app y que se pierde al desinstalar.
  - VERIFICAR si el formulario de seguridad de datos de Play (`docs/play.md`) cambia: la copia la guarda el usuario en
    su almacenamiento y la app no la envía a nadie.
  - Tests: TDD de la rotación (qué se borra y en qué orden) y de la secuencia escribir → releer → rotar, con un
    `ArchivoCopia` falso que falla en cada paso.
