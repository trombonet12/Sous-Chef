package app.trombito.souschef.domain.copia

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.PasoReal
import app.trombito.souschef.domain.Preferencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CopiaHistorialTest {
    private val curry =
        EntradaHistorial(
            id = "curry-2026-10-01",
            slug = "curry",
            titulo = "Curry",
            fecha = "2026-10-01",
            base = "arroz",
            tecnica = listOf("sofrito"),
            raciones = 2,
            minutosReales = 52,
            nota = "Menos picante",
            pasosReales = listOf(PasoReal("p1", "Sofrito", 610)),
        )
    private val tacos = EntradaHistorial("tacos-2026-10-05", "tacos", "Tacos", "2026-10-05")

    @Test
    fun `exportar y leer devuelve lo mismo`() {
        val prefs = listOf(Preferencia("curry", favorito = true, raciones = 4))
        val copia = CopiaHistorial.leer(CopiaHistorial.exportar(listOf(curry, tacos), prefs, "2026-10-18"))
        assertEquals(Copia(listOf(curry, tacos), prefs), copia)
    }

    @Test
    fun `un archivo que no es una copia no se lee`() {
        assertNull(CopiaHistorial.leer("no es json"))
        assertNull(CopiaHistorial.leer("""{"formato":"otra-cosa","version":1}"""))
        assertNull(CopiaHistorial.leer("""{"formato":"sous-chef-historial","version":99}"""))
    }

    @Test
    fun `importar solo añade lo que no esta, sin duplicar`() {
        val copia = Copia(listOf(curry, tacos, tacos), emptyList())
        assertEquals(listOf(tacos), CopiaHistorial.nuevas(listOf(curry), copia))
    }

    @Test
    fun `las preferencias suman favoritos y no pisan raciones`() {
        val actual = mapOf("curry" to Preferencia("curry", favorito = false, raciones = 3))
        val copia =
            Copia(
                emptyList(),
                listOf(
                    Preferencia("curry", favorito = true, raciones = 6),
                    Preferencia("tacos", raciones = 4),
                    Preferencia("sopa"),
                ),
            )
        assertEquals(
            listOf(Preferencia("curry", favorito = true, raciones = 3), Preferencia("tacos", raciones = 4)),
            CopiaHistorial.preferenciasNuevas(actual, copia),
        )
    }
}
