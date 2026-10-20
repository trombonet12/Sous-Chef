package app.trombito.souschef.ui.crono

import androidx.compose.ui.graphics.Color
import app.trombito.souschef.R
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.ConsultasCrono
import app.trombito.souschef.receta.crono.EstadoCarril
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.PlanCrono
import app.trombito.souschef.receta.crono.VistaEstado

/** Lo que las piezas de la cocina necesitan para pintarse: receta, estado y reloj en un solo paquete. */
class DatosCrono(
    val receta: Receta,
    val estado: EstadoCrono,
    val ahoraMs: Long,
) {
    val carriles: List<Carril> = ConsultasCrono.carriles(receta)
    val plan: PlanCrono by lazy { ConsultasCrono.plan(estado, receta, ahoraMs) }

    fun carril(id: String): Carril? = carriles.firstOrNull { it.id == id }

    fun nombre(id: String): String = carril(id)?.nombre ?: id

    fun ec(id: String): EstadoCarril? = estado.carriles[id]

    fun paso(id: String): Paso? = pasoActual(receta, estado, id)

    fun quedan(id: String): Int = ConsultasCrono.quedan(estado, receta, id, ahoraMs)

    fun vista(id: String): VistaEstado = ConsultasCrono.vista(estado, receta, id, ahoraMs)

    fun cola(id: String): List<Paso> = colaDe(receta, id)

    /** Color del carril; los neutros (grises o beige) reciben uno de la paleta para distinguirlos. */
    fun color(id: String): Color {
        val i = carriles.indexOfFirst { it.id == id }
        return colorDeCarril(carriles.getOrNull(i)?.color.orEmpty(), i)
    }
}

private const val SATURACION_MINIMA = 0.2f
private val COLORES_NEUTROS =
    listOf(Color(0xFF7FB7D6), Color(0xFFB79BE0), Color(0xFF6FCBB5), Color(0xFFE58FB0))

internal fun colorDeCarril(
    hex: String,
    indice: Int,
): Color {
    val argb = runCatching { android.graphics.Color.parseColor(hex) }.getOrNull()
    val hsv = FloatArray(3)
    if (argb != null) android.graphics.Color.colorToHSV(argb, hsv)
    return if (argb != null && hsv[1] >= SATURACION_MINIMA) {
        Color(argb)
    } else {
        COLORES_NEUTROS[indice.coerceAtLeast(0) % COLORES_NEUTROS.size]
    }
}

/** Recurso de "tapada / destapada / a medias" según el campo `tapa` del paso, o null si no hay valor claro. */
fun recursoTapa(tapa: String?): Int? {
    val t = tapa?.uppercase().orEmpty()
    return when {
        t.isBlank() -> null
        "MEDIAS" in t -> R.string.crono_tapa_medias
        "SIN" in t || "FUERA" in t -> R.string.crono_destapada
        "PUESTA" in t -> R.string.crono_tapada
        else -> null
    }
}
