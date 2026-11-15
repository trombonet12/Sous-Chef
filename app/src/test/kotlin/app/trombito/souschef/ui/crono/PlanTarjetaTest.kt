package app.trombito.souschef.ui.crono

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reparto del alto de la tarjeta: la nota y METO/SACO van por delante del reloj grande, que baja hasta 36 sp antes de
 * ocultar la nota. La pila de texto (prioridad y medida real) se prueba en `PilaPrioritariaTest`. Referencia: 782 dp
 * útiles para las tarjetas (móvil de 411 × 914 dp).
 */
class PlanTarjetaTest {
    @Test
    fun `con altura de sobra la tarjeta lo muestra todo`() {
        val plan = planTarjeta(alturaDp = 560f, carriles = 3, hayMetoSaco = true, hayNota = true)
        assertTrue(plan.nota)
        assertTrue(plan.metoSaco)
        assertEquals(2, plan.tituloLineas)
        assertEquals(88, plan.relojSp)
    }

    @Test
    fun `dos carriles a 782 dp llevan nota y meto saco y el reloj no baja del minimo`() {
        val plan = planTarjeta(alturaDp = 782f / 2, carriles = 2, hayMetoSaco = true, hayNota = true)
        assertTrue(plan.nota)
        assertTrue(plan.metoSaco)
        assertTrue(plan.relojSp >= 36)
    }

    @Test
    fun `tres carriles en marcha a 782 dp pasan a compacta para no perder la nota`() {
        val porTarjeta = 782f / 3
        assertTrue(tarjetaCompacta(alturaDp = porTarjeta, escalaLetra = 1f))
    }

    @Test
    fun `a 355 dp con nota y meto saco la tarjeta pasa a compacta en vez de ocultar meto saco`() {
        // Dos tarjetas de ~355 dp y una fila de 56 dp: no caben nota y METO/SACO con el reloj a 36 sp.
        assertTrue(tarjetaCompacta(alturaDp = 355f, escalaLetra = 1f, hayNota = true, hayMetoSaco = true))
        assertTrue(tarjetaCompacta(alturaDp = 355f, escalaLetra = 1f))
    }

    @Test
    fun `a 355 dp con nota y sin meto saco se conserva la nota y el reloj minimo`() {
        val plan = planTarjeta(alturaDp = 355f, carriles = 3, hayMetoSaco = false, hayNota = true)
        assertFalse(tarjetaCompacta(alturaDp = 355f, escalaLetra = 1f, hayNota = true, hayMetoSaco = false))
        assertTrue(plan.nota)
        assertTrue(plan.relojSp >= 36)
    }

    @Test
    fun `el reloj baja antes de ocultar la nota`() {
        val plan = planTarjeta(alturaDp = 300f, carriles = 3, hayMetoSaco = false, hayNota = true)
        assertTrue(plan.nota)
        assertEquals(1, plan.tituloLineas)
        assertEquals(36, plan.relojSp)
    }

    @Test
    fun `el reloj baja antes de ocultar meto saco y por encima del umbral caben las dos cosas`() {
        val plan = planTarjeta(alturaDp = 450f, carriles = 3, hayMetoSaco = true, hayNota = true)
        assertFalse(tarjetaCompacta(alturaDp = 450f, escalaLetra = 1f))
        assertTrue(plan.nota)
        assertTrue(plan.metoSaco)
        assertEquals(1, plan.tituloLineas)
        assertTrue(plan.relojSp >= 36)
    }

    @Test
    fun `si la tarjeta no es compacta el plan nunca oculta la nota ni meto saco`() {
        (200..700 step 5).forEach { alto ->
            if (!tarjetaCompacta(alto.toFloat(), escalaLetra = 1f)) {
                val plan = planTarjeta(alto.toFloat(), carriles = 3, hayMetoSaco = true, hayNota = true)
                assertTrue("alto $alto", plan.nota && plan.metoSaco)
            }
        }
    }

