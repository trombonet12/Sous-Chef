package app.trombito.souschef.ui.crono

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.ui.ajustes.SelectorDisposicion
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.crono.guia.GuiaIds
import app.trombito.souschef.ui.crono.guia.guia
import app.trombito.souschef.ui.crono.voz.ColoresOrdenesVoz
import app.trombito.souschef.ui.crono.voz.HojaOrdenesVoz
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette

private const val PASO_SERVIR_MIN = 15
private val BOTON_CABECERA = 48.dp

/** A dónde puede ir el usuario desde el crono: salir, o abrir una técnica. */
class SalidasCrono(
    val salir: () -> Unit,
    val tecnica: (String) -> Unit,
)

/**
 * Cocina en marcha: cabecera y los carriles repartidos según la disposición elegida en Ajustes. Todo a la vista, sin
 * scroll; solo "Ver el porqué" (hoja) y el menú de tres puntos quedan detrás de un toque.
 */
@Composable
fun CronoCocina(
    ui: CronoUi,
    viewModel: CronoViewModel,
    salidas: SalidasCrono,
    slotMicrofono: @Composable () -> Unit = {},
) {
    val receta = ui.receta ?: return
    val estado = ui.estado ?: return
    val datos = DatosCrono(receta, estado, ui.ahoraMs, ui.cocina, ui.recetaUi ?: receta)
    var porque by remember { mutableStateOf<Pair<String, Int>?>(null) }
    // Atrás vuelve a la ficha con la cocinada viva (sigue en segundo plano); abandonar solo desde el menú ⋮, o al
    // salir de la ficha.
    var abandonar by remember { mutableStateOf(false) }
    var diagrama by remember { mutableStateOf(false) }
    var interfaz by remember { mutableStateOf(false) }
    var ordenes by remember { mutableStateOf(false) }
    var ingredientes by remember { mutableStateOf(false) }
    BackHandler { salidas.salir() }
    val acciones =
        AccionesCarril(
            hecho = viewModel::hecho,
            alternar = viewModel::alternar,
            masTreinta = viewModel::masTreinta,
            empezarIgual = viewModel::empezarIgual,
            enfocar = viewModel::enfocar,
            deshacer = viewModel::deshacer,
            porque = { id -> porque = id to (datos.ec(id)?.i ?: 0) },
        )
    Column(Modifier.fillMaxSize()) {
        if (ui.banda.isNotEmpty()) BandaPerdidos(ui, viewModel)
        val menu =
            AccionesMenu(
                volver = salidas.salir,
                abandonar = { abandonar = true },
                diagrama = { diagrama = true },
                interfaz = { interfaz = true },
                ordenesVoz = { ordenes = true },
                ingredientes = { ingredientes = true },
            )
        Cabecera(datos, ui.reiniciarArmado, viewModel, menu, slotMicrofono)
        CuerpoCrono(
            datos,
            ui.disposicion,
            acciones,
            ui.tecnicas,
            salidas.tecnica,
            relleno = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            rellenoClasica = PaddingValues(0.dp),
        )
    }
    HojaPorque(datos, porque, ui, salidas) { porque = null }
    if (diagrama) DialogoDiagrama(datos) { diagrama = false }
    if (interfaz) DialogoInterfaz(ui.disposicion, viewModel::elegirDisposicion) { interfaz = false }
    if (ordenes) HojaOrdenesCocina { ordenes = false }
    if (ingredientes) HojaIngredientesCocina(ui, viewModel) { ingredientes = false }
    if (abandonar) {
        DialogoAbandonar(
            onSi = {
                abandonar = false
                viewModel.abandonar()
            },
            onNo = { abandonar = false },
        )
    }
}

/** La hoja de órdenes de voz con los colores (siempre oscuros) de la cocina. */
@Composable
private fun HojaOrdenesCocina(onCerrar: () -> Unit) {
    HojaOrdenesVoz(onCerrar, ColoresOrdenesVoz(CronoPalette.carta, CronoPalette.texto, CronoPalette.acento))
}

