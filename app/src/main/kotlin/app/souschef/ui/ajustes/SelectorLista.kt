package app.souschef.ui.ajustes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.souschef.R
import app.souschef.domain.DisposicionLista

private val PAPEL = Color(0xFFEFEDE3)
private val BORDE_PAPEL = Color(0xFFD9D5C3)
private val TARJETA_CLARA = Color(0xFFFBFAF6)
private val TINTES = listOf(Color(0xFFF5EBD3), Color(0xFFDEEAF0), Color(0xFFF3E3D6))
private val COLORES_BASE = listOf(Color(0xFFC99A2E), Color(0xFF4F86A6), Color(0xFFB9774A))
private val TEXTO_SUAVE = Color(0xFFB8B3A0)
private val TEXTO_FUERTE = Color(0xFF5A564A)

private class OpcionLista(
    val lista: DisposicionLista,
    val titulo: Int,
    val detalle: Int,
)

private val OPCIONES_LISTA =
    listOf(
        OpcionLista(DisposicionLista.TINTE, R.string.ajustes_lista_tinte, R.string.ajustes_lista_tinte_detalle),
        OpcionLista(DisposicionLista.FRANJA, R.string.ajustes_lista_franja, R.string.ajustes_lista_franja_detalle),
        OpcionLista(DisposicionLista.CLASICA, R.string.ajustes_lista_clasica, R.string.ajustes_lista_clasica_detalle),
    )

/**
 * Los tres diseños de la lista de recetas como tarjetas con una miniatura (un esquema con la paleta clara de la
 * app), con el mismo aspecto que el selector del cronómetro.
 */
@Composable
fun SelectorLista(
    elegida: DisposicionLista,
    onElegir: (DisposicionLista) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tarjetas =
        OPCIONES_LISTA.map {
            OpcionTarjeta(it.titulo, it.detalle, "lista-${it.lista.name}", it.lista == elegida)
        }
    SelectorTarjetas(tarjetas, { onElegir(OPCIONES_LISTA[it].lista) }, modifier) { i, mod ->
        MiniaturaLista(OPCIONES_LISTA[i].lista, mod)
    }
}

/** Esquema de la lista de recetas sobre el papel, en proporción de móvil. */
@Composable
private fun MiniaturaLista(
    lista: DisposicionLista,
    modifier: Modifier,
) {
    Canvas(
        modifier
            .aspectRatio(PROPORCION)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, BORDE_PAPEL, RoundedCornerShape(8.dp))
            .clearAndSetSemantics {},
    ) {
        drawRect(PAPEL)
        val m = size.width * 0.06f
        // Cabecera: título y buscador.
        caja(Offset(m, m), Size(size.width * 0.4f, size.height * 0.04f), TEXTO_FUERTE)
        val buscador = size.height * 0.06f
        caja(Offset(m, m * 2 + size.height * 0.04f), Size(size.width - 2 * m, buscador), TARJETA_CLARA)
        val arriba = m * 3 + size.height * 0.04f + buscador
        when (lista) {
            DisposicionLista.TINTE -> tinte(arriba, m)
            DisposicionLista.FRANJA -> franja(arriba, m)
            DisposicionLista.CLASICA -> clasicaLista(arriba, m)
        }
    }
}

private fun DrawScope.caja(
    origen: Offset,
    tamano: Size,
    color: Color,
) = drawRoundRect(color, origen, tamano, CornerRadius(tamano.minDimension * 0.18f))

/** Alto de cada una de las tres tarjetas apiladas desde [arriba]. */
private fun DrawScope.altoTarjeta(
    arriba: Float,
    m: Float,
) = (size.height - arriba - m - 2 * m) / 3

private fun DrawScope.tinte(
    arriba: Float,
    m: Float,
) {
    val alto = altoTarjeta(arriba, m)
    val ancho = size.width - 2 * m
    TINTES.forEachIndexed { i, tinte ->
        val origen = Offset(m, arriba + i * (alto + m))
        caja(origen, Size(ancho, alto), tinte)
        val lado = alto * 0.6f
        val p = (alto - lado) / 2
        // Bloque del tiempo a la izquierda y dos líneas de texto.
        caja(origen + Offset(p, p), Size(lado, lado), COLORES_BASE[i].copy(alpha = 0.55f))
        caja(origen + Offset(p * 2 + lado, alto * 0.3f), Size(ancho * 0.45f, alto * 0.14f), TEXTO_FUERTE)
        caja(origen + Offset(p * 2 + lado, alto * 0.56f), Size(ancho * 0.3f, alto * 0.1f), TEXTO_SUAVE)
    }
}

private fun DrawScope.franja(
    arriba: Float,
    m: Float,
) {
    val alto = altoTarjeta(arriba, m)
    val ancho = size.width - 2 * m
    COLORES_BASE.forEachIndexed { i, color ->
        val origen = Offset(m, arriba + i * (alto + m))
        caja(origen, Size(ancho, alto), TARJETA_CLARA)
        drawRect(color, origen + Offset(0f, alto * 0.1f), Size(ancho * 0.03f, alto * 0.8f))
        val x = ancho * 0.1f
        caja(origen + Offset(x, alto * 0.28f), Size(ancho * 0.55f, alto * 0.14f), TEXTO_FUERTE)
        caja(origen + Offset(x, alto * 0.54f), Size(ancho * 0.4f, alto * 0.1f), TEXTO_SUAVE)
    }
}

private fun DrawScope.clasicaLista(
    arriba: Float,
    m: Float,
) {
    // Con el mini-gantt de recipientes: barritas finas de colores.
    val alto = altoTarjeta(arriba, m)
    val ancho = size.width - 2 * m
    repeat(3) { i ->
        val origen = Offset(m, arriba + i * (alto + m))
        caja(origen, Size(ancho, alto), TARJETA_CLARA)
        val p = ancho * 0.07f
        caja(origen + Offset(p, alto * 0.1f), Size(ancho * 0.5f, alto * 0.12f), TEXTO_FUERTE)
        val barras = if (i == 1) 2 else 3
        val altoBarra = alto * 0.09f
        repeat(barras) { j ->
            val inicio = (j * 0.18f + i * 0.05f).coerceAtMost(0.5f)
            val largo = 0.5f - j * 0.08f
            caja(
                origen + Offset(p + (ancho - 2 * p) * inicio, alto * (0.38f + j * 0.18f)),
                Size((ancho - 2 * p) * largo, altoBarra),
                COLORES_BASE[(i + j) % COLORES_BASE.size],
            )
        }
    }
}
