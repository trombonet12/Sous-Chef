package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La pila de texto de la tarjeta: se mide con el ancho real y solo coloca, por orden de prioridad, los bloques que
 * caben enteros (hasta 2 líneas con «…»); ninguno se parte en vertical ni se mueve.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PilaPrioritariaTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    // Alto de cada bloque con 1 y 2 líneas (cada línea 20, salvo el «grande», que tiene 30 por línea).
    private fun altura(
        lineasNecesarias: List<Int>,
        porLinea: List<Int> = List(lineasNecesarias.size) { 20 },
    ): (Int, Int) -> Int = { bloque, lineas -> minOf(lineas, lineasNecesarias[bloque]) * porLinea[bloque] }

    @Test
    fun `con sitio de sobra caben todos con el maximo de dos lineas`() {
        val lineas = repartirPila(3, hueco = 500, separacion = 2, altura = altura(listOf(2, 1, 2)))
        // Un bloque de una sola línea con tope de 2 mide lo mismo: el tope es máximo, no reserva.
        assertEquals(listOf(2, 2, 2), lineas)
    }

    @Test
    fun `si no caben las dos lineas de un bloque se corta a una con puntos suspensivos`() {
        val lineas = repartirPila(2, hueco = 30, separacion = 2, altura = altura(listOf(2, 1)))
        assertEquals(listOf(1, 0), lineas)
    }

    @Test
    fun `un bloque que no cabe ni en una linea se omite`() {
        val lineas = repartirPila(2, hueco = 15, separacion = 2, altura = altura(listOf(2, 1)))
        assertEquals(listOf(0, 0), lineas)
    }

    @Test
    fun `lo de menos prioridad nunca ocupa el sitio de lo de mas prioridad`() {
        // El segundo bloque (una línea de 30) no cabe en los 30 que quedan; el tercero sí cabría, pero se queda fuera.
        val lineas =
            repartirPila(
                3,
                hueco = 70,
                separacion = 2,
                altura = altura(listOf(2, 1, 1), porLinea = listOf(20, 30, 20)),
            )
        assertEquals(listOf(2, 0, 0), lineas)
    }

    @Test
    fun `las separaciones cuentan para el hueco`() {
        // 20 + 2 + 20 = 42: cabe en 42 y no en 41.
        assertEquals(listOf(2, 2), repartirPila(2, hueco = 42, separacion = 2, altura = altura(listOf(1, 1))))
        assertEquals(listOf(2, 0), repartirPila(2, hueco = 41, separacion = 2, altura = altura(listOf(1, 1))))
    }

    @Test
    fun `sin alto no se coloca nada y sin bloques tampoco`() {
        assertEquals(listOf(0, 0), repartirPila(2, hueco = 0, separacion = 2, altura = altura(listOf(1, 1))))
        assertTrue(repartirPila(0, hueco = 100, separacion = 2, altura = altura(emptyList())).isEmpty())
    }

    private val bloques =
        listOf(
            BloquePila(AnnotatedString("SEÑAL primera"), Color.Black),
            BloquePila(AnnotatedString("a la vez que ②"), Color.Black),
            BloquePila(AnnotatedString("METO " + "texto largo ".repeat(20)), Color.Black),
            BloquePila(AnnotatedString("SACO el final"), Color.Black),
            BloquePila(AnnotatedString("Después: lo último"), Color.Black),
        )

    private fun pintados(alturaDp: Int): List<String> {
        val banco = BancoCompose()
        banco.poner {
            MaterialTheme {
                Box(Modifier.width(300.dp).height(alturaDp.dp).testTag("caja")) { PilaPrioritaria(bloques) }
            }
        }
        val textos = banco.textos(banco.conEtiqueta("caja"))
        return bloques.map { it.texto.text }.filter { it in textos }
    }

    @Test
    fun `en pantalla solo se pintan bloques completos y siempre un prefijo de la prioridad`() {
        val todos = bloques.map { it.texto.text }
        var anterior = -1
        listOf(0, 10, 30, 60, 100, 160, 400).forEach { alto ->
            val colocados = pintados(alto)
            assertEquals("alto $alto: orden de prioridad", todos.take(colocados.size), colocados)
            assertTrue("alto $alto: más alto, nunca menos bloques", colocados.size >= anterior)
            anterior = colocados.size
        }
        assertEquals(todos, pintados(400))
        assertTrue(pintados(0).isEmpty())
    }
}
