package app.trombito.souschef.ui.ficha

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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Gantt
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.LocalCocina
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.comun.colorCarril
import app.trombito.souschef.ui.comun.nivel
import app.trombito.souschef.ui.theme.FichaPalette
import app.trombito.souschef.ui.theme.contraste

private const val CADA_REJILLA_MIN = 10

/** Colores del diagrama: la ficha lo pinta en papel y el cronómetro en oscuro. */
data class PaletaDiagrama(
    val meta: Color,
    val leyenda: Color,
    val fondoBarra: Color,
    val rejilla: Color,
    val borde: Color,
    val ahora: Color,
    val colorDe: (Carril?) -> Color,
)

/** Diagrama sobre la ficha: claro u oscuro según el tema, con las barras aclaradas si el papel es oscuro. */
val PaletaPapel: PaletaDiagrama
    @Composable
    @ReadOnlyComposable
    get() {
        val ficha = FichaPalette
        return PaletaDiagrama(
            meta = ficha.meta,
            leyenda = ficha.tinta2,
            fondoBarra = ficha.papel2,
            rejilla = ficha.linea,
            borde = ficha.tinta2,
            ahora = ficha.tinta,
            colorDe = { colorBarraSobre(colorCarril(it?.color ?: ""), ficha.papel2) },
        )
    }

private const val CONTRASTE_BARRA = 3f
private const val PASOS_ACLARADO = 20
private val CLARO_BARRA = Color(0xFFF4F1E8)

/**
 * Color de la barra de un carril sobre [fondo]: en papel claro es el del carril; en papel oscuro se aclara hasta llegar
 * a 3:1 (elemento gráfico) para que los carriles oscuros (violeta, marrón) no se pierdan.
 */
fun colorBarraSobre(
    color: Color,
    fondo: Color,
): Color =
    if (fondo.luminance() >= LUMINANCIA_CLARA) {
        color
    } else {
        (0..PASOS_ACLARADO)
            .map { lerp(color, CLARO_BARRA, it / PASOS_ACLARADO.toFloat()) }
            .firstOrNull { contraste(it, fondo) >= CONTRASTE_BARRA } ?: CLARO_BARRA
    }

private const val LUMINANCIA_CLARA = 0.5f

/**
 * Eje, una fila por paso con su barra sobre el carril y leyenda. Con [ahoraMin] (minuto del plan, 0..total) pinta una
 * línea vertical en cada barra para ver por dónde va la cocina.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagramaTiempos(
    receta: Receta,
    paleta: PaletaDiagrama,
    modifier: Modifier = Modifier,
    ahoraMin: Double? = null,
) {
    val total = receta.totalEfectivo
    if (total <= 0 || receta.pasos.isEmpty()) return
    val marcas = remember(total) { Gantt.marcas(total) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        EjeTiempos(marcas, total, paleta)
        receta.pasos.forEach { FilaGantt(it, receta, total, paleta, ahoraMin) }
        FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            receta.carriles.forEach { Leyenda(it, paleta) }
        }
        Text(
            stringResource(R.string.ficha_eje),
            style = MaterialTheme.typography.labelSmall,
            color = paleta.meta,
        )
    }
}

@Composable
internal fun EjeTiempos(
    marcas: List<Double>,
    total: Double,
    paleta: PaletaDiagrama,
    modifier: Modifier = Modifier,
) {
    val medidor = rememberTextMeasurer()
    val estilo =
        MaterialTheme.typography.labelSmall
            .merge(Tabular)
            .copy(color = paleta.meta)
    Canvas(modifier.fillMaxWidth().height(16.dp).clearAndSetSemantics {}) {
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
    paleta: PaletaDiagrama,
    ahoraMin: Double?,
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
            color = paleta.meta,
        )
        BarraGantt(paso, carril, total, paleta, ahoraMin)
    }
}

@Composable
private fun subLinea(
    paso: Paso,
    carril: Carril?,
): String {
    val rango = stringResource(R.string.ficha_min_rango, Escalado.minutos(paso.off), Escalado.minutos(paso.fin))
    val cocina = LocalCocina.current
    val fuego =
        paso.f?.let { stringResource(R.string.ficha_fuego_bajo, cocina.nivel(it)) }
            ?: stringResource(R.string.ficha_sin_fuego_bajo)
    return listOfNotNull(carril?.nombre?.uppercase(), rango, fuego, paso.tapa?.uppercase()).joinToString(" · ")
}

@Composable
private fun BarraGantt(
    paso: Paso,
    carril: Carril?,
    total: Double,
    paleta: PaletaDiagrama,
    ahoraMin: Double?,
) {
    val color = paleta.colorDe(carril)
    val fondo = paleta.fondoBarra
    val rejilla = paleta.rejilla
    val borde = paleta.borde
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
        ahoraMin?.let {
            val x = (it / total * size.width).toFloat()
            drawLine(paleta.ahora, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
private fun Leyenda(
    carril: Carril,
    paleta: PaletaDiagrama,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(paleta.colorDe(carril), CircleShape))
        Text(carril.nombre, style = MaterialTheme.typography.labelSmall, color = paleta.leyenda)
    }
}
