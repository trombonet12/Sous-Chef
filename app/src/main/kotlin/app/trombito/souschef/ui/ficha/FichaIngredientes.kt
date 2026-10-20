package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.FichaPalette

/** Ingredientes: «Todo junto» o «Por zona», avisos de raciones y la nota de escalado detrás de un chip ⓘ. */
@Composable
fun PestanaIngredientes(
    receta: Receta,
    estado: FichaUiState,
) {
    val permitidas = estado.racionesPermitidas
    val f = Escalado.factor(estado.raciones, receta.raciones)
    var porZona by rememberSaveable(receta.slug) { mutableStateOf(false) }
    SelectorIngredientes(porZona) { porZona = it }
    if (porZona) {
        receta.grupos.forEach { g ->
            Text(g.n, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
            g.items.forEach { FilaIngrediente(it, f) }
        }
    } else {
        Escalado.listaGeneral(receta).forEach { FilaIngrediente(it, f) }
    }
    receta.notaEscala?.takeIf { it.isNotBlank() }?.let { NotaEscala(it) }
    if (estado.fueraDeBase) NotaFicha(stringResource(R.string.ficha_aviso_fuera_de_base, receta.raciones))
    if (estado.ajustadoAlRango && permitidas.isNotEmpty()) {
        NotaFicha(stringResource(R.string.ficha_aviso_rango, permitidas.first(), permitidas.last()))
    }
}

@Composable
private fun NotaEscala(texto: String) {
    var abierta by rememberSaveable { mutableStateOf(false) }
    AssistChip(
        onClick = { abierta = !abierta },
        label = { Text(stringResource(R.string.ficha_nota_escala_chip)) },
        leadingIcon = { Icon(Icono.Info.vector, contentDescription = null) },
        colors = coloresAsistentePapel(),
        border = bordeAsistentePapel(),
    )
    if (abierta) {
        Text(
            texto,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = FichaPalette.meta,
        )
    }
}

@Composable
private fun SelectorIngredientes(
    porZona: Boolean,
    onPorZona: (Boolean) -> Unit,
) {
    val colores =
        SegmentedButtonDefaults.colors(
            activeContainerColor = FichaPalette.papel2,
            activeContentColor = FichaPalette.tinta,
            activeBorderColor = FichaPalette.tinta3,
            inactiveContainerColor = FichaPalette.papel,
            inactiveContentColor = FichaPalette.tinta2,
            inactiveBorderColor = FichaPalette.tinta3,
        )
    val opciones = listOf(R.string.ficha_ing_todo_junto, R.string.ficha_ing_por_zona)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, texto ->
            SegmentedButton(
                selected = porZona == (i == 1),
                onClick = { onPorZona(i == 1) },
                shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                colors = colores,
            ) { Text(stringResource(texto)) }
        }
    }
}

@Composable
private fun FilaIngrediente(
    item: Ingrediente,
    f: Double,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            Escalado.cantidad(item, f),
            Modifier.width(84.dp).padding(end = 12.dp),
            style = MaterialTheme.typography.bodyMedium.merge(Tabular),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
        Text(item.n, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}