    @Test
    fun `sin nota ni meto saco el reloj ocupa lo que sobra`() {
        val plan = planTarjeta(alturaDp = 380f, carriles = 2, hayMetoSaco = false, hayNota = false)
        assertFalse(plan.nota)
        assertFalse(plan.metoSaco)
        assertEquals(2, plan.tituloLineas)
        assertTrue(plan.relojSp > 36)
    }

    @Test
    fun `meto y saco ya no se ocultan por tener cuatro carriles`() {
        val plan = planTarjeta(alturaDp = 560f, carriles = 4, hayMetoSaco = true, hayNota = true)
        assertTrue(plan.metoSaco)
    }

    @Test
    fun `el plan nunca baja el reloj de 36 sp`() {
        listOf(250f, 280f, 300f, 330f, 360f, 400f, 450f).forEach { alto ->
            val plan = planTarjeta(alto, carriles = 3, hayMetoSaco = true, hayNota = true)
            assertTrue("alto $alto", plan.relojSp in 36..88)
        }
    }

    @Test
    fun `con cuatro carriles en un movil la tarjeta pasa a compacta`() {
        assertTrue(tarjetaCompacta(alturaDp = 150f, escalaLetra = 1f))
        assertTrue(tarjetaCompacta(alturaDp = 260f, escalaLetra = 1f))
        assertFalse(tarjetaCompacta(alturaDp = 350f, escalaLetra = 1f, hayNota = true, hayMetoSaco = false))
        assertTrue(tarjetaCompacta(alturaDp = 320f, escalaLetra = 1f, hayNota = true, hayMetoSaco = true))
    }

    @Test
    fun `con la letra del sistema grande se compacta antes`() {
        assertTrue(tarjetaCompacta(alturaDp = 320f, escalaLetra = 1.3f))
        assertFalse(tarjetaCompacta(alturaDp = 450f, escalaLetra = 1.3f, hayNota = true, hayMetoSaco = false))
    }

    @Test
    fun `el plan reserva a la pila de texto el alto de la nota y de meto saco`() {
        val todo = planTarjeta(alturaDp = 450f, carriles = 2, hayMetoSaco = true, hayNota = true)
        assertEquals(60 + 100, todo.textoDp)
        val soloNota = planTarjeta(alturaDp = 320f, carriles = 2, hayMetoSaco = false, hayNota = true)
        assertEquals(60, soloNota.textoDp)
        val nada = planTarjeta(alturaDp = 300f, carriles = 2, hayMetoSaco = false, hayNota = false)
        assertEquals(0, nada.textoDp)
    }

    @Test
    fun `lo que sobra con el reloj a su maximo es para el texto y no queda en blanco`() {
        val justo = planTarjeta(alturaDp = 450f, carriles = 1, hayMetoSaco = true, hayNota = true)
        val alta = planTarjeta(alturaDp = 700f, carriles = 1, hayMetoSaco = true, hayNota = true)
        assertEquals(88, alta.relojSp)
        assertTrue(alta.textoDp > justo.textoDp)
        // Con el reloj a 88 sp (106 dp) y la parte fija, todo el resto va al texto.
        val resto = 700 - (12 + 48 + 32 + 52 + 48 + 20) - 2 * 28 - 88 * 1.2f
        assertEquals(resto, alta.textoDp.toFloat(), 1.5f)
    }

    @Test
    fun `el despues se queda sin repetir la potencia si el titulo ya la dice`() {
        assertEquals("Subir al 5", quitarPotenciaRepetida("Subir al 5 · fuego 5"))
        assertEquals("Reposo · fuego 8", quitarPotenciaRepetida("Reposo · fuego 8"))
        assertEquals("Subir al fuego 5", quitarPotenciaRepetida("Subir al fuego 5 · fuego 5"))
        assertEquals("Emplatar", quitarPotenciaRepetida("Emplatar"))
        assertEquals("Subir a 50 g · fuego 5", quitarPotenciaRepetida("Subir a 50 g · fuego 5"))
    }
}
