package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.TipoCarril
import app.trombito.souschef.ui.comun.colorCarril
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.colorCarrilTexto

/** "Ver el porqué": el paso completo y enlaces a las técnicas de la receta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PorqueSheet(
    paso: Paso,
    carril: Carril,
    tecnicas: Map<String, String>,
    onTecnica: (String) -> Unit,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CronoPalette.carta,
        contentColor = CronoPalette.texto,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "${carril.nombre} · ${minutosPaso(paso)}",
                style = MaterialTheme.typography.labelLarge,
                color = colorCarrilTexto(colorCarril(carril.color)),
            )
            Text(paso.t, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            paso.p?.takeIf { it.isNotBlank() }?.let { Text(textoRico(it), style = MaterialTheme.typography.bodyLarge) }
            paso.nota?.takeIf { it.isNotBlank() }?.let {
                Text(textoRico(it), style = MaterialTheme.typography.bodyMedium, color = CronoPalette.apagado)
            }
            paso.meto?.takeIf { it.isNotBlank() }?.let { LineaEtiquetada(R.string.crono_meto, CronoPalette.meto, it) }
            paso.saco?.takeIf { it.isNotBlank() }?.let { LineaEtiquetada(R.string.crono_saco, CronoPalette.saco, it) }
            paso.tapa?.takeIf { it.isNotBlank() }?.let { LineaEtiquetada(R.string.crono_tapa, CronoPalette.tapa, it) }
            val potencia = paso.f
            if (carril.tipoCarril == TipoCarril.FUEGO && potencia != null) {
                Text(
                    stringResource(R.string.crono_potencia, potencia),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val hitos = paso.hitosValidos
            if (hitos.isNotEmpty()) {
                Text(
                    stringResource(R.string.crono_porque_hitos),
                    style = MaterialTheme.typography.labelLarge,
                    color = CronoPalette.acento,
                )
                hitos.forEach {
                    Text("${formatoReloj(it.en.toInt())} · ${it.t}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            tecnicas.forEach { (slug, titulo) ->
                TextButton(onClick = {
                    onCerrar()
                    onTecnica(slug)
                }) {
                    Text(stringResource(R.string.crono_tecnica_enlace, titulo), color = CronoPalette.acento)
                }
            }
        }
    }
}
