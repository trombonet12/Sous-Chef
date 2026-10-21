package app.trombito.souschef.ui.bienvenida

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.DisposicionLista

private val ALTO_SEGMENTO = 48.dp

/** Diseño de las listas como tres botones de texto (sin miniaturas: la vista real queda debajo). */
@Composable
internal fun SelectorListaTexto(
    elegida: DisposicionLista,
    onElegir: (DisposicionLista) -> Unit,
) {
    val opciones =
        listOf(
            DisposicionLista.TINTE to R.string.ajustes_lista_tinte,
            DisposicionLista.FRANJA to R.string.ajustes_lista_franja,
            DisposicionLista.CLASICA to R.string.ajustes_lista_clasica,
        )
    SegmentosTexto(opciones, elegida, { "lista-${it.name}" }, onElegir)
}

/** Disposición del cronómetro como tres botones de texto (sin miniaturas: la cocina real queda debajo). */
@Composable
internal fun SelectorDisposicionTexto(
    elegida: DisposicionCrono,
    onElegir: (DisposicionCrono) -> Unit,
) {
    val opciones =
        listOf(
            DisposicionCrono.APILADA to R.string.ajustes_resumen_apilada,
            DisposicionCrono.FOCO to R.string.ajustes_resumen_foco,
            DisposicionCrono.CLASICA to R.string.ajustes_resumen_clasica,
        )
    SegmentosTexto(opciones, elegida, { "disposicion-${it.name}" }, onElegir)
}

@Composable
private fun <T> SegmentosTexto(
    opciones: List<Pair<T, Int>>,
    elegida: T,
    etiqueta: (T) -> String,
    onElegir: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, (valor, texto) ->
            SegmentedButton(
                selected = valor == elegida,
                onClick = { onElegir(valor) },
                shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                modifier = Modifier.heightIn(min = ALTO_SEGMENTO).testTag(etiqueta(valor)),
            ) {
                Text(stringResource(texto))
            }
        }
    }
}
