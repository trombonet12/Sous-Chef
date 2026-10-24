package app.trombito.souschef.data.novedades

import app.trombito.souschef.data.seed.FuenteSemilla
import app.trombito.souschef.domain.FuenteNovedades
import app.trombito.souschef.domain.LineaNovedad
import app.trombito.souschef.domain.Novedades
import app.trombito.souschef.domain.VersionMenor
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
    val destacar: Boolean = false,
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
        override suspend fun todas(): List<Novedades> =
            fuente
                .listar(DIRECTORIO)
                .filter { it.endsWith(".json") }
                .mapNotNull { ruta -> leerOnull(ruta) }
                .sortedByDescending { VersionMenor.de(it.version) }

        @Suppress("SwallowedException")
        private fun leerOnull(ruta: String): Novedades? =
            try {
                parsear(fuente.leer(ruta))
            } catch (e: IOException) {
                null // ilegible: se ignora
            }

        companion object {
            const val DIRECTORIO = "novedades"

            private val json = Json { ignoreUnknownKeys = true }

            /** Una entrada, o null si el JSON está roto o le falta lo imprescindible (versión, título, líneas). */
            fun parsear(texto: String): Novedades? = leerDto(texto)?.let(::aNovedades)

            // SerializationException es una IllegalArgumentException.
            @Suppress("SwallowedException")
            private fun leerDto(texto: String): NovedadesDto? =
                try {
                    json.decodeFromString(NovedadesDto.serializer(), texto)
                } catch (e: IllegalArgumentException) {
                    null
                }

            private fun aNovedades(dto: NovedadesDto): Novedades? {
                val version = VersionMenor.de(dto.version)
                val lineas =
                    dto.lineas
                        .filter { it.texto.isNotBlank() }
                        .map { LineaNovedad(it.texto, it.destino?.takeIf(String::isNotBlank)) }
                return if (version == null || dto.titulo.isBlank() || lineas.isEmpty()) {
                    null
                } else {
                    Novedades(version.toString(), dto.titulo, dto.destacar, lineas)
                }
            }
        }
    }
