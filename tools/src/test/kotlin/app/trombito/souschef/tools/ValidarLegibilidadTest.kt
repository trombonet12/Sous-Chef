package app.trombito.souschef.tools

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Hallazgo
import app.trombito.souschef.receta.Nivel
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Validador
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
    fun soloLosErroresOEstrictoConAvisosHacenFallar() {
        assertTrue(validarFalla(errores = 0, avisos = 1, estricto = true))
        assertFalse(validarFalla(errores = 0, avisos = 1, estricto = false))
        assertTrue(validarFalla(errores = 1, avisos = 0, estricto = false))
        assertFalse(validarFalla(errores = 0, avisos = 0, estricto = true))
    }

    @Test
    fun unaRecetaQueIncumpleUnTopeFallaConEstricto() {
        val receta =
            Receta(
                slug = "r",
                titulo = "R",
                totalMin = 10.0,
                carriles = listOf(Carril("a", tipo = "fuego")),
                pasos = listOf(Paso("p1", "x".repeat(80), "a", 0.0, 10.0, f = 5, p = "Porque si, " + "x".repeat(120))),
            )
        val topes = Validador.receta(receta).filter { it.regla in 30..36 }
        assertTrue(topes.any { it.regla == 30 })
        assertTrue(topes.all { it.nivel == Nivel.AVISO })
        assertTrue(validarFalla(errores = 0, avisos = topes.size, estricto = true))
        assertFalse(validarFalla(errores = 0, avisos = topes.size, estricto = false))
    }
}
