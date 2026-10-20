package app.trombito.souschef.ui.crono.guia

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.ui.theme.CronoPalette

private val VELO = Color.Black.copy(alpha = 0.78f)
private val MARGEN_HUECO = 6.dp
private val RADIO_HUECO = 14.dp

private class TextoPunto(
    @StringRes val titulo: Int,
    @StringRes val texto: Int,
)

private fun textoDe(id: String): TextoPunto =
    when (id) {
        GuiaIds.CARRIL -> TextoPunto(R.string.guia_carril_titulo, R.string.guia_carril_texto)
        GuiaIds.HECHO -> TextoPunto(R.string.guia_hecho_titulo, R.string.guia_hecho_texto)
        GuiaIds.PRORROGA -> TextoPunto(R.string.guia_prorroga_titulo, R.string.guia_prorroga_texto)
        GuiaIds.DESHACER -> TextoPunto(R.string.guia_deshacer_titulo, R.string.guia_deshacer_texto)
        GuiaIds.MICRO -> TextoPunto(R.string.guia_micro_titulo, R.string.guia_micro_texto)
        else -> TextoPunto(R.string.guia_menu_titulo, R.string.guia_menu_texto)
    }

/** Lo que dice el globo del punto actual. */
private class ContenidoGlobo(
    val textos: TextoPunto,
    val progreso: Pair<Int, Int>,
    val avisoReloj: Boolean,
    val ultimo: Boolean,
)

/** Acciones del globo; agrupadas para no pasar tantos callbacks sueltos. */
private class MandosGuia(
    val siguiente: () -> Unit,
    val saltar: () -> Unit,
)

/**
 * Guía de la primera cocinada: velo con un hueco sobre cada control real de [estado], uno tras otro, con un globo que
 * lo explica. Los puntos que la disposición no tiene se saltan. Al acabar o saltarla llama a [alTerminar]. No pausa el
 * cronómetro y tocar fuera no hace nada.
 */
@Composable
fun GuiaCocina(
    estado: GuiaEstado,
    alTerminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var desde by rememberSaveable { mutableIntStateOf(0) }
    val visibles = estado.ids()
    val indice = primerPuntoDesde(visibles, desde)
    val terminar by rememberUpdatedState(alTerminar)
    // Sin ningún punto aún (primer fotograma) se espera; si ya se avanzó y no queda ninguno, se da por acabada.
    LaunchedEffect(indice, desde) { if (indice == null && desde > 0) terminar() }
    if (indice == null) return
    val hueco = estado.limite(GuiaIds.ORDEN[indice]) ?: return
    BackHandler { terminar() }
    val ultimo = !hayMasPuntos(visibles, indice)
    val numero = numeroDePunto(visibles, indice)
    val mandos = MandosGuia(siguiente = { if (ultimo) terminar() else desde = indice + 1 }, saltar = { terminar() })
    SuperposicionGuia(
        hueco = hueco,
        contenido = ContenidoGlobo(textoDe(GuiaIds.ORDEN[indice]), numero to visibles.size, numero == 1, ultimo),
        mandos = mandos,
        modifier = modifier,
    )
}

@Composable
private fun SuperposicionGuia(
    hueco: Rect,
    contenido: ContenidoGlobo,
    mandos: MandosGuia,
    modifier: Modifier = Modifier,
) {
    var origen by remember { mutableStateOf(Offset.Zero) }
    var tamano by remember { mutableStateOf(IntSize.Zero) }
    val densidad = LocalDensity.current
    val margen = with(densidad) { MARGEN_HUECO.toPx() }
    val radio = with(densidad) { RADIO_HUECO.toPx() }
    // El globo va en la mitad con más sitio libre: arriba si el hueco está en la parte baja.
    val arriba = tamano.height > 0 && (hueco.center.y - origen.y) > tamano.height / 2f
    Box(
        modifier
            .fillMaxSize()
            .testTag("guia")
            .onGloballyPositioned {
                origen = it.positionInRoot()
                tamano = it.size
            }.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                drawRect(VELO)
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(hueco.left - origen.x - margen, hueco.top - origen.y - margen),
                    size = Size(hueco.width + 2 * margen, hueco.height + 2 * margen),
                    cornerRadius = CornerRadius(radio),
                    blendMode = BlendMode.Clear,
                )
            }.pointerInput(Unit) { detectTapGestures { } },
    ) {
        val lado = if (arriba) Alignment.TopCenter else Alignment.BottomCenter
        Globo(contenido, mandos, Modifier.align(lado))
    }
}

@Composable
private fun Globo(
    contenido: ContenidoGlobo,
    mandos: MandosGuia,
    modifier: Modifier = Modifier,
) {
    val textos = contenido.textos
    val titulo = stringResource(textos.titulo)
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CronoPalette.carta2)
            .padding(16.dp)
            .testTag("guia-globo")
            .semantics {
                paneTitle = titulo
                liveRegion = LiveRegionMode.Polite
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.guia_progreso, contenido.progreso.first, contenido.progreso.second),
            style = MaterialTheme.typography.labelMedium,
            color = CronoPalette.tenue,
        )
        Text(
            titulo,
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CronoPalette.texto,
        )
        Text(stringResource(textos.texto), style = MaterialTheme.typography.bodyLarge, color = CronoPalette.texto)
        if (contenido.avisoReloj) {
            Text(
                stringResource(R.string.guia_aviso_reloj),
                style = MaterialTheme.typography.bodyMedium,
                color = CronoPalette.acento,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = mandos.saltar, Modifier.heightIn(min = 48.dp).testTag("guia-saltar")) {
                Text(stringResource(R.string.guia_saltar), color = CronoPalette.apagado)
            }
            Box(Modifier.weight(1f))
            Button(
                onClick = mandos.siguiente,
                modifier = Modifier.heightIn(min = 48.dp).testTag("guia-siguiente"),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = CronoPalette.principal,
                        contentColor = CronoPalette.onPrincipal,
                    ),
            ) {
                Text(
                    stringResource(if (contenido.ultimo) R.string.guia_a_cocinar else R.string.guia_siguiente),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
