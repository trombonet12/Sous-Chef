package app.souschef.ui.crono

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.souschef.R
import app.souschef.ui.comun.Tabular
import app.souschef.ui.comun.textoRico
import app.souschef.ui.theme.CronoPalette
import app.souschef.ui.theme.LocalFonts
import kotlin.math.roundToInt

@Composable
internal fun BloqueReloj(
    v: VistaCarril,
    color: Color,
    relojSp: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Text(
            formatoReloj(v.quedanS),
            Modifier.fillMaxWidth(),
            color = v.colorReloj(),
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.displayLarge.merge(Tabular),
            textAlign = TextAlign.Center,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = relojSp.sp),
        )
        BarraProgreso(v, if (v.quedanS < 0) CronoPalette.alerta else color)
        v.proximoHito?.let { h ->
            val segundos = (h.fraccion * v.duracionS).roundToInt()
            Text(
                stringResource(R.string.crono_proximo_hito, formatoReloj(segundos), h.texto),
                Modifier.fillMaxWidth().padding(top = 2.dp),
                color = CronoPalette.acento,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun BarraProgreso(
    v: VistaCarril,
    color: Color,
) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(v.progreso, 0f..1f) },
    ) {
        val alto = size.height
        drawRoundRect(CronoPalette.linea, size = Size(size.width, alto), cornerRadius = CornerRadius(alto / 2))
        if (v.progreso > 0f) {
            drawRoundRect(color, size = Size(size.width * v.progreso, alto), cornerRadius = CornerRadius(alto / 2))
        }
        v.hitos.forEach {
            val x = size.width * it.fraccion
            val marca = if (it.avisado) CronoPalette.tenue else CronoPalette.texto
            drawLine(marca, Offset(x, 0f), Offset(x, alto), strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
internal fun LineasMetoSaco(v: VistaCarril) {
    val meto = v.meto?.takeIf { it.isNotBlank() }
    if (meto != null) LineaEtiquetada(R.string.crono_meto, CronoPalette.meto, meto, maxLineas = 1)
    val saco = v.saco?.takeIf { it.isNotBlank() }
    if (saco != null) LineaEtiquetada(R.string.crono_saco, CronoPalette.saco, saco, maxLineas = 1)
}

@Composable
internal fun NotaDelPaso(v: VistaCarril) {
    val nota = v.nota?.takeIf { it.isNotBlank() } ?: return
    Text(
        textoRico(nota),
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = LocalFonts.current.newsreader,
        fontStyle = FontStyle.Italic,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
