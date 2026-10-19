package app.souschef.ui.ajustes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import app.souschef.ui.recetas.ColorBase
import app.souschef.ui.recetas.FamiliaBase
import app.souschef.ui.recetas.colorBase

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

/** Colores de la miniatura, tomados del tema en uso (el que se ve en la app) para que la muestra coincida. */
private class PaletaLista(
    val papel: Color,
    val borde: Color,
    val tarjeta: Color,
    val textoFuerte: Color,
    val textoSuave: Color,
    val familias: List<ColorBase>,
)

private val FAMILIAS_MUESTRA = listOf(FamiliaBase.PASTA, FamiliaBase.PESCADO, FamiliaBase.PATATA)

@Composable
private fun paletaLista(): PaletaLista {
    val c = MaterialTheme.colorScheme
    return PaletaLista(
        papel = c.background,
        borde = c.outlineVariant,
        tarjeta = c.surfaceContainerLow,
        textoFuerte = c.onSurface,
        textoSuave = c.onSurfaceVariant,
        familias = FAMILIAS_MUESTRA.map { colorBase(it) },
    )
}

/**
 * Los tres diseños de la lista de recetas como tarjetas con una miniatura (un esquema con los colores del tema en
 * uso), con el mismo aspecto que el selector del cronómetro.
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
    val paleta = paletaLista()
    SelectorTarjetas(tarjetas, { onElegir(OPCIONES_LISTA[it].lista) }, modifier) { i, mod ->
        MiniaturaLista(OPCIONES_LISTA[i].lista, paleta, mod)
    }
}

/** Esquema de la lista de recetas sobre el papel, en proporción de móvil. */
@Composable
private fun MiniaturaLista(
    lista: DisposicionLista,
    paleta: PaletaLista,
    modifier: Modifier,
) {
    Canvas(
        modifier
            .aspectRatio(PROPORCION)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, paleta.borde, RoundedCornerShape(8.dp))
            .clearAndSetSemantics {},
    ) {
        drawRect(paleta.papel)
        val m = size.width * 0.06f
        // Cabecera: título y buscador.
        caja(Offset(m, m), Size(size.width * 0.4f, size.height * 0.04f), paleta.textoFuerte)
        val buscador = size.height * 0.06f
        caja(Offset(m, m * 2 + size.height * 0.04f), Size(size.width - 2 * m, buscador), paleta.tarjeta)
        val arriba = m * 3 + size.height * 0.04f + buscador
        when (lista) {
            DisposicionLista.TINTE -> tinte(paleta, arriba, m)
            DisposicionLista.FRANJA -> franja(paleta, arriba, m)
            DisposicionLista.CLASICA -> clasicaLista(paleta, arriba, m)
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
    paleta: PaletaLista,
    arriba: Float,
    m: Float,
) {
    val alto = altoTarjeta(arriba, m)
    val ancho = size.width - 2 * m
    paleta.familias.forEachIndexed { i, familia ->
        val origen = Offset(m, arriba + i * (alto + m))
        caja(origen, Size(ancho, alto), familia.tinte)
        val lado = alto * 0.6f
        val p = (alto - lado) / 2
        // Bloque del tiempo a la izquierda y dos líneas de texto.
        caja(origen + Offset(p, p), Size(lado, lado), familia.acento.copy(alpha = 0.55f))
        caja(origen + Offset(p * 2 + lado, alto * 0.3f), Size(ancho * 0.45f, alto * 0.14f), paleta.textoFuerte)
        caja(origen + Offset(p * 2 + lado, alto * 0.56f), Size(ancho * 0.3f, alto * 0.1f), paleta.textoSuave)
    }
}

private fun DrawScope.franja(
    paleta: PaletaLista,
    arriba: Float,
    m: Float,
) {
    val alto = altoTarjeta(arriba, m)
    val ancho = size.width - 2 * m
    paleta.familias.forEachIndexed { i, familia ->
        val origen = Offset(m, arriba + i * (alto + m))
        caja(origen, Size(ancho, alto), paleta.tarjeta)
        drawRect(familia.acento, origen + Offset(0f, alto * 0.1f), Size(ancho * 0.03f, alto * 0.8f))
        val x = ancho * 0.1f
        caja(origen + Offset(x, alto * 0.28f), Size(ancho * 0.55f, alto * 0.14f), paleta.textoFuerte)
        caja(origen + Offset(x, alto * 0.54f), Size(ancho * 0.4f, alto * 0.1f), paleta.textoSuave)
    }
}

private fun DrawScope.clasicaLista(
    paleta: PaletaLista,
    arriba: Float,
    m: Float,
) {
    // Con el mini-gantt de recipientes: barritas finas de colores.
    val alto = altoTarjeta(arriba, m)
    val ancho = size.width - 2 * m
    repeat(3) { i ->
        val origen = Offset(m, arriba + i * (alto + m))
        caja(origen, Size(ancho, alto), paleta.tarjeta)
        val p = ancho * 0.07f
        caja(origen + Offset(p, alto * 0.1f), Size(ancho * 0.5f, alto * 0.12f), paleta.textoFuerte)
        val barras = if (i == 1) 2 else 3
        val altoBarra = alto * 0.09f
        repeat(barras) { j ->
            val inicio = (j * 0.18f + i * 0.05f).coerceAtMost(0.5f)
            val largo = 0.5f - j * 0.08f
            caja(
                origen + Offset(p + (ancho - 2 * p) * inicio, alto * (0.38f + j * 0.18f)),
                Size((ancho - 2 * p) * largo, altoBarra),
                paleta.familias[(i + j) % paleta.familias.size].acento,
            )
        }
    }
}
