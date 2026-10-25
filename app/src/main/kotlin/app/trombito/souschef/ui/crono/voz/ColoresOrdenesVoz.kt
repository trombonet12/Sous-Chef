package app.trombito.souschef.ui.crono.voz

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colores de la lista de órdenes: los de Material por defecto o los del cronómetro (siempre oscuro). */
class ColoresOrdenesVoz(
    val fondo: Color,
    val texto: Color,
    val grupo: Color,
) {
    companion object {
        @Composable
        fun material(): ColoresOrdenesVoz {
            val fondo = MaterialTheme.colorScheme.surfaceContainerLow
            return ColoresOrdenesVoz(fondo, contentColorFor(fondo), MaterialTheme.colorScheme.primary)
        }
    }
}
