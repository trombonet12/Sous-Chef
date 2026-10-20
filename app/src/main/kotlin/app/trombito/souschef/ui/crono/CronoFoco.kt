package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** FOCO: el carril más urgente en tarjeta completa y el resto en filas compactas debajo. Sin scroll. */
@Composable
fun CronoFoco(
    vistas: List<VistaCarril>,
    focoId: String?,
    acciones: AccionesCarril,
    modifier: Modifier = Modifier,
) {
    val foco = vistas.firstOrNull { it.id == focoId && !it.terminado }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (foco != null) TarjetaCarril(foco, acciones, vistas.size, Modifier.weight(1f).fillMaxWidth())
        vistas.filter { it.id != foco?.id }.forEach { FilaCarrilCompacta(it, acciones) }
    }
}
