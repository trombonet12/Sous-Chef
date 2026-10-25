package app.trombito.souschef.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ContadorPropuestasTest {
    private val hoy = LocalDate.of(2026, 10, 25)
    private val ayer = hoy.minusDays(1)

    @Test
    fun `otra suma uno cada pulsacion del mismo dia`() {
        val primera = ContadorPropuestas.siguienteOtra(null, hoy)
        assertEquals("2026-10-25|1", primera)
        assertEquals("2026-10-25|2", ContadorPropuestas.siguienteOtra(primera, hoy))
    }

    @Test
    fun `otra se reinicia cada dia`() {
        val deAyer = ContadorPropuestas.siguienteOtra(ContadorPropuestas.siguienteOtra(null, ayer), ayer)
        assertEquals(2, ContadorPropuestas.vecesHoy(ContadorPropuestas.otraDe(deAyer), ayer))
        assertEquals(0, ContadorPropuestas.vecesHoy(ContadorPropuestas.otraDe(deAyer), hoy))
        assertEquals("2026-10-25|1", ContadorPropuestas.siguienteOtra(deAyer, hoy))
    }

    @Test
    fun `un valor ilegible de otra cuenta como cero`() {
        listOf(null, "", "basura", "2026-10-25", "2026-10-25|x", "2026-10-25|-3", "ayer|2").forEach {
            assertNull(it, ContadorPropuestas.otraDe(it))
            assertEquals("2026-10-25|1", ContadorPropuestas.siguienteOtra(it, hoy))
        }
    }

    @Test
    fun `lo mostrado se acumula durante el dia`() {
        val v1 = ContadorPropuestas.mostrar(null, hoy, listOf("a", "b", "c"))
        val v2 = ContadorPropuestas.mostrar(v1, hoy, listOf("c", "d"))
        assertEquals(setOf("a", "b", "c", "d"), ContadorPropuestas.mostradasDe(v2, hoy))
    }

    @Test
    fun `mostrar lo mismo no cambia el valor`() {
        val v1 = ContadorPropuestas.mostrar(null, hoy, listOf("a", "b"))
        assertEquals(v1, ContadorPropuestas.mostrar(v1, hoy, listOf("b", "a")))
    }

    @Test
    fun `lo mostrado de otro dia se olvida`() {
        val deAyer = ContadorPropuestas.mostrar(null, ayer, listOf("a", "b"))
        assertTrue(ContadorPropuestas.mostradasDe(deAyer, hoy).isEmpty())
        val deHoy = ContadorPropuestas.mostrar(deAyer, hoy, listOf("c"))
        assertEquals(setOf("c"), ContadorPropuestas.mostradasDe(deHoy, hoy))
    }

    @Test
    fun `una cocinada cuenta si estaba propuesta ese dia`() {
        val v = ContadorPropuestas.mostrar(null, hoy, listOf("curry", "lasana"))
        assertTrue(ContadorPropuestas.eraPropuesta(v, "curry", "2026-10-25"))
        assertTrue(ContadorPropuestas.eraPropuesta(v, "lasana", "2026-10-25T13:00"))
        assertFalse(ContadorPropuestas.eraPropuesta(v, "paella", "2026-10-25"))
        assertFalse(ContadorPropuestas.eraPropuesta(v, "curry", "2026-10-24"))
    }

    @Test
    fun `sin nada mostrado o con fecha rota no cuenta`() {
        assertFalse(ContadorPropuestas.eraPropuesta(null, "curry", "2026-10-25"))
        assertFalse(ContadorPropuestas.eraPropuesta("basura", "curry", "2026-10-25"))
        assertFalse(ContadorPropuestas.eraPropuesta("2026-10-25|curry", "curry", "no es fecha"))
    }
}
