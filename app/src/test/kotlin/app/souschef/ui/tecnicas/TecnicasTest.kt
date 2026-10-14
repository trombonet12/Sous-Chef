package app.souschef.ui.tecnicas

import app.souschef.receta.PasoTecnica
import app.souschef.receta.Tecnica
import app.souschef.ui.receta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TecnicasTest {
    private val todas =
        listOf(
            Tecnica("b", "Reducción", "Concentrar salsas", "salsas", listOf(PasoTecnica("Hervir", "A fuego vivo"))),
            Tecnica("a", "Árbol de cortes", "Juliana", "cortes", consejos = listOf("Cuchillo afilado")),
            Tecnica("c", "Glaseado", "Brillo", "salsas"),
        )

    @Test
    fun ordenAlfabeticoSinAcentos() {
        assertEquals(listOf("a", "c", "b"), construirEstadoTecnicas(todas, "", null).tecnicas.map { it.slug })
    }

    @Test
    fun buscaEnPasosYConsejos() {
        assertEquals(listOf("b"), construirEstadoTecnicas(todas, "fuego VIVO", null).tecnicas.map { it.slug })
        assertEquals(listOf("a"), construirEstadoTecnicas(todas, "afilado", null).tecnicas.map { it.slug })
    }

    @Test
    fun chipsPorCategoriaSoloSiHayMasDeUna() {
        val e = construirEstadoTecnicas(todas, "", null)
        assertTrue(e.mostrarChips)
        assertEquals(listOf("cortes" to 1, "salsas" to 2), e.categorias)
        assertFalse(construirEstadoTecnicas(todas, "glaseado", null).mostrarChips)
        assertEquals(
            listOf("c", "b"),
            construirEstadoTecnicas(todas, "", "salsas").tecnicas.map { it.slug }.sortedDescending(),
        )
    }

    @Test
    fun enlacesInversosDeRecetasPorTitulo() {
        val rs =
            listOf(
                receta("z", "Zarzuela", tecnicas = listOf("x")),
                receta("m", "Migas", tecnicas = listOf("x", "y")),
                receta("n", "Nada"),
            )
        assertEquals(listOf("m", "z"), recetasConTecnica("x", rs).map { it.slug })
    }
}
