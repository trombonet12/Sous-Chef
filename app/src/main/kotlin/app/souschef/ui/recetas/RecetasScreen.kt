package app.souschef.ui.recetas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import app.souschef.ui.theme.Espacio

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
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
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
                    style = MaterialTheme.typography.labelMedium,
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
        Column(Modifier.padding(start = Espacio.l, top = Espacio.s, end = Espacio.xs, bottom = Espacio.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    receta.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                BotonFavorita(tarjeta.favorita, receta.titulo, onFavorita)
            }
            Column(Modifier.padding(end = Espacio.m), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                if (receta.descripcion.isNotBlank()) {
                    Text(
                        receta.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Huella(receta)
                PieTarjeta(tarjeta)
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

/** Una sola línea de metadatos (tiempo, fuegos, base, última vez) y el estado de compra a la derecha. */
@Composable
private fun PieTarjeta(tarjeta: TarjetaReceta) {
    val receta = tarjeta.receta
    val hace = tarjeta.hace.texto()
    val resalte = if (tarjeta.hace.resaltado) MaterialTheme.colorScheme.error else Color.Unspecified
    val fuegos =
        if (tarjeta.fuegos >
            0
        ) {
            pluralStringResource(R.plurals.tarjeta_fuegos, tarjeta.fuegos, tarjeta.fuegos)
        } else {
            null
        }
    val inicio =
        listOfNotNull(
            stringResource(R.string.tarjeta_minutos, Escalado.minutos(receta.totalEfectivo)),
            fuegos,
        ).joinToString(" · ")
    val texto =
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(inicio) }
            append(" · ")
            withStyle(SpanStyle(color = resalte)) { append(hace) }
        }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Text(
            texto,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (receta.compra.isNotEmpty()) {
            Etiqueta(stringResource(R.string.tarjeta_compra), tono = Tono.Teja)
        } else {
            Etiqueta(stringResource(R.string.tarjeta_despensa), tono = Tono.Oliva)
        }
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
