package app.trombito.souschef.data.novedades

import app.trombito.souschef.data.seed.FuenteSemilla
import app.trombito.souschef.domain.FuenteNovedades
import app.trombito.souschef.domain.LineaNovedad
import app.trombito.souschef.domain.Novedades
import app.trombito.souschef.domain.VersionMenor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

@Serializable
private data class LineaDto(
    val texto: String = "",
    val destino: String? = null,
)

@Serializable
private data class NovedadesDto(
    val version: String = "",
    val titulo: String = "",
    val lineas: List<LineaDto> = emptyList(),
)

/**
 * Lee `novedades/<major.minor>.json` de los assets (los copia el build desde `distribution/novedades/`). Un archivo
 * ilegible o con un formato roto se ignora: unas novedades malas nunca tumban la app.
 */
class FuenteNovedadesAssets
    @Inject
    constructor(
        private val fuente: FuenteSemilla,
    ) : FuenteNovedades {
        // Los assets se leen en IO: nunca en el hilo principal.
        override suspend fun todas(): List<Novedades> =
            withContext(Dispatchers.IO) {
                fuente
                    .listar(DIRECTORIO)
                    .filter { it.endsWith(".json") }
                    .mapNotNull { ruta -> leerOnull(ruta) }
                    .sortedByDescending { VersionMenor.de(it.version) }
            }

        @Suppress("SwallowedException")
        private fun leerOnull(ruta: String): Novedades? =
            try {
                parsear(fuente.leer(ruta), ruta.substringAfterLast('/').removeSuffix(".json"))
            } catch (e: IOException) {
                null // ilegible: se ignora
            }

        companion object {
            const val DIRECTORIO = "novedades"

            private val json = Json { ignoreUnknownKeys = true }

            /** Líneas por entrada y largo máximo de cada una; la tarea Gradle `comprobarNovedades` exige lo mismo. */
            const val MIN_LINEAS = 3
            const val MAX_LINEAS = 5
            const val MAX_CARACTERES = 90

            /**
             * Una entrada, o null si el JSON está roto o incumple las reglas: título no vacío, `version` legible (e
             * igual a [nombreArchivo], si se da), de [MIN_LINEAS] a [MAX_LINEAS] líneas de hasta [MAX_CARACTERES].
             */
            fun parsear(
                texto: String,
                nombreArchivo: String? = null,
            ): Novedades? = leerDto(texto)?.let { aNovedades(it, nombreArchivo) }

            // SerializationException es una IllegalArgumentException.
            @Suppress("SwallowedException")
            private fun leerDto(texto: String): NovedadesDto? =
                try {
                    json.decodeFromString(NovedadesDto.serializer(), texto)
                } catch (e: IllegalArgumentException) {
                    null
                }

            private fun aNovedades(
                dto: NovedadesDto,
                nombreArchivo: String?,
            ): Novedades? {
                val version = VersionMenor.de(dto.version)
                val lineas =
                    dto.lineas
                        .filter { it.texto.isNotBlank() }
                        .map { LineaNovedad(it.texto, it.destino?.takeIf(String::isNotBlank)) }
                val valida =
                    version != null &&
                        dto.version == version.toString() &&
                        (nombreArchivo == null || dto.version == nombreArchivo) &&
                        dto.titulo.isNotBlank() &&
                        lineas.size in MIN_LINEAS..MAX_LINEAS &&
                        lineas.all { it.texto.length <= MAX_CARACTERES }
                return if (valida) Novedades(dto.version, dto.titulo, lineas) else null
            }
        }
    }
