package app.trombito.souschef.ui.crono

import androidx.annotation.StringRes
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.trombito.souschef.R
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette

/** Etiqueta de prueba del icono (decorativo) de cada entrada del menú ⋮. */
internal fun etiquetaIconoMenu(icono: Icono): String = "icono-menu-${icono.name}"

/**
 * Una entrada del menú ⋮: icono de línea a la izquierda y texto en frase normal, todas con el mismo tamaño y peso.
 * Cierra el menú ([cerrar]) y hace su [accion]. Icono y texto comparten [color].
 */
@Composable
internal fun ItemMenu(
    @StringRes texto: Int,
    icono: Icono,
    cerrar: () -> Unit,
    color: Color = CronoPalette.texto,
    accion: () -> Unit,
) {
    EntradaMenu(stringResource(texto), icono, color) {
        cerrar()
        accion()
    }
}

@Composable
private fun EntradaMenu(
    texto: String,
    icono: Icono,
    color: Color,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(texto, color = color) },
        leadingIcon = {
            Icon(icono.vector, contentDescription = null, Modifier.testTag(etiquetaIconoMenu(icono)), tint = color)
        },
        onClick = onClick,
    )
}

/**
 * Las entradas del menú ⋮ de la cocina: diagrama de tiempos, interfaz, temporizador, órdenes de voz, reiniciar (doble
 * toque: el primero pide confirmación, y el menú queda abierto hasta confirmar) y abandonar (con diálogo).
 */
@Composable
internal fun EntradasMenuCocina(
    reiniciarArmado: Boolean,
    menu: AccionesMenu,
    cerrar: () -> Unit,
    onReiniciar: () -> Unit,
) {
    ItemMenu(R.string.crono_diagrama, Icono.Gantt, cerrar, accion = menu.diagrama)
    ItemMenu(R.string.ajustes_interfaz, Icono.Rejilla, cerrar, accion = menu.interfaz)
    ItemMenu(R.string.crono_temporizador_menu, Icono.RelojArena, cerrar, accion = menu.temporizador)
    ItemMenu(R.string.crono_ordenes_voz, Icono.Microfono, cerrar, accion = menu.ordenes)
    EntradaMenu(
        stringResource(if (reiniciarArmado) R.string.crono_reiniciar_seguro else R.string.crono_reiniciar),
        Icono.Reiniciar,
        if (reiniciarArmado) CronoPalette.alerta else CronoPalette.texto,
    ) {
        if (reiniciarArmado) cerrar()
        onReiniciar()
    }
    ItemMenu(R.string.crono_abandonar, Icono.Salir, cerrar, CronoPalette.alerta, menu.abandonar)
}
