package app.trombito.souschef.ui.crono

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.theme.CronoPalette

private val ALTO_FILA = 64.dp

/**
 * Fila compacta (64 dp como mínimo) de un carril que no está en foco: nombre, hora prevista, título del paso (1 línea,
 * quieto), fuego, reloj y, solo si toca (Hecho o Empezar), un botón pequeño. Tocar la fila la fija en el foco.
 */
@Composable
fun FilaCarrilCompacta(
    v: VistaCarril,
    acciones: AccionesCarril,
    modifier: Modifier = Modifier,
) {
    val color = colorDeCarril(v.colorHex, v.indiceCarril)
    val etiquetaEnfocar = stringResource(R.string.crono_enfocar, v.nombre)
    BoxWithConstraints(
        modifier
            .testTag("fila-${v.id}")
            .fillMaxWidth()
            .heightIn(min = ALTO_FILA)
            .clip(RoundedCornerShape(12.dp))
            .background(if (v.vencido) CronoPalette.parpadeo else CronoPalette.carta)
            .avisoVencido(v)
            .clickable(onClickLabel = etiquetaEnfocar) { acciones.enfocar(v.id) }
            .padding(start = 10.dp, end = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Con letra grande (o pantalla estrecha) el reloj y los mandos no dejan sitio a la cabecera: bajan a su línea.
        val apilar = maxWidth.value / maxOf(1f, LocalDensity.current.fontScale) < ANCHO_FILA_UNA_LINEA_DP
        if (apilar) {
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FranjaCarril(color, 36.dp)
                    CabeceraFila(v, color, Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) { FinalFila(v, acciones) }
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FranjaCarril(color, 36.dp)
                CabeceraFila(v, color, Modifier.weight(1f))
                FinalFila(v, acciones)
            }
        }
    }
}

// Ancho (en dp a letra 1,0×) por debajo del cual la fila pone el reloj y los mandos en una segunda línea.
private const val ANCHO_FILA_UNA_LINEA_DP = 340f

/**
 * Cabecera de la fila: el nombre tiene la prioridad de ancho; lo que cede es el título (1 línea con «…») y la hora
 * prevista, que baja a la línea del título.
 */
@Composable
private fun CabeceraFila(
    v: VistaCarril,
    color: Color,
    modifier: Modifier,
) {
    CabeceraPaso(
        insignia = { InsigniaCarril(v, color, tamano = 20.dp) },
        nombre = { NombreCarril(v, color, MaterialTheme.typography.labelMedium) },
        prevista = { HoraPrevista(v, estilo = MaterialTheme.typography.labelMedium) },
        extras = {},
        titulo = {
            Text(
                v.titulo ?: stringResource(R.string.crono_terminado),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
    )
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
