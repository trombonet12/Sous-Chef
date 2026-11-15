package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.trombito.souschef.receta.crono.ClaseEstado

// Alto (dp, a letra 1,0×) que necesita cada tarjeta para enseñar su orden: antes de empezar, cabecera + METE + fuego +
// acción; en marcha o vencida, la tarjeta compacta (reloj con mandos y la pila de textos).
private const val TARJETA_POR_EMPEZAR_DP = 200
private const val TARJETA_EN_MARCHA_DP = 150

// Una fila compacta y la franja «Ahora / Luego», con holgura.
private const val FILA_DP = 80
private const val FRANJA_DP = 108
private const val SEPARACION_DP = 6

/** Prioridad al repartir el sitio: vencido, toca ya, en marcha y, al final, lo que espera. */
private fun VistaCarril.prioridad(): Int =
    when {
        vencido -> 0
        clase == ClaseEstado.YA -> 1
        momento == MomentoPaso.EN_MARCHA -> 2
        clase == ClaseEstado.ESPERA -> 3
        else -> 4
    }

/**
 * Qué carriles se pintan como tarjeta (el resto, como fila de una línea) con [alturaDp] para los carriles y la franja.
 * Se parte de todas las tarjetas y se pasa a fila la de menos prioridad hasta que quepan; siempre queda al menos una.
 * Con letra grande cada tarjeta pide más sitio, así que caben menos (como mucho dos con cuatro carriles).
 */
internal fun carrilesEnTarjeta(
    vistas: List<VistaCarril>,
    alturaDp: Float,
    escala: Float,
): Set<String> {
    val k = maxOf(1f, escala)
    val vivas = vistas.filter { !it.terminado }
    val terminados = vistas.size - vivas.size
    val espacio = alturaDp - FRANJA_DP - SEPARACION_DP * vistas.size - terminados * FILA_DP * k
    val orden = vivas.sortedWith(compareBy({ it.prioridad() }, { it.sugeridoMs ?: Long.MAX_VALUE }))
    val tarjetas = orden.toMutableList()

    fun alto(v: VistaCarril): Float =
        (if (v.momento == MomentoPaso.POR_EMPEZAR) TARJETA_POR_EMPEZAR_DP else TARJETA_EN_MARCHA_DP) * k

    fun necesario(): Float =
        tarjetas.sumOf { alto(it).toDouble() }.toFloat() + (orden.size - tarjetas.size) * FILA_DP * k
    while (tarjetas.size > 1 && necesario() > espacio) tarjetas.removeAt(tarjetas.lastIndex)
    return tarjetas.map { it.id }.toSet()
}

/**
 * APILADA: la franja «Ahora / Luego» y, debajo, los carriles uno debajo de otro en su orden de siempre. Cada uno es una
 * tarjeta que reparte la altura; los que no caben (por poca altura o letra grande) y los terminados quedan en fila.
 */
@Composable
fun CronoApilado(
    vistas: List<VistaCarril>,
    acciones: AccionesCarril,
    modifier: Modifier = Modifier,
) {
    val escala = LocalDensity.current.fontScale
    BoxWithConstraints(modifier) {
        val tarjetas = carrilesEnTarjeta(vistas, maxHeight.value, escala)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(SEPARACION_DP.dp)) {
            FranjaAhora(vistas, acciones)
            vistas.forEach { v ->
                if (v.id in tarjetas) {
                    TarjetaCarril(v, acciones, vistas.size, Modifier.weight(1f).fillMaxWidth())
                } else {
                    FilaCarrilCompacta(v, acciones)
                }
            }
        }
    }
}
