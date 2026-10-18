package app.souschef.ui.crono

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import app.souschef.R
import app.souschef.ui.comun.Tabular
import app.souschef.ui.theme.CronoPalette
import kotlin.math.abs

/** "+3", "−2" o null si el paso va en hora (menos de un minuto de desvío). */
internal fun textoDesvio(desvioMin: Int): String? =
    when {
        desvioMin > 0 -> "+$desvioMin"
        desvioMin < 0 -> "−${abs(desvioMin)}"
        else -> null
    }

/** Hora prevista de empezar el paso ("14:52") y, si se desvía, los minutos de más (alerta) o de menos (ok). */
@Composable
internal fun HoraPrevista(
    v: VistaCarril,
    modifier: Modifier = Modifier,
    estilo: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val sugerido = v.sugeridoMs ?: return
    val hora = formatoHora(sugerido)
    val desvio = textoDesvio(v.desvioMin)
    val descripcion =
        when {
            v.desvioMin > 0 -> stringResource(R.string.crono_prevista_tarde, hora, v.desvioMin)
            v.desvioMin < 0 -> stringResource(R.string.crono_prevista_pronto, hora, -v.desvioMin)
            else -> stringResource(R.string.crono_prevista, hora)
        }
    Text(
        buildAnnotatedString {
            append(hora)
            if (desvio != null) {
                append(" ")
                withStyle(SpanStyle(color = if (v.desvioMin > 0) CronoPalette.alerta else CronoPalette.ok)) {
                    append(desvio)
                }
            }
        },
        modifier.semantics { contentDescription = descripcion },
        color = CronoPalette.apagado,
        style = estilo.merge(Tabular),
        maxLines = 1,
    )
}
