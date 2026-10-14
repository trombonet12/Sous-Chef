package app.souschef.tools

import app.souschef.receta.Grupo
import app.souschef.receta.Ingrediente
import app.souschef.receta.Nivel
import app.souschef.receta.Receta
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private fun receta(
        vararg n: String,
        compra: List<String> = emptyList(),
    ) = Receta(
        slug = "r",
        titulo = "R",
        grupos = listOf(Grupo("G", n.map { Ingrediente(n = it, a = 1.0) })),
        compra = compra.map { Ingrediente(n = it, a = 1.0) },
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
    }

    @Test
    fun compraLoDeclara() =
        assertEquals(emptyList(), Despensa.comprobar(receta("salmón", compra = listOf("salmón")), despensa))

    @Test
    fun nombresConDescripcionYAguaImplicita() {
        val r = receta("diente de ajo, en láminas (4 dientes)", "cebolla (1 grande)", "agua", "cebolla, picada")
        assertEquals(emptyList(), Despensa.comprobar(r, despensa))
    }

    @Test
    fun noCasaPorSubcadena() = assertTrue(Despensa.comprobar(receta("bac"), despensa).isNotEmpty())
}
