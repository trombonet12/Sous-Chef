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
    /** false si no se pudo escribir. */
    suspend fun escribir(
        destino: String,
        texto: String,
    ): Boolean

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
        val ok = archivo.escribir(destino, CopiaHistorial.exportar(entradas, prefs, fecha))
        return if (ok) ResultadoCopia.Guardada(entradas.size) else ResultadoCopia.Fallo
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
        CopiaHistorial.preferenciasNuevas(actuales, copia).forEach { p ->
            val ya = actuales[p.slug]
            if (p.favorito && ya?.favorito != true) preferencias.fijarFavorito(p.slug, true)
            p.raciones?.takeIf { it != ya?.raciones }?.let { preferencias.fijarRaciones(p.slug, it) }
        }
        return insertadas
    }
}
