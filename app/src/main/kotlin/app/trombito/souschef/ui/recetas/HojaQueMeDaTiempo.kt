package app.trombito.souschef.ui.recetas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.ui.ficha.Servir
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.Espacio

/** Minutos que mueven los botones de la hora de servir. */
const val PASO_SERVIR_MIN = 15

/** Lo que se puede hacer con «¿Qué me da tiempo?»; lo implementa el ViewModel. */
class AccionesTiempo(
    val activar: () -> Unit = {},
    val salir: () -> Unit = {},
    val mover: (Int) -> Unit = {},
    val soloLlego: (Boolean) -> Unit = {},
)

/** Chip junto al buscador: apagado abre la hoja y enciende el modo; encendido muestra la hora y una ✕ para salir. */
@Composable
internal fun ChipTiempo(
    estado: RecetasUiState,
    onAbrir: () -> Unit,
    onSalir: () -> Unit,
) {
    val modo = estado.tiempo
    if (modo == null) {
        AssistChip(
            onClick = onAbrir,
            label = { Text(stringResource(R.string.que_tiempo_chip)) },
            leadingIcon = { Icon(Icono.Reloj.vector, contentDescription = null) },
            modifier = Modifier.testTag("chip-tiempo"),
        )
    } else {
        val hora = Servir.formato(modo.servirMin)
        val abrir = stringResource(R.string.que_tiempo_chip_abrir, hora)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = true,
                onClick = onAbrir,
                label = { Text(stringResource(R.string.que_tiempo_chip_activo, hora)) },
                leadingIcon = { Icon(Icono.Reloj.vector, contentDescription = null) },
                modifier = Modifier.testTag("chip-tiempo").semantics { contentDescription = abrir },
            )
            IconButton(onClick = onSalir, modifier = Modifier.testTag("chip-tiempo-quitar")) {
                Icon(Icono.Cerrar.vector, contentDescription = stringResource(R.string.que_tiempo_salir))
            }
        }
    }
}

/** Hoja inferior de «¿Qué me da tiempo?»: la hora de servir (±15 min) y «Solo las que llego». Aplica al momento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaQueMeDaTiempo(
    estado: RecetasUiState,
    acciones: AccionesTiempo,
    onCerrar: () -> Unit,
) {
    val modo = estado.tiempo ?: return
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("hoja-tiempo"),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Espacio.l),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            Text(
                stringResource(R.string.que_tiempo_titulo),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.que_tiempo_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectorServir(modo.servirMin, acciones.mover)
            if (estado.tiempoVencido) {
                Text(
                    stringResource(R.string.que_tiempo_vencido),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("tiempo-vencido"),
                )
            }
            InterruptorSoloLlego(modo.soloLlego, acciones.soloLlego)
            Button(onClick = onCerrar, modifier = Modifier.fillMaxWidth().padding(bottom = Espacio.m)) {
                Text(pluralStringResource(R.plurals.filtros_ver, estado.tarjetas.size, estado.tarjetas.size))
            }
        }
    }
}

@Composable
private fun SelectorServir(
    servirMin: Int,
    onMover: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { onMover(-PASO_SERVIR_MIN) }, modifier = Modifier.testTag("servir-menos")) {
            Icon(Icono.Menos.vector, contentDescription = stringResource(R.string.que_tiempo_servir_menos))
        }
        Text(
            stringResource(R.string.que_tiempo_servir, Servir.formato(servirMin)),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).testTag("servir-hora"),
        )
        IconButton(onClick = { onMover(PASO_SERVIR_MIN) }, modifier = Modifier.testTag("servir-mas")) {
            Icon(Icono.Anadir.vector, contentDescription = stringResource(R.string.que_tiempo_servir_mas))
        }
    }
}

@Composable
private fun InterruptorSoloLlego(
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = MINIMO_TACTIL)
            .testTag("solo-llego")
            .toggleable(value = activo, role = Role.Switch, onValueChange = onCambio),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Text(
            stringResource(R.string.que_tiempo_solo),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = activo, onCheckedChange = null)
    }
}

private val MINIMO_TACTIL = 48.dp
