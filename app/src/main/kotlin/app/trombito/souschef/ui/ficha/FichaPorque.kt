package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trombito.souschef.receta.Corte
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.CorteSvg
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.theme.FichaPalette

/** ¿Por qué?: los cortes y las decisiones, cada uno plegado con su título a la vista, y el pie. */
@Composable
fun PestanaPorque(receta: Receta) {
    receta.cortes.forEachIndexed { i, corte ->
        key("corte-$i") {
            PlegableFicha(corte.t) { CorteCuerpo(corte) }
            HorizontalDivider(color = FichaPalette.linea)
        }
    }
    receta.porque.filter { it.isNotEmpty() }.forEachIndexed { i, par ->
        key("porque-$i") {
            PlegableFicha(par[0]) {
                par.getOrNull(1)?.let {
                    Text(textoRico(it), Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(color = FichaPalette.linea)
        }
    }
    receta.pie?.takeIf { it.isNotBlank() }?.let {
        Text(it, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = FichaPalette.meta)
    }
}

/** Dibujo SVG del corte y su prosa (el título ya lo pone el bloque plegable). */
@Composable
private fun CorteCuerpo(corte: Corte) {
    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        corte.svg?.takeIf { it.isNotBlank() }?.let {
            CorteSvg(it, corte.t, Modifier.width(120.dp).aspectRatio(120f / 88f))
        }
        corte.p?.takeIf { it.isNotBlank() }?.let {
            Text(textoRico(it), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** La pestaña ¿Por qué? tiene algo que enseñar. */
fun Receta.tienePorque(): Boolean = cortes.isNotEmpty() || porque.any { it.isNotEmpty() } || !pie.isNullOrBlank()
