package app.trombito.souschef.ui.recetas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.ui.theme.Espacio

private val PUNTO_BASE = 10.dp

/** Lo que la hoja necesita hacer; lo implementa el ViewModel. */
class AccionesFiltro(
    val alternar: (OpcionFiltro) -> Unit,
    val limpiar: () -> Unit,
    val ordenar: (OrdenRecetas) -> Unit,
)

/**
 * Hoja inferior con todos los filtros por secciones y el orden. Cada chip dice cuántas recetas quedarían; los cambios
 * se aplican al momento y el botón de abajo solo cierra.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaFiltros(
    estado: RecetasUiState,
    acciones: AccionesFiltro,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("hoja-filtros"),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Espacio.l)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.filtros_titulo),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextButton(onClick = acciones.limpiar, enabled = estado.activos.isNotEmpty()) {
                    Text(stringResource(R.string.filtros_limpiar))
                }
            }
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Espacio.l),
            ) {
                estado.secciones.forEach { SeccionChips(it, acciones.alternar) }
                SeccionOrden(estado.orden, acciones.ordenar)
            }
            Button(
                onClick = onCerrar,
                modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.m),
            ) {
                Text(pluralStringResource(R.plurals.filtros_ver, estado.tarjetas.size, estado.tarjetas.size))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionChips(
    grupo: GrupoFiltro,
    onAlternar: (OpcionFiltro) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        TituloSeccion(stringResource(grupo.seccion.titulo()))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            grupo.chips.forEach { chip ->
                FilterChip(
                    selected = chip.activo,
                    onClick = { onAlternar(chip.opcion) },
                    enabled = chip.activo || chip.n > 0,
                    label = { Text(stringResource(R.string.filtro_n, textoOpcion(chip.opcion), chip.n)) },
                    leadingIcon = (chip.opcion as? OpcionFiltro.Base)?.let { b -> { PuntoBase(b.valor) } },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionOrden(
    actual: OrdenRecetas,
    onOrdenar: (OrdenRecetas) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        TituloSeccion(stringResource(R.string.filtros_seccion_orden))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            OrdenRecetas.entries.forEach { o ->
                FilterChip(
                    selected = o == actual,
                    onClick = { onOrdenar(o) },
                    label = { Text(textoOrden(o)) },
                )
            }
        }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
fun PuntoBase(familia: FamiliaBase) {
    Box(Modifier.size(PUNTO_BASE).background(colorBase(familia).acento, CircleShape))
}
