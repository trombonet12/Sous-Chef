package app.trombito.souschef.ui.ficha

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.FichaPalette

private val ANCHO_CANTIDAD_MIN = 84.dp
private val ANCHO_CANTIDAD_MAX = 132.dp
private val SEPARACION_CANTIDAD = 12.dp

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
            ListaIngredientes(g.items, f)
        }
    } else {
        ListaIngredientes(Escalado.listaGeneral(receta), f)
    }
    BotonCompartir(receta, estado.raciones, porZona)
    receta.notaEscala?.takeIf { it.isNotBlank() }?.let { NotaEscala(it) }
    if (estado.fueraDeBase) NotaFicha(stringResource(R.string.ficha_aviso_fuera_de_base, receta.raciones))
    if (estado.ajustadoAlRango && permitidas.isNotEmpty()) {
        NotaFicha(stringResource(R.string.ficha_aviso_rango, permitidas.first(), permitidas.last()))
    }
}

/** Manda como texto plano la lista que se ve (según «Todo junto» / «Por zona» y las raciones elegidas). */
@Composable
private fun BotonCompartir(
    receta: Receta,
    raciones: Int,
    porZona: Boolean,
) {
    val contexto = LocalContext.current
    val descripcion = stringResource(R.string.ficha_compartir_ingredientes_descripcion)
    OutlinedButton(
        onClick = { compartirIngredientes(contexto, receta, raciones, porZona) },
        modifier =
            Modifier
                .padding(top = 8.dp)
                .heightIn(min = 48.dp)
                .testTag("compartir-ingredientes")
                .semantics { contentDescription = descripcion },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FichaPalette.tinta),
        border = BorderStroke(1.dp, FichaPalette.tinta3),
    ) { Text(stringResource(R.string.ficha_compartir_ingredientes)) }
}

private fun compartirIngredientes(
    contexto: Context,
    receta: Receta,
    raciones: Int,
    porZona: Boolean,
) {
    val r = contexto.resources
    val texto =
        textoIngredientes(receta, raciones, porZona) { n ->
            "$n " + r.getQuantityString(R.plurals.ficha_unidad_racion, n)
        }
    val envio =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
    val selector = Intent.createChooser(envio, r.getString(R.string.ficha_compartir_titulo))
    if (contexto !is Activity) selector.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    contexto.startActivity(selector)
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

/**
 * Las filas de un bloque con la columna de cantidades del mismo ancho: el del texto más largo, entre 84 y 132 dp, para
 * que «1 1/2 cucharadas» quepa sin descolocar los nombres. Más allá de 132 dp la cantidad pasa a dos líneas.
 */
@Composable
private fun ListaIngredientes(
    items: List<Ingrediente>,
    f: Double,
) {
    val medidor = rememberTextMeasurer()
    val densidad = LocalDensity.current
    val base = MaterialTheme.typography.bodyMedium
    val estilo = base.merge(Tabular).copy(fontWeight = FontWeight.SemiBold)
    val ancho =
        remember(items, f, estilo, densidad) {
            val maxPx = items.maxOfOrNull { medidor.measure(Escalado.cantidad(it, f), estilo).size.width } ?: 0
            (with(densidad) { maxPx.toDp() } + SEPARACION_CANTIDAD).coerceIn(ANCHO_CANTIDAD_MIN, ANCHO_CANTIDAD_MAX)
        }
    items.forEach { FilaIngrediente(it, f, ancho) }
}

@Composable
private fun FilaIngrediente(
    item: Ingrediente,
    f: Double,
    anchoCantidad: Dp,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            Escalado.cantidad(item, f),
            Modifier
                .widthIn(min = ANCHO_CANTIDAD_MIN, max = ANCHO_CANTIDAD_MAX)
                .width(anchoCantidad)
                .padding(end = SEPARACION_CANTIDAD),
            style = MaterialTheme.typography.bodyMedium.merge(Tabular),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
        Text(item.n, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}
