package app.trombito.souschef.domain

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val COCINADAS_TARDA = 3
private const val COCINADAS_TARDA_MIN = 2
private const val MINUTOS_REALES_MAX = 720

private val masRecientePrimero = compareByDescending<EntradaHistorial> { it.fecha }.thenByDescending { it.id }

/**
 * Lo que la ficha cuenta del historial de una receta. [veces] cuenta todas las cocinadas, también las anotadas sin
 * cronómetro; [tardaMin] solo sale con 2 o más cocinadas con duración real. [planMin] es el previsto de la cocinada más
 * reciente que lo guardó o, si ninguna lo guardó, el total actual de la receta.
 */
data class ResumenReceta(
    val veces: Int,
    /** Fecha ISO (yyyy-MM-dd) de la última vez. */
    val ultimaFecha: String,
    val tardaMin: Int?,
    val planMin: Int,
    /** La valoración más reciente que exista; null si ninguna cocinada se valoró. */
    val valoracion: Valoracion?,
)

/**
 * Resumen de las cocinadas de [slug] en [entradas], o null si nunca se ha cocinado. Es una función pura sobre la lista
 * que la ficha ya observa: no hace consultas nuevas. [tardaMin] es la mediana de las 3 duraciones válidas más recientes
 * (0 < m ≤ 720); una entrada sin duración (anotada sin cronómetro) no cuenta para el ritmo, pero sí para [veces] y
 * [ResumenReceta.ultimaFecha].
 */
fun resumenDeReceta(
    entradas: List<EntradaHistorial>,
    slug: String,
    totalEfectivo: Double,
): ResumenReceta? {
    val propias = entradas.filter { it.slug == slug }.sortedWith(masRecientePrimero)
    if (propias.isEmpty()) return null
    val duraciones =
        propias
            .mapNotNull { e -> e.minutosReales?.takeIf { it in 1..MINUTOS_REALES_MAX } }
            .take(COCINADAS_TARDA)
    return ResumenReceta(
        veces = propias.size,
        ultimaFecha = propias.first().fecha.take(LONGITUD_FECHA),
        tardaMin = if (duraciones.size >= COCINADAS_TARDA_MIN) medianaRedondeada(duraciones) else null,
        planMin = propias.firstNotNullOfOrNull { e -> e.previstoMin?.takeIf { it > 0 } } ?: ceil(totalEfectivo).toInt(),
        valoracion = propias.firstNotNullOfOrNull { it.valoracion },
    )
}

private fun medianaRedondeada(valores: List<Int>): Int {
    val v = valores.sorted()
    val mitad = v.size / 2
    return if (v.size % 2 == 1) v[mitad] else ((v[mitad - 1] + v[mitad]) / 2.0).roundToInt()
}

/**
 * Resumen del mes en curso para el Historial: cocinadas del mes, recetas distintas entre ellas y recetas que nunca se
 * han cocinado ([sinEstrenar]) sobre el total de recetas ([totalRecetas]).
 */
data class ResumenMes(
    val cocinadas: Int,
    val recetasDistintas: Int,
    val sinEstrenar: Int,
    val totalRecetas: Int,
)

/**
 * El resumen del mes de [hoy]. El mes se toma de la fecha de cada entrada; si no se entiende, la entrada no cuenta
 * en el mes. «Sin estrenar» mira todo el historial, no solo el mes, y solo las recetas de [slugsRecetas].
 */
fun resumenDelMes(
    entradas: List<EntradaHistorial>,
    slugsRecetas: Set<String>,
    hoy: LocalDate,
): ResumenMes {
    val mes = YearMonth.from(hoy)
    val delMes =
        entradas.filter { e ->
            runCatching { YearMonth.from(LocalDate.parse(e.fecha.take(LONGITUD_FECHA))) }.getOrNull() == mes
        }
    val cocinadas = entradas.mapTo(HashSet()) { it.slug }
    return ResumenMes(
        cocinadas = delMes.size,
        recetasDistintas = delMes.mapTo(HashSet()) { it.slug }.size,
        sinEstrenar = slugsRecetas.count { it !in cocinadas },
        totalRecetas = slugsRecetas.size,
    )
}

private const val LONGITUD_FECHA = 10
