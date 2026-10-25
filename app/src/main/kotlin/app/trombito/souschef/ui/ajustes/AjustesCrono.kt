package app.trombito.souschef.ui.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.ui.crono.voz.HojaOrdenesVoz

@Composable
fun AjustesCronoScreen(
    onAtras: () -> Unit,
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val ajustes by viewModel.estado.collectAsStateWithLifecycle()
    AjustesCrono(
        ajustes,
        onAtras,
        viewModel::elegirDisposicion,
        viewModel::activarVoz,
        onRepetirGuia = { viewModel.marcarGuiaCronoVista(false) },
    )
}

@Composable
internal fun AjustesCrono(
    ajustes: Ajustes,
    onAtras: () -> Unit,
    onDisposicion: (DisposicionCrono) -> Unit,
    onVoz: (Boolean) -> Unit,
    onRepetirGuia: () -> Unit = {},
) {
    PantallaAjustes(stringResource(R.string.ajustes_crono), onAtras) {
        SelectorDisposicion(ajustes.disposicion, onDisposicion)
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(ajustes.vozActiva, role = Role.Switch, onValueChange = onVoz)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ajustes_voz), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.ajustes_voz_detalle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = ajustes.vozActiva, onCheckedChange = null)
        }
        var ordenes by remember { mutableStateOf(false) }
        FilaOrdenesVoz { ordenes = true }
        FilaRepetirGuia(ajustes.guiaCronoVista, onRepetirGuia)
        if (ordenes) HojaOrdenesVoz(onCerrar = { ordenes = false })
    }
}

/** «Qué puedes decirle al Chef»: abre la hoja con las órdenes de voz. */
@Composable
internal fun FilaOrdenesVoz(onAbrir: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("ajustes-ordenes-voz")
            .clickable(role = Role.Button, onClick = onAbrir)
            .padding(vertical = 8.dp),
    ) {
        Text(stringResource(R.string.voz_ordenes_titulo), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.voz_ordenes_ajustes_detalle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** «Repetir la guía de la cocina»: la deja sin ver; mientras está pendiente, la fila lo confirma. */
@Composable
internal fun FilaRepetirGuia(
    vista: Boolean,
    onRepetir: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("ajustes-repetir-guia")
            .clickable(role = Role.Button, onClick = onRepetir)
            .padding(vertical = 8.dp),
    ) {
        Text(stringResource(R.string.ajustes_guia_repetir), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(if (vista) R.string.ajustes_guia_repetir_detalle else R.string.ajustes_guia_repetida),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AjustesHistorialScreen(
    onAtras: () -> Unit,
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val copia by viewModel.copia.collectAsStateWithLifecycle()
    PantallaAjustes(stringResource(R.string.ajustes_historial), onAtras) {
        SeccionCopia(copia, viewModel::guardarCopia, viewModel::cargarCopia)
    }
}
