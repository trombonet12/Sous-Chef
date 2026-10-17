package app.souschef.receta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DespensaTest {
    private val md =
        """
        # La despensa
        Texto libre · que no cuenta
        ## Carnes
        Bacon · carne picada · jamón serrano en lonchas

        ## Verduras
        Ajos · cebolla · hélices o pajaritas · tomate triturado
        ---
        ## Consecuencias
        Sal · no debe leerse
        """.trimIndent()

    private val despensa = Despensa.parsearSemilla(md)

    private fun receta(vararg n: String) =
        Receta(
            slug = "r",
            titulo = "R",
            grupos = listOf(Grupo("G", n.map { Ingrediente(n = it, a = 1.0) })),
        )

    @Test
    fun parseaSeccionesYSeparadores() {
        assertEquals(
            setOf(
                "Bacon",
                "carne picada",
                "jamón serrano en lonchas",
                "Ajos",
                "cebolla",
                "hélices",
                "pajaritas",
                "tomate triturado",
            ),
            despensa,
        )
    }

    @Test
    fun ingredientesDeLaDespensaPasan() {
        val r = receta("bacon", "ajo", "Cebollas", "jamón serrano", "HELICES", "tomate triturado")
        assertEquals(emptyList(), Despensa.comprobar(r, despensa))
    }

    @Test
    fun ingredienteFueraDeDespensaEsErrorRegla12() {
        val h = Despensa.comprobar(receta("bacon", "salmón"), despensa)
        assertEquals(listOf(12), h.map { it.regla })
        assertEquals(Nivel.ERROR, h.single().nivel)
        assertEquals("ingrediente 'salmón' no está en la despensa", h.single().texto)
    }

    @Test
    fun yaNoHayExcepcionPorCompra() {
        // Antes un ingrediente declarado en `compra` pasaba; ahora solo vale la despensa.
        val json =
            """{"slug":"r","titulo":"R","compra":[{"n":"salmón","a":1}],""" +
                """"grupos":[{"n":"G","items":[{"n":"salmón","a":1}]}]}"""
        val r = leerReceta(json)
        val h = Despensa.comprobar(r, despensa)
        assertEquals(listOf(12), h.map { it.regla })
        assertEquals(Nivel.ERROR, h.single().nivel)
    }

    @Test
    fun nombresConDescripcionYAguaImplicita() {
        val r = receta("diente de ajo, en láminas (4 dientes)", "cebolla (1 grande)", "agua", "cebolla, picada")
        assertEquals(emptyList(), Despensa.comprobar(r, despensa))
    }

    @Test
    fun noCasaPorSubcadena() = assertTrue(Despensa.comprobar(receta("bac"), despensa).isNotEmpty())

    @Test
    fun aguaEsImplicita() {
        assertTrue(Despensa.esImplicito("agua"))
        assertTrue(Despensa.esImplicito("Agua"))
        assertFalse(Despensa.esImplicito("sal"))
    }

    @Test
    fun casaEsPublicaYPorPalabraCompleta() {
        assertTrue(Despensa.casa("diente de ajo", "Ajos"))
        assertTrue(Despensa.casa("cebolla (1 grande)", "cebolla"))
        assertFalse(Despensa.casa("bac", "Bacon"))
    }

    @Test
    fun entradaDeDevuelveLaEntradaCorrecta() {
        assertEquals("Ajos", Despensa.entradaDe("diente de ajo, en láminas", despensa))
        assertEquals("cebolla", Despensa.entradaDe("Cebollas", despensa))
        assertEquals("hélices", Despensa.entradaDe("HELICES", despensa))
        assertEquals("jamón serrano en lonchas", Despensa.entradaDe("jamón serrano", despensa))
    }

    @Test
    fun entradaDeDevuelveNullSiNoCasa() {
        assertNull(Despensa.entradaDe("salmón", despensa))
        assertNull(Despensa.entradaDe("bac", despensa))
        assertNull(Despensa.entradaDe("agua", despensa))
        assertNull(Despensa.entradaDe("bacon", emptyList()))
    }

    @Test
    fun entradaDeEligeLaPrimeraEnElOrdenDeLaColeccion() {
        assertEquals("tomate", Despensa.entradaDe("tomate triturado", listOf("tomate", "tomate triturado")))
        assertEquals("tomate triturado", Despensa.entradaDe("tomate triturado", listOf("tomate triturado", "tomate")))
    }
}
