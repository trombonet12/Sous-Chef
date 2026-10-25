package app.trombito.souschef.domain

import app.trombito.souschef.ui.receta
import app.trombito.souschef.ui.recetas.Hace
import app.trombito.souschef.ui.recetas.UnidadHace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CocinadaAnotadaTest {
    private val receta =
        receta("sopa", titulo = "Sopa", base = "caldo", tecnicas = listOf("sofrito")).copy(
            totalMin = 46.2,
            creada = "2026-10-10T00:00:00Z",
        )
    private val momento = LocalDateTime.of(2026, 10, 12, 21, 5, 9)

    @Test
    fun `la entrada anotada no lleva pasos ni duracion y si el plan de la receta`() {
        val e = entradaAnotada(receta, 3, momento, "  con menos sal  ")
        assertEquals("sopa-2026-10-12-210509", e.id)
        assertEquals("2026-10-12", e.fecha)
        assertEquals("Sopa", e.titulo)
        assertEquals("caldo", e.base)
        assertEquals(listOf("sofrito"), e.tecnica)
        assertEquals(3, e.raciones)
        assertNull(e.minutosReales)
        assertEquals(emptyList<PasoReal>(), e.pasosReales)
        assertEquals(47, e.previstoMin)
        assertEquals("con menos sal", e.nota)
        assertNull(e.valoracion)
    }

    @Test
    fun `la nota vacia se guarda como nula y la larga se recorta`() {
        assertNull(entradaAnotada(receta, 2, momento, "   ").nota)
        assertEquals(MAX_NOTA, entradaAnotada(receta, 2, momento, "x".repeat(MAX_NOTA + 40)).nota?.length)
    }

    @Test
    fun `normalizarNota recorta, limita y devuelve nulo si queda vacia`() {
        assertEquals("hola", normalizarNota(" hola "))
        assertNull(normalizarNota(""))
        assertNull(normalizarNota(null))
        assertEquals(MAX_NOTA, normalizarNota("y".repeat(MAX_NOTA * 2))?.length)
    }

    @Test
    fun `una anotada no cuenta para el ritmo`() {
        val anotada = entradaAnotada(receta, 2, momento, "")
        val reales =
            listOf(
                EntradaHistorial("a", "sopa", "Sopa", "2026-10-01", minutosReales = 60),
                EntradaHistorial("b", "sopa", "Sopa", "2026-10-02", minutosReales = 62),
            )
        // Con las dos reales sola ya hay desvío (+14 y +16); la anotada, aunque sea la más reciente, no lo cambia.
        val solas = desvioHabitualMin(reales, "sopa", 46)
        assertEquals(solas, desvioHabitualMin(reales + anotada, "sopa", 46))
        // Una real y una anotada no bastan: faltan 2 con minutos.
        assertNull(desvioHabitualMin(listOf(reales.first(), anotada), "sopa", 46))
    }

    @Test
    fun `una anotada cuenta para hace y deja de ser nueva`() {
        val hoy = LocalDate.of(2026, 10, 24)
        val anotada = entradaAnotada(receta, 2, momento, "")
        val ultima = listOf(anotada).maxOf { it.fecha }
        assertEquals(UnidadHace.SEMANAS, Hace.de(ultima, hoy).unidad)
        assertEquals(1, Hace.de(ultima, hoy).cantidad)
        assertTrue(esNueva(receta, null, hoy))
        assertFalse(esNueva(receta, ultima, hoy))
    }

    @Test
    fun `ajustarMomento mueve dias y minutos sin pasar de ahora`() {
        val ahora = LocalDateTime.of(2026, 10, 24, 18, 30, 0)
        val actual = LocalDateTime.of(2026, 10, 24, 18, 30, 0)
        assertEquals(LocalDateTime.of(2026, 10, 23, 18, 30), ajustarMomento(actual, ahora, dias = -1))
        assertEquals(LocalDateTime.of(2026, 10, 24, 18, 15), ajustarMomento(actual, ahora, minutos = -15))
        // No se anota el futuro: se queda en ahora.
        assertEquals(ahora, ajustarMomento(actual, ahora, dias = 1))
        assertEquals(ahora, ajustarMomento(actual, ahora, minutos = PASO_HORA_ANOTAR_MIN))
    }

    @Test
    fun `los minutos dan la vuelta a medianoche sin cambiar la fecha`() {
        val ahora = LocalDateTime.of(2026, 10, 24, 23, 59, 0)
        val actual = LocalDateTime.of(2026, 10, 20, 0, 5, 0)
        assertEquals(LocalDateTime.of(2026, 10, 20, 23, 50), ajustarMomento(actual, ahora, minutos = -15))
        assertEquals(LocalDateTime.of(2026, 10, 20, 0, 20), ajustarMomento(actual, ahora, minutos = 15))
    }
}
