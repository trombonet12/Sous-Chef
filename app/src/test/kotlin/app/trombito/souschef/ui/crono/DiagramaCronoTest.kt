package app.trombito.souschef.ui.crono

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiagramaCronoTest {
    private val servir = 1_800_000_000_000L

    @Test
    fun `el minuto del plan cuenta desde servir menos el total`() {
        assertEquals(0.0, minutoDelPlan(servir - 47 * 60_000L, servir, 47.0)!!, 1e-9)
        assertEquals(17.0, minutoDelPlan(servir - 30 * 60_000L, servir, 47.0)!!, 1e-9)
        assertEquals(47.0, minutoDelPlan(servir, servir, 47.0)!!, 1e-9)
    }

    @Test
    fun `el total se redondea a medio minuto como el motor`() {
        assertEquals(0.5, minutoDelPlan(servir - 47 * 60_000L, servir, 47.4)!!, 1e-9)
    }

    @Test
    fun `fuera del plan no hay linea de ahora`() {
        assertNull(minutoDelPlan(servir - 48 * 60_000L, servir, 47.0))
        assertNull(minutoDelPlan(servir + 1, servir, 47.0))
    }
}
