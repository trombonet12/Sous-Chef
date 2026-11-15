package app.trombito.souschef.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trombito.souschef.receta.Bloque
import app.trombito.souschef.receta.Bloques

private val ESPACIO_ENTRE_BLOQUES = 8.dp
private val SANGRIA_VINETA = 16.dp

/**
 * Prosa estructurada (ADR-0017) pintada en párrafos y listas: espacio entre bloques, `<b>` en negrita y viñetas con
 * sangría colgante (el texto de una línea larga no vuelve bajo la viñeta). Sin cursiva: es para bloques largos.
 */
@Composable
fun TextoBloques(
    texto: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    separacion: Dp = ESPACIO_ENTRE_BLOQUES,
) {
    val bloques = remember(texto) { Bloques.analizar(texto) }
    if (bloques.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(separacion)) {
        bloques.forEach { bloque ->
            when (bloque) {
                is Bloque.Parrafo -> {
                    Text(textoRico(bloque.texto), style = style, color = color)
                }

                is Bloque.Lista -> {
                    Column(verticalArrangement = Arrangement.spacedBy(separacion / 2)) {
                        bloque.elementos.forEach { Elemento(it, style, color) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Elemento(
    texto: String,
    style: TextStyle,
    color: Color,
) {
    Row {
        Text("•", Modifier.width(SANGRIA_VINETA).padding(start = 4.dp), style = style, color = color)
        Text(textoRico(texto), Modifier.weight(1f), style = style, color = color)
    }
}
