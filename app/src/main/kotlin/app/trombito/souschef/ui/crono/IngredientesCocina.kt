package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.theme.CronoPalette

private const val PESO_CANTIDAD = 0.36f
private const val PESO_NOMBRE = 0.64f

/** Clave de la casilla de un ingrediente: su zona y su posición en ella (la receta no cambia durante la cocinada). */
fun claveIngrediente(
    zona: Int,
    posicion: Int,
): String = "$zona:$posicion"

/** La hoja «Ingredientes» de la cocina en marcha; no pinta nada si aún no hay receta o estado. */
@Composable
internal fun HojaIngredientesCocina(
    ui: CronoUi,
    viewModel: CronoViewModel,
    onCerrar: () -> Unit,
) {
    val receta = ui.receta ?: return
    val estado = ui.estado ?: return
    HojaIngredientes(receta, estado.raciones, ui.ingredientesMarcados, viewModel::marcarIngrediente, onCerrar)
}

/**
 * Hoja modal «Ingredientes» de la cocina: la lista «Por zona» de la ficha, escalada a las [raciones] de la cocinada,
 * con una casilla por ingrediente. Las casillas ([marcados]) las guarda el ViewModel mientras dura la cocinada, no
 * Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HojaIngredientes(
    receta: Receta,
    raciones: Int,
    marcados: Set<String>,
    onMarcar: (clave: String, marcada: Boolean) -> Unit,
    onCerrar: () -> Unit,
) {
    val f = Escalado.factor(raciones, receta.raciones)
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CronoPalette.carta,
        contentColor = CronoPalette.texto,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("hoja-ingredientes-cocina"),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                stringResource(R.string.crono_ingredientes),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                pluralStringResource(R.plurals.crono_ingredientes_raciones, raciones, raciones),
                style = MaterialTheme.typography.bodyMedium,
                color = CronoPalette.apagado,
            )
            receta.grupos.forEachIndexed { zona, grupo ->
                Text(
                    grupo.n,
                    Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = CronoPalette.acento,
                )
                grupo.items.forEachIndexed { posicion, item ->
                    val clave = claveIngrediente(zona, posicion)
                    FilaIngredienteCocina(item, f, clave in marcados) { onMarcar(clave, it) }
                }
            }
            Text(
                stringResource(R.string.crono_ingredientes_nota),
                Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = CronoPalette.tenue,
            )
        }
    }
}

@Composable
private fun FilaIngredienteCocina(
    item: Ingrediente,
    f: Double,
    marcada: Boolean,
    onCambio: (Boolean) -> Unit,
) {
    val color = if (marcada) CronoPalette.tenue else CronoPalette.texto
    val tachado = if (marcada) TextDecoration.LineThrough else TextDecoration.None
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = marcada, role = Role.Checkbox, onValueChange = onCambio),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = marcada,
            onCheckedChange = null,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = CronoPalette.ok,
                    checkmarkColor = CronoPalette.onPrincipal,
                    uncheckedColor = CronoPalette.tenue,
                ),
        )
        Text(
            Escalado.cantidad(item, f),
            Modifier.weight(PESO_CANTIDAD).padding(start = 8.dp, end = 12.dp),
            style = MaterialTheme.typography.bodyMedium.merge(Tabular),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            color = color,
            textDecoration = tachado,
        )
        Text(
            item.n,
            Modifier.weight(PESO_NOMBRE),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            textDecoration = tachado,
        )
    }
}
