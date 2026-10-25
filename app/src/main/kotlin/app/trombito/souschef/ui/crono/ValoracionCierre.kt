package app.trombito.souschef.ui.crono

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.Valoracion
import app.trombito.souschef.ui.theme.CronoPalette

private val ALTO_VALORACION = 48.dp

@StringRes
private fun Valoracion.etiqueta(): Int =
    when (this) {
        Valoracion.REPETIR -> R.string.valoracion_repetir
        Valoracion.AJUSTAR -> R.string.valoracion_ajustar
        Valoracion.NO_REPETIR -> R.string.valoracion_no_repetir
    }

/** La valoración elegida y cómo cambiarla: lo que el cierre y la ventana de deshacer necesitan del selector. */
class ValoracionCierreUi(
    val actual: Valoracion? = null,
    val onValorar: (Valoracion?) -> Unit = {},
)

/**
 * «¿Cómo ha salido?»: tres botones opcionales de selección única (sin estrellas). Pulsar el elegido lo quita. Cada uno
 * es un botón de radio con su estado seleccionado, de 48 dp como mínimo.
 */
@Composable
fun ValoracionCierre(ui: ValoracionCierreUi) {
    val grupo = stringResource(R.string.valoracion_grupo)
    val colores =
        SegmentedButtonDefaults.colors(
            activeContainerColor = CronoPalette.principal,
            activeContentColor = CronoPalette.onPrincipal,
            activeBorderColor = CronoPalette.principal,
            inactiveContainerColor = CronoPalette.carta,
            inactiveContentColor = CronoPalette.texto,
            inactiveBorderColor = CronoPalette.linea,
        )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.valoracion_titulo), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(
            Modifier.fillMaxWidth().semantics { contentDescription = grupo },
        ) {
            Valoracion.entries.forEachIndexed { indice, v ->
                val elegida = v == ui.actual
                SegmentedButton(
                    selected = elegida,
                    onClick = { ui.onValorar(if (elegida) null else v) },
                    shape = SegmentedButtonDefaults.itemShape(indice, Valoracion.entries.size),
                    modifier = Modifier.heightIn(min = ALTO_VALORACION).testTag("valoracion-${v.codigo}"),
                    colors = colores,
                ) {
                    Text(stringResource(v.etiqueta()), maxLines = 2, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
