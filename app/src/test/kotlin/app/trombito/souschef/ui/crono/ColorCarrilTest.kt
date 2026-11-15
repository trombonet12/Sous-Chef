package app.trombito.souschef.ui.crono

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * El color que reciben los carriles neutros (la mesa, `#C0B7A6`) tiene que distinguirse de los que llevan los demás
 * carriles de las recetas (auditoría ronda 19, M7): el azul `#7FB7D6` quedaba a ΔE 4-6 de la olla `#6FBEE8`.
 */
@RunWith(RobolectricTestRunner::class)
class ColorCarrilTest {
    /** Colores de carril con saturación propia que usan hoy las 33 recetas de `data/recetas`. */
    private val coloresDeRecetas =
        listOf(0xF0AE4A, 0xE8836B, 0xB58CD0, 0x6FBEE8, 0xA8C686, 0xF0BE4A, 0xE8734A, 0x7FC4DE)

    private fun lab(rgb: Int): Triple<Double, Double, Double> {
        fun lineal(c: Int): Double =
            (c / 255.0).let { if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        val r = lineal(rgb shr 16 and 0xFF)
        val g = lineal(rgb shr 8 and 0xFF)
        val b = lineal(rgb and 0xFF)
        val x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
        val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
        val z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883

        fun f(t: Double): Double = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116
        return Triple(116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z)))
    }

    /** Distancia CIE76 entre dos colores `0xRRGGBB`. */
    private fun deltaE(
        a: Int,
        b: Int,
    ): Double {
        val (l1, a1, b1) = lab(a)
        val (l2, a2, b2) = lab(b)
        return sqrt((l1 - l2).pow(2) + (a1 - a2).pow(2) + (b1 - b2).pow(2))
    }

    private fun distanciaMinima(rgb: Int): Double = coloresDeRecetas.minOf { deltaE(rgb, it) }

    @Test
    fun `el color de la mesa esta lejos de todos los colores de carril de las recetas`() {
        // La mesa es el primer carril de las 33 recetas: el índice 0 es el que más se ve.
        assertTrue("mesa: ${distanciaMinima(NEUTROS_CARRIL_RGB[0])}", distanciaMinima(NEUTROS_CARRIL_RGB[0]) >= 30)
    }

    @Test
    fun `ningun color de neutro se confunde con uno de receta`() {
        NEUTROS_CARRIL_RGB.forEachIndexed { i, rgb ->
            assertTrue("neutro $i: ${distanciaMinima(rgb)}", distanciaMinima(rgb) >= 20)
        }
    }

    @Test
    fun `el azul de antes si se confundia con la olla`() {
        // Prueba de que la medida detecta el problema: el viejo #7FB7D6 contra la olla #6FBEE8.
        assertTrue(deltaE(0x7FB7D6, 0x6FBEE8) < 10)
    }

    @Test
    fun `los neutros distintos entre si`() {
        for (i in NEUTROS_CARRIL_RGB.indices) {
            for (j in i + 1 until NEUTROS_CARRIL_RGB.size) {
                assertTrue("$i-$j", deltaE(NEUTROS_CARRIL_RGB[i], NEUTROS_CARRIL_RGB[j]) >= 20)
            }
        }
    }

    @Test
    fun `colorDeCarril da el neutro por posicion, el propio si tiene saturacion y da la vuelta`() {
        assertEquals(NEUTROS_CARRIL_RGB[0], colorDeCarril("#C0B7A6", 0).toArgb() and 0xFFFFFF)
        assertEquals(NEUTROS_CARRIL_RGB[1], colorDeCarril("#6B6C5F", 1).toArgb() and 0xFFFFFF)
        assertEquals(NEUTROS_CARRIL_RGB[0], colorDeCarril("raro", NEUTROS_CARRIL_RGB.size).toArgb() and 0xFFFFFF)
        assertEquals(0x6FBEE8, colorDeCarril("#6FBEE8", 0).toArgb() and 0xFFFFFF)
    }
}
