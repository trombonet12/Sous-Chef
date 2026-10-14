package app.souschef.ui.tecnicas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.souschef.R
import app.souschef.receta.Tecnica
import app.souschef.ui.comun.BarraTitulo
import app.souschef.ui.comun.CampoBusqueda
import app.souschef.ui.comun.ChipsFiltro
import app.souschef.ui.comun.EstadoVacio
import app.souschef.ui.comun.Etiqueta

@Composable
fun TecnicasScreen(
    onAtras: () -> Unit,
    onAbrir: (String) -> Unit,
    viewModel: TecnicasViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        BarraTitulo(stringResource(R.string.tecnicas_titulo), onAtras)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CampoBusqueda(estado.consulta, stringResource(R.string.tecnicas_buscar), viewModel::buscar) }
            if (estado.mostrarChips) {
                item {
                    ChipsFiltro(
                        estado.categorias,
                        estado.categoriaActiva,
                        estado.coincidentes,
                        viewModel::elegirCategoria,
                    )
                }
            }
            cuerpo(estado, onAbrir)
        }
    }
}

private fun LazyListScope.cuerpo(
    estado: TecnicasUiState,
    onAbrir: (String) -> Unit,
) {
    when {
        estado.cargando -> {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
        }

        estado.tecnicas.isEmpty() -> {
            val titulo = if (estado.total == 0) R.string.tecnicas_vacio else R.string.tecnicas_nada
            item { EstadoVacio(stringResource(titulo), null) }
        }

        else -> {
            item {
                Text(
                    pluralStringResource(R.plurals.tecnicas_conteo, estado.tecnicas.size, estado.tecnicas.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(estado.tecnicas, key = { it.slug }) { t -> TarjetaTecnica(t) { onAbrir(t.slug) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaTecnica(
    tecnica: Tecnica,
    onAbrir: () -> Unit,
) {
    Card(onClick = onAbrir, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tecnica.titulo, style = MaterialTheme.typography.titleLarge)
            if (tecnica.resumen.isNotBlank()) {
                Text(
                    tecnica.resumen,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (tecnica.categoria.isNotBlank()) Etiqueta(tecnica.categoria)
                val n = tecnica.pasos.size
                Etiqueta(pluralStringResource(R.plurals.tecnica_pasos_n, n, n))
            }
        }
    }
}
