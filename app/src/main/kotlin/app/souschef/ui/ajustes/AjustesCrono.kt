package app.souschef.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.souschef.R
import app.souschef.domain.Ajustes
import app.souschef.domain.DisposicionCrono

@Composable
fun AjustesCronoScreen(
    onAtras: () -> Unit,
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val ajustes by viewModel.estado.collectAsStateWithLifecycle()
    AjustesCrono(ajustes, onAtras, viewModel::elegirDisposicion, viewModel::activarVoz)
}

@Composable
internal fun AjustesCrono(
    ajustes: Ajustes,
    onAtras: () -> Unit,
    onDisposicion: (DisposicionCrono) -> Unit,
    onVoz: (Boolean) -> Unit,
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