/** La hoja del porqué se cierra sola cuando el carril cambia de paso. */
@Composable
private fun HojaPorque(
    datos: DatosCrono,
    abierto: Pair<String, Int>?,
    ui: CronoUi,
    salidas: SalidasCrono,
    onCerrar: () -> Unit,
) {
    val (id, indice) = abierto ?: return
    val paso = datos.paso(id)
    val carril = datos.carril(id)
    if (paso == null || carril == null || datos.ec(id)?.i != indice) return
    PorqueSheet(paso, carril, datos.color(id), ui.tecnicas, salidas.tecnica, onCerrar)
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
    menu: AccionesMenu,
    slotMicrofono: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = menu.volver, Modifier.size(BOTON_CABECERA)) {
            Icon(Icono.Atras.vector, stringResource(R.string.atras))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ viewModel.cambiarServir(-PASO_SERVIR_MIN) }, Modifier.size(BOTON_CABECERA)) {
                Icon(Icono.Menos.vector, stringResource(R.string.ficha_servir_menos))
            }
            DatoCabecera(stringResource(R.string.crono_servir), formatoHora(datos.estado.servirMs), CronoPalette.texto)
            IconButton({ viewModel.cambiarServir(PASO_SERVIR_MIN) }, Modifier.size(BOTON_CABECERA)) {
                Icon(Icono.Anadir.vector, stringResource(R.string.ficha_servir_mas))
            }
        }
        val plan = datos.plan
        val rotuloLlegada =
            if (plan.tarde) {
                stringResource(R.string.crono_llegada_tarde, plan.minutosPorDetras)
            } else {
                stringResource(R.string.crono_llegada)
            }
        DatoCabecera(
            rotuloLlegada,
            formatoHora(plan.llegadaMs),
            if (plan.tarde) CronoPalette.alerta else CronoPalette.ok,
            rotuloColor = if (plan.tarde) CronoPalette.alerta else CronoPalette.tenue,
        )
        slotMicrofono()
        MenuCocina(reiniciarArmado, viewModel, menu)
    }
}

@Composable
private fun DatoCabecera(
    rotulo: String,
    valor: String,
    color: Color,
    rotuloColor: Color = CronoPalette.tenue,
) {
    Column(Modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = rotuloColor)
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium.merge(Tabular),
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/** Lo que abre el menú de tres puntos y la flecha de la cabecera. */
internal class AccionesMenu(
    /** Volver a la ficha sin abandonar (flecha y gesto atrás). */
    val volver: () -> Unit,
    val abandonar: () -> Unit,
    val diagrama: () -> Unit,
    val interfaz: () -> Unit,
    val ordenesVoz: () -> Unit = {},
    val ingredientes: () -> Unit = {},
)

/**
 * Menú de tres puntos: diagrama de tiempos, ingredientes, interfaz, órdenes de voz, reiniciar (doble toque, el primero
 * pide confirmación) y abandonar (con diálogo).
 */
@Composable
private fun MenuCocina(
    reiniciarArmado: Boolean,
    viewModel: CronoViewModel,
    menu: AccionesMenu,
) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton({ abierto = true }, Modifier.size(BOTON_CABECERA).guia(GuiaIds.MENU)) {
            Icon(Icono.Mas.vector, stringResource(R.string.menu_mas))
        }
        DropdownMenu(abierto, { abierto = false }, containerColor = CronoPalette.carta2) {
            ItemMenu(R.string.crono_diagrama, { abierto = false }, menu.diagrama)
            ItemMenu(R.string.crono_ingredientes, { abierto = false }, menu.ingredientes)
            ItemMenu(R.string.ajustes_interfaz, { abierto = false }, menu.interfaz)
            ItemMenu(R.string.crono_ordenes_voz, { abierto = false }, menu.ordenesVoz)
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
            ItemMenu(R.string.crono_abandonar, { abierto = false }, menu.abandonar)
        }
    }
}

/** Elegir la disposición sin salir de la cocina; se cierra al elegir. */
@Composable
private fun DialogoInterfaz(
    actual: DisposicionCrono,
    onElegir: (DisposicionCrono) -> Unit,
    onCerrar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = CronoPalette.carta2,
        titleContentColor = CronoPalette.texto,
        title = { Text(stringResource(R.string.ajustes_interfaz)) },
        text = {
            SelectorDisposicion(actual, onElegir = {
                onElegir(it)
                onCerrar()
            })
        },
        confirmButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(R.string.crono_diagrama_cerrar_corto), color = CronoPalette.texto)
            }
        },
    )
}

/** «¿Abandonar esta cocinada?»: desde el menú, la flecha de la cabecera o el gesto atrás. */
@Composable
private fun DialogoAbandonar(
    onSi: () -> Unit,
    onNo: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onNo,
        containerColor = CronoPalette.carta2,
        titleContentColor = CronoPalette.texto,
        textContentColor = CronoPalette.apagado,
        title = { Text(stringResource(R.string.crono_abandonar_titulo)) },
        text = { Text(stringResource(R.string.crono_abandonar_detalle)) },
        confirmButton = {
            TextButton(onClick = onSi) {
                Text(stringResource(R.string.crono_abandonar_si), color = CronoPalette.alerta)
            }
        },
        dismissButton = {
            TextButton(onClick = onNo) {
                Text(stringResource(R.string.crono_abandonar_no), color = CronoPalette.texto)
            }
        },
    )
}
