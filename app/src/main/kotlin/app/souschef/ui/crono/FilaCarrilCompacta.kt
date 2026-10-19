package app.souschef.ui.crono

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.souschef.R
import app.souschef.ui.comun.Tabular
import app.souschef.ui.theme.CronoPalette
import app.souschef.ui.theme.colorCarrilTexto

private val ALTO_FILA = 64.dp

/**
 * Fila compacta (64 dp) de un carril que no está en foco: nombre, título del paso, fuego, reloj y, solo si toca
 * (Hecho o Empezar), un botón pequeño. Tocar la fila la fija en el foco.
 */
@Composable
fun FilaCarrilCompacta(
    v: VistaCarril,
    acciones: AccionesCarril,
    modifier: Modifier = Modifier,
) {
    val color = colorDeCarril(v.colorHex, v.indiceCarril)
    val etiquetaEnfocar = stringResource(R.string.crono_enfocar, v.nombre)
    Row(
        modifier
            .testTag("fila-${v.id}")
            .fillMaxWidth()
            .height(ALTO_FILA)
            .clip(RoundedCornerShape(12.dp))
            .background(if (v.vencido) CronoPalette.parpadeo else CronoPalette.carta)
            .clickable(onClickLabel = etiquetaEnfocar) { acciones.enfocar(v.id) }
            .padding(start = 10.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(
            Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InsigniaCarril(v, color, tamano = 20.dp)
                Text(
                    v.nombre,
                    Modifier.weight(1f, fill = false),
                    color = colorCarrilTexto(color),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                HoraPrevista(v, estilo = MaterialTheme.typography.labelMedium)
            }
            Text(
                v.titulo ?: stringResource(R.string.crono_terminado),
                Modifier.desplazarSiNoCabe(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
        FinalFila(v, acciones)
    }
}

/** Fuego, reloj (si no terminó), deshacer y, solo si toca (Hecho o Empezar), el botón pequeño. */
@Composable
private fun FinalFila(
    v: VistaCarril,
    acciones: AccionesCarril,
) {
    ChipFuego(v)
    if (!v.terminado) {
        Text(
            formatoReloj(v.quedanS),
            Modifier.padding(horizontal = 2.dp),
            color = v.colorReloj(),
            style = MaterialTheme.typography.titleLarge.merge(Tabular),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
    BotonDeshacer(v, acciones)
    if (v.accion == AccionCarril.HECHO || v.accion == AccionCarril.EMPEZAR) {
        BotonAccion(v, acciones, compacto = true)
    }
}
