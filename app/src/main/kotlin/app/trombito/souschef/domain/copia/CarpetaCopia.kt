package app.trombito.souschef.domain.copia

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Un archivo dentro de la carpeta elegida: [id] es el documento SAF (opaco) y [nombre] el que ve el usuario. */
data class ArchivoEnCarpeta(
    val id: String,
    val nombre: String,
)

/** Resultado de crear y escribir la copia nueva dentro de la carpeta. */
sealed interface ResultadoCreacion {
    /** Creada y escrita entera; [documento] es su id para releerla o borrarla. */
    data class Creado(
        val documento: String,
    ) : ResultadoCreacion

    /** La carpeta ya no vale (permiso retirado, borrada, proveedor desinstalado): hay que elegir otra. */
    data object CarpetaNoVale : ResultadoCreacion

    /** La carpeta vale pero la escritura no salió; el documento a medias, si se creó, ya se intentó borrar. */
    data class Rechazada(
        val motivo: ResultadoEscritura,
    ) : ResultadoCreacion
}

/**
 * Acceso a la carpeta elegida por el usuario (Storage Access Framework), implementado en `data/copia/`. Todo con ids
 * de documento opacos; nada de esto escribe la URI ni los nombres en el log (ADR-0015).
 */
interface CarpetaCopia {
    /** Se queda con el permiso persistente de lectura y escritura de [carpeta]; false si el sistema no lo da. */
    suspend fun adoptar(carpeta: String): Boolean

    /** Suelta el permiso persistente de [carpeta] (al elegir otra o desactivar). */
    suspend fun soltar(carpeta: String)

    /**
     * Crea [nombre] dentro de [carpeta] y escribe [bytes] con [EscrituraSegura] (que valida antes de abrir nada). Nunca
     * abre un archivo que ya existe. Hace `sync` si el proveedor da un descriptor de archivo.
     */
    suspend fun crear(
        carpeta: String,
        nombre: String,
        bytes: ByteArray,
        valida: (ByteArray) -> Boolean,
    ): ResultadoCreacion

    /** El contenido de [documento] (como mucho [ArchivoCopia.MAX_BYTES]); null si no se puede leer. */
    suspend fun leer(documento: String): ByteArray?

    /** Los archivos de [carpeta]; null si no se puede listar. */
    suspend fun listar(carpeta: String): List<ArchivoEnCarpeta>?

    /** false si no se pudo borrar. */
    suspend fun borrar(documento: String): Boolean
}

/** Los nombres de las copias automáticas: `souschef-historial-AAAAMMDD-HHmm.json` (ADR-0015). */
object NombresCopiaAuto {
    const val PREFIJO = "souschef-historial-"

    private val formato = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

    /** Acepta el « (1)» que un proveedor añade si ya existía el nombre (dos cierres en el mismo minuto). */
    private val patron = Regex("""souschef-historial-(\d{8}-\d{4})(?:\s?\((\d+)\))?\.json""")

    fun nombre(
        ahoraMs: Long,
        zona: ZoneId = ZoneId.systemDefault(),
    ): String = PREFIJO + formato.format(Instant.ofEpochMilli(ahoraMs).atZone(zona)) + ".json"

    /** Solo estos nombres entran en la rotación: ni las copias manuales ni otros archivos. */
    fun esAutomatica(nombre: String): Boolean = patron.matches(nombre)

    /** (sello `AAAAMMDD-HHmm`, número del « (n)»; 0 si no lo lleva) o null si no es una copia automática. */
    internal fun orden(nombre: String): Pair<String, Int>? =
        patron.matchEntire(nombre)?.let { it.groupValues[1] to (it.groupValues[2].toIntOrNull() ?: 0) }
}

/** Qué copias automáticas sobran tras escribir una nueva. */
object RotacionCopias {
    const val CONSERVAR = 5

    /**
     * Las copias automáticas de [existentes] que hay que borrar para dejar [conservar] (la nueva, [nuevoId], siempre
     * entre ellas, aunque el reloj hubiera retrocedido), de la más vieja a la más reciente. Se ordena por el sello del
     * nombre y, a igual sello, por el « (n)». Lo que no tiene nombre automático nunca se toca.
     */
    fun aBorrar(
        existentes: List<ArchivoEnCarpeta>,
        nuevoId: String,
        conservar: Int = CONSERVAR,
    ): List<ArchivoEnCarpeta> {
        val otras =
            existentes
                .filter { it.id != nuevoId }
                .mapNotNull { archivo -> NombresCopiaAuto.orden(archivo.nombre)?.let { archivo to it } }
                .sortedWith(compareBy({ it.second.first }, { it.second.second }))
                .map { it.first }
        return otras.dropLast((conservar - 1).coerceAtLeast(0))
    }
}
