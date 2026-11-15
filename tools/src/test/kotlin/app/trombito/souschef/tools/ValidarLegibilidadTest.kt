package app.trombito.souschef.tools

import app.trombito.souschef.receta.Hallazgo
import app.trombito.souschef.receta.Nivel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidarLegibilidadTest {
    private fun h(
        regla: Int,
        nivel: Nivel = Nivel.AVISO,
    ) = Hallazgo("r", regla, nivel, "x")

    @Test
    fun resumenCuentaPorReglaEnOrdenNumerico() {
        val r = resumenPorRegla(listOf(h(31), h(30), h(31), h(35, Nivel.ERROR)), 30..36)
        assertEquals(listOf("R30: 1", "R31: 2", "R35: 1"), r)
    }

    @Test
    fun resumenSoloIncluyeElRangoPedido() {
        assertEquals(listOf("R30: 1"), resumenPorRegla(listOf(h(30), h(21)), 30..36))
    }

    @Test
    fun legibilidadHaceFallarConAvisosComoEstricto() {
        assertTrue(validarFalla(errores = 0, avisos = 1, estricto = false, legibilidad = true))
        assertTrue(validarFalla(errores = 0, avisos = 1, estricto = true, legibilidad = false))
        assertFalse(validarFalla(errores = 0, avisos = 1, estricto = false, legibilidad = false))
        assertTrue(validarFalla(errores = 1, avisos = 0, estricto = false, legibilidad = false))
        assertFalse(validarFalla(errores = 0, avisos = 0, estricto = true, legibilidad = true))
    }
}
