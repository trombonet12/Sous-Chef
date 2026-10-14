package app.souschef.ui.crono

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.souschef.R
import app.souschef.receta.Receta
import app.souschef.receta.crono.ClaseEstado
import app.souschef.receta.crono.ConsultasCrono
import app.souschef.receta.crono.EstadoCrono
import app.souschef.ui.comun.Tabular
import app.souschef.ui.shell.Icono
import app.souschef.ui.theme.CronoPalette

private const val CHIP_ALFA = 0.18f

@Composable
internal fun PlanPlegable(
    datos: DatosCrono,
    viewModel: CronoViewModel,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    val plan = datos.plan
    val aviso = plan.vencido != null || plan.tarde
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = CronoPalette.carta,
        border = BorderStroke(1.dp, if (aviso) CronoPalette.bordeMal else CronoPalette.linea),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { abierto = !abierto }
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    cabeceraPlan(datos),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (aviso) CronoPalette.alerta else CronoPalette.apagado,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icono.Atras.vector,
                    stringResource(if (abierto) R.string.crono_plan_plegar else R.string.crono_plan_desplegar),
                    Modifier.size(40.dp).padding(8.dp).rotate(if (abierto) ROTAR_PLEGAR else ROTAR_DESPLEGAR),
                    tint = CronoPalette.apagado,
                )
            }
            if (abierto) CuerpoPlan(datos, viewModel)
        }
    }
}

private const val ROTAR_PLEGAR = 90f
private const val ROTAR_DESPLEGAR = -90f
private const val MIN_POR_HORA = 60
private const val MS_POR_MIN = 60_000L

@Composable
private fun duracion(minutos: Int): String =
    when {
        minutos < MIN_POR_HORA -> stringResource(R.string.crono_duracion_min, minutos)
        minutos % MIN_POR_HORA == 0 -> stringResource(R.string.crono_duracion_h, minutos / MIN_POR_HORA)
        else -> stringResource(R.string.crono_duracion_hmin, minutos / MIN_POR_HORA, minutos % MIN_POR_HORA)
    }

@Composable
private fun cabeceraPlan(datos: DatosCrono): String {
    val plan = datos.plan
    val vencido = plan.vencido
    val adelanto = ((datos.estado.servirMs - plan.llegadaMs) / MS_POR_MIN).toInt()
    return when {
        vencido != null -> {
            stringResource(R.string.crono_plan_vencido, datos.nombre(vencido))
        }

        plan.tarde -> {
            stringResource(R.string.crono_plan_tarde, plan.minutosPorDetras)
        }

        datos.sinEmpezar && adelanto > 1 -> {
            stringResource(R.string.crono_plan_sin_empezar, duracion(adelanto))
        }

        datos.sinEmpezar -> {
            stringResource(R.string.crono_plan_sin_empezar_justo, formatoHora(plan.llegadaMs))
        }

        else -> {
            stringResource(R.string.crono_plan_en_hora)
        }
    }
}

@Composable
private fun CuerpoPlan(
    datos: DatosCrono,
    viewModel: CronoViewModel,
) {
    val plan = datos.plan
    val vivos =
        datos.carriles
            .filter { datos.ec(it.id)?.fin == false }
            .sortedBy { datos.vista(it.id).clase.orden }
    Column(
        Modifier.padding(horizontal = 12.dp).padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.crono_ahora_toca),
            style = MaterialTheme.typography.labelSmall,
            color = CronoPalette.tenue,
        )
        vivos.forEach { c -> FilaPlan(datos, c.id) { viewModel.enfocar(c.id) } }
        if (plan.tarde) {
            Text(
                stringResource(
                    R.string.crono_prevista_realista,
                    formatoHora(datos.estado.servirMs),
                    formatoHora(plan.llegadaMs),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (plan.aguantan.isNotEmpty()) {
                Text(
                    stringResource(R.string.crono_aguantan, plan.aguantan.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (plan.noEsperan.isNotEmpty()) {
                Text(
                    stringResource(R.string.crono_no_esperan, plan.noEsperan.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Línea de la cronología: hora tabular, chip del recipiente, paso (1 línea) y estado si no es solo espera. */
@Composable
private fun FilaPlan(
    datos: DatosCrono,
    carrilId: String,
    onClick: () -> Unit,
) {
    val carril = datos.carril(carrilId) ?: return
    val vista = datos.vista(carrilId)
    val color = datos.color(carrilId)
    val hora = ConsultasCrono.sugerido(datos.estado, datos.receta, carrilId) ?: datos.ahoraMs
    val simple = vista.clase == ClaseEstado.ESPERA || vista.clase == ClaseEstado.YA
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            formatoHora(hora),
            Modifier.width(52.dp),
            color = CronoPalette.apagado,
            style = MaterialTheme.typography.labelLarge.merge(Tabular),
        )
        Text(
            carril.nombre,
            Modifier
                .width(84.dp)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = CHIP_ALFA))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            datos.paso(carrilId)?.t.orEmpty(),
            Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!simple) {
            Text(
                textoEstado(datos, vista),
                Modifier.widthIn(max = 96.dp),
                color = CronoPalette.apagado,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
