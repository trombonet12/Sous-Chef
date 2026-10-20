package app.trombito.souschef.ui.ajustes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.ui.theme.DarkColors
import app.trombito.souschef.ui.theme.LightColors

@Composable
fun AjustesAparienciaScreen(
    onAtras: () -> Unit,
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val ajustes by viewModel.estado.collectAsStateWithLifecycle()
    AjustesApariencia(ajustes, onAtras, viewModel::elegirTema, viewModel::elegirLista)
}

@Composable
internal fun AjustesApariencia(
    ajustes: Ajustes,
    onAtras: () -> Unit,
    onTema: (TemaApp) -> Unit,
    onLista: (DisposicionLista) -> Unit,
) {
    PantallaAjustes(stringResource(R.string.ajustes_apariencia), onAtras) {
        Text(
            stringResource(R.string.ajustes_tema),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        SelectorTema(ajustes.tema, onTema)
        Text(
            stringResource(R.string.ajustes_diseno_listas),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        SelectorLista(ajustes.lista, onLista)
    }
}

private class OpcionTema(
    val tema: TemaApp,
    val titulo: Int,
    val detalle: Int,
)

private val OPCIONES_TEMA =
    listOf(
        OpcionTema(TemaApp.AUTOMATICO, R.string.ajustes_tema_automatico, R.string.ajustes_tema_automatico_detalle),
        OpcionTema(TemaApp.CLARO, R.string.ajustes_tema_claro, R.string.ajustes_tema_claro_detalle),
        OpcionTema(TemaApp.OSCURO, R.string.ajustes_tema_oscuro, R.string.ajustes_tema_oscuro_detalle),
    )

/** Automático, claro u oscuro como tarjetas con una miniatura de la app en ese tema. */
@Composable
fun SelectorTema(
    elegido: TemaApp,
    onElegir: (TemaApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tarjetas =
        OPCIONES_TEMA.map {
            OpcionTarjeta(it.titulo, it.detalle, "tema-${it.tema.name}", it.tema == elegido)
        }
    val borde = MaterialTheme.colorScheme.outlineVariant
    SelectorTarjetas(tarjetas, { onElegir(OPCIONES_TEMA[it].tema) }, modifier) { i, mod ->
        MiniaturaTema(OPCIONES_TEMA[i].tema, borde, mod)
    }
}

/** Esquema de una pantalla de la app con la paleta de cada tema; el automático parte la imagen en diagonal. */
@Composable
private fun MiniaturaTema(
    tema: TemaApp,
    borde: Color,
    modifier: Modifier,
) {
    Canvas(
        modifier
            .aspectRatio(PROPORCION)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borde, RoundedCornerShape(8.dp))
            .clearAndSetSemantics {},
    ) {
        when (tema) {
            TemaApp.CLARO -> {
                pantalla(LightColors)
            }

            TemaApp.OSCURO -> {
                pantalla(DarkColors)
            }

            TemaApp.AUTOMATICO -> {
                pantalla(LightColors)
                val oscuro =
                    Path().apply {
                        moveTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                clipPath(oscuro) { pantalla(DarkColors) }
            }
        }
    }
}

/** Una pantalla de la lista: título, buscador y tres tarjetas con franja. */
private fun DrawScope.pantalla(c: ColorScheme) {
    drawRect(c.background)
    val m = size.width * 0.06f
    caja(Offset(m, m), Size(size.width * 0.4f, size.height * 0.04f), c.onBackground)
    val buscador = size.height * 0.06f
    caja(Offset(m, m * 2 + size.height * 0.04f), Size(size.width - 2 * m, buscador), c.surfaceContainerLow)
    val arriba = m * 3 + size.height * 0.04f + buscador
    val alto = (size.height - arriba - m - 2 * m) / 3
    val ancho = size.width - 2 * m
    val acentos = listOf(c.primary, c.secondary, c.tertiary)
    acentos.forEachIndexed { i, acento ->
        val origen = Offset(m, arriba + i * (alto + m))
        caja(origen, Size(ancho, alto), c.surfaceContainer)
        drawRect(acento, origen + Offset(0f, alto * 0.1f), Size(ancho * 0.03f, alto * 0.8f))
        val x = ancho * 0.1f
        caja(origen + Offset(x, alto * 0.28f), Size(ancho * 0.55f, alto * 0.14f), c.onSurface)
        caja(origen + Offset(x, alto * 0.54f), Size(ancho * 0.4f, alto * 0.1f), c.onSurfaceVariant)
    }
}

private fun DrawScope.caja(
    origen: Offset,
    tamano: Size,
    color: Color,
) = drawRoundRect(color, origen, tamano, CornerRadius(tamano.minDimension * 0.18f))
