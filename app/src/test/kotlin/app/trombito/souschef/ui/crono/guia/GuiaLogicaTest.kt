package app.trombito.souschef.ui.crono.guia

import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.ui.crono.CronoUi
import app.trombito.souschef.ui.crono.recetaCrono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuiaLogicaTest {
    private val todos = GuiaIds.ORDEN.toSet()

    @Test
    fun `el orden es carril hecho prorroga deshacer micro menu`() {
        assertEquals(
            listOf("carril", "hecho", "prorroga", "deshacer", "micro", "menu"),
            GuiaIds.ORDEN,
        )
    }

    @Test
    fun `salta los puntos que la disposicion no tiene`() {
        val visibles = setOf(GuiaIds.CARRIL, GuiaIds.HECHO, GuiaIds.MENU)
        assertEquals(0, primerPuntoDesde(visibles, 0))
        assertEquals(1, primerPuntoDesde(visibles, 1))
        // Tras «hecho» no hay prórroga, deshacer ni micro: el siguiente es el menú.
        assertEquals(5, primerPuntoDesde(visibles, 2))
        assertNull(primerPuntoDesde(visibles, 6))
    }

    @Test
    fun `el ultimo punto es el que no tiene otro detras`() {
        val visibles = setOf(GuiaIds.CARRIL, GuiaIds.HECHO, GuiaIds.MENU)
        assertTrue(hayMasPuntos(visibles, 0))
        assertTrue(hayMasPuntos(visibles, 1))
        assertFalse(hayMasPuntos(visibles, 5))
        assertFalse(hayMasPuntos(emptySet(), 0))
    }

    @Test
    fun `numera solo los puntos que existen`() {
        val visibles = setOf(GuiaIds.CARRIL, GuiaIds.HECHO, GuiaIds.MENU)
        assertEquals(1, numeroDePunto(visibles, 0))
        assertEquals(2, numeroDePunto(visibles, 1))
        assertEquals(3, numeroDePunto(visibles, 5))
        assertEquals(6, numeroDePunto(todos, 5))
    }

    private fun cocina(vista: Boolean) =
        CronoUi(
            cargando = false,
            receta = recetaCrono(),
            estado = EstadoCrono("r", 2, 0L, emptyMap(), inicioMs = 1L, actualizadoMs = 1L),
            guiaVista = vista,
        )

    @Test
    fun `sale en la cocina si no se ha visto y no si ya se vio`() {
        assertTrue(mostrarGuia(cocina(vista = false)))
        assertFalse(mostrarGuia(cocina(vista = true)))
    }

    @Test
    fun `no sale en la portada ni mientras carga`() {
        val portada = cocina(false).copy(estado = EstadoCrono("r", 2, 0L, emptyMap(), actualizadoMs = 1L))
        assertFalse(mostrarGuia(portada))
        assertFalse(mostrarGuia(CronoUi(guiaVista = false)))
    }
}
