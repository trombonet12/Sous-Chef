package app.souschef.ui.recetas

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.souschef.R
import app.souschef.ui.comun.EstadoVacio
import app.souschef.ui.theme.Espacio

@Composable
fun RecetasScreen(
    onAbrir: (String) -> Unit,
    viewModel: RecetasViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val acciones =
        remember(viewModel) {
            AccionesFiltro(viewModel::alternarFiltro, viewModel::limpiarFiltros, viewModel::elegirOrden)
        }
    RecetasContenido(estado, viewModel::buscar, acciones, viewModel::alternarFavorita, onAbrir)
}

@Composable
fun RecetasContenido(
    estado: RecetasUiState,
    onBuscar: (String) -> Unit,
    acciones: AccionesFiltro,
    onFavorita: (String, Boolean) -> Unit,
    onAbrir: (String) -> Unit,
) {
    var hojaAbierta by rememberSaveable { mutableStateOf(false) }
    if (hojaAbierta) HojaFiltros(estado, acciones) { hojaAbierta = false }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        item { CabeceraRecetas(estado, onBuscar, acciones.alternar) { hojaAbierta = true } }
        cuerpo(estado, onFavorita, onAbrir)
    }
}

private fun LazyListScope.cuerpo(
    estado: RecetasUiState,
    onFavorita: (String, Boolean) -> Unit,
    onAbrir: (String) -> Unit,
) {
    when {
        estado.cargando -> {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
        }

        estado.tarjetas.isEmpty() -> {
            item { RecetasVacio(estado) }
        }

        else -> {
            items(estado.tarjetas, key = { it.receta.slug }) { t ->
                TarjetaLista(
                    t,
                    estado.disposicion,
                    { onFavorita(t.receta.slug, !t.favorita) },
                    { onAbrir(t.receta.slug) },
                )
            }
        }
    }
}

@Composable
private fun RecetasVacio(estado: RecetasUiState) {
    if (estado.totalRecetas == 0) {
        EstadoVacio(stringResource(R.string.recetas_vacio_titulo), stringResource(R.string.recetas_vacio_detalle))
    } else {
        val detalle =
            if (estado.activos.isNotEmpty()) R.string.recetas_nada_detalle_filtro else R.string.recetas_nada_detalle
        EstadoVacio(stringResource(R.string.recetas_nada_titulo), stringResource(detalle))
    }
}

@Composable
fun Hace.texto(): String =
    when (unidad) {
        UnidadHace.SIN_ESTRENAR -> stringResource(R.string.hace_sin_estrenar)
        UnidadHace.HOY -> stringResource(R.string.hace_hoy)
        UnidadHace.AYER -> stringResource(R.string.hace_ayer)
        UnidadHace.DIAS -> pluralStringResource(R.plurals.hace_dias, cantidad, cantidad)
        UnidadHace.SEMANAS -> pluralStringResource(R.plurals.hace_semanas, cantidad, cantidad)
        UnidadHace.MESES -> pluralStringResource(R.plurals.hace_meses, cantidad, cantidad)
        UnidadHace.ANOS -> pluralStringResource(R.plurals.hace_anos, cantidad, cantidad)
    }
