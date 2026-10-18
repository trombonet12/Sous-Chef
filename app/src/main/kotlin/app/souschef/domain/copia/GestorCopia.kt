package app.souschef.domain.copia

import app.souschef.domain.HistorialRepo
import app.souschef.domain.PreferenciasRepo
import app.souschef.domain.Reloj
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

    /** null si no se puede leer. */
    suspend fun leer(origen: String): String?
}

sealed interface ResultadoCopia {
    data class Guardada(
        val cocinadas: Int,
    ) : ResultadoCopia

    data class Cargada(
        val cocinadasNuevas: Int,
    ) : ResultadoCopia

    /** El archivo no es una copia de Sous Chef. */
    data object NoValida : ResultadoCopia

    /** No se pudo leer o escribir el archivo. */
    data object Fallo : ResultadoCopia
}

/** Exporta el historial (y favoritos/raciones) a un archivo del móvil, y lo vuelve a cargar sin pisar nada. */
class GestorCopia(
    private val historial: HistorialRepo,
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
        val texto = archivo.leer(origen)
        val copia = texto?.let(CopiaHistorial::leer)
        return when {
            texto == null -> ResultadoCopia.Fallo
            copia == null -> ResultadoCopia.NoValida
            else -> ResultadoCopia.Cargada(aplicar(copia))
        }
    }

    /** Añade lo que falta y devuelve cuántas cocinadas nuevas hay. */
    private suspend fun aplicar(copia: Copia): Int {
        val nuevas = CopiaHistorial.nuevas(historial.observarTodo().first(), copia)
        nuevas.forEach { historial.agregar(it) }
        val actuales = preferencias.observarTodas().first()
        CopiaHistorial.preferenciasNuevas(actuales, copia).forEach { p ->
            val ya = actuales[p.slug]
            if (p.favorito && ya?.favorito != true) preferencias.fijarFavorito(p.slug, true)
            p.raciones?.takeIf { it != ya?.raciones }?.let { preferencias.fijarRaciones(p.slug, it) }
        }
        return nuevas.size
    }
}
