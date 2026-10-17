package app.souschef.ui.crono

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.souschef.R
import app.souschef.ui.comun.Tabular
import app.souschef.ui.theme.CronoPalette

private val NOMBRE_MAX = 110.dp

/**
 * Contenido de la tarjeta cuando falta alto (4 carriles o letra grande): arriba número, nombre, paso, fuego y tapa;
 * abajo el reloj con su barra, los mandos secundarios y el botón principal. Tocar la fila de arriba abre el porqué.
 */
@Composable
internal fun ContenidoCompacto(
    v: VistaCarril,
    color: Color,
    acciones: AccionesCarril,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        CabeceraCompacta(v, color, acciones)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    formatoReloj(v.quedanS),
                    color = v.colorReloj(),
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.displaySmall.merge(Tabular),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 44.sp),
                )
                BarraProgreso(v, if (v.quedanS < 0) CronoPalette.alerta else color)
            }
            MandosSecundarios(v, acciones, compacto = true)
            if (v.accion != AccionCarril.BLOQUEADO) BotonAccion(v, acciones, compacto = true)
        }
    }
}

@Composable
private fun CabeceraCompacta(
    v: VistaCarril,
    color: Color,
    acciones: AccionesCarril,
) {
    val etiquetaPorque = stringResource(R.string.crono_ver_porque)
    val toque =
        if (v.tienePorque) Modifier.clickable(onClickLabel = etiquetaPorque) { acciones.porque(v.id) } else Modifier
    Row(
        Modifier.fillMaxWidth().then(toque),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InsigniaCarril(v, color, tamano = 20.dp)
        Text(
            v.nombre,
            Modifier.widthIn(max = NOMBRE_MAX),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            v.titulo.orEmpty(),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ChipFuego(v)
        // «destapada» es lo normal: en compacto solo se avisa de la tapa puesta o a medias.
        if (recursoTapa(v.tapa) != R.string.crono_destapada) ChipTapa(v)
        if (v.tienePorque) IconoPorque()
    }
}
