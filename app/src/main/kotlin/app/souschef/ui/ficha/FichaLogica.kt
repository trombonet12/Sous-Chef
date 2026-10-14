package app.souschef.ui.ficha

import app.souschef.receta.Escalado
import app.souschef.receta.Ingrediente
import app.souschef.receta.Receta
import app.souschef.receta.Tecnica
import java.util.Locale

/** Hora de servir en minutos desde medianoche (no se recuerda entre fichas). */
object Servir {
    const val DEFECTO = 15 * 60 + 30
    const val PASO = 15
    private const val DIA = 24 * 60

    fun ajustar(
        minutos: Int,
        delta: Int,
    ): Int = (minutos + delta).mod(DIA)

    fun formato(minutos: Int): String = String.format(Locale.ROOT, "%02d:%02d", minutos / 60, minutos % 60)
}

enum class MensajeFicha { COPIADA, ANADIDA }

data class TecnicaEnlace(
    val slug: String,
    val titulo: String,
)

data class FichaUiState(
    val cargando: Boolean = true,
    val receta: Receta? = null,
    val raciones: Int = 2,
    val servirMin: Int = Servir.DEFECTO,
    val tecnicas: List<TecnicaEnlace> = emptyList(),
    val mensaje: MensajeFicha? = null,
)

/** Enlaces solo a las técnicas de `tags.tecnica` que existen. */
fun enlacesTecnicas(
    receta: Receta,
    existentes: List<Tecnica>,
): List<TecnicaEnlace> =
    receta.tags.tecnica.mapNotNull { slug ->
        existentes.firstOrNull { it.slug == slug }?.let { TecnicaEnlace(it.slug, it.titulo) }
    }

/** Líneas "cantidad nombre" de la compra escalada, sin el asterisco final. */
fun lineasCompra(
    receta: Receta,
    raciones: Int,
): List<String> {
    val f = Escalado.factor(raciones, receta.raciones)
    return receta.compra.map { lineaIngrediente(it, f) }
}

fun lineaIngrediente(
    item: Ingrediente,
    f: Double,
): String = "${Escalado.cantidad(item, f).removeSuffix(" *")} ${item.n}".trim()

/** Hito en segundos -> "+m:ss". */
fun formatoHito(segundos: Double): String {
    val total = Math.round(segundos).toInt()
    return String.format(Locale.ROOT, "+%d:%02d", total / 60, total % 60)
}
