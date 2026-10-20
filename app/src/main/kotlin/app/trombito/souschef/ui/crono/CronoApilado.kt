package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** APILADA: todos los carriles uno debajo de otro repartiendo la altura. Los terminados quedan en fila compacta. */
@Composable
fun CronoApilado(
    vistas: List<VistaCarril>,
    acciones: AccionesCarril,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        vistas.forEach { v ->
            if (v.terminado) {
                FilaCarrilCompacta(v, acciones)
            } else {
                TarjetaCarril(v, acciones, vistas.size, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}
