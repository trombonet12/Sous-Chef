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

// Alto mínimo (dp, a letra 1,0×) de una tarjeta para que se vea entera con su botón; con menos, no hay tarjeta.
private const val TARJETA_MIN_DP = 120

// Una fila compacta (medida en el emulador: 64 dp a una línea; apilada en pantalla estrecha, ~104 dp a 1,0× y ~112 dp
// a 1,3×) y la franja «Ahora / Luego» (~100 dp; ~80 dp con letra grande, con una sola fila de LUEGO), con holgura.
private const val FILA_DP = 68
private const val FILA_APILADA_DP = 108
private const val CRECE_FILA_CON_LETRA = 0.25f
private const val ANCHO_SIN_LIMITE_DP = 10_000f
private const val FRANJA_DP = 108
private const val FRANJA_LETRA_GRANDE_DP = 88
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
 * Orden en que los carriles piden tarjeta: por prioridad y, entre vencidos, el más antiguo primero (es el AHORA de la
 * franja); entre los que corren, el que acaba antes; el resto, por su hora prevista.
 */
private fun ordenDeReparto(tocado: String?): Comparator<VistaCarril> =
    compareBy(
        { it.prioridad(tocado) },
        {
            val corre = it.vencido || it.momento == MomentoPaso.EN_MARCHA
            (if (corre) it.finAtMs else it.sugeridoMs) ?: Long.MAX_VALUE
        },
    )

private fun altoTarjeta(
    v: VistaCarril,
    k: Float,
): Float = (if (v.momento == MomentoPaso.POR_EMPEZAR) TARJETA_POR_EMPEZAR_DP else TARJETA_EN_MARCHA_DP) * k

/**
 * Qué carriles se expanden (el resto, como fila de una línea) con [alturaDp] para los carriles y la franja. Se parte
 * de todos expandidos y se pasa a fila el de menos prioridad hasta que quepan; queda una sola tarjeta mientras tenga
 * el alto mínimo para verse con su botón (con la altura y la letra de [escala] que hay, [anchoDp] decide si las filas
 * van apiladas); si ni una cabe, todos en fila y la franja lleva el botón. Con letra grande cada tarjeta pide más
 * sitio y como mucho se expanden dos (las de más prioridad).
 */
internal fun carrilesEnTarjeta(
    vistas: List<VistaCarril>,
    alturaDp: Float,
    escala: Float,
    tocado: String? = null,
    anchoDp: Float = ANCHO_SIN_LIMITE_DP,
): Set<String> {
    val k = maxOf(1f, escala)
    val filaBase = if (filaApilada(anchoDp, escala)) FILA_APILADA_DP else FILA_DP
    val fila = filaBase * (1f + CRECE_FILA_CON_LETRA * (k - 1f))
    val franja = if (escala > LETRA_GRANDE) FRANJA_LETRA_GRANDE_DP else FRANJA_DP
    val vivas = vistas.filter { !it.terminado }
    val terminados = vistas.size - vivas.size
    val espacio = alturaDp - franja - SEPARACION_DP * vistas.size - terminados * fila
    val orden = vivas.sortedWith(ordenDeReparto(tocado))
    val tarjetas = orden.toMutableList()

    fun necesario(): Float =
        tarjetas.sumOf { altoTarjeta(it, k).toDouble() }.toFloat() + (orden.size - tarjetas.size) * fila
    val maximo = if (escala > LETRA_GRANDE) TARJETAS_CON_LETRA_GRANDE else Int.MAX_VALUE
    while (tarjetas.size > 1 && (tarjetas.size > maximo || necesario() > espacio)) tarjetas.removeAt(tarjetas.lastIndex)
    // Con una sola, que tenga alto para verse entera: lo que sobra tras las filas de los demás.
    val cabeUna = espacio - (orden.size - tarjetas.size) * fila >= TARJETA_MIN_DP * k
    if (tarjetas.size == 1 && !cabeUna) tarjetas.clear()
    return tarjetas.map { it.id }.toSet()
}

/**
 * Carriles: la franja «Ahora / Luego» y, debajo, todos los carriles en su sitio de siempre (nada salta de lugar). El
 * que te necesita (vencido, toca ya, en marcha que acaba antes) es una tarjeta expandida que reparte el alto; los que
 * esperan, los que no caben (poca altura o letra grande) y los terminados, una fila de una línea. Tocar una fila
 * expande ese carril. El botón del carril que toca sale una sola vez: en su tarjeta si está expandida y, si está en
 * fila, en la franja (la fila de ese carril no lo repite).
 */
@Composable
fun CronoCarriles(
    vistas: List<VistaCarril>,
    acciones: AccionesCarril,
    modifier: Modifier = Modifier,
) {
    var tocado by remember { mutableStateOf<String?>(null) }
    val conToque =
        acciones.conEnfocar { id ->
            tocado = id
            acciones.enfocar(id)
        }
    RepartoCarriles(vistas, conToque, tocado, modifier)
}

/** El reparto de [CronoCarriles] sin estado: [tocado] es el carril que Joan tocó para verlo. */
@Composable
internal fun RepartoCarriles(
    vistas: List<VistaCarril>,
    acciones: AccionesCarril,
    tocado: String?,
    modifier: Modifier = Modifier,
) {
    val escala = LocalDensity.current.fontScale
    BoxWithConstraints(modifier) {
        val tarjetas = carrilesEnTarjeta(vistas, maxHeight.value, escala, tocado, maxWidth.value)
        val enFranja = idBotonFranja(vistas)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(SEPARACION_DP.dp)) {
            FranjaAhora(vistas, acciones, enTarjeta = tarjetas)
            vistas.forEach { v ->
                if (v.id in tarjetas) {
                    TarjetaCarril(v, acciones, vistas.size, Modifier.weight(1f).fillMaxWidth())
                } else {
                    FilaCarrilCompacta(v, acciones, conAccion = v.id != enFranja)
                }
            }
        }
    }
}
