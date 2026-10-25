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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.ResumenMes
import app.trombito.souschef.ui.comun.CampoBusqueda
import app.trombito.souschef.ui.comun.EstadoVacio
import app.trombito.souschef.ui.comun.SEPARADOR_META
import app.trombito.souschef.ui.theme.Espacio
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun HistorialScreen(
    onRepetir: (String) -> Unit,
    viewModel: HistorialViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    var editando by remember { mutableStateOf<EntradaHistorial?>(null) }
    AvisoDeshacer(
        estado.borrada,
        avisos,
        onDeshacer = viewModel::deshacerBorrado,
        onCaducado = viewModel::descartarBorrado,
    )
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            if (estado.total > 0) {
                item { ResumenDelMes(estado.mes) }
                item { CampoBusqueda(estado.consulta, stringResource(R.string.historial_buscar), viewModel::buscar) }
            }
            cuerpo(estado, viewModel::borrar, onRepetir) { editando = it }
        }
        SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter))
    }
    editando?.let { entrada ->
        DialogoNota(
            titulo = entrada.titulo,
            notaInicial = entrada.nota.orEmpty(),
            onGuardar = {
                viewModel.editarNota(entrada.id, it)
                editando = null
            },
            onCancelar = { editando = null },
        )
    }
}

/**
 * «Cocinada borrada · Deshacer» durante [VENTANA_DESHACER_MS]. Si llega otro borrado, el aviso anterior se cierra y
 * solo queda por deshacer el último.
 */
@Composable
private fun AvisoDeshacer(
    borrada: EntradaHistorial?,
    avisos: SnackbarHostState,
    onDeshacer: () -> Unit,
    onCaducado: () -> Unit,
) {
    if (borrada == null) return
    val mensaje = stringResource(R.string.historial_borrada)
    val accion = stringResource(R.string.historial_deshacer)
    LaunchedEffect(borrada) {
        val resultado =
            withTimeoutOrNull(VENTANA_DESHACER_MS) {
                avisos.showSnackbar(mensaje, accion, duration = SnackbarDuration.Indefinite)
            }
        if (resultado == SnackbarResult.ActionPerformed) onDeshacer() else onCaducado()
    }
}

/** «Este mes: 4 cocinadas · 3 recetas distintas · sin estrenar 5 de 22». */
@Composable
private fun ResumenDelMes(mes: ResumenMes) {
    val partes =
        listOfNotNull(
            pluralStringResource(R.plurals.historial_mes_cocinadas, mes.cocinadas, mes.cocinadas),
            pluralStringResource(R.plurals.historial_mes_distintas, mes.recetasDistintas, mes.recetasDistintas),
            if (mes.totalRecetas > 0) {
                stringResource(R.string.historial_mes_sin_estrenar, mes.sinEstrenar, mes.totalRecetas)
            } else {
                null
            },
        )
    Text(
        stringResource(R.string.historial_mes, partes.joinToString(SEPARADOR_META)),
        modifier = Modifier.testTag("historial-mes"),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun LazyListScope.cuerpo(
    estado: HistorialUiState,
    onBorrar: (EntradaHistorial) -> Unit,
    onRepetir: (String) -> Unit,
    onEditarNota: (EntradaHistorial) -> Unit,
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
                    onBorrar = { onBorrar(fila.entrada) },
                    onRepetir = { onRepetir(fila.entrada.slug) },
                    onEditarNota = { onEditarNota(fila.entrada) },
                )
            }
        }
    }
}
