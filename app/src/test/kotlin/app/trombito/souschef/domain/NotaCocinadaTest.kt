package app.trombito.souschef.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NotaCocinadaTest {
    private fun entrada(
        id: String,
        slug: String = "sopa",
        fecha: String = "2026-01-01",
        nota: String? = "algo",
    ) = EntradaHistorial(id = id, slug = slug, titulo = slug, fecha = fecha, nota = nota)

    @Test
    fun filtraPorSlug() {
        val entradas = listOf(entrada("1", "sopa", nota = "a"), entrada("2", "arroz", nota = "b"))
        assertEquals(listOf(NotaCocinada("2026-01-01", "a")), notasDe(entradas, "sopa"))
    }

    @Test
    fun ignoraNotasNulasOEnBlanco() {
        val entradas = listOf(entrada("1", nota = null), entrada("2", nota = "   "), entrada("3", nota = ""))
        assertEquals(emptyList<NotaCocinada>(), notasDe(entradas, "sopa"))
    }

    @Test
    fun recortaLaNota() {
        val entradas = listOf(entrada("1", nota = "  sal \n"))
        assertEquals(listOf(NotaCocinada("2026-01-01", "sal")), notasDe(entradas, "sopa"))
    }

    @Test
    fun ordenaDeLaMasRecienteALaMasAntigua() {
        val entradas =
            listOf(
                entrada("1", fecha = "2026-01-01", nota = "vieja"),
                entrada("2", fecha = "2026-03-01", nota = "nueva"),
                entrada("3", fecha = "2026-02-01", nota = "media"),
            )
        assertEquals(listOf("nueva", "media", "vieja"), notasDe(entradas, "sopa").map { it.nota })
    }

    @Test
    fun losEmpatesDeFechaSeDesempatanPorIdDescendente() {
        val entradas =
            listOf(
                entrada("2026-01-01T10:00", nota = "mañana"),
                entrada("2026-01-01T20:00", nota = "noche"),
                entrada("2026-01-01T15:00", nota = "tarde"),
            )
        assertEquals(listOf("noche", "tarde", "mañana"), notasDe(entradas, "sopa").map { it.nota })
    }
}
