package app.souschef.ui.ficha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.souschef.R
import app.souschef.receta.Corte
import app.souschef.receta.Receta
import app.souschef.ui.comun.CorteSvg
import app.souschef.ui.comun.SeccionPapel
import app.souschef.ui.comun.textoRico
import app.souschef.ui.shell.Icono
import app.souschef.ui.theme.FichaPalette
import kotlinx.coroutines.launch

/** Un corte: dibujo SVG (coil-svg) sobre papel, título y prosa. */
@Composable
fun CorteVista(corte: Corte) {
    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        corte.svg?.takeIf { it.isNotBlank() }?.let {
            CorteSvg(it, corte.t, Modifier.width(120.dp).aspectRatio(120f / 88f))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(corte.t, style = MaterialTheme.typography.titleSmall)
            corte.p?.takeIf { it.isNotBlank() }?.let {
                Text(
                    textoRico(it),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompraFicha(
    receta: Receta,
    estado: FichaUiState,
    viewModel: FichaViewModel,
) {
    SeccionPapel(stringResource(R.string.ficha_sec_compra)) {
        if (receta.compra.isEmpty()) {
            Text(stringResource(R.string.ficha_compra_vacia), style = MaterialTheme.typography.bodyMedium)
        } else {
            lineasCompra(receta, estado.raciones).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonCopiar(viewModel)
                OutlinedButton(onClick = viewModel::anadirALaCompra, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icono.Carrito.vector, contentDescription = null)
                    Text(stringResource(R.string.ficha_anadir_compra), Modifier.padding(start = 8.dp))
                }
            }
        }
        estado.mensaje?.let {
            val texto = if (it == MensajeFicha.COPIADA) R.string.ficha_copiada else R.string.ficha_anadida
            Text(stringResource(texto), style = MaterialTheme.typography.labelLarge, color = FichaPalette.meta)
        }
    }
}

@Composable
private fun BotonCopiar(viewModel: FichaViewModel) {
    val portapapeles = LocalClipboard.current
    val alcance = rememberCoroutineScope()
    val etiqueta = stringResource(R.string.ficha_clip_etiqueta)
    OutlinedButton(
        onClick = {
            val texto = viewModel.textoCompra() ?: return@OutlinedButton
            alcance.launch {
                portapapeles.setClipEntry(ClipEntry(android.content.ClipData.newPlainText(etiqueta, texto)))
                viewModel.avisar(MensajeFicha.COPIADA)
            }
        },
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Icon(Icono.Copiar.vector, contentDescription = null)
        Text(stringResource(R.string.ficha_copiar), Modifier.padding(start = 8.dp))
    }
}
