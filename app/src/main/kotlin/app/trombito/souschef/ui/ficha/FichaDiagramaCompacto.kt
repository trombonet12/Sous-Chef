package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.trombito.souschef.R
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Gantt
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.FichaPalette

private val ANCHO_ROTULO = 72.dp
private val DP_POR_MINUTO = 10.dp
private const val CADA_REJILLA_MIN = 10

/**
 * Resumen del tiempo: una fila por recipiente (carril) con sus tramos en su color sobre un eje común. El detalle paso
 * a paso queda en [DialogoDiagramaFicha].
 */
@Composable
fun DiagramaCompacto(
    receta: Receta,
    modifier: Modifier = Modifier,
) {
    val total = receta.totalEfectivo
    if (total <= 0 || receta.pasos.isEmpty()) return
    val marcas = remember(total) { Gantt.marcas(total) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(ANCHO_ROTULO))
            EjeTiempos(marcas, total, PaletaPapel, Modifier.weight(1f))
        }
        receta.carriles.forEach { carril ->
            val pasos = receta.pasos.filter { it.carril == carril.id }
            if (pasos.isNotEmpty()) FilaCarril(carril, pasos, total)
        }
        Text(
            stringResource(R.string.ficha_eje),
            style = MaterialTheme.typography.labelSmall,
            color = PaletaPapel.meta,
        )
    }
}

@Composable
private fun FilaCarril(
    carril: Carril,
    pasos: List<Paso>,
    total: Double,
) {
    val descripcion =
        stringResource(
            R.string.ficha_carril_resumen,
            carril.nombre,
            Escalado.minutos(pasos.minOf { it.off }),
            Escalado.minutos(pasos.maxOf { it.fin }),
        )
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            carril.nombre,
            Modifier.width(ANCHO_ROTULO),
            style = MaterialTheme.typography.labelSmall,
            color = PaletaPapel.leyenda,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TramosCarril(carril, pasos, total, Modifier.weight(1f))
    }
}

@Composable
private fun TramosCarril(
    carril: Carril,
    pasos: List<Paso>,
    total: Double,
    modifier: Modifier,
) {
    val paleta = PaletaPapel
    val color = paleta.colorDe(carril)
    Canvas(modifier.height(20.dp).clearAndSetSemantics {}) {
        val radio = CornerRadius(3.dp.toPx())
        drawRoundRect(paleta.fondoBarra, size = size, cornerRadius = radio)
        generateSequence(CADA_REJILLA_MIN.toDouble()) { it + CADA_REJILLA_MIN }.takeWhile { it < total }.forEach {
            val x = (it / total * size.width).toFloat()
            drawLine(paleta.rejilla, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        }
        pasos.forEach { paso ->
            val (izq, ancho) = Gantt.barra(paso, total, anchoMinimo = 0.01)
            val origen = Offset(izq * size.width, 2.dp.toPx())
            drawRoundRect(color, origen, Size(ancho * size.width, size.height - 4.dp.toPx()), radio)
        }
    }
}

/** El diagrama de pasos de hoy, a pantalla completa y con scroll en los dos ejes, sobre papel. */
@Composable
fun DialogoDiagramaFicha(
    receta: Receta,
    onCerrar: () -> Unit,
) {
    Dialog(onCerrar, DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = FichaPalette.papel, contentColor = FichaPalette.tinta) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.ficha_diagrama_titulo),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onCerrar) {
                        Icon(Icono.Cerrar.vector, stringResource(R.string.ficha_diagrama_cerrar))
                    }
                }
                // El ancho se mide fuera del scroll: dentro de horizontalScroll el máximo es infinito.
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val ancho = maxOf(maxWidth, DP_POR_MINUTO * receta.totalEfectivo.toFloat() + 32.dp)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        DiagramaTiempos(receta, PaletaPapel, Modifier.width(ancho).padding(16.dp))
                    }
                }
            }
        }
    }
}
