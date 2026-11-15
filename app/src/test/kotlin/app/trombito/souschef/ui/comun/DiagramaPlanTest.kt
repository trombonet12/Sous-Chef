package app.trombito.souschef.ui.comun

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import app.trombito.souschef.ui.crono.NEUTROS_CARRIL_RGB
import app.trombito.souschef.ui.crono.colorDeCarril
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val NBSP = ' '

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class DiagramaPlanTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    /** Un paso de la mesa va después de otros dos del fuego: el array no sigue el orden del reloj. */
    private val huevos =
        Receta(
            slug = "h",
            titulo = "Huevos rotos",
            totalMin = 47.0,
            carriles =
                listOf(
                    Carril("mesa", "MESA", "#6B6C5F", "mesa"),
                    Carril("s1", "SARTÉN GRANDE", "#A8421C", "fuego"),
                    Carril("s2", "SARTÉN PEQUEÑA", "#E9C46A", "fuego"),
                ),
            pasos =
                listOf(
                    Paso("a", "Freír la patata", "s1", 6.0, 20.0, f = 4, tapa = "sin tapa"),
                    Paso("c", "Subir al 8", "s1", 31.0, 5.0, f = 8, dep = listOf("a")),
                    Paso("b", "Calentar la sartén pequeña", "s2", 28.0, 2.0, f = 5, junto = "a"),
                    Paso("z", "Servir", "mesa", 45.0, 2.0),
                ),
        )

    // ---- Lógica pura

    @Test
    fun `una fila por carril, por el off de su primer paso`() {
        val filas = filasPlan(huevos)
        assertEquals(listOf("s1", "s2", "mesa"), filas.map { it.carril.id })
        assertTrue(filas.first().fuego)
        assertFalse(filas.last().fuego)
    }

    @Test
    fun `los pasos se numeran por orden de off, como en la ficha`() {
        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 3, "z" to 4), numerosPlan(huevos))
    }

    @Test
    fun `el color es el de la cocina, el propio si lo tiene y los neutros por posicion`() {
        val carriles =
            listOf(
                Carril("n0", "N0", "#B0A899"),
                Carril("p", "P", "#A8421C"),
                Carril("n2", "N2", "#6B6C5F"),
                Carril("raro", "R", "red;x"),
            )
        assertEquals(0xA8421C, rgbCarrilPlan(carriles, "p"))
        assertEquals(NEUTROS_CARRIL_RGB[0], rgbCarrilPlan(carriles, "n0"))
        assertEquals(NEUTROS_CARRIL_RGB[2], rgbCarrilPlan(carriles, "n2"))
        assertEquals(NEUTROS_CARRIL_RGB[3], rgbCarrilPlan(carriles, "raro"))
    }

    @Test
    fun `el color coincide con el que usa la cocina para todos los carriles de una receta`() {
        val carriles =
            app.trombito.souschef.receta.crono.ConsultasCrono
                .carriles(huevos)
        carriles.forEachIndexed { i, c ->
            val cocina = colorDeCarril(c.color, i).toArgb() and 0xFFFFFF
            assertEquals(c.id, cocina, rgbCarrilPlan(carriles, c.id))
        }
    }

    @Test
    fun `la altura del escalon de fuego es proporcional a la potencia`() {
        assertEquals(0f, alturaFuego(0), 1e-6f)
        assertEquals(4 / 9f, alturaFuego(4), 1e-6f)
        assertEquals(1f, alturaFuego(9), 1e-6f)
        assertEquals(1f, alturaFuego(12), 1e-6f)
    }

    // ---- Composable

    private fun pintar(
        densidad: DensidadPlan,
        receta: Receta = huevos,
        vivo: VivoPlan? = null,
    ): BancoCompose {
        val banco = BancoCompose()
        banco.poner {
            MaterialTheme {
                DiagramaPlan(receta, densidad, paleta = paletaPlanFicha(), inicioPlanMin = 14 * 60 + 43, vivo = vivo)
            }
        }
        return banco
    }

    private fun BancoCompose.descripciones(): List<String> =
        nodos().flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }

    @Test
    fun `la huella no lleva texto ni semantica`() {
        val banco = pintar(DensidadPlan.HUELLA)
        assertTrue(banco.textos(banco.raiz()).isEmpty())
        assertTrue(banco.descripciones().isEmpty())
    }

    @Test
    fun `el resumen tiene una fila por carril descrita para TalkBack y el eje con horas`() {
        val banco = pintar(DensidadPlan.RESUMEN)
        val descripciones = banco.descripciones()
        assertEquals(
            listOf(
                "SARTÉN GRANDE, de 6 a 36 minutos",
                "SARTÉN PEQUEÑA, de 28 a 30 minutos",
                "MESA, de 45 a 47 minutos",
            ),
            descripciones,
        )
        assertTrue(banco.textos(banco.raiz()).any { it.startsWith("Eje en minutos") })
    }

    @Test
    fun `el detalle lista los pasos numerados con sus relaciones`() {
        val banco = pintar(DensidadPlan.DETALLE)
        val textos = banco.textos(banco.raiz()).map { it.replace(NBSP, ' ') }
        listOf("1", "2", "3", "4").forEach { assertTrue("falta el paso $it", it in textos) }
        assertTrue(textos.any { it.startsWith("SARTÉN GRANDE · min 6-26 · fuego 4 · sin tapa") })
        // c (3) va tras a (1); b (2) va a la vez que a (1).
        assertTrue(textos.any { it.startsWith("SARTÉN GRANDE · min 31-36") && it.endsWith("tras 1") })
        assertTrue(textos.any { it.startsWith("SARTÉN PEQUEÑA") && it.endsWith("a la vez que 1") })
    }

    @Test
    fun `solo se desplaza a lo ancho si pasa de 90 minutos`() {
        assertFalse(pintar(DensidadPlan.RESUMEN).hayDesplazable())
        val larga = huevos.copy(totalMin = 120.0)
        assertTrue(pintar(DensidadPlan.RESUMEN, larga).hayDesplazable())
    }

    @Test
    fun `el estado vivo marca en la lista el paso hecho y el que esta en marcha`() {
        val vivo =
            VivoPlan(
                mapOf(
                    "a" to SituacionPaso.HECHO,
                    "b" to SituacionPaso.EN_MARCHA,
                    "c" to SituacionPaso.PENDIENTE,
                    "z" to SituacionPaso.PENDIENTE,
                ),
                ahoraMin = 10.0,
            )
        val banco = pintar(DensidadPlan.DETALLE, vivo = vivo)
        val estados =
            banco.nodos().mapNotNull { it.config.getOrNull(SemanticsProperties.StateDescription) }
        assertEquals(listOf("hecho", "en marcha"), estados)
    }
}
