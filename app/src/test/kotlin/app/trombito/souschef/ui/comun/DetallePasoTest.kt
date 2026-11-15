package app.trombito.souschef.ui.comun

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Hito
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** El detalle de paso que comparten la ficha y el cronómetro: contexto puro y pintura. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-xxhdpi")
class DetallePasoTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val receta =
        Receta(
            slug = "patatas",
            titulo = "Patatas",
            totalMin = 40.0,
            carriles =
                listOf(
                    Carril("g", nombre = "SARTÉN GRANDE", color = "#E4572E", tipo = "fuego"),
                    Carril("m", nombre = "MESA", color = "#3E8FD1", tipo = "mesa"),
                ),
            pasos =
                listOf(
                    Paso(
                        "fr1",
                        "1ª fritura",
                        "g",
                        11.0,
                        18.0,
                        f = 4,
                        tapa = "sin tapa",
                        holgura = "critico",
                        dep = listOf("pel"),
                        junto = "ras",
                        meto = "500 g de patata · la fuente",
                        nota = "Tiernas y pálidas.",
                        saco = "la patata, al plato",
                        hitos = listOf(Hito(120.0, "Despega del fondo"), Hito(540.0, "Dales la vuelta")),
                        p = "Aquí se cuece.\n\n- Despegar a los 2 minutos.\n- Pinchar a los 15.",
                    ),
                    Paso("fr2", "2ª fritura", "g", 29.0, 6.0, f = 7, tapa = "sin tapa", dep = listOf("fr1")),
                    Paso("sil", "Sin avisos", "g", 36.0, 6.0, f = 7, sinHitos = "no hay nada que vigilar"),
                    Paso("pel", "Pelar", "m", 0.0, 10.0),
                    Paso("ras", "Rasgar el jamón", "m", 11.0, 3.0),
                ),
        )

    @Test
    fun `el contexto numera por orden de reloj y cuenta el carril`() {
        val c = contextoPaso(receta, receta.paso("fr1")!!, inicioPlanMin = 14 * 60 + 43)
        // Orden de reloj: pel(0), fr1(11), ras(11), fr2(29), sil(36) → fr1 es el 2.º, aunque en el array sea el 1.º.
        assertEquals(2, c.numero)
        assertEquals(1, c.indiceCarril)
        assertEquals(3, c.totalCarril)
        assertNull(c.fuegoAnterior)
        assertEquals(listOf(ReferenciaPaso(1, "Pelar")), c.tras)
        assertEquals(ReferenciaPaso(3, "Rasgar el jamón"), c.juntoCon)
    }

    @Test
    fun `el fuego anterior es el del paso previo del carril`() {
        val c = contextoPaso(receta, receta.paso("fr2")!!)
        assertEquals(4, c.fuegoAnterior)
        assertEquals(listOf(ReferenciaPaso(2, "1ª fritura")), c.tras)
        assertEquals(2, c.indiceCarril)
    }

    @Test
    fun `un paso fuera de la receta no inventa numeracion`() {
        val c = contextoPaso(receta, Paso("x", "Suelto", "g", 0.0, 1.0))
        assertEquals(0, c.numero)
        assertEquals(0, c.indiceCarril)
        assertEquals(0, c.totalCarril)
        assertNull(c.fuegoAnterior)
    }

    @Test
    fun `la hora del plan suma los minutos y da la vuelta al dia`() {
        assertEquals("14:54", horaDelPlan(14 * 60 + 43, 11.0))
        assertEquals("00:10", horaDelPlan(23 * 60 + 50, 20.0))
        assertNull(horaDelPlan(null, 5.0))
    }

    @Test
    fun `la marca de un aviso es mm ss`() {
        assertEquals("02:00", marcaAviso(120.0))
        assertEquals("15:30", marcaAviso(930.0))
        assertEquals("1:02:00", marcaAviso(3720.0))
    }

    private fun pintar(
        paso: Paso,
        conPorque: Boolean = true,
        carril: String = paso.carril,
    ): BancoCompose {
        val c = receta.carril(carril)!!
        val banco = BancoCompose()
        banco.poner {
            MaterialTheme {
                DetallePaso(
                    paso = paso,
                    carrilNombre = c.nombre,
                    tipoCarril = c.tipoCarril,
                    contexto = contextoPaso(receta, paso, 14 * 60 + 43),
                    paleta = paletaDetalleFicha(),
                    conPorque = conPorque,
                )
            }
        }
        return banco
    }

    @Test
    fun `pinta la cabecera, las ordenes y el porque en este orden`() {
        val banco = pintar(receta.paso("fr1")!!)
        val textos = banco.textos(banco.raiz())
        val cabecera = textos.first { it.startsWith("SARTÉN GRANDE") }
        assertEquals("SARTÉN GRANDE · paso 1 de 3 · min 11-29 (14:54-15:12)", cabecera)
        val orden = listOf("METE", "FUEGO", "AVISOS", "SEÑAL", "SACA", "POR QUÉ").map { textos.indexOf(it) }
        assertTrue(textos.toString(), orden.all { it >= 0 })
        assertEquals(orden.sorted(), orden)
        assertTrue(textos.toString(), "500 g de patata · la fuente" in textos)
        assertTrue(textos.toString(), "4 · sin tapa · 18 min" in textos)
        assertTrue(textos.toString(), "02:00" in textos && "09:00" in textos)
        assertTrue(textos.toString(), "Dales la vuelta" in textos)
        assertTrue(textos.toString(), "tras 1 (Pelar) · a la vez que 3 (Rasgar el jamón) · no espera" in textos)
        // El porqué sale en párrafo y lista.
        assertTrue(textos.toString(), "Aquí se cuece." in textos)
        assertTrue(textos.toString(), "Despegar a los 2 minutos." in textos)
    }

    @Test
    fun `el fuego que cambia respecto al paso anterior del carril sale con flecha`() {
        val banco = pintar(receta.paso("fr2")!!)
        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "4 → 7 · sin tapa · 6 min" in textos)
        assertTrue(textos.toString(), "tras 2 (1ª fritura)" in textos)
    }

    @Test
    fun `solo ordenes quita el porque y deja lo ejecutivo`() {
        val banco = pintar(receta.paso("fr1")!!, conPorque = false)
        val textos = banco.textos(banco.raiz())
        assertFalse(textos.toString(), "POR QUÉ" in textos)
        assertFalse(textos.toString(), "Aquí se cuece." in textos)
        assertTrue(textos.toString(), "METE" in textos && "SACA" in textos)
    }

    @Test
    fun `sin fuego la linea de datos es de tiempo y sinHitos explica el silencio`() {
        val banco = pintar(receta.paso("pel")!!)
        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "TIEMPO" in textos && "10 min" in textos)
        assertFalse(textos.toString(), "AVISOS" in textos)

        val silencio = pintar(receta.paso("sil")!!)
        val t2 = silencio.textos(silencio.raiz())
        assertTrue(t2.toString(), "AVISOS" in t2)
        assertTrue(t2.toString(), "Sin avisos: no hay nada que vigilar" in t2)
    }
}
