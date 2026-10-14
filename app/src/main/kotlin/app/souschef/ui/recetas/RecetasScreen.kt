package app.souschef.ui.recetas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.souschef.receta.Escalado
import app.souschef.ui.comun.CampoBusqueda
import app.souschef.ui.comun.ChipsFiltro
import app.souschef.ui.comun.EstadoVacio
import app.souschef.ui.comun.Etiqueta
import app.souschef.ui.comun.Tono
import app.souschef.ui.shell.Icono

@Composable
fun RecetasScreen(
    onAbrir: (String) -> Unit,
    viewModel: RecetasViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    RecetasContenido(estado, viewModel::buscar, viewModel::elegirBase, viewModel::alternarFavorita, onAbrir)
}

@Composable
fun RecetasContenido(
    estado: RecetasUiState,
    onBuscar: (String) -> Unit,
    onBase: (String?) -> Unit,
    onFavorita: (String, Boolean) -> Unit,
    onAbrir: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { CampoBusqueda(estado.consulta, stringResource(R.string.recetas_buscar), onBuscar) }
        if (estado.mostrarChips) {
            item {
                ChipsFiltro(estado.bases.map { it.base to it.n }, estado.baseActiva, estado.coincidentes, onBase)
            }
        }
        if (!estado.cargando && estado.coincidentes > 0) {
            item {
                Text(
                    pluralStringResource(R.plurals.recetas_conteo, estado.tarjetas.size, estado.tarjetas.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
                TarjetaRecetaVista(t, { onFavorita(t.receta.slug, !t.favorita) }, { onAbrir(t.receta.slug) })
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
            if (estado.baseActiva != null) R.string.recetas_nada_detalle_filtro else R.string.recetas_nada_detalle
        EstadoVacio(stringResource(R.string.recetas_nada_titulo), stringResource(detalle))
    }
}

@Composable
private fun TarjetaRecetaVista(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    onAbrir: () -> Unit,
) {
    val receta = tarjeta.receta
    Card(onClick = onAbrir, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    receta.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                )
                BotonFavorita(tarjeta.favorita, receta.titulo, onFavorita)
            }
            Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (receta.descripcion.isNotBlank()) {
                    Text(
                        receta.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Huella(receta)
                EtiquetasTarjeta(tarjeta)
            }
        }
    }
}

@Composable
private fun BotonFavorita(
    favorita: Boolean,
    titulo: String,
    onClick: () -> Unit,
) {
    val descripcion =
        stringResource(if (favorita) R.string.favorita_quitar else R.string.favorita_marcar, titulo)
    IconButton(onClick = onClick) {
        Icon(
            if (favorita) Icono.EstrellaLlena.vector else Icono.Estrella.vector,
            contentDescription = descripcion,
            tint = if (favorita) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EtiquetasTarjeta(tarjeta: TarjetaReceta) {
    val receta = tarjeta.receta
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Etiqueta(
            stringResource(R.string.tarjeta_minutos, Escalado.minutos(receta.totalEfectivo)),
            negrita = true,
        )
        if (tarjeta.fuegos > 0) {
            Etiqueta(pluralStringResource(R.plurals.tarjeta_fuegos, tarjeta.fuegos, tarjeta.fuegos))
        }
        receta.tags.base?.let { Etiqueta(it) }
        if (receta.compra.isNotEmpty()) {
            Etiqueta(stringResource(R.string.tarjeta_compra), tono = Tono.Teja)
        } else {
            Etiqueta(stringResource(R.string.tarjeta_despensa), tono = Tono.Oliva)
        }
        Etiqueta(tarjeta.hace.texto(), tono = if (tarjeta.hace.resaltado) Tono.Teja else Tono.Neutro)
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
