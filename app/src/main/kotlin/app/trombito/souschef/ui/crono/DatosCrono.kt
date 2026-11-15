package app.trombito.souschef.ui.crono

import androidx.compose.ui.graphics.Color
import app.trombito.souschef.R
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Potencia
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.ConsultasCrono
import app.trombito.souschef.receta.crono.EstadoCarril
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.PasoPrevisto
import app.trombito.souschef.receta.crono.PlanCrono
import app.trombito.souschef.receta.crono.VistaEstado

/**
 * Lo que las piezas de la cocina necesitan para pintarse: receta, estado y reloj en un solo paquete.
 *
 * [receta] es la canónica (vitro 0-9): con ella trabaja el motor. [recetaUi] es la misma traducida a la [cocina] del
 * usuario (ADR-0014): de ahí salen los textos que se ven y se oyen (`paso`, `cola`...). `f` es el mismo en las dos.
 */
class DatosCrono(
    val receta: Receta,
    val estado: EstadoCrono,
    val ahoraMs: Long,
    val cocina: Cocina = Cocina.VITRO_9,
    val recetaUi: Receta = receta,
) {
    val carriles: List<Carril> = ConsultasCrono.carriles(receta)

    /** Horas previstas de cada paso (ADR-0018), calculadas una vez por tic y compartidas por vista, plan y sugerido. */
    val previstos: Map<String, PasoPrevisto> by lazy { ConsultasCrono.previstos(estado, receta, ahoraMs) }
    val plan: PlanCrono by lazy { ConsultasCrono.plan(estado, receta, ahoraMs, previstos) }

    fun carril(id: String): Carril? = carriles.firstOrNull { it.id == id }

    fun nombre(id: String): String = carril(id)?.nombre ?: id

    fun ec(id: String): EstadoCarril? = estado.carriles[id]

    fun paso(id: String): Paso? = pasoActual(recetaUi, estado, id)

    fun quedan(id: String): Int = ConsultasCrono.quedan(estado, receta, id, ahoraMs)

    /** Hora prevista de empezar el paso actual del carril (sigue al cocinero, ADR-0018). */
    fun sugerido(id: String): Long? = ConsultasCrono.sugerido(estado, receta, id, ahoraMs, previstos)

    fun vista(id: String): VistaEstado = ConsultasCrono.vista(estado, receta, id, ahoraMs, previstos)

    fun cola(id: String): List<Paso> = colaDe(recetaUi, id)

    /** Texto de una fila del motor (`Paralelo.paso`...), que sale de la receta canónica, en la cocina del usuario. */
    fun texto(canonico: String): String = Potencia.reescribir(canonico, cocina)

    /** Color del carril; los neutros (grises o beige) reciben uno de la paleta para distinguirlos. */
    fun color(id: String): Color {
        val i = carriles.indexOfFirst { it.id == id }
        return colorDeCarril(carriles.getOrNull(i)?.color.orEmpty(), i)
    }
}

private const val SATURACION_MINIMA = 0.2f
private const val OPACO = 0xFF000000.toInt()

/**
 * Colores que reciben los carriles neutros (grises o beige, como la mesa), por su posición en la receta. Elegidos lejos
 * de los que llevan los demás carriles de las 33 recetas (naranja, salmón, malva, azul y verde): el primero, que es el
 * de la mesa en todas, es un turquesa a ΔE76 ≥ 32 de cualquiera de ellos. El azul anterior (#7FB7D6) quedaba a ΔE 4-6
 * de la olla (#6FBEE8) en diez recetas. El diagrama de la ficha debe usar los mismos (`rgbCarrilPlan`).
 */
internal val NEUTROS_CARRIL_RGB = intArrayOf(0x38C6B2, 0xF4A3BF, 0xB3BFF3, 0xB5A573)

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
        Color(OPACO or NEUTROS_CARRIL_RGB[indice.coerceAtLeast(0) % NEUTROS_CARRIL_RGB.size])
    }
}

/** Recurso de "tapada / sin tapa / a medias" según el campo `tapa` del paso, o null si no hay valor claro. */
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
