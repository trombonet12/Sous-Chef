package app.souschef.ui.ficha

import app.souschef.domain.EstadoDespensa
import app.souschef.domain.ItemDespensa
import app.souschef.receta.Despensa
import app.souschef.receta.Escalado
import app.souschef.receta.Ingrediente
import app.souschef.receta.Receta
import app.souschef.receta.Tecnica
import java.util.Locale
import kotlin.math.ceil

/** Hora de servir en minutos desde medianoche (no se recuerda entre fichas). */
object Servir {
    const val DEFECTO = 15 * 60 + 30
    const val PASO = 15
    private const val DIA = 24 * 60

    fun ajustar(
        minutos: Int,
        delta: Int,
    ): Int = (minutos + delta).mod(DIA)

    /** Hora de empezar: servir − total de la receta, redondeado hacia arriba para no ir justo. */
    fun inicio(
        servirMin: Int,
        receta: Receta,
    ): Int = ajustar(servirMin, -ceil(receta.totalEfectivo).toInt())

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
    val faltas: List<Falta> = emptyList(),
)

/** Enlaces solo a las técnicas de `tags.tecnica` que existen. */
fun enlacesTecnicas(
    receta: Receta,
    existentes: List<Tecnica>,
): List<TecnicaEnlace> =
    receta.tags.tecnica.mapNotNull { slug ->
        existentes.firstOrNull { it.slug == slug }?.let { TecnicaEnlace(it.slug, it.titulo) }
    }

/** Ingrediente que la despensa no cubre; [estado] null = no está en la despensa de la app. */
data class Falta(
    val linea: String,
    val estado: EstadoDespensa?,
)

/** Lo que falta para la receta escalada: sin entrada en la despensa, o con estado FALTA o POCO. El agua no cuenta. */
fun faltas(
    receta: Receta,
    raciones: Int,
    despensa: List<ItemDespensa>,
): List<Falta> {
    val f = Escalado.factor(raciones, receta.raciones)
    val nombres = despensa.map { it.nombre }
    return receta.grupos
        .flatMap { it.items }
        .filterNot { Despensa.esImplicito(it.n) }
        .mapNotNull { i ->
            val entrada = Despensa.entradaDe(i.n, nombres)
            val estado = entrada?.let { e -> despensa.first { it.nombre == e }.estado }
            when (estado) {
                null, EstadoDespensa.FALTA, EstadoDespensa.POCO -> Falta(lineaIngrediente(i, f), estado)
                EstadoDespensa.HAY, EstadoDespensa.VETADO -> null
            }
        }
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
