package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.TipoCarril
import app.trombito.souschef.ui.comun.DetallePaso
import app.trombito.souschef.ui.comun.contextoPaso
import app.trombito.souschef.ui.comun.paletaDetalleFicha
import app.trombito.souschef.ui.comun.pasosPorReloj
import app.trombito.souschef.ui.theme.FichaPalette

private const val TODOS = ""

/**
 * Pasos como lectura: todos desplegados, por orden de reloj, con el mismo detalle que el cronómetro. El filtro por
 * carril se conserva y «Solo órdenes» pliega los porqués para un repaso rápido. No lleva scroll propio: lo da la
 * ficha entera. [inicioPlanMin] es la hora de empezar (servir − total) para enseñar las horas de cada paso.
 */
@Composable
fun PestanaPasos(
    receta: Receta,
    inicioPlanMin: Int? = null,
    tecnicas: Map<String, String> = emptyMap(),
    onTecnica: (String) -> Unit = {},
) {
    var filtro by rememberSaveable { mutableStateOf(TODOS) }
    var soloOrdenes by rememberSaveable { mutableStateOf(false) }
    val porReloj = remember(receta) { pasosPorReloj(receta) }
    val visibles = porReloj.filter { filtro == TODOS || it.carril == filtro }
    Column(Modifier.fillMaxWidth().testTag("ficha-pasos")) {
        FiltrosCarril(receta, filtro) { filtro = it }
        InterruptorSoloOrdenes(soloOrdenes) { soloOrdenes = it }
        Column(Modifier.padding(horizontal = 16.dp)) {
            visibles.forEach { paso ->
                PasoDesplegado(paso, receta, inicioPlanMin, soloOrdenes)
                HorizontalDivider(color = FichaPalette.linea)
            }
            tecnicas.forEach { (slug, titulo) ->
                TextButton(onClick = { onTecnica(slug) }) {
                    Text(stringResource(R.string.detalle_tecnica, titulo), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun InterruptorSoloOrdenes(
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .testTag("ficha-solo-ordenes")
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = activo, role = Role.Switch, onValueChange = onCambio)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ficha_pasos_solo_ordenes), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.ficha_pasos_solo_ordenes_ayuda),
                style = MaterialTheme.typography.bodySmall,
                color = FichaPalette.meta,
            )
        }
        Switch(checked = activo, onCheckedChange = null)
    }
}

@Composable
private fun FiltrosCarril(
    receta: Receta,
    filtro: String,
    onFiltro: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = filtro == TODOS,
            onClick = { onFiltro(TODOS) },
            label = { Text(stringResource(R.string.ficha_filtro_todos)) },
            modifier = Modifier.testTag("ficha-filtro-todos"),
            colors = coloresFiltroPapel(),
            border = bordeFiltroPapel(filtro == TODOS),
        )
        receta.carriles.filter { c -> receta.pasos.any { it.carril == c.id } }.forEach { c ->
            FilterChip(
                selected = filtro == c.id,
                onClick = { onFiltro(c.id) },
                label = { Text(c.nombre) },
                modifier = Modifier.testTag("ficha-filtro-${c.id}"),
                leadingIcon = { Box(Modifier.size(10.dp).background(PaletaPapel.colorDe(c), CircleShape)) },
                colors = coloresFiltroPapel(),
                border = bordeFiltroPapel(filtro == c.id),
            )
        }
    }
}

/** Un paso con su franja del color del carril y el [DetallePaso] entero (o solo las órdenes). */
@Composable
private fun PasoDesplegado(
    paso: Paso,
    receta: Receta,
    inicioPlanMin: Int?,
    soloOrdenes: Boolean,
) {
    val carril = receta.carril(paso.carril)
    Row(
        Modifier
            .testTag("ficha-paso-${paso.id}")
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(PaletaPapel.colorDe(carril), RoundedCornerShape(2.dp)))
        DetallePaso(
            paso = paso,
            carrilNombre = carril?.nombre ?: paso.carril,
            tipoCarril = carril?.tipoCarril ?: TipoCarril.MESA,
            contexto = contextoPaso(receta, paso, inicioPlanMin),
            paleta = paletaDetalleFicha(),
            modifier = Modifier.weight(1f),
            conPorque = !soloOrdenes,
            cuerpo = MaterialTheme.typography.bodyMedium,
        )
    }
}
