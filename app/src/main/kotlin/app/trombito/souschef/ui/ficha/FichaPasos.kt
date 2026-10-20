package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.TipoCarril
import app.trombito.souschef.ui.comun.LocalCocina
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.comun.nivel
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.crono.recursoTapa
import app.trombito.souschef.ui.theme.FichaPalette

private const val TODOS = ""

/** Pasos: filtro por carril fijo arriba y una fila compacta por paso; el paso entero sale en una hoja. */
@Composable
fun PestanaPasos(
    receta: Receta,
    scroll: ScrollState,
) {
    var filtro by rememberSaveable { mutableStateOf(TODOS) }
    var abierto by rememberSaveable { mutableStateOf<String?>(null) }
    val visibles = receta.pasos.withIndex().filter { filtro == TODOS || it.value.carril == filtro }
    Column(Modifier.fillMaxSize().testTag("ficha-pasos")) {
        FiltrosCarril(receta, filtro) { filtro = it }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 16.dp)) {
            visibles.forEach { (i, paso) ->
                FilaPaso(i + 1, paso, receta) { abierto = paso.id }
                HorizontalDivider(color = FichaPalette.linea)
            }
        }
    }
    receta.paso(abierto.orEmpty())?.let { PasoSheet(it, receta) { abierto = null } }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaPaso(
    numero: Int,
    paso: Paso,
    receta: Receta,
    onAbrir: () -> Unit,
) {
    val carril = receta.carril(paso.carril)
    val alTocar = stringResource(R.string.ficha_paso_abrir)
    Row(
        Modifier
            .testTag("ficha-paso-${paso.id}")
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = alTocar, onClick = onAbrir)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(PaletaPapel.colorDe(carril), RoundedCornerShape(2.dp)))
        Text("$numero.", Modifier.width(28.dp), style = MaterialTheme.typography.titleSmall.merge(Tabular))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row {
                Text(paso.t, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.ficha_duracion, Escalado.minutos(paso.min)),
                    Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge.merge(Tabular),
                    color = FichaPalette.meta,
                )
            }
            val fuego = paso.f?.takeIf { carril?.tipoCarril == TipoCarril.FUEGO }
            val tapa = recursoTapa(paso.tapa)?.let { stringResource(it) }
            if (fuego != null || tapa != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    fuego?.let { ChipPapel(stringResource(R.string.ficha_fuego_bajo, LocalCocina.current.nivel(it))) }
                    tapa?.let { ChipPapel(it) }
                }
            }
            paso.nota?.takeIf { it.isNotBlank() }?.let {
                Text(
                    textoRico(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = FichaPalette.tinta2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** El paso entero: `p`, la nota, los hitos y lo que entra y sale. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasoSheet(
    paso: Paso,
    receta: Receta,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FichaPalette.papel,
        contentColor = FichaPalette.tinta,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(lineaMetaPaso(paso, receta), style = MaterialTheme.typography.labelMedium, color = FichaPalette.meta)
            Text(paso.t, style = MaterialTheme.typography.headlineSmall)
            paso.p?.takeIf { it.isNotBlank() }?.let { Text(textoRico(it), style = MaterialTheme.typography.bodyLarge) }
            paso.nota?.takeIf { it.isNotBlank() }?.let {
                Text(
                    textoRico(it),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = FichaPalette.tinta2,
                )
            }
            val hitos = paso.hitosValidos
            if (hitos.isNotEmpty()) {
                Text(stringResource(R.string.ficha_paso_hitos), style = MaterialTheme.typography.labelLarge)
                hitos.forEach {
                    Text(
                        "${formatoHito(it.en)} ${it.t}",
                        style = MaterialTheme.typography.bodyMedium.merge(Tabular),
                        color = FichaPalette.tinta2,
                    )
                }
            }
            paso.meto?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.ficha_paso_entra, it)) }
            paso.saco?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.ficha_paso_sale, it)) }
        }
    }
}

/** "SARTÉN · 12 MIN · FUEGO 8 · SIN TAPA". */
@Composable
fun lineaMetaPaso(
    paso: Paso,
    receta: Receta,
): String {
    val cocina = LocalCocina.current
    val partes =
        buildList {
            add((receta.carril(paso.carril)?.nombre ?: paso.carril).uppercase())
            add(stringResource(R.string.ficha_minutos_meta, Escalado.minutos(paso.min)).uppercase())
            add(
                paso.f?.let { stringResource(R.string.ficha_fuego_meta, cocina.nivel(it).uppercase()) }
                    ?: stringResource(R.string.ficha_sin_fuego),
            )
            paso.tapa?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        }
    return partes.joinToString(" · ")
}
