package app.trombito.souschef.ui.recetas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.colorCarril

private const val ANCHO_MINIMO = 0.02

/** Mini-gantt de la tarjeta: una fila de 5 dp por carril y leyenda. Es decorativo para TalkBack. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Huella(
    receta: Receta,
    modifier: Modifier = Modifier,
) {
    val total = receta.totalEfectivo
    if (total > 0 && receta.carriles.isNotEmpty()) {
        Column(modifier.clearAndSetSemantics {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            receta.carriles.forEach { c ->
                FilaHuella(c, receta.pasos.filter { it.carril == c.id }, total)
            }
            FlowRow(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                receta.carriles.forEach { LeyendaCarril(it) }
            }
        }
    }
}

@Composable
private fun FilaHuella(
    carril: Carril,
    pasos: List<Paso>,
    total: Double,
) {
    val color = colorCarril(carril.color)
    val pista = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(5.dp)) {
        drawRoundRect(pista, size = size, cornerRadius = CornerRadius(2.5.dp.toPx()), alpha = 0.5f)
        pasos.forEach { p ->
            val izq = (p.off / total).coerceIn(0.0, 1.0)
            val ancho = (p.min / total).coerceAtLeast(ANCHO_MINIMO).coerceAtMost(1.0 - izq)
            drawRoundRect(
                color,
                topLeft = Offset((izq * size.width).toFloat(), 0f),
                size = Size((ancho * size.width).toFloat(), size.height),
                cornerRadius = CornerRadius(2.5.dp.toPx()),
            )
        }
    }
}

@Composable
private fun LeyendaCarril(carril: Carril) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(colorCarril(carril.color), CircleShape))
        Text(
            carril.nombre,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
