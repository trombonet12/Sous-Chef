package app.souschef.domain.crono

import app.souschef.receta.crono.EstadoCarril
import app.souschef.receta.crono.MotorCrono
import app.souschef.receta.crono.SesionCrono
import app.souschef.ui.crono.T0
import app.souschef.ui.crono.recetaCrono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarrilesNotificacionTest {
    private val receta = recetaCrono()

    private fun abierto() = SesionCrono.abrir(SesionCrono.inicial(receta, T0 + 15 * 60_000L, T0, 2), receta, T0)

    private fun carril(
        lista: List<CarrilNotificacion>,
        id: String,
    ) = lista.single { it.carril == id }

    @Test
    fun sinAbrirLaCocinaNoHayCarriles() {
        val portada = SesionCrono.inicial(receta, T0 + 15 * 60_000L, T0, 2)
        assertTrue(ConstructorCarriles.construir(receta, portada, T0).isEmpty())
    }

    @Test
    fun tituloPasoIDeNYProgreso() {
        val e = MotorCrono.hecho(MotorCrono.empezar(abierto(), receta, "f0", T0), receta, "f0", T0)
        val f0 = carril(ConstructorCarriles.construir(receta, e, T0), "f0")
        assertEquals("F0 · paso 2/2", f0.titulo)
        assertEquals("Hervir", f0.texto)
        assertEquals(1, f0.hechos)
        assertEquals(2, f0.total)
        assertEquals(SituacionCarril.SIN_ARRANCAR, f0.situacion)
        assertNull(f0.finAtMs)
    }

    @Test
    fun corriendoLlevaCuentaAtrasYVencidoNo() {
        val e = MotorCrono.empezar(abierto(), receta, "m", T0)
        val m = carril(ConstructorCarriles.construir(receta, e, T0 + 10_000L), "m")
        assertEquals(SituacionCarril.CORRIENDO, m.situacion)
        assertEquals(T0 + 2 * 60_000L, m.finAtMs)
        val tarde = carril(ConstructorCarriles.construir(receta, e, T0 + 3 * 60_000L), "m")
        assertEquals(SituacionCarril.VENCIDO, tarde.situacion)
        assertNull(tarde.finAtMs)
    }

    @Test
    fun carrilTerminadoSeQuitaYIdEsEstable() {
        val e = abierto()
        val fin = e.copy(carriles = e.carriles + ("m" to EstadoCarril(fin = true)))
        val lista = ConstructorCarriles.construir(receta, fin, T0)
        assertEquals(listOf("f0"), lista.map { it.carril })
        assertEquals("f0".hashCode(), lista.single().id)
    }
}
