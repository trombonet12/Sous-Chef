package app.trombito.souschef.ui.ficha

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Equipo
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.equipoDe
import app.trombito.souschef.ui.comun.Etiqueta
import app.trombito.souschef.ui.comun.Tono

/** Un aparato que pide la receta y si el usuario no lo tiene. */
data class AparatoPedido(
    val aparato: Equipo,
    val falta: Boolean,
)

/** Los aparatos que pide [receta], en el orden del enum, marcando los que no están en [tiene]. */
fun aparatosPedidos(
    receta: Receta,
    tiene: Set<Equipo>,
): List<AparatoPedido> {
    val pide = equipoDe(receta)
    return Equipo.entries.filter { it in pide }.map { AparatoPedido(it, falta = it !in tiene) }
}

@StringRes
private fun Equipo.textoPide(): Int =
    when (this) {
        Equipo.HORNO -> R.string.lista_pide_horno
        Equipo.BATIDORA -> R.string.ficha_pide_batidora
        Equipo.MICROONDAS -> R.string.ficha_pide_microondas
        Equipo.PLANCHA -> R.string.ficha_pide_plancha
    }

@StringRes
private fun Equipo.textoNoTienes(): Int =
    when (this) {
        Equipo.HORNO -> R.string.ficha_no_tienes_horno
        Equipo.BATIDORA -> R.string.ficha_no_tienes_batidora
        Equipo.MICROONDAS -> R.string.ficha_no_tienes_microondas
        Equipo.PLANCHA -> R.string.ficha_no_tienes_plancha
    }

/** «Pide horno», «Pide batidora»…; en color de aviso y como «No tienes batidora» si el usuario no lo tiene. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EtiquetasEquipoFicha(
    receta: Receta,
    tiene: Set<Equipo>,
) {
    val pedidos = aparatosPedidos(receta, tiene)
    if (pedidos.isEmpty()) return
    FlowRow(
        Modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        pedidos.forEach { p ->
            if (p.falta) {
                Etiqueta(
                    stringResource(p.aparato.textoNoTienes()),
                    modifier = Modifier.testTag("equipo-falta-${p.aparato.name.lowercase()}"),
                    tono = Tono.Teja,
                )
            } else {
                Etiqueta(
                    stringResource(p.aparato.textoPide()),
                    modifier = Modifier.testTag("equipo-pide-${p.aparato.name.lowercase()}"),
                )
            }
        }
    }
}
