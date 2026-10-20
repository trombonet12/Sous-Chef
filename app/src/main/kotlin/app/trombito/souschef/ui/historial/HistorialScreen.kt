package app.trombito.souschef.ui.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.ui.comun.CampoBusqueda
import app.trombito.souschef.ui.comun.EstadoVacio
import app.trombito.souschef.ui.theme.Espacio

@Composable
fun HistorialScreen(
    onRepetir: (String) -> Unit,
    viewModel: HistorialViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        if (estado.total > 0) {
            item { CampoBusqueda(estado.consulta, stringResource(R.string.historial_buscar), viewModel::buscar) }
        }
        cuerpo(estado, viewModel::tocarBorrar, onRepetir)
    }
}

private fun LazyListScope.cuerpo(
    estado: HistorialUiState,
    onBorrar: (String) -> Unit,
    onRepetir: (String) -> Unit,
) {
    when {
        estado.cargando -> {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
        }

        estado.total == 0 -> {
            item {
                EstadoVacio(
                    stringResource(R.string.historial_vacio_titulo),
                    stringResource(R.string.historial_vacio_detalle),
                )
            }
        }

        estado.filas.isEmpty() -> {
            item { EstadoVacio(stringResource(R.string.historial_nada), null) }
        }

        else -> {
            item {
                Text(
                    pluralStringResource(R.plurals.historial_conteo, estado.filas.size, estado.filas.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(estado.filas, key = { it.entrada.id }) { fila ->
                TarjetaHistorial(
                    fila,
                    estado.disposicion,
                    confirmando = estado.pendienteBorrar == fila.entrada.id,
                    onBorrar = { onBorrar(fila.entrada.id) },
                    onRepetir = { onRepetir(fila.entrada.slug) },
                )
            }
        }
    }
}
