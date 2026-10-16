package app.souschef.ui.crono.trombito

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.souschef.R
import app.souschef.receta.crono.Consejo
import app.souschef.ui.shell.Icono
import app.souschef.ui.theme.CronoPalette

private val TAMANO_MASCOTA = 72.dp
private val ZONA_TOQUE = 48.dp
private val ANCHO_MAX_BOCADILLO = 240.dp

/**
 * Compañero flotante del cronómetro: Trombito con gorro de chef y, si hay [consejo], su bocadillo. Tocar a Trombito
 * muestra u oculta el bocadillo; la X lo oculta hasta que el texto del consejo cambie. Solo estado local de UI.
 */
@Composable
fun TrombitoConsejero(
    consejo: Consejo?,
    modifier: Modifier = Modifier,
) {
    val texto = consejo?.texto
    var oculto by rememberSaveable(texto) { mutableStateOf(false) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (texto != null && !oculto) Bocadillo(texto, { oculto = true }, Modifier.weight(1f, fill = false))
        TrombitoChef(
            Modifier
                .size(TAMANO_MASCOTA)
                .clickable(enabled = texto != null) { oculto = !oculto },
        )
    }
}

/** Globo de texto con botón de cerrar (48 dp). */
@Composable
fun Bocadillo(
    texto: String,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier.widthIn(max = ANCHO_MAX_BOCADILLO),
        shape = RoundedCornerShape(16.dp),
        color = CronoPalette.carta2,
        contentColor = CronoPalette.texto,
        border = BorderStroke(1.dp, CronoPalette.linea),
        shadowElevation = 4.dp,
    ) {
        Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                texto,
                Modifier.weight(1f, fill = false).padding(vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onCerrar, Modifier.size(ZONA_TOQUE)) {
                Icon(Icono.Cerrar.vector, stringResource(R.string.trombito_cerrar_consejo))
            }
        }
    }
}
