@file:Suppress("TooManyFunctions") // cabecera, menú, diálogos y hojas de la cocina

package app.trombito.souschef.ui.crono

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.receta.crono.ConsultasCrono
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
            )
        Cabecera(datos, ui.reiniciarArmado, viewModel, menu, slotMicrofono)
        BandaTemporizadores(estado, ui.ahoraMs, viewModel::quitarTemporizador)
        // Sin nada arrancado, el paso que toca primero lleva EMPEZAR lleno aunque falte más de 3 min (el plan sigue al
        // primer arranque, ADR-0018).
        val primero = if (ConsultasCrono.arrancada(estado)) null else primerCarrilSinArrancar(vistasCarriles(datos))
        CompositionLocalProvider(LocalPrimerArranque provides primero) {
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
    }
    HojaPorque(datos, porque, ui, salidas) { porque = null }
    if (diagrama) DialogoDiagrama(datos) { diagrama = false }
    if (interfaz) DialogoInterfaz(ui.disposicion, viewModel::elegirDisposicion) { interfaz = false }
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
    PorqueSheet(
        paso,
        carril,
        datos.color(id),
        ui.tecnicas,
        salidas.tecnica,
        onCerrar,
        contexto = contextoPasoCrono(datos, paso),
    )
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

/**
 * Cabecera: atrás, «Llegas a las HH:mm» (siempre del mismo color, sin diferencia ni rojo), micrófono y menú ⋮. La hora
 * de servir ya no está en la cabecera: se ajusta con un toque en «Llegas a las…» ([DialogoServir]).
 */
@Composable
private fun Cabecera(
    datos: DatosCrono,
    reiniciarArmado: Boolean,
    viewModel: CronoViewModel,
    menu: AccionesMenu,
    slotMicrofono: @Composable () -> Unit,
) {
    var ajustarServir by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = menu.volver, Modifier.size(BOTON_CABECERA)) {
            Icon(Icono.Atras.vector, stringResource(R.string.atras))
        }
        // Tras el primer arranque la hora de servir ya no mueve nada (ADR-0018): «Llegas a las…» solo informa.
        val ajustable = !ConsultasCrono.arrancada(datos.estado)
        LlegadaCabecera(
            formatoHora(datos.plan.llegadaMs),
            Modifier.weight(1f),
            if (ajustable) ({ ajustarServir = true }) else null,
            gramosPara = datos.receta.raciones.takeIf { datos.estado.raciones != it },
        )
        slotMicrofono()
        MenuCocina(reiniciarArmado, datos.estado.libresLlenos(), viewModel, menu)
    }
    if (ajustarServir) {
        DialogoServir(datos.estado.servirMs, viewModel::cambiarServir) { ajustarServir = false }
    }
}

/** «Llegas a las 15:21»: un toque abre el ajuste de la hora de servir. */
@Composable
internal fun LlegadaCabecera(
    hora: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?,
    gramosPara: Int? = null,
) {
    val descripcion = stringResource(R.string.crono_a11y_llegas_a_las, hora)
    val etiqueta = stringResource(R.string.crono_llegas_ajustar)
    val base = modifier.heightIn(min = BOTON_CABECERA).testTag("llegada")
    Box(
        (if (onClick != null) base.clickable(onClickLabel = etiqueta, role = Role.Button, onClick = onClick) else base)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(Modifier.padding(horizontal = 8.dp)) {
            Text(
                stringResource(R.string.crono_llegas_a_las, hora),
                style = MaterialTheme.typography.titleMedium.merge(Tabular),
                fontWeight = FontWeight.Bold,
                color = CronoPalette.texto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // METE y SACA llevan los gramos de la receta base: con otras raciones, aviso discreto (M13).
            if (gramosPara != null) {
                Text(
                    pluralStringResource(R.plurals.crono_gramos_para_raciones, gramosPara, gramosPara),
                    color = CronoPalette.apagado,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Hora de servir con − y + de 15 en 15 minutos; la llegada de la cabecera se recalcula sola. */
@Composable
private fun DialogoServir(
    servirMs: Long,
    onCambiar: (Int) -> Unit,
    onCerrar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = CronoPalette.carta2,
        titleContentColor = CronoPalette.texto,
        title = { Text(stringResource(R.string.crono_servir_a_las)) },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton({ onCambiar(-PASO_SERVIR_MIN) }, Modifier.size(BOTON_CABECERA)) {
                    Icon(Icono.Menos.vector, stringResource(R.string.ficha_servir_menos))
                }
                Text(
                    formatoHora(servirMs),
                    Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.headlineMedium.merge(Tabular),
                    fontWeight = FontWeight.Bold,
                    color = CronoPalette.texto,
                )
                IconButton({ onCambiar(PASO_SERVIR_MIN) }, Modifier.size(BOTON_CABECERA)) {
                    Icon(Icono.Anadir.vector, stringResource(R.string.ficha_servir_mas))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(R.string.crono_diagrama_cerrar_corto), color = CronoPalette.texto)
            }
        },
    )
}

/** Lo que abre el menú de tres puntos y la flecha de la cabecera. */
internal class AccionesMenu(
    /** Volver a la ficha sin abandonar (flecha y gesto atrás). */
    val volver: () -> Unit,
    val abandonar: () -> Unit,
    val diagrama: () -> Unit,
    val interfaz: () -> Unit,
    val temporizador: () -> Unit = {},
    val ordenes: () -> Unit = {},
)

/**
 * Menú de tres puntos: diagrama de tiempos, interfaz, temporizador, órdenes de voz, reiniciar (doble
 * toque, el primero pide confirmación) y abandonar (con diálogo).
 */
@Composable
private fun MenuCocina(
    reiniciarArmado: Boolean,
    libresLlenos: Boolean,
    viewModel: CronoViewModel,
    menu: AccionesMenu,
) {
    var abierto by remember { mutableStateOf(false) }
    var temporizador by remember { mutableStateOf(false) }
    var ordenes by remember { mutableStateOf(false) }
    Box {
        IconButton({ abierto = true }, Modifier.size(BOTON_CABECERA).guia(GuiaIds.MENU)) {
            Icon(Icono.Mas.vector, stringResource(R.string.menu_mas))
        }
        DropdownMenu(abierto, { abierto = false }, containerColor = CronoPalette.carta2) {
            EntradasMenuCocina(
                reiniciarArmado,
                AccionesMenu(
                    menu.volver,
                    menu.abandonar,
                    menu.diagrama,
                    menu.interfaz,
                    temporizador = { temporizador = true },
                    ordenes = { ordenes = true },
                ),
                cerrar = { abierto = false },
                onReiniciar = viewModel::reiniciar,
            )
        }
        if (ordenes) HojaOrdenesCocina { ordenes = false }
        if (temporizador) {
            DialogoTemporizador(libresLlenos, { viewModel.temporizador(it) }) { temporizador = false }
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
