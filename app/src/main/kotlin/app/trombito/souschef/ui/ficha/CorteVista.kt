package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trombito.souschef.receta.Corte
import app.trombito.souschef.ui.comun.CorteSvg
import app.trombito.souschef.ui.comun.textoRico

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
