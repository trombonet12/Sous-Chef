package app.souschef.domain.crono

import app.souschef.domain.CronoEstadoRepo
import app.souschef.receta.crono.EstadoCrono
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Acceso serializado al estado del cronómetro: la pantalla y el receptor de alarmas lo modifican a la vez, así que
 * toda lectura-modificación-escritura pasa por el mismo cerrojo y parte siempre del dato más reciente de Room.
 * Un JSON ilegible se trata como "no hay estado".
 */
class AlmacenCrono(
    private val repo: CronoEstadoRepo,
) {
    private val cerrojo = Mutex()

    suspend fun leer(slug: String): EstadoCrono? = cerrojo.withLock { leerSinCerrojo(slug) }

    suspend fun todos(): List<EstadoCrono> = cerrojo.withLock { repo.todos().mapNotNull { decodificar(it.json) } }

    /** Guarda [nuevo] tal cual, sustituyendo lo que hubiera. */
    suspend fun fijar(nuevo: EstadoCrono) = cerrojo.withLock { repo.guardar(nuevo.slug, codificar(nuevo)) }

    /**
     * Aplica [cambio] al estado actual y lo guarda si ha cambiado. Devuelve el estado resultante o null si no hay
     * estado para ese slug.
     */
    suspend fun modificar(
        slug: String,
        cambio: (EstadoCrono) -> EstadoCrono,
    ): EstadoCrono? =
        cerrojo.withLock {
            val actual = leerSinCerrojo(slug) ?: return@withLock null
            val nuevo = cambio(actual)
            if (nuevo != actual) repo.guardar(slug, codificar(nuevo))
            nuevo
        }

    suspend fun borrar(slug: String) = cerrojo.withLock { repo.borrar(slug) }

    private suspend fun leerSinCerrojo(slug: String): EstadoCrono? = repo.obtener(slug)?.let { decodificar(it.json) }

    private fun decodificar(json: String): EstadoCrono? =
        runCatching { FORMATO.decodeFromString(EstadoCrono.serializer(), json) }.getOrNull()

    private fun codificar(e: EstadoCrono): String = FORMATO.encodeToString(EstadoCrono.serializer(), e)

    private companion object {
        val FORMATO = Json { ignoreUnknownKeys = true }
    }
}
