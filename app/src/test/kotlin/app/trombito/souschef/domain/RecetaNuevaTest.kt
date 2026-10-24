package app.trombito.souschef.domain

import app.trombito.souschef.ui.receta
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecetaNuevaTest {
    private val hoy = LocalDate.of(2026, 10, 24)

    private fun conCreada(creada: String?) = receta("x").copy(creada = creada)

    @Test
    fun `nunca cocinada y creada hace poco es nueva`() {
        assertTrue(esNueva(conCreada("2026-10-24T00:00:00Z"), null, hoy))
        assertTrue(esNueva(conCreada("2026-10-10T00:00:00Z"), null, hoy))
    }

    @Test
    fun `el dia 29 aun es nueva y el 30 ya no`() {
        assertTrue(esNueva(conCreada("2026-09-25T00:00:00Z"), null, hoy))
        assertFalse(esNueva(conCreada("2026-09-24T00:00:00Z"), null, hoy))
    }

    @Test
    fun `si ya se ha cocinado no es nueva`() {
        assertFalse(esNueva(conCreada("2026-10-20T00:00:00Z"), "2026-10-21", hoy))
    }

    @Test
    fun `con mas de 30 dias no es nueva`() {
        assertFalse(esNueva(conCreada("2026-01-05T00:00:00Z"), null, hoy))
    }

    @Test
    fun `sin creada o ilegible no es nueva`() {
        assertFalse(esNueva(conCreada(null), null, hoy))
        assertFalse(esNueva(conCreada(""), null, hoy))
        assertFalse(esNueva(conCreada("ayer"), null, hoy))
    }

    @Test
    fun `una fecha de creacion en el futuro no cuenta`() {
        assertFalse(esNueva(conCreada("2026-11-01T00:00:00Z"), null, hoy))
    }
}
