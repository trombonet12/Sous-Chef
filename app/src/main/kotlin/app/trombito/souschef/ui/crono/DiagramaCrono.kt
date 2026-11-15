package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import app.trombito.souschef.R
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.ui.comun.DensidadPlan
import app.trombito.souschef.ui.comun.DiagramaPlan
import app.trombito.souschef.ui.comun.SituacionPaso
import app.trombito.souschef.ui.comun.VivoPlan
import app.trombito.souschef.ui.comun.paletaPlanCrono
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette
import java.time.Instant
import java.time.ZoneId

private const val MS_MINUTO = 60_000.0
private const val MIN_HORA = 60

/** Situación de cada paso y minuto del plan por el que va la cocina, a partir del estado real (ADR-0018). */
internal fun vivoPlan(datos: DatosCrono): VivoPlan {
    val situacion = datos.receta.pasos.associate { it.id to situacionDe(datos, it) }
    val ahora =
        ahoraEnPlan(datos.receta.pasos.map { it to situacion.getValue(it.id) }) {
            progreso(it.segundos, datos.quedan(it.carril)).toDouble()
        }
    return VivoPlan(situacion, ahora)
}

private fun situacionDe(
    datos: DatosCrono,
    paso: Paso,
): SituacionPaso {
    val hecho = datos.previstos[paso.id]?.hecho ?: pasoHecho(datos.receta, datos.estado, paso.id)
    val ec = datos.estado.carriles[paso.carril]
    val enCurso =
        ec != null && !ec.fin && (ec.corriendo || ec.pausado) &&
            pasoActual(datos.receta, datos.estado, paso.carril)?.id == paso.id
    return when {
        hecho -> SituacionPaso.HECHO
        enCurso -> SituacionPaso.EN_MARCHA
        else -> SituacionPaso.PENDIENTE
    }
}

/**
 * Minuto del plan por el que va la cocina según su avance real, no según el reloj: el más avanzado entre el final
 * de los pasos hechos y el punto en que está cada paso en marcha (`off` + [avance] 0..1 de su duración). Sin ningún
 * paso hecho ni en marcha no hay línea (null). Así sigue a la cocina aunque se vaya adelantado o con retraso.
 */
internal fun ahoraEnPlan(
    pasos: List<Pair<Paso, SituacionPaso>>,
    avance: (Paso) -> Double,
): Double? =
    pasos
        .mapNotNull { (paso, situacion) ->
            when (situacion) {
                SituacionPaso.HECHO -> paso.fin
                SituacionPaso.EN_MARCHA -> paso.off + avance(paso).coerceIn(0.0, 1.0) * paso.min
                SituacionPaso.PENDIENTE -> null
            }
        }.maxOrNull()

/** Minutos desde medianoche a los que empieza el plan (servir − total, redondeado a 0,5 min como el motor). */
internal fun inicioPlanMin(datos: DatosCrono): Int {
    val totalMs = (Math.round(datos.receta.totalEfectivo * 2) / 2.0 * MS_MINUTO).toLong()
    val inicio = Instant.ofEpochMilli(datos.estado.servirMs - totalMs).atZone(ZoneId.systemDefault())
    return inicio.hour * MIN_HORA + inicio.minute
}

/** El diagrama de la ficha, a pantalla completa y vivo: hecho apagado, en marcha lleno y la línea «ahora». */
@Composable
internal fun DialogoDiagrama(
    datos: DatosCrono,
    onCerrar: () -> Unit,
) {
    val receta = datos.recetaUi
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
                                formatoHora(
                                    datos.estado.servirMs -
                                        (Math.round(receta.totalEfectivo * 2) / 2.0 * MS_MINUTO).toLong(),
                                ),
                                formatoHora(datos.estado.servirMs),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = CronoPalette.tenue,
                        )
                    }
                    IconButton(onCerrar) { Icon(Icono.Cerrar.vector, stringResource(R.string.crono_diagrama_cerrar)) }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    DiagramaPlan(
                        receta,
                        DensidadPlan.DETALLE,
                        paleta = paletaPlanCrono(),
                        inicioPlanMin = inicioPlanMin(datos),
                        vivo = vivoPlan(datos),
                    )
                }
            }
        }
    }
}
