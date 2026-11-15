@file:Suppress("MatchingDeclarationName") // la pila y la cabecera comparten archivo; la clase es solo su dato

package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Un bloque de texto de la pila: su contenido, su color, su estilo y, si hace falta, lo que lee TalkBack. */
@Immutable
internal data class BloquePila(
    val texto: AnnotatedString,
    val color: Color,
    val estilo: TextStyle? = null,
    val descripcion: String? = null,
)

/** Líneas máximas de cada bloque de la pila: si pasa, se corta con «…» y el resto está en el detalle. */
internal const val LINEAS_BLOQUE = 2

/**
 * Elige cuántas líneas (1 o [LINEAS_BLOQUE]) se dan a cada bloque, en orden de prioridad, con el [hueco] de alto que
 * hay. Un bloque solo se coloca ENTERO (o cortado con «…» a su máximo de líneas): se intenta con 2 líneas y, si no
 * caben, con 1; si ni en 1 línea cabe, se omite y se corta la pila (los de menos prioridad no le quitan su sitio).
 * [altura] da el alto real, ya medido, de un bloque con N líneas. Devuelve las líneas de cada bloque (0 = omitido).
 */
internal fun repartirPila(
    bloques: Int,
    hueco: Int,
    separacion: Int,
    altura: (bloque: Int, lineas: Int) -> Int,
): List<Int> {
    val lineas = MutableList(bloques) { 0 }
    var usado = 0
    for (i in 0 until bloques) {
        val sep = if (usado > 0) separacion else 0
        val elegidas = (LINEAS_BLOQUE downTo 1).firstOrNull { altura(i, it) + sep <= hueco - usado } ?: break
        lineas[i] = elegidas
        usado += sep + altura(i, elegidas)
    }
    return lineas
}

/**
 * Pila de bloques de texto por prioridad (el primero es el que más importa), de arriba abajo y sin moverse. Se miden
 * con el ancho real y solo se pintan los que caben enteros (hasta 2 líneas con «…»); el alto de la pila es el de lo
 * pintado, nunca más que el que le da el padre.
 */
@Composable
internal fun PilaPrioritaria(
    bloques: List<BloquePila>,
    modifier: Modifier = Modifier,
    estiloBase: TextStyle = MaterialTheme.typography.bodyMedium,
    separacion: Dp = 2.dp,
) {
    if (bloques.isEmpty()) return
    val medidor = rememberTextMeasurer()
    val baseGlobal = LocalTextStyle.current
    SubcomposeLayout(modifier) { constraints ->
        val ancho = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
        val sep = separacion.roundToPx()
        val hueco = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        val alturas = HashMap<Pair<Int, Int>, Int>()

        fun altura(
            i: Int,
            lineas: Int,
        ): Int =
            alturas.getOrPut(i to lineas) {
                medidor
                    .measure(
                        text = bloques[i].texto,
                        style = baseGlobal.merge(bloques[i].estilo ?: estiloBase),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = lineas,
                        constraints = Constraints(maxWidth = ancho),
                        layoutDirection = layoutDirection,
                        density = this,
                    ).size.height
            }
        val lineas = repartirPila(bloques.size, hueco, sep, ::altura)
        val colocados =
            bloques.indices.filter { lineas[it] > 0 }.map { i ->
                val b = bloques[i]
                val medible =
                    subcompose(i) {
                        Text(
                            b.texto,
                            Modifier
                                .fillMaxWidth()
                                .semantics { b.descripcion?.let { contentDescription = it } },
                            color = b.color,
                            style = b.estilo ?: estiloBase,
                            maxLines = lineas[i],
                            overflow = TextOverflow.Ellipsis,
                        )
                    }.first()
                medible.measure(Constraints.fixedWidth(ancho))
            }
        val alto = colocados.sumOf { it.height } + sep * (colocados.size - 1).coerceAtLeast(0)
        layout(ancho, alto.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            colocados.forEach {
                it.placeRelative(0, y)
                y += it.height + sep
            }
        }
    }
}

private const val ANCHO_MIN_TITULO = 0.5f

/**
 * Cabecera de una tarjeta o fila compacta, en dos líneas que nunca parten una palabra:
 *  1. [insignia] + [nombre] del carril + [prevista] (hora prevista) a la izquierda y [extras] (chips, «i») a la
 *     derecha;
 *  2. el [titulo] del paso, siempre en su propia línea.
 * El nombre tiene la prioridad de ancho (baja la letra antes de cortarse); lo que cede es la prevista, que si no cabe
 * junto al nombre baja a la línea del título y, si así le quita más de la mitad del ancho al título, a una línea
 * propia.
 * Cada parte va en su propio contenedor, así que puede emitir lo que quiera (o nada).
 */
@Suppress("LongParameterList") // una ranura por parte de la cabecera
@Composable
internal fun CabeceraPaso(
    insignia: @Composable () -> Unit,
    nombre: @Composable () -> Unit,
    prevista: @Composable () -> Unit,
    extras: @Composable () -> Unit,
    titulo: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    separacion: Dp = 6.dp,
    separacionVertical: Dp = 2.dp,
) {
    Layout(
        content = {
            Box { insignia() }
            Box { nombre() }
            Box { prevista() }
            Row(
                horizontalArrangement = Arrangement.spacedBy(separacion),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                extras()
            }
            Box { titulo() }
        },
        modifier = modifier,
    ) { medibles, constraints ->
        val sep = separacion.roundToPx()
        val sepV = separacionVertical.roundToPx()
        val ancho = constraints.maxWidth
        val suelto = Constraints(maxWidth = ancho)
        val i = medibles[0].measure(suelto)
        val e = medibles[3].measure(suelto)
        val huecoExtras = if (e.width > 0) e.width + sep else 0
        val n = medibles[1].measure(Constraints(maxWidth = (ancho - i.width - sep - huecoExtras).coerceAtLeast(0)))
        val p = medibles[2].measure(suelto)
        val huecoPrevista = ancho - i.width - sep - n.width - huecoExtras
        val hayPrevista = p.width > 0
        val previstaArriba = hayPrevista && p.width + sep <= huecoPrevista
        val anchoConPrevista = (ancho - p.width - sep).coerceAtLeast(0)
        val previstaEnTitulo = hayPrevista && !previstaArriba && anchoConPrevista >= ancho * ANCHO_MIN_TITULO
        val previstaSola = hayPrevista && !previstaArriba && !previstaEnTitulo
        val t = medibles[4].measure(Constraints(maxWidth = if (previstaEnTitulo) anchoConPrevista else ancho))

        val alto1 = maxOf(i.height, n.height, e.height, if (previstaArriba) p.height else 0)
        val alto2 = if (previstaSola) p.height + sepV else 0
        val alto3 = if (previstaEnTitulo) maxOf(t.height, p.height) else t.height
        val alto = alto1 + alto2 + alto3 + sepV
        layout(ancho, alto.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            i.placeRelative(0, (alto1 - i.height) / 2)
            n.placeRelative(i.width + sep, (alto1 - n.height) / 2)
            if (previstaArriba) p.placeRelative(i.width + sep + n.width + sep, (alto1 - p.height) / 2)
            e.placeRelative(ancho - e.width, (alto1 - e.height) / 2)
            var y = alto1 + sepV
            if (previstaSola) {
                p.placeRelative(0, y)
                y += alto2
            }
            if (previstaEnTitulo) {
                p.placeRelative(0, y + (alto3 - p.height) / 2)
                t.placeRelative(p.width + sep, y + (alto3 - t.height) / 2)
            } else {
                t.placeRelative(0, y)
            }
        }
    }
}
