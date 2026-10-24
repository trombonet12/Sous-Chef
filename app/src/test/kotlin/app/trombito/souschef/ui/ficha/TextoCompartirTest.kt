package app.trombito.souschef.ui.ficha

import app.trombito.souschef.receta.Grupo
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Receta
import org.junit.Assert.assertEquals
import org.junit.Test

class TextoCompartirTest {
    private val receta =
        Receta(
            slug = "arroz",
            titulo = "Arroz con pollo",
            descripcion = "",
            raciones = 2,
            totalMin = 30.0,
            grupos =
                listOf(
                    Grupo(
                        "Base",
                        listOf(Ingrediente("arroz", a = 100.0, u = "g"), Ingrediente("sal", txt = "al gusto")),
                    ),
                    Grupo(
                        "Guiso",
                        listOf(Ingrediente("pollo", a = 200.0, u = "g"), Ingrediente("arroz", a = 50.0, u = "g")),
                    ),
                ),
        )

    private val raciones: (Int) -> String = { if (it == 1) "1 ración" else "$it raciones" }

    @Test
    fun todoJuntoUneLosIngredientesRepetidos() {
        val esperado =
            """
            Arroz con pollo · 2 raciones

            • 150 g arroz
            • al gusto sal
            • 200 g pollo
            """.trimIndent()
        assertEquals(esperado, textoIngredientes(receta, 2, porZona = false, textoRaciones = raciones))
    }

    @Test
    fun porZonaPoneCadaGrupoConSusItems() {
        val esperado =
            """
            Arroz con pollo · 2 raciones

            Base
            • 100 g arroz
            • al gusto sal

            Guiso
            • 200 g pollo
            • 50 g arroz
            """.trimIndent()
        assertEquals(esperado, textoIngredientes(receta, 2, porZona = true, textoRaciones = raciones))
    }

    @Test
    fun lasRacionesEscalanLasCantidades() {
        val texto = textoIngredientes(receta, 4, porZona = true, textoRaciones = raciones)
        assertEquals("Arroz con pollo · 4 raciones", texto.lines().first())
        assertEquals(true, texto.contains("• 200 g arroz"))
        assertEquals(true, texto.contains("• 400 g pollo"))
    }

    @Test
    fun unaRacionVaEnSingular() {
        val texto = textoIngredientes(receta, 1, porZona = false, textoRaciones = raciones)
        assertEquals("Arroz con pollo · 1 ración", texto.lines().first())
        assertEquals(true, texto.contains("• 75 g arroz"))
    }

    @Test
    fun sinCantidadNoDejaDobleEspacio() {
        val r = receta.copy(grupos = listOf(Grupo("Base", listOf(Ingrediente("agua")))))
        assertEquals("• agua", textoIngredientes(r, 2, porZona = false, textoRaciones = raciones).lines().last())
    }
}
