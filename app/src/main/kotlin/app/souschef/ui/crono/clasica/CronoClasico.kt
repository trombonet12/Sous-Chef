package app.souschef.ui.crono.clasica

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.souschef.ui.crono.AccionesCarril
import app.souschef.ui.crono.DatosCrono

/**
 * Disposición CLÁSICA, el cronómetro de antes de la v0.6: plan plegable, pestañas de recipientes, un panel por
 * recipiente en un pager con scroll vertical y la fila de mandos del recipiente en foco.
 */
@Composable
internal fun CronoClasico(
    datos: DatosCrono,
    tecnicas: Map<String, String>,
    acciones: AccionesCarril,
    onTecnica: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val indiceFoco = datos.carriles.indexOfFirst { it.id == datos.estado.foco }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = indiceFoco) { datos.carriles.size }
    val focoActual by rememberUpdatedState(datos.estado.foco)
    LaunchedEffect(indiceFoco) { if (pager.currentPage != indiceFoco) pager.animateScrollToPage(indiceFoco) }
    LaunchedEffect(pager, datos.carriles) {
        snapshotFlow { pager.settledPage }.collect { pagina ->
            datos.carriles
                .getOrNull(pagina)
                ?.takeIf { it.id != focoActual }
                ?.let { acciones.enfocar(it.id) }
        }
    }
    Column(modifier.testTag("crono-clasico")) {
        PlanPlegable(datos, acciones)
        PestanasCarriles(datos, indiceFoco, acciones.enfocar)
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), key = { datos.carriles[it].id }) { pagina ->
            PanelCarril(datos, datos.carriles[pagina], tecnicas, onTecnica, acciones.enfocar)
        }
        datos.carriles.getOrNull(indiceFoco)?.let { MandosCarril(datos, it, acciones) }
    }
}
