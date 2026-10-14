package app.souschef.domain.crono

import app.souschef.receta.crono.MotorCrono
import app.souschef.receta.crono.SesionCrono
import app.souschef.ui.crono.T0
import app.souschef.ui.crono.recetaCrono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccionSiguienteTest {
    private val receta = recetaCrono()
    private val abierto = SesionCrono.abrir(SesionCrono.inicial(receta, T0 + 15 * 60_000L, T0, 2), receta, T0)

    @Test
    fun hechoDelQueAcabaAntesYElOtroCarrilSigueIntacto() {
        val e = MotorCrono.empezar(MotorCrono.empezar(abierto, receta, "f0", T0), receta, "m", T0)
        val r = AccionSiguiente.aplicar(e, receta, T0 + 30_000L)
        assertEquals("m", r.carril)
        assertTrue(
            r.estado.carriles
                .getValue("m")
                .fin,
        )
        assertEquals(
            0,
            r.estado.carriles
                .getValue("f0")
                .i,
        )
    }

    @Test
    fun conSiguientePasoEnElCarrilLoArranca() {
        val e = MotorCrono.empezar(abierto, receta, "f0", T0)
        val ahora = T0 + 60_000L
        val r = AccionSiguiente.aplicar(e, receta, ahora)
        assertEquals("f0", r.carril)
        val ec = r.estado.carriles.getValue("f0")
        assertEquals(1, ec.i)
        assertEquals(ahora + 10 * 60_000L, ec.finAtMs)
        assertEquals(60, r.estado.pasosReales["p1"])
    }

    @Test
    fun siNadaCorreArrancaElProximoAccionable() {
        val r = AccionSiguiente.aplicar(abierto, receta, T0)
        assertTrue(r.carril != null)
        assertTrue(
            r.estado.carriles
                .getValue(checkNotNull(r.carril))
                .corriendo,
        )
    }

    @Test
    fun conTodoTerminadoNoHaceNada() {
        val a = MotorCrono.hecho(abierto, receta, "f0", T0)
        val b = MotorCrono.hecho(a, receta, "f0", T0)
        val fin = MotorCrono.hecho(b, receta, "m", T0)
        val r = AccionSiguiente.aplicar(fin, receta, T0)
        assertNull(r.carril)
        assertEquals(fin, r.estado)
    }
}
