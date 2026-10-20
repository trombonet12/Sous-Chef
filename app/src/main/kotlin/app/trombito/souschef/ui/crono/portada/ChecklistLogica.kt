package app.trombito.souschef.ui.crono.portada

import app.trombito.souschef.receta.LineaMep
import app.trombito.souschef.ui.crono.BloqueCasillas
import app.trombito.souschef.ui.crono.Casilla

private const val PALABRAS_TITULO = 5
private val ESPACIOS = Regex("\\s+")

/** Casillas hechas de un total; `completo` solo si hay alguna casilla. */
internal data class Progreso(
    val hechas: Int,
    val total: Int,
) {
    val completo: Boolean get() = total > 0 && hechas == total
    val fraccion: Float get() = if (total == 0) 0f else hechas.toFloat() / total
}

internal fun progreso(
    casillas: List<Casilla>,
    marcados: Set<String>,
) = Progreso(casillas.count { it.id in marcados }, casillas.size)

internal fun progresoTotal(
    bloques: List<BloqueCasillas>,
    marcados: Set<String>,
) = progreso(bloques.flatMap { it.casillas }, marcados)

/** Lo que se ve de una casilla en la fila (titulo, minuto) y bajo la ⓘ (detalle, solo si añade algo). */
internal data class VistaCasilla(
    val titulo: String,
    val detalle: String?,
    val minuto: Int?,
)

/** Cabecera de la línea; sin ella, sus primeras palabras. El detalle solo existe si no cabe ya en el título. */
internal fun vistaCasilla(texto: String): VistaCasilla {
    val linea = LineaMep.parse(texto)
    val cabecera = linea.cabecera?.takeIf { it.isNotBlank() }
    if (cabecera != null) return VistaCasilla(cabecera, linea.detalle.ifBlank { null }, linea.minuto)
    val palabras = linea.detalle.split(ESPACIOS).filter { it.isNotEmpty() }
    return if (palabras.size <= PALABRAS_TITULO) {
        VistaCasilla(palabras.joinToString(" "), null, linea.minuto)
    } else {
        VistaCasilla(palabras.take(PALABRAS_TITULO).joinToString(" ") + "…", linea.detalle, linea.minuto)
    }
}
