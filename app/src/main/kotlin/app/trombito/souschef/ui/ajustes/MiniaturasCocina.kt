package app.trombito.souschef.ui.ajustes

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.trombito.souschef.receta.TipoCocina
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---- Miniaturas: una placa vista desde arriba, con los colores del tema en uso ----

private class PaletaPlaca(
    val fondo: Color,
    val placa: Color,
    val calor: Color,
    val linea: Color,
    val borde: Color,
)

@Composable
internal fun MiniaturaTipo(
    tipo: TipoCocina,
    modifier: Modifier,
) {
    val c = MaterialTheme.colorScheme
    val paleta = PaletaPlaca(c.surfaceContainerLow, c.onSurface, c.primary, c.onSurfaceVariant, c.outlineVariant)
    Canvas(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, paleta.borde, RoundedCornerShape(8.dp))
            .clearAndSetSemantics {},
    ) {
        drawRect(paleta.fondo)
        when (tipo) {
            TipoCocina.VITRO -> vitro(paleta)
            TipoCocina.INDUCCION -> induccion(paleta)
            TipoCocina.GAS -> gas(paleta)
        }
    }
}

/** Centros de los cuatro fuegos, en las esquinas de la placa. */
private fun DrawScope.centros(): List<Offset> {
    val a = size.width * 0.28f
    val b = size.width * 0.72f
    return listOf(Offset(a, a), Offset(b, a), Offset(a, b), Offset(b, b))
}

private fun DrawScope.placa(paleta: PaletaPlaca) {
    val m = size.width * 0.08f
    drawRoundRect(
        paleta.placa.copy(alpha = 0.9f),
        Offset(m, m),
        Size(size.width - 2 * m, size.height - 2 * m),
        CornerRadius(m),
    )
}

/** Cristal oscuro con cuatro zonas redondas; dos están encendidas. */
private fun DrawScope.vitro(paleta: PaletaPlaca) {
    placa(paleta)
    val r = size.width * 0.14f
    centros().forEachIndexed { i, centro ->
        if (i % 2 == 0) drawCircle(paleta.calor.copy(alpha = 0.45f), r, centro)
        drawCircle(paleta.calor, r, centro, style = Stroke(size.width * 0.025f))
    }
}

/** Cristal liso con dos zonas de anillos concéntricos y la franja de control abajo. */
private fun DrawScope.induccion(paleta: PaletaPlaca) {
    placa(paleta)
    val ancho = size.width * 0.02f
    val r = size.width * 0.17f
    val zonas = listOf(Offset(size.width * 0.32f, size.height * 0.4f), Offset(size.width * 0.68f, size.height * 0.4f))
    zonas.forEach { centro ->
        drawCircle(paleta.calor, r, centro, style = Stroke(ancho))
        drawCircle(paleta.calor.copy(alpha = 0.6f), r * 0.62f, centro, style = Stroke(ancho))
        drawCircle(paleta.calor.copy(alpha = 0.35f), r * 0.28f, centro)
    }
    repeat(3) { i ->
        drawCircle(paleta.fondo, size.width * 0.03f, Offset(size.width * (0.38f + i * 0.12f), size.height * 0.78f))
    }
}

/** Parrilla con cuatro quemadores: aro, centro y llamas alrededor. */
private fun DrawScope.gas(paleta: PaletaPlaca) {
    val m = size.width * 0.08f
    val fino = size.width * 0.02f
    drawRoundRect(
        paleta.linea,
        Offset(m, m),
        Size(size.width - 2 * m, size.height - 2 * m),
        CornerRadius(m),
        style = Stroke(fino),
    )
    drawLine(paleta.linea, Offset(size.width / 2, m), Offset(size.width / 2, size.height - m), fino)
    drawLine(paleta.linea, Offset(m, size.height / 2), Offset(size.width - m, size.height / 2), fino)
    val r = size.width * 0.1f
    centros().forEach { centro ->
        drawCircle(paleta.placa, r, centro, style = Stroke(fino))
        drawCircle(paleta.placa, r * 0.4f, centro)
        repeat(LLAMAS) { k ->
            val ang = 2 * PI * k / LLAMAS
            val dentro = Offset(centro.x + cos(ang).toFloat() * r * 1.25f, centro.y + sin(ang).toFloat() * r * 1.25f)
            val fuera = Offset(centro.x + cos(ang).toFloat() * r * 1.7f, centro.y + sin(ang).toFloat() * r * 1.7f)
            drawLine(paleta.calor, dentro, fuera, fino)
        }
    }
}

private const val LLAMAS = 8
