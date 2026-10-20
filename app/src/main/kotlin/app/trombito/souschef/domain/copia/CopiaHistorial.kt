package app.trombito.souschef.domain.copia

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.PasoReal
import app.trombito.souschef.domain.Preferencia
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Copia del historial en un archivo JSON que el usuario guarda donde quiera del móvil (fuera de los datos de la app),
 * para recuperarlo tras borrar datos o reinstalar. Puro: sin Android ni disco.
 */
object CopiaHistorial {
    const val VERSION = 1
    private const val FORMATO = "sous-chef-historial"

    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun exportar(
        historial: List<EntradaHistorial>,
        preferencias: List<Preferencia>,
        fecha: String,
    ): String =
        json.encodeToString(
            CopiaDto.serializer(),
            CopiaDto(
                formato = FORMATO,
                version = VERSION,
                fecha = fecha,
                historial = historial.map { it.aDto() },
                preferencias = preferencias.map { PreferenciaDto(it.slug, it.favorito, it.raciones) },
            ),
        )

    /** La copia leída, o null si el texto no es una copia de Sous Chef (o es de una versión futura). */
    fun leer(texto: String): Copia? =
        runCatching { json.decodeFromString(CopiaDto.serializer(), texto) }
            .getOrNull()
            ?.takeIf { it.formato == FORMATO && it.version <= VERSION }
            ?.let { dto ->
                Copia(
                    historial = dto.historial.map { it.aDominio() },
                    preferencias = dto.preferencias.map { Preferencia(it.slug, it.favorito, it.raciones) },
                )
            }

    /** Lo que hay que añadir: las cocinadas de la copia que no están ya (por id). Nunca se pisa ni se borra nada. */
    fun nuevas(
        actual: List<EntradaHistorial>,
        copia: Copia,
    ): List<EntradaHistorial> {
        val ids = actual.map { it.id }.toSet()
        return copia.historial.filter { it.id !in ids }.distinctBy { it.id }
    }

    /**
     * Preferencias a aplicar: un favorito de la copia se suma (nunca quita uno actual) y las raciones solo se toman si
     * aún no hay. Devuelve únicamente las que cambian algo.
     */
    fun preferenciasNuevas(
        actual: Map<String, Preferencia>,
        copia: Copia,
    ): List<Preferencia> =
        copia.preferencias.mapNotNull { p ->
            val ya = actual[p.slug]
            val fusion =
                Preferencia(
                    slug = p.slug,
                    favorito = (ya?.favorito ?: false) || p.favorito,
                    raciones = ya?.raciones ?: p.raciones,
                )
            fusion.takeIf { it != (ya ?: Preferencia(p.slug)) }
        }
}

data class Copia(
    val historial: List<EntradaHistorial>,
    val preferencias: List<Preferencia>,
)

@Serializable
private data class CopiaDto(
    val formato: String,
    val version: Int,
    val fecha: String = "",
    val historial: List<EntradaDto> = emptyList(),
    val preferencias: List<PreferenciaDto> = emptyList(),
)

@Serializable
private data class EntradaDto(
    val id: String,
    val slug: String,
    val titulo: String,
    val fecha: String,
    val base: String? = null,
    val tecnica: List<String> = emptyList(),
    val raciones: Int? = null,
    val minutosReales: Int? = null,
    val nota: String? = null,
    val pasosReales: List<PasoDto> = emptyList(),
)

@Serializable
private data class PasoDto(
    val id: String,
    val t: String,
    @SerialName("s") val segundos: Int,
)

@Serializable
private data class PreferenciaDto(
    val slug: String,
    val favorito: Boolean = false,
    val raciones: Int? = null,
)

private fun EntradaHistorial.aDto() =
    EntradaDto(
        id,
        slug,
        titulo,
        fecha,
        base,
        tecnica,
        raciones,
        minutosReales,
        nota,
        pasosReales.map { PasoDto(it.id, it.t, it.segundos) },
    )

private fun EntradaDto.aDominio() =
    EntradaHistorial(
        id,
        slug,
        titulo,
        fecha,
        base,
        tecnica,
        raciones,
        minutosReales,
        nota,
        pasosReales.map { PasoReal(it.id, it.t, it.segundos) },
    )
