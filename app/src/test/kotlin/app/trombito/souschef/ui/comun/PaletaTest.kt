package app.trombito.souschef.ui.comun

import androidx.compose.ui.graphics.Color
import app.trombito.souschef.ui.theme.DarkColors
import app.trombito.souschef.ui.theme.LightColors
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class PaletaTest {
    private fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= UMBRAL_LINEAL) v / DIVISOR_LINEAL else ((v + OFFSET) / ESCALA).pow(EXPONENTE)
    }

    private fun luminance(c: Color): Double =
        PESO_R * channel(c.red) + PESO_G * channel(c.green) + PESO_B * channel(c.blue)

    private fun ratio(
        a: Color,
        b: Color,
    ): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + MARGEN) / (minOf(la, lb) + MARGEN)
    }

    @Test
    fun `el texto principal contrasta con el tinte de cada tono en claro y oscuro`() {
        val fallos =
            TonoPaleta.entries.flatMap { tono ->
                listOf(false to LightColors, true to DarkColors).mapNotNull { (oscuro, esquema) ->
                    val r = ratio(esquema.onSurface, Paleta.de(tono, oscuro).tinte)
                    if (r < MIN_AA) "$tono oscuro=$oscuro ${"%.2f".format(r)}" else null
                }
            }
        assertTrue("Contraste < $MIN_AA: $fallos", fallos.isEmpty())
    }

    @Test
    fun `los tonos claro y oscuro son distintos`() {
        TonoPaleta.entries.forEach { tono ->
            assertTrue(tono.name, Paleta.de(tono, false) != Paleta.de(tono, true))
        }
    }

    private companion object {
        const val MIN_AA = 4.5
        const val UMBRAL_LINEAL = 0.03928
        const val DIVISOR_LINEAL = 12.92
        const val OFFSET = 0.055
        const val ESCALA = 1.055
        const val EXPONENTE = 2.4
        const val PESO_R = 0.2126
        const val PESO_G = 0.7152
        const val PESO_B = 0.0722
        const val MARGEN = 0.05
    }
}
