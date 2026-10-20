package app.trombito.souschef.ui.tecnicas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import app.trombito.souschef.ui.comun.BarraTitulo
import app.trombito.souschef.ui.comun.CampoBusqueda
import app.trombito.souschef.ui.comun.CategoriaVista
import app.trombito.souschef.ui.comun.EstadoVacio
import app.trombito.souschef.ui.comun.RejillaCategorias
import app.trombito.souschef.ui.comun.colorTono
import app.trombito.souschef.ui.theme.Espacio

@Composable
fun TecnicasScreen(
    /** null cuando es una pestaña: sin barra de título propia. */
    onAtras: (() -> Unit)?,
    onAbrir: (String) -> Unit,
    onCategoria: (String) -> Unit = {},
    viewModel: TecnicasViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    TecnicasContenido(estado, onAtras, viewModel::buscar, onAbrir, onCategoria)
}

@Composable
fun TecnicasContenido(
    estado: TecnicasUiState,
    onAtras: (() -> Unit)?,
    onBuscar: (String) -> Unit,
    onAbrir: (String) -> Unit,
    onCategoria: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        val fija = estado.categoriaFija
        if (fija != null) {
            BarraTitulo(nombreCategoria(estado, fija), onAtras ?: {})
        } else if (onAtras != null) {
            BarraTitulo(stringResource(R.string.tecnicas_titulo), onAtras)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            if (estado.mostrarRejilla) item { Cuadraditos(estado, onCategoria) }
            if (fija == null) {
                item {
                    Text(stringResource(R.string.tecnicas_todas), style = MaterialTheme.typography.titleMedium)
                }
            }
            item { CampoBusqueda(estado.consulta, stringResource(R.string.tecnicas_buscar), onBuscar) }
            cuerpo(estado, onAbrir)
        }
    }
}

@Composable
private fun nombreCategoria(
    estado: TecnicasUiState,
    categoria: CategoriaTecnica,
): String = estado.nombres[categoria] ?: stringResource(R.string.tecnicas_categoria_otra)

@Composable
private fun Cuadraditos(
    estado: TecnicasUiState,
    onCategoria: (String) -> Unit,
) {
    val vistas =
        estado.categorias.map { (categoria, n) ->
            CategoriaVista(
                clave = categoria.clave,
                nombre = nombreCategoria(estado, categoria),
                icono = categoria.icono,
                color = colorTono(categoria.tono),
                recuento = pluralStringResource(R.plurals.tecnicas_conteo, n, n),
            )
        }
    RejillaCategorias(vistas, onCategoria)
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
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(estado.tecnicas, key = { it.slug }) { t ->
                TarjetaTecnica(t, estado.disposicion, { onAbrir(t.slug) })
            }
        }
    }
}
