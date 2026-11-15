package app.trombito.souschef.ui.crono

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trombito.souschef.R
import app.trombito.souschef.receta.crono.OrdenPaso
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.theme.CronoPalette
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
                maxLines = 2,
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
    val pista = CronoPalette.linea
    val marcaAvisada = CronoPalette.tenue
    val marcaPendiente = CronoPalette.texto
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(v.progreso, 0f..1f) },
    ) {
        val alto = size.height
        drawRoundRect(pista, size = Size(size.width, alto), cornerRadius = CornerRadius(alto / 2))
        if (v.progreso > 0f) {
            drawRoundRect(color, size = Size(size.width * v.progreso, alto), cornerRadius = CornerRadius(alto / 2))
        }
        v.hitos.forEach {
            val x = size.width * it.fraccion
            val marca = if (it.avisado) marcaAvisada else marcaPendiente
            drawLine(marca, Offset(x, 0f), Offset(x, alto), strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
internal fun LineasMetoSaco(v: VistaCarril) {
    val meto = v.meto?.takeIf { it.isNotBlank() }
    if (meto != null) LineaEtiquetada(R.string.crono_meto, CronoPalette.meto, meto, maxLineas = 2)
    val saco = v.saco?.takeIf { it.isNotBlank() }
    if (saco != null) LineaEtiquetada(R.string.crono_saco, CronoPalette.saco, saco, maxLineas = 2)
}

/** La nota del paso es la señal: letra normal y sin serifa, a 16 sp, hasta 2 líneas. */
@Composable
internal fun NotaDelPaso(v: VistaCarril) {
    val nota = v.nota?.takeIf { it.isNotBlank() } ?: return
    val etiqueta = stringResource(R.string.crono_senal)
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = CronoPalette.acento, fontWeight = FontWeight.ExtraBold)) {
                append("$etiqueta  ")
            }
            append(textoRico(nota))
        },
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private val POTENCIA_REPETIDA = Regex("""^(.*) · fuego (\S+)$""")

/**
 * «Subir al 5 · fuego 5» repite la potencia: si el título del paso ya la dice se quita el «· fuego N» del final.
 */
internal fun quitarPotenciaRepetida(despues: String): String {
    val partes = POTENCIA_REPETIDA.matchEntire(despues) ?: return despues
    val (titulo, potencia) = partes.destructured
    val yaLaDice = Regex("""(?<![\p{L}\p{N}])${Regex.escape(potencia)}(?![\p{L}\p{N}])""").containsMatchIn(titulo)
    return if (yaLaDice) titulo else despues
}

/**
 * El título del paso siguiente sin repetir la potencia: el «· fuego N» del final solo queda si ese paso la cambia
 * ([fuegoSiguiente] distinto del [fuego] de ahora) y el título no la dice ya.
 */
internal fun luegoSinPotencia(
    despues: String,
    fuego: Int?,
    fuegoSiguiente: Int?,
): String {
    val partes = POTENCIA_REPETIDA.matchEntire(despues) ?: return despues
    return if (fuego != null && fuego == fuegoSiguiente) partes.groupValues[1] else quitarPotenciaRepetida(despues)
}

/** «Luego: Subir al 5 · 2 min» (o «Último paso»): el paso siguiente del carril, con su duración en el formato corto. */
@Composable
internal fun textoLuego(v: VistaCarril): String {
    val despues = v.despues ?: return stringResource(R.string.crono_ultimo)
    val limpio = luegoSinPotencia(despues, v.fuego, v.despuesF)
    return OrdenPaso.duracionCorta(v.despuesS)?.let { stringResource(R.string.crono_luego_tiempo, limpio, it) }
        ?: stringResource(R.string.crono_luego_corto, limpio)
}

/** Lo que lee TalkBack en vez de «a la vez que ②»: «a la vez que el carril 2». Null si el paso no tiene `junto`. */
@Composable
internal fun descripcionJunto(v: VistaCarril): String? =
    v.juntoN?.let { stringResource(R.string.crono_a11y_a_la_vez_carril, it) }
