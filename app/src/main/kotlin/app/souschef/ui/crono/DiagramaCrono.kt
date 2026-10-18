package app.souschef.ui.crono

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.souschef.R
import app.souschef.ui.ficha.DiagramaTiempos
import app.souschef.ui.ficha.PaletaDiagrama
import app.souschef.ui.shell.Icono
import app.souschef.ui.theme.CronoPalette

/** Ancho por minuto del plan: las recetas largas se ensanchan y se recorren con scroll horizontal. */
private val DP_POR_MINUTO = 10.dp
private const val MS_MINUTO = 60_000.0

/**
 * Minuto del plan en que estamos (0 = empezar, `total` = servir), o null fuera de ese tramo. El inicio es
 * `servir − total` con el total redondeado a 0,5 min, como el motor (`Plano.totalMs`).
 */
internal fun minutoDelPlan(
    ahoraMs: Long,
    servirMs: Long,
    totalMin: Double,
): Double? {
    val total = Math.round(totalMin * 2) / 2.0
    val minuto = (ahoraMs - (servirMs - total * MS_MINUTO)) / MS_MINUTO
    return minuto.takeIf { it in 0.0..total }
}

/** El diagrama de tiempos de la ficha, a pantalla completa y navegable, con una línea donde va la cocina. */
@Composable
internal fun DialogoDiagrama(
    datos: DatosCrono,
    onCerrar: () -> Unit,
) {
    val receta = datos.receta
    val paleta =
        PaletaDiagrama(
            meta = CronoPalette.tenue,
            leyenda = CronoPalette.apagado,
            fondoBarra = CronoPalette.carta2,
            rejilla = CronoPalette.linea,
            borde = CronoPalette.fondo,
            ahora = CronoPalette.texto,
            colorDe = { datos.color(it?.id.orEmpty()) },
        )
    val ahora = minutoDelPlan(datos.ahoraMs, datos.estado.servirMs, receta.totalEfectivo)
    Dialog(onCerrar, DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxSize().testTag("diagrama"),
            color = CronoPalette.fondo,
            contentColor = CronoPalette.texto,
        ) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.crono_diagrama),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(
                                R.string.crono_diagrama_horas,
                                formatoHora(datos.estado.servirMs - (receta.totalEfectivo * MS_MINUTO).toLong()),
                                formatoHora(datos.estado.servirMs),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = CronoPalette.tenue,
                        )
                    }
                    IconButton(onCerrar) { Icon(Icono.Cerrar.vector, stringResource(R.string.crono_diagrama_cerrar)) }
                }
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                ) {
                    val ancho = maxOf(maxWidth - 32.dp, DP_POR_MINUTO * receta.totalEfectivo.toFloat())
                    DiagramaTiempos(receta, paleta, Modifier.width(ancho).padding(16.dp), ahora)
                }
            }
        }
    }
}
