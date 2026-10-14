package app.souschef.data.seed

import app.souschef.domain.EstadoDespensa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SemillaDespensaTest {
    @Test
    fun parseaCategoriasYElementos() {
        val md =
            """
            # Titulo
            texto suelto

            ## Carnes
            Bacon · carne picada

            ## Fruta
            Manzanas

            ---

            ## Consecuencias
            No cuenta · tampoco
            """.trimIndent()
        val items = SemillaDespensa.parsear(md).filter { it.estado == EstadoDespensa.HAY }
        assertEquals(
            listOf("Bacon" to "Carnes", "Carne picada" to "Carnes", "Manzanas" to "Fruta"),
            items.map { it.nombre to it.categoria },
        )
    }

    @Test
    fun anadeLosVetosComoVetados() {
        val vetados = SemillaDespensa.parsear("## A\nx").filter { it.estado == EstadoDespensa.VETADO }
        assertEquals(SemillaDespensa.VETOS.size, vetados.size)
        assertTrue(vetados.any { it.nombre == "Garbanzos" })
        assertTrue(vetados.all { it.categoria == SemillaDespensa.CATEGORIA_VETADOS })
    }

    @Test
    fun parseaLaSemillaReal() {
        val items = SemillaDespensa.parsear(File("../data/despensa-semilla.md").readText())
        val hay = items.filter { it.estado == EstadoDespensa.HAY }
        assertTrue("pocos elementos: ${hay.size}", hay.size > 100)
        assertTrue(hay.any { it.nombre == "Grana Padano" && it.categoria == "Lácteos y huevos" })
        assertFalse("la sección Consecuencias no es despensa", hay.any { it.categoria.startsWith("Consecuencias") })
    }
}
