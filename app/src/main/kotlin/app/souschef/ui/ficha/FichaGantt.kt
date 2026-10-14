package app.souschef.ui.ficha

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.souschef.R
import app.souschef.receta.Carril
import app.souschef.receta.Escalado
import app.souschef.receta.Gantt
import app.souschef.receta.Paso
import app.souschef.receta.Receta
import app.souschef.ui.comun.SeccionPapel
import app.souschef.ui.comun.Tabular
import app.souschef.ui.comun.colorCarril
import app.souschef.ui.theme.FichaPalette

private const val CADA_REJILLA_MIN = 10

/** "El tiempo": eje, una fila por paso con su barra sobre el carril, leyenda y lectura. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TiempoFicha(receta: Receta) {
    val total = receta.totalEfectivo
    if (total <= 0 || receta.pasos.isEmpty()) return
    val marcas = remember(total) { Gantt.marcas(total) }
    SeccionPapel(stringResource(R.string.ficha_sec_tiempo)) {
        EjeTiempos(marcas, total)
        receta.pasos.forEach { FilaGantt(it, receta, total) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            receta.carriles.forEach { LeyendaPapel(it) }
        }
        Text(
            stringResource(R.string.ficha_eje),
            style = MaterialTheme.typography.labelSmall,
            color = FichaPalette.meta,
        )
        receta.lectura?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun EjeTiempos(
    marcas: List<Double>,
    total: Double,
) {
    val medidor = rememberTextMeasurer()
    val estilo =
        MaterialTheme.typography.labelSmall
            .merge(Tabular)
            .copy(color = FichaPalette.meta)
    Canvas(Modifier.fillMaxWidth().height(16.dp).clearAndSetSemantics {}) {
        marcas.forEach { m ->
            val texto = medidor.measure(Escalado.minutos(m), estilo)
            val x = (m / total * size.width).toFloat() - texto.size.width / 2f
            drawText(texto, topLeft = Offset(x.coerceIn(0f, (size.width - texto.size.width).coerceAtLeast(0f)), 0f))
        }
    }
}

@Composable
private fun FilaGantt(
    paso: Paso,
    receta: Receta,
    total: Double,
) {
    val carril = receta.carril(paso.carril)
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(paso.t, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.ficha_duracion, Escalado.minutos(paso.min)),
                style = MaterialTheme.typography.labelLarge.merge(Tabular),
            )
        }
        Text(
            subLinea(paso, carril),
            style = MaterialTheme.typography.labelSmall,
            color = FichaPalette.meta,
        )
        BarraGantt(paso, carril, total)
    }
}

@Composable
private fun subLinea(
    paso: Paso,
    carril: Carril?,
): String {
    val rango = stringResource(R.string.ficha_min_rango, Escalado.minutos(paso.off), Escalado.minutos(paso.fin))
    val fuego =
        paso.f?.let { stringResource(R.string.ficha_fuego_bajo, it) } ?: stringResource(R.string.ficha_sin_fuego_bajo)
    return listOfNotNull(carril?.nombre?.uppercase(), rango, fuego, paso.tapa?.uppercase()).joinToString(" · ")
}

@Composable
private fun BarraGantt(
    paso: Paso,
    carril: Carril?,
    total: Double,
) {
    val color = colorCarril(carril?.color ?: "")
    val fondo = FichaPalette.papel2
    val rejilla = FichaPalette.linea
    val borde = FichaPalette.tinta2
    val (izq, ancho) = Gantt.barra(paso, total, anchoMinimo = 0.01)
    Canvas(Modifier.fillMaxWidth().height(14.dp).clearAndSetSemantics {}) {
        drawRoundRect(fondo, size = size, cornerRadius = CornerRadius(3.dp.toPx()))
        generateSequence(CADA_REJILLA_MIN.toDouble()) { it + CADA_REJILLA_MIN }.takeWhile { it < total }.forEach {
            val x = (it / total * size.width).toFloat()
            drawLine(rejilla, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        }
        val topLeft = Offset(izq * size.width, 1.dp.toPx())
        val tamano = Size(ancho * size.width, size.height - 2.dp.toPx())
        drawRoundRect(color, topLeft, tamano, CornerRadius(3.dp.toPx()))
        drawRoundRect(borde, topLeft, tamano, CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx()), alpha = 0.6f)
    }
}

@Composable
private fun LeyendaPapel(carril: Carril) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(colorCarril(carril.color), CircleShape))
        Text(carril.nombre, style = MaterialTheme.typography.labelSmall, color = FichaPalette.tinta2)
    }
}
