package app.trombito.souschef.domain.copia

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.MAX_NOTA
import app.trombito.souschef.domain.PasoReal
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.SlugReceta
import app.trombito.souschef.domain.Valoracion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * Copia del historial en un archivo JSON que el usuario guarda donde quiera del móvil (fuera de los datos de la app),
 * para recuperarlo tras borrar datos o reinstalar. Puro: sin Android ni disco.
 */
object CopiaHistorial {
    /**
     * 2 (v1.7): las entradas llevan `valoracion` y `previstoMin`, y las preferencias `quiereHacerla`. [leer] sigue
     * aceptando la 1 (esos campos quedan a null/false).
     */
    const val VERSION = 2
    private const val FORMATO = "sous-chef-historial"

    /** Entradas (y preferencias) que admite una copia; más es un archivo sospechoso y se rechaza entero. */
    const val MAX_ENTRADAS = 5000
    const val MAX_TITULO = 120
    const val MAX_PASOS = 200
    const val RACIONES_MIN = 1
    const val RACIONES_MAX = 20

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
                preferencias = preferencias.map { PreferenciaDto(it.slug, it.favorito, it.raciones, it.quiereHacerla) },
            ),
        )

    /**
     * La copia leída, o null si el texto no es una copia de Sous Chef, es de una versión futura o trae más de
     * [MAX_ENTRADAS] entradas. Las entradas y preferencias inválidas se descartan y se cuentan en
     * [Copia.descartadas]; título y nota se recortan a su máximo. Una entrada de historial solo exige un slug bien
     * formado (una receta retirada del catálogo sigue teniendo cocinadas, con su título guardado); una preferencia
     * además exige que el slug esté en [slugsValidos], para no crear preferencias de recetas que no existen.
     */
    fun leer(
        texto: String,
        slugsValidos: Set<String>,
    ): Copia? =
        runCatching { json.decodeFromString(CopiaDto.serializer(), texto) }
            .getOrNull()
            ?.takeIf { it.formato == FORMATO && it.version <= VERSION }
            ?.takeIf { it.historial.size <= MAX_ENTRADAS && it.preferencias.size <= MAX_ENTRADAS }
            ?.let { dto ->
                val entradas = dto.historial.map { it.validar() }
                val prefs = dto.preferencias.filter { it.valida(slugsValidos) }
                Copia(
                    historial = entradas.filterNotNull(),
                    preferencias = prefs.map { Preferencia(it.slug, it.favorito, it.raciones, it.quiereHacerla) },
                    descartadas = entradas.count { it == null } + (dto.preferencias.size - prefs.size),
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
     * Valoraciones de la copia que hay que rellenar en cocinadas que ya están (mismo id). Regla de fusión: si la
     * cocinada local no tiene valoración y la copia sí, se rellena; si las dos tienen, gana la local (nunca se pisa);
     * si la copia no tiene, no hay nada que hacer. Las cocinadas que no existen en local no salen aquí: entran enteras
     * por [nuevas]. Con ids repetidos en la copia vale el primero. El `UPDATE ... WHERE valoracion IS NULL` del
     * repositorio vuelve a imponer la misma regla dentro de la transacción de importar.
     */
    fun valoracionesNuevas(
        actual: List<EntradaHistorial>,
        copia: Copia,
    ): Map<String, Valoracion> {
        val sinValorar = actual.filter { it.valoracion == null }.mapTo(HashSet()) { it.id }
        return copia.historial
            .filter { it.valoracion != null && it.id in sinValorar }
            .distinctBy { it.id }
            .associate { it.id to checkNotNull(it.valoracion) }
    }

    /**
     * Preferencias a aplicar: un favorito o un «quiero hacerla» de la copia se suma (nunca quita el actual) y las
     * raciones solo se toman si aún no hay. Devuelve únicamente las que cambian algo.
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
                    quiereHacerla = (ya?.quiereHacerla ?: false) || p.quiereHacerla,
                )
            fusion.takeIf { it != (ya ?: Preferencia(p.slug)) }
        }
}

data class Copia(
    val historial: List<EntradaHistorial>,
    val preferencias: List<Preferencia>,
    /** Entradas y preferencias de la copia que no eran válidas y no se han cargado. */
    val descartadas: Int = 0,
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
    // v2: ausentes en una copia v1.
    val valoracion: String? = null,
    val previstoMin: Int? = null,
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
    val quiereHacerla: Boolean = false, // v2
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
        valoracion?.codigo,
        previstoMin,
    )

private fun slugValido(
    slug: String,
    slugsValidos: Set<String>,
) = SlugReceta.valido(slug) && slug in slugsValidos

private fun fechaValida(fecha: String) = runCatching { LocalDate.parse(fecha) }.isSuccess

private fun racionesValidas(raciones: Int?) =
    raciones == null || raciones in CopiaHistorial.RACIONES_MIN..CopiaHistorial.RACIONES_MAX

private fun PreferenciaDto.valida(slugsValidos: Set<String>) =
    slugValido(slug, slugsValidos) && racionesValidas(raciones)

/** La entrada ya saneada, o null si no es válida. */
private fun EntradaDto.validar(): EntradaHistorial? {
    val valida =
        id.length in 1..MAX_ID_ENTRADA &&
            SlugReceta.valido(slug) &&
            fechaValida(fecha) &&
            racionesValidas(raciones) &&
            (minutosReales ?: 0) >= 0 &&
            (previstoMin ?: 0) >= 0 &&
            pasosReales.size <= CopiaHistorial.MAX_PASOS &&
            pasosReales.all { it.segundos >= 0 }
    return if (valida) {
        EntradaHistorial(
            id,
            slug,
            titulo.trim().take(CopiaHistorial.MAX_TITULO),
            fecha,
            base,
            tecnica,
            raciones,
            minutosReales,
            nota?.trim()?.take(MAX_NOTA)?.ifBlank { null },
            pasosReales.map { PasoReal(it.id, it.t, it.segundos) },
            Valoracion.deCodigo(valoracion), // un código que no se conoce se queda sin valorar, no tumba la entrada
            previstoMin,
        )
    } else {
        null
    }
}

private const val MAX_ID_ENTRADA = 128
