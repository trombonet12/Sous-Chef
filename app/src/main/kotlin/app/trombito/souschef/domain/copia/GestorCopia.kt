package app.trombito.souschef.domain.copia

import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

/** Leer y escribir el texto de la copia en un archivo elegido por el usuario (`destino`/`origen`: su URI). */
interface ArchivoCopia {
    /** Bajo nivel, sin validar: false si no se pudo escribir. Para copias, usar [escribirSeguro]. */
    suspend fun escribir(
        destino: String,
        texto: String,
    ): Boolean

    /**
     * Escribe [bytes] solo si [valida] los da por buenos (una copia que se vuelve a leer) y no pasan de [MAX_BYTES]:
     * si no, el destino no se toca. La implementación real nunca deja un archivo a medias sin avisar.
     */
    suspend fun escribirSeguro(
        destino: String,
        bytes: ByteArray,
        valida: (ByteArray) -> Boolean,
    ): ResultadoEscritura =
        when {
            bytes.size > MAX_BYTES -> ResultadoEscritura.Grande
            !valida(bytes) -> ResultadoEscritura.Invalida
            escribir(destino, String(bytes, Charsets.UTF_8)) -> ResultadoEscritura.Escrita
            else -> ResultadoEscritura.Fallo
        }

    /** null si no se puede leer. Lanza [ArchivoDemasiadoGrande] si pasa de [MAX_BYTES]. */
    suspend fun leer(origen: String): String?

    companion object {
        /** Tamaño máximo de una copia: sobra para miles de cocinadas y evita cargar un archivo enorme en memoria. */
        const val MAX_BYTES = 2 * 1024 * 1024
    }
}

/** El archivo elegido supera [ArchivoCopia.MAX_BYTES]. */
class ArchivoDemasiadoGrande : Exception("Archivo de copia demasiado grande")

sealed interface ResultadoCopia {
    data class Guardada(
        val cocinadas: Int,
    ) : ResultadoCopia

    data class Cargada(
        val cocinadasNuevas: Int,
        /** Entradas del archivo que no eran válidas y se han ignorado. */
        val descartadas: Int = 0,
    ) : ResultadoCopia

    /** El archivo es mayor de [ArchivoCopia.MAX_BYTES]. */
    data object Grande : ResultadoCopia

    /** El archivo no es una copia de Sous Chef. */
    data object NoValida : ResultadoCopia

    /** No se pudo leer o escribir el archivo. */
    data object Fallo : ResultadoCopia
}

/** Exporta el historial (y favoritos/raciones) a un archivo del móvil, y lo vuelve a cargar sin pisar nada. */
class GestorCopia(
    private val historial: HistorialRepo,
    private val recetas: RecetasRepo,
    private val preferencias: PreferenciasRepo,
    private val archivo: ArchivoCopia,
    private val reloj: Reloj,
) {
    suspend fun exportar(destino: String): ResultadoCopia {
        val entradas = historial.observarTodo().first()
        val prefs =
            preferencias
                .observarTodas()
                .first()
                .values
                .toList()
        val fecha =
            Instant
                .ofEpochMilli(reloj.ahoraMs())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        val slugs = recetas.observarTodas().first().mapTo(HashSet()) { it.slug }
        val bytes = CopiaHistorial.exportar(entradas, prefs, fecha).toByteArray(Charsets.UTF_8)
        // Se valida con el mismo lector de «Cargar»: lo que no se puede volver a leer no se escribe.
        val resultado = archivo.escribirSeguro(destino, bytes) { validaCopia(it, slugs) }
        return when (resultado) {
            ResultadoEscritura.Escrita -> ResultadoCopia.Guardada(entradas.size)
            ResultadoEscritura.Grande -> ResultadoCopia.Grande
            else -> ResultadoCopia.Fallo // Invalida, SinPermiso o Fallo: la UI solo distingue «no se pudo guardar»
        }
    }

    suspend fun importar(origen: String): ResultadoCopia {
        val texto =
            try {
                archivo.leer(origen)
            } catch (_: ArchivoDemasiadoGrande) {
                return ResultadoCopia.Grande
            }
        val slugs = recetas.observarTodas().first().mapTo(HashSet()) { it.slug }
        val copia = texto?.let { CopiaHistorial.leer(it, slugs) }
        return when {
            texto == null -> ResultadoCopia.Fallo
            copia == null -> ResultadoCopia.NoValida
            else -> ResultadoCopia.Cargada(aplicar(copia), copia.descartadas)
        }
    }

    /** Añade lo que falta (en lote, sin pisar ninguna cocinada) y devuelve cuántas cocinadas nuevas hay. */
    private suspend fun aplicar(copia: Copia): Int {
        val nuevas = CopiaHistorial.nuevas(historial.observarTodo().first(), copia)
        val insertadas = historial.importar(nuevas)
        val actuales = preferencias.observarTodas().first()
        // Ya fusionadas con las actuales (favorito suma, raciones no pisa): se guardan todas o ninguna.
        val prefs = CopiaHistorial.preferenciasNuevas(actuales, copia)
        if (prefs.isNotEmpty()) preferencias.importar(prefs)
        return insertadas
    }
}

/** true si [bytes] es una copia de Sous Chef que `CopiaHistorial.leer` entiende. */
internal fun validaCopia(
    bytes: ByteArray,
    slugs: Set<String>,
): Boolean = CopiaHistorial.leer(String(bytes, Charsets.UTF_8), slugs) != null
