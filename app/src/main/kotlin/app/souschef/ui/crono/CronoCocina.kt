package app.souschef.ui.crono

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.souschef.R
import app.souschef.receta.Receta
import app.souschef.receta.crono.EstadoCrono
import app.souschef.ui.comun.Tabular
import app.souschef.ui.comun.colorCarril
import app.souschef.ui.shell.Icono
import app.souschef.ui.theme.CronoPalette

private const val PASO_SERVIR_MIN = 15

/** Cocina en marcha: cabecera, plan, pestañas de recipientes, panel del paso y mandos. */
@Composable
fun CronoCocina(
    receta: Receta,
    estado: EstadoCrono,
    ui: CronoUi,
    viewModel: CronoViewModel,
    onSalir: () -> Unit,
) {
    val datos = DatosCrono(receta, estado, ui.ahoraMs)
    val indiceFoco = datos.carriles.indexOfFirst { it.id == estado.foco }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = indiceFoco) { datos.carriles.size }
    val focoActual = rememberUpdatedState(estado.foco)
    LaunchedEffect(indiceFoco) { if (pager.currentPage != indiceFoco) pager.animateScrollToPage(indiceFoco) }
    LaunchedEffect(pager, datos.carriles) {
        snapshotFlow { pager.settledPage }.collect { pagina ->
            datos.carriles
                .getOrNull(pagina)
                ?.takeIf { it.id != focoActual.value }
                ?.let { viewModel.enfocar(it.id) }
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (ui.banda.isNotEmpty()) BandaPerdidos(ui, viewModel)
        Cabecera(datos, viewModel, onSalir)
        PlanPlegable(datos, ui.reiniciarArmado, viewModel)
        PestanasCarriles(datos, indiceFoco, viewModel::enfocar)
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), key = { datos.carriles[it].id }) { pagina ->
            PanelCarril(datos, datos.carriles[pagina], viewModel::enfocar)
        }
        datos.carriles.getOrNull(indiceFoco)?.let { MandosCarril(datos, it, viewModel) }
    }
}

@Composable
private fun BandaPerdidos(
    ui: CronoUi,
    viewModel: CronoViewModel,
) {
    Column(Modifier.fillMaxWidth().background(CronoPalette.carta2).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.crono_mientras_no_mirabas),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = CronoPalette.acento,
            )
            BotonReiniciar(ui.reiniciarArmado, viewModel::reiniciar)
            IconButton(onClick = viewModel::cerrarBanda, Modifier.heightIn(min = 48.dp)) {
                Icon(Icono.Cerrar.vector, stringResource(R.string.crono_cerrar_aviso))
            }
        }
        ui.banda.forEach {
            Text(
                stringResource(R.string.crono_linea_perdida, it.carril, it.texto, it.haceMin),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** REINICIAR con doble toque: el primero pide confirmación 4 s en rojo. */
@Composable
private fun BotonReiniciar(
    armado: Boolean,
    onReiniciar: () -> Unit,
) {
    TextButton(onClick = onReiniciar, Modifier.heightIn(min = 48.dp)) {
        Text(
            stringResource(if (armado) R.string.crono_reiniciar_seguro else R.string.crono_reiniciar),
            color = if (armado) CronoPalette.alerta else CronoPalette.apagado,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Cabecera(
    datos: DatosCrono,
    viewModel: CronoViewModel,
    onSalir: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onSalir, Modifier.heightIn(min = 48.dp)) {
            Icon(Icono.Atras.vector, stringResource(R.string.atras))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ viewModel.cambiarServir(-PASO_SERVIR_MIN) }, Modifier.heightIn(min = 48.dp)) {
                Icon(Icono.Menos.vector, stringResource(R.string.ficha_servir_menos))
            }
            DatoCabecera(R.string.crono_servir, formatoHora(datos.estado.servirMs), CronoPalette.texto)
            IconButton({ viewModel.cambiarServir(PASO_SERVIR_MIN) }, Modifier.heightIn(min = 48.dp)) {
                Icon(Icono.Anadir.vector, stringResource(R.string.ficha_servir_mas))
            }
        }
        DatoCabecera(R.string.crono_ahora, formatoHora(datos.ahoraMs), CronoPalette.texto)
        val llegada = datos.plan
        DatoCabecera(
            R.string.crono_llegada,
            formatoHora(llegada.llegadaMs),
            if (llegada.tarde) CronoPalette.alerta else CronoPalette.ok,
        )
    }
}

@Composable
private fun DatoCabecera(
    rotulo: Int,
    valor: String,
    color: Color,
) {
    Column(Modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(rotulo), style = MaterialTheme.typography.labelSmall, color = CronoPalette.tenue)
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium.merge(Tabular),
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun PlanPlegable(
    datos: DatosCrono,
    reiniciarArmado: Boolean,
    viewModel: CronoViewModel,
) {
    var abierto by rememberSaveable { mutableStateOf(true) }
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
                    .heightIn(
                        min = 48.dp,
                    ).clickable { abierto = !abierto }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(cabeceraPlan(datos), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(if (abierto) "▴" else "▾", color = CronoPalette.apagado)
            }
            if (abierto) CuerpoPlan(datos, reiniciarArmado, viewModel)
        }
    }
}

@Composable
private fun cabeceraPlan(datos: DatosCrono): String {
    val plan = datos.plan
    val vencido = plan.vencido
    return when {
        vencido != null -> {
            stringResource(R.string.crono_plan_vencido, datos.nombre(vencido))
        }

        plan.tarde -> {
            stringResource(R.string.crono_plan_tarde, plan.minutosPorDetras, formatoHora(plan.llegadaMs))
        }

        else -> {
            stringResource(
                R.string.crono_plan_en_hora,
                formatoHora(plan.llegadaMs),
                formatoHora(datos.estado.servirMs),
            )
        }
    }
}

@Composable
private fun CuerpoPlan(
    datos: DatosCrono,
    reiniciarArmado: Boolean,
    viewModel: CronoViewModel,
) {
    val plan = datos.plan
    val vivos =
        datos.carriles
            .filter { datos.ec(it.id)?.fin == false }
            .sortedBy { datos.vista(it.id).clase.orden }
    Column(
        Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.crono_ahora_toca),
            style = MaterialTheme.typography.labelSmall,
            color = CronoPalette.tenue,
        )
        vivos.forEachIndexed { i, c -> FilaPlan(datos, i + 1, c.id) { viewModel.enfocar(c.id) } }
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            BotonReiniciar(reiniciarArmado, viewModel::reiniciar)
        }
    }
}

@Composable
private fun FilaPlan(
    datos: DatosCrono,
    numero: Int,
    carrilId: String,
    onClick: () -> Unit,
) {
    val carril = datos.carril(carrilId) ?: return
    val vista = datos.vista(carrilId)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("$numero", color = CronoPalette.tenue, fontSize = 13.sp)
        Text(carril.nombre, color = colorCarril(carril.color), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(
            datos.paso(carrilId)?.t.orEmpty(),
            Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
        )
        Text(
            textoEstado(datos, vista, mostrarTocaba = datos.plan.tarde),
            color = CronoPalette.apagado,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
