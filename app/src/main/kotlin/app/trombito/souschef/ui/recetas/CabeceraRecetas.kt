package app.trombito.souschef.ui.recetas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.ui.comun.CampoBusqueda
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.Espacio

private val BOTON_FILTROS = 56.dp

/**
 * Buscador con el botón de filtros, el chip de «¿Qué me da tiempo?», los filtros puestos (cada uno con su ✕) y la
 * línea «N recetas · Orden».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CabeceraRecetas(
    estado: RecetasUiState,
    onBuscar: (String) -> Unit,
    onQuitar: (OpcionFiltro) -> Unit,
    onAbrirFiltros: () -> Unit,
    chipTiempo: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            CampoBusqueda(estado.consulta, stringResource(R.string.recetas_buscar), onBuscar, Modifier.weight(1f))
            BotonFiltros(estado.activos.size, onAbrirFiltros)
        }
        if (chipTiempo != null || estado.activos.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                verticalArrangement = Arrangement.Center,
            ) {
                chipTiempo?.invoke()
                estado.activos.forEach { ChipActivo(it, onQuitar) }
            }
        }
        if (!estado.cargando && estado.totalRecetas > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pluralStringResource(R.plurals.recetas_conteo, estado.tarjetas.size, estado.tarjetas.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onAbrirFiltros) {
                    Text(
                        stringResource(R.string.filtros_orden_actual, textoOrden(estado.orden)) + " ▾",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonFiltros(
    activos: Int,
    onClick: () -> Unit,
) {
    val descripcion =
        if (activos == 0) {
            stringResource(R.string.filtros_abrir)
        } else {
            pluralStringResource(R.plurals.filtros_abrir_activos, activos, activos)
        }
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(BOTON_FILTROS).testTag("boton-filtros").semantics { contentDescription = descripcion },
    ) {
        BadgedBox(badge = { if (activos > 0) Badge { Text("$activos") } }) {
            Icon(Icono.Filtros.vector, contentDescription = null)
        }
    }
}

@Composable
private fun ChipActivo(
    opcion: OpcionFiltro,
    onQuitar: (OpcionFiltro) -> Unit,
) {
    val texto = textoOpcion(opcion)
    val quitar = stringResource(R.string.filtros_quitar, texto)
    InputChip(
        selected = true,
        onClick = { onQuitar(opcion) },
        label = { Text(texto) },
        modifier = Modifier.semantics { contentDescription = quitar },
        leadingIcon = (opcion as? OpcionFiltro.Base)?.let { b -> { PuntoBase(b.valor) } },
        trailingIcon = {
            Icon(Icono.Cerrar.vector, contentDescription = null, Modifier.size(InputChipDefaults.IconSize))
        },
    )
}
