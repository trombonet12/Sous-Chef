package app.trombito.souschef.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

private const val CONTRASTE_MINIMO = 4.5f
private const val PASOS_MEZCLA = 20
private val TINTA = Color(0xFF191A14)

/**
 * Color de un carril para pintar **texto o iconos**: en el tema claro se mezcla con la tinta hasta llegar a 4,5:1 sobre
 * la carta; en el oscuro queda igual. Barras, franjas y bordes usan el color original.
 */
@Composable
@ReadOnlyComposable
fun colorCarrilTexto(
    color: Color,
    sobre: Color = CronoPalette.carta,
): Color = colorCarrilTextoSobre(color, sobre, claro = CronoPalette.fondo.luminance() > 0.5f)

/** Versión pura (testeable en JVM) de [colorCarrilTexto]: [claro] indica que el fondo es papel claro. */
fun colorCarrilTextoSobre(
    color: Color,
    fondo: Color,
    claro: Boolean,
): Color =
    if (claro) {
        (0..PASOS_MEZCLA)
            .map { lerp(color, TINTA, it / PASOS_MEZCLA.toFloat()) }
            .firstOrNull { contraste(it, fondo) >= CONTRASTE_MINIMO } ?: TINTA
    } else {
        color
    }

/** Relación de contraste WCAG entre dos colores opacos. */
fun contraste(
    a: Color,
    b: Color,
): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}
