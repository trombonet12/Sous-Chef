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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import app.souschef.ui.crono.trombito.TrombitoConsejero
import app.souschef.ui.shell.Icono
import app.souschef.ui.theme.CronoPalette

private const val PASO_SERVIR_MIN = 15
private val BOTON_CABECERA = 44.dp

/** A dónde puede ir el usuario desde el crono: salir, o abrir una técnica. */
class SalidasCrono(
    val salir: () -> Unit,
    val tecnica: (String) -> Unit,
)

/** Cocina en marcha: cabecera, plan, pestañas de recipientes, panel del paso y mandos. */
@Composable
fun CronoCocina(
    receta: Receta,
    estado: EstadoCrono,
    ui: CronoUi,
    viewModel: CronoViewModel,
    salidas: SalidasCrono,
) {
    val datos = DatosCrono(receta, estado, ui.ahoraMs)
    var alturaMandos by remember { mutableIntStateOf(0) }
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
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (ui.banda.isNotEmpty()) BandaPerdidos(ui, viewModel)
            Cabecera(datos, ui.reiniciarArmado, viewModel, salidas.salir)
            PlanPlegable(datos, viewModel)
            PestanasCarriles(datos, indiceFoco, viewModel::enfocar)
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), key = { datos.carriles[it].id }) { pagina ->
                PanelCarril(datos, datos.carriles[pagina], ui.tecnicas, salidas.tecnica, viewModel::enfocar)
            }
            Box(Modifier.onSizeChanged { alturaMandos = it.height }) {
                datos.carriles.getOrNull(indiceFoco)?.let { MandosCarril(datos, it, viewModel) }
            }
        }
        TrombitoConsejero(
            ui.consejo,
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = with(LocalDensity.current) { alturaMandos.toDp() } + 4.dp),
        )
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
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onReiniciar, modifier.heightIn(min = 48.dp)) {
        Text(
            stringResource(if (armado) R.string.crono_reiniciar_seguro else R.string.crono_reiniciar),
            color = if (armado) CronoPalette.alerta else CronoPalette.tenue,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (armado) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun Cabecera(
    datos: DatosCrono,
    reiniciarArmado: Boolean,
    viewModel: CronoViewModel,
    onSalir: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onSalir, Modifier.size(BOTON_CABECERA)) {
            Icon(Icono.Atras.vector, stringResource(R.string.atras))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ viewModel.cambiarServir(-PASO_SERVIR_MIN) }, Modifier.size(BOTON_CABECERA)) {
                Icon(Icono.Menos.vector, stringResource(R.string.ficha_servir_menos))
            }
            DatoCabecera(R.string.crono_servir, formatoHora(datos.estado.servirMs), CronoPalette.texto)
            IconButton({ viewModel.cambiarServir(PASO_SERVIR_MIN) }, Modifier.size(BOTON_CABECERA)) {
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
        MenuCocina(reiniciarArmado, viewModel)
    }
}

@Composable
private fun DatoCabecera(
    rotulo: Int,
    valor: String,
    color: Color,
) {
    Column(Modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(rotulo), style = MaterialTheme.typography.labelSmall, color = CronoPalette.tenue)
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium.merge(Tabular),
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/** Menú de tres puntos: reiniciar (doble toque, el primero pide confirmación) y abandonar (con diálogo). */
@Composable
private fun MenuCocina(
    reiniciarArmado: Boolean,
    viewModel: CronoViewModel,
) {
    var abierto by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }
    Box {
        IconButton({ abierto = true }, Modifier.size(BOTON_CABECERA)) {
            Icon(Icono.Mas.vector, stringResource(R.string.menu_mas))
        }
        DropdownMenu(abierto, { abierto = false }, containerColor = CronoPalette.carta2) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (reiniciarArmado) R.string.crono_reiniciar_seguro else R.string.crono_reiniciar,
                        ),
                        color = if (reiniciarArmado) CronoPalette.alerta else CronoPalette.texto,
                        fontWeight = if (reiniciarArmado) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                onClick = {
                    if (reiniciarArmado) abierto = false
                    viewModel.reiniciar()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.crono_abandonar), color = CronoPalette.texto) },
                onClick = {
                    abierto = false
                    confirmar = true
                },
            )
        }
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            containerColor = CronoPalette.carta2,
            titleContentColor = CronoPalette.texto,
            textContentColor = CronoPalette.apagado,
            title = { Text(stringResource(R.string.crono_abandonar_titulo)) },
            text = { Text(stringResource(R.string.crono_abandonar_detalle)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = false
                    viewModel.abandonar()
                }) {
                    Text(stringResource(R.string.crono_abandonar_si), color = CronoPalette.alerta)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) {
                    Text(stringResource(R.string.crono_abandonar_no), color = CronoPalette.texto)
                }
            },
        )
    }
}
