package app.trombito.souschef.receta

import kotlin.math.abs
import kotlin.math.roundToInt

private const val SEG_MIN = 60

/** Tiempo real que tardó un paso en una cocinada. La app lo mapea desde su historial. */
data class PasoMedido(
    val id: String,
    val segundos: Int,
)

/** Previsto frente a real de un paso; [deltaS] positivo = tardó más de lo previsto. */
data class DesvioPaso(
    val id: String,
    val titulo: String,
    val previstoS: Int,
    val realS: Int,
    val deltaS: Int,
)

/** Desvío de cada paso con tiempo real, en el orden de la receta (si un id se repite, vale el último). */
fun desvios(
    receta: Receta,
    reales: List<PasoMedido>,
): List<DesvioPaso> {
    val porId = reales.associate { it.id to it.segundos }
    return receta.pasos.mapNotNull { p ->
        porId[p.id]?.let { DesvioPaso(p.id, p.t, p.segundos, it, it - p.segundos) }
    }
}

/** «+45 s» por debajo de un minuto; si no, minutos redondeados («+3 min», «-2 min»). */
fun formatoDelta(deltaS: Int): String {
    val signo =
        when {
            deltaS > 0 -> "+"
            deltaS < 0 -> "-"
            else -> ""
        }
    val absoluto = abs(deltaS)
    val minutos = (absoluto.toDouble() / SEG_MIN).roundToInt()
    return if (absoluto < SEG_MIN) "$signo$absoluto s" else "$signo$minutos min"
}

/**
 * Texto plano de la cocinada para compartir: título, fecha, raciones, total previsto frente al real y los pasos por
 * |delta| de mayor a menor. Los títulos van canónicos (`{N}`), porque el informe es para el repo.
 */
fun textoInforme(
    receta: Receta,
    fecha: String,
    raciones: Int,
    minutosReales: Int?,
    desvios: List<DesvioPaso>,
): String {
    val previsto = receta.totalEfectivo.roundToInt()
    val total =
        if (minutosReales == null) {
            "Total: previsto $previsto min"
        } else {
            val delta = formatoDelta((minutosReales - previsto) * SEG_MIN)
            "Total: previsto $previsto min · real $minutosReales min ($delta)"
        }
    val unidad = if (raciones == 1) "1 ración" else "$raciones raciones"
    val lineas = mutableListOf(receta.titulo, "$fecha · $unidad", total)
    if (desvios.isNotEmpty()) {
        lineas += ""
        lineas += desvios.sortedByDescending { abs(it.deltaS) }.map { "${formatoDelta(it.deltaS)} · ${it.titulo}" }
    }
    return lineas.joinToString("\n")
}
