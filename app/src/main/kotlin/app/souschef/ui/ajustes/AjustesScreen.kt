package app.souschef.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import app.souschef.domain.DisposicionCrono
import app.souschef.ui.comun.BarraTitulo

@Composable
fun AjustesScreen(
    onAtras: () -> Unit,
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val ajustes by viewModel.estado.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        BarraTitulo(stringResource(R.string.ajustes_titulo), onAtras)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.ajustes_crono),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.selectableGroup()) {
                OpcionDisposicion(
                    stringResource(R.string.ajustes_foco),
                    stringResource(R.string.ajustes_foco_detalle),
                    ajustes.disposicion == DisposicionCrono.FOCO,
                ) { viewModel.elegirDisposicion(DisposicionCrono.FOCO) }
                OpcionDisposicion(
                    stringResource(R.string.ajustes_apilada),
                    stringResource(R.string.ajustes_apilada_detalle),
                    ajustes.disposicion == DisposicionCrono.APILADA,
                ) { viewModel.elegirDisposicion(DisposicionCrono.APILADA) }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(ajustes.vozActiva, role = Role.Switch, onValueChange = viewModel::activarVoz)
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
}

@Composable
private fun OpcionDisposicion(
    titulo: String,
    detalle: String,
    elegida: Boolean,
    onElegir: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = elegida, role = Role.RadioButton, onClick = onElegir)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = elegida, onClick = null)
        Column {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
