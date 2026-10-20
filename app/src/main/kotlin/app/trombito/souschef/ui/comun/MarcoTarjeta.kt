package app.trombito.souschef.ui.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.ui.theme.Espacio

/** Medidas del [MarcoTarjeta]. */
object MarcoTarjeta {
    /** Ancho de la columna del bloque lateral en la disposición Tinte. */
    val ANCHO_BLOQUE = 64.dp
    val GROSOR_SEPARADOR = 1.dp
    val ANCHO_FRANJA = 5.dp
}

private val SIN_ELEVACION = 0.dp

/**
 * Armazón de tarjeta de lista según la [disposicion] elegida en Ajustes.
 *
 * - TINTE: fondo del tinte de [color], [bloque] (si hay) a la izquierda con un separador de acento, y [contenido].
 * - FRANJA: tarjeta clara con una franja de acento a la izquierda y [contenido]; ignora [bloque].
 * - CLASICA: la tarjeta por defecto con [contenido]; ignora [bloque].
 *
 * Con [onClick] nulo la tarjeta no es pulsable. Los paddings internos los pone quien llama.
 */
@Suppress("LongParameterList") // API fijada: disposición, color, clic, modificador y dos huecos
@Composable
fun MarcoTarjeta(
    disposicion: DisposicionLista,
    color: ColorFamilia,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    bloque: (@Composable ColumnScope.() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    when (disposicion) {
        DisposicionLista.TINTE -> {
            val colores =
                CardDefaults.cardColors(
                    containerColor = color.tinte,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            val elevacion = CardDefaults.cardElevation(defaultElevation = SIN_ELEVACION)
            TarjetaBase(onClick, modifier, colores, elevacion) {
                Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    if (bloque != null) {
                        Column(
                            Modifier.width(MarcoTarjeta.ANCHO_BLOQUE),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            content = bloque,
                        )
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .padding(vertical = Espacio.s)
                                .width(MarcoTarjeta.GROSOR_SEPARADOR)
                                .background(color.acento),
                        )
                    }
                    Column(Modifier.weight(1f), content = contenido)
                }
            }
        }

        DisposicionLista.FRANJA -> {
            val colores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            TarjetaBase(onClick, modifier, colores, CardDefaults.cardElevation()) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.fillMaxHeight().width(MarcoTarjeta.ANCHO_FRANJA).background(color.acento))
                    Column(Modifier.weight(1f), content = contenido)
                }
            }
        }

        DisposicionLista.CLASICA -> {
            TarjetaBase(onClick, modifier, CardDefaults.cardColors(), CardDefaults.cardElevation(), contenido)
        }
    }
}

@Composable
private fun TarjetaBase(
    onClick: (() -> Unit)?,
    modifier: Modifier,
    colores: CardColors,
    elevacion: CardElevation,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val m = modifier.fillMaxWidth()
    if (onClick != null) {
        Card(onClick = onClick, modifier = m, colors = colores, elevation = elevacion, content = contenido)
    } else {
        Card(modifier = m, colors = colores, elevation = elevacion, content = contenido)
    }
}
