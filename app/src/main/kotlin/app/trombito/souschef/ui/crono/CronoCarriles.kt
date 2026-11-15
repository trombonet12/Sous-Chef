package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

// Con la letra del sistema por encima de esto, como mucho dos carriles se expanden: el resto, en fila.
private const val LETRA_GRANDE = 1.15f
private const val TARJETAS_CON_LETRA_GRANDE = 2

/**
 * Prioridad al repartir el sitio: vencido, toca ya, el que Joan tocó para verlo ([tocado]), en marcha y, al final, lo
 * que espera.
 */
private fun VistaCarril.prioridad(tocado: String?): Int =
    when {
        vencido -> 0
        clase == ClaseEstado.YA -> 1
        id == tocado -> 2
        momento == MomentoPaso.EN_MARCHA -> 3
        clase == ClaseEstado.ESPERA -> 4
        else -> 5
    }

/**
 * Qué carriles se expanden (el resto, como fila de una línea) con [alturaDp] para los carriles y la franja. Se parte
 * de todos expandidos y se pasa a fila el de menos prioridad hasta que quepan; siempre queda al menos uno. Con letra
 * grande cada tarjeta pide más sitio y como mucho se expanden dos (las de más prioridad).
 */
internal fun carrilesEnTarjeta(
    vistas: List<VistaCarril>,
    alturaDp: Float,
    escala: Float,
    tocado: String? = null,
): Set<String> {
    val k = maxOf(1f, escala)
    val vivas = vistas.filter { !it.terminado }
    val terminados = vistas.size - vivas.size
    val espacio = alturaDp - FRANJA_DP - SEPARACION_DP * vistas.size - terminados * FILA_DP * k
    // Entre vencidos, el más antiguo primero (es el AHORA de la franja); entre los que corren, el que acaba antes; el
    // resto, por su hora prevista.
    val orden =
        vivas.sortedWith(
            compareBy(
                { it.prioridad(tocado) },
                {
                    val corre = it.vencido || it.momento == MomentoPaso.EN_MARCHA
                    (if (corre) it.finAtMs else it.sugeridoMs) ?: Long.MAX_VALUE
                },
            ),
        )
    val tarjetas = orden.toMutableList()

    fun alto(v: VistaCarril): Float =
        (if (v.momento == MomentoPaso.POR_EMPEZAR) TARJETA_POR_EMPEZAR_DP else TARJETA_EN_MARCHA_DP) * k

    fun necesario(): Float =
        tarjetas.sumOf { alto(it).toDouble() }.toFloat() + (orden.size - tarjetas.size) * FILA_DP * k
    val maximo = if (escala > LETRA_GRANDE) TARJETAS_CON_LETRA_GRANDE else Int.MAX_VALUE
    while (tarjetas.size > 1 && (tarjetas.size > maximo || necesario() > espacio)) tarjetas.removeAt(tarjetas.lastIndex)
    return tarjetas.map { it.id }.toSet()
}

/**
 * Carriles: la franja «Ahora / Luego» y, debajo, todos los carriles en su sitio de siempre (nada salta de lugar). El
 * que te necesita (vencido, toca ya, en marcha que acaba antes) es una tarjeta expandida que reparte el alto; los que
 * esperan, los que no caben (poca altura o letra grande) y los terminados, una fila de una línea. Tocar una fila
 * expande ese carril. El botón del carril que toca sale una sola vez: en su tarjeta si está expandida y, si está en
 * fila, en la franja.
 */
@Composable
fun CronoCarriles(
    vistas: List<VistaCarril>,
    acciones: AccionesCarril,
    modifier: Modifier = Modifier,
) {
    val escala = LocalDensity.current.fontScale
    var tocado by remember { mutableStateOf<String?>(null) }
    val conToque =
        acciones.conEnfocar { id ->
            tocado = id
            acciones.enfocar(id)
        }
    BoxWithConstraints(modifier) {
        val tarjetas = carrilesEnTarjeta(vistas, maxHeight.value, escala, tocado)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(SEPARACION_DP.dp)) {
            FranjaAhora(vistas, conToque, enTarjeta = tarjetas)
            vistas.forEach { v ->
                if (v.id in tarjetas) {
                    TarjetaCarril(v, conToque, vistas.size, Modifier.weight(1f).fillMaxWidth())
                } else {
                    FilaCarrilCompacta(v, conToque)
                }
            }
        }
    }
}
