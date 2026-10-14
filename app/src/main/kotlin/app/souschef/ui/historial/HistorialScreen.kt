package app.souschef.ui.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.souschef.R
import app.souschef.ui.comun.CampoBusqueda
import app.souschef.ui.comun.EstadoVacio
import app.souschef.ui.comun.Etiqueta
import app.souschef.ui.comun.Tono
import app.souschef.ui.comun.fechaCorta

@Composable
fun HistorialScreen(
    onRepetir: (String) -> Unit,
    viewModel: HistorialViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                FilaHistorialVista(
                    fila,
                    confirmando = estado.pendienteBorrar == fila.entrada.id,
                    onBorrar = { onBorrar(fila.entrada.id) },
                    onRepetir = { onRepetir(fila.entrada.slug) },
                )
            }
        }
    }
}

@Composable
private fun FilaHistorialVista(
    fila: FilaHistorial,
    confirmando: Boolean,
    onBorrar: () -> Unit,
    onRepetir: () -> Unit,
) {
    val e = fila.entrada
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    fechaCorta(e.fecha),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (fila.repite && e.base != null) {
                    Etiqueta(stringResource(R.string.historial_dos_seguidas, e.base), tono = Tono.Teja)
                }
            }
            Text(e.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            val minutos = e.minutosReales?.let { stringResource(R.string.historial_min_reales, it) }
            val meta =
                (listOfNotNull(e.base.takeUnless { fila.repite }) + e.tecnica.take(3) + listOfNotNull(minutos))
                    .joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            e.nota?.takeIf { it.isNotBlank() }?.let {
                Text(
                    stringResource(R.string.historial_nota, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                )
            }
            AccionesFila(fila, confirmando, onBorrar, onRepetir)
        }
    }
}

@Composable
private fun AccionesFila(
    fila: FilaHistorial,
    confirmando: Boolean,
    onBorrar: () -> Unit,
    onRepetir: () -> Unit,
) {
    val titulo = fila.entrada.titulo
    val descripcionBorrar = stringResource(R.string.historial_borrar_desc, titulo)
    val descripcionRepetir = stringResource(R.string.historial_repetir_desc, titulo)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (fila.recetaExiste) {
            TextButton(
                onClick = onRepetir,
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = descripcionRepetir },
            ) { Text(stringResource(R.string.historial_repetir)) }
        }
        TextButton(
            onClick = onBorrar,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = descripcionBorrar },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(if (confirmando) R.string.historial_seguro else R.string.historial_borrar))
        }
    }
}
