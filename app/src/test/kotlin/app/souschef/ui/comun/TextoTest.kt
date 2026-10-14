package app.souschef.ui.comun

import org.junit.Assert.assertEquals
import org.junit.Test

class TextoTest {
    @Test
    fun normalizaAcentosYMayusculas() {
        assertEquals("pimiento de padron", "Pimiento de Padrón".normalizado())
    }

    @Test
    fun separaNegritas() {
        assertEquals(
            listOf(Trozo("Patata ", false), Trozo("500 g", true), Trozo(" pelada", false)),
            conNegritas("Patata <b>500 g</b> pelada"),
        )
        assertEquals(listOf(Trozo("sin marcas", false)), conNegritas("sin marcas"))
    }

    @Test
    fun fechaCortaEnEspanol() {
        assertEquals("12 sep", fechaCorta("2026-09-12"))
        assertEquals("rara", fechaCorta("rara"))
    }
}
