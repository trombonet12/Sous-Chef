package app.souschef.domain.crono

import app.souschef.receta.crono.EstadoCarril
import app.souschef.receta.crono.EstadoCrono
import app.souschef.receta.crono.MotorCrono
import app.souschef.receta.crono.SesionCrono
import app.souschef.receta.crono.TipoAviso
import app.souschef.ui.crono.T0
import app.souschef.ui.crono.recetaCrono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnMarchaTest {
    private val receta = recetaCrono()

    private fun portada(): EstadoCrono = SesionCrono.inicial(receta, T0 + 15 * 60_000L, T0, 2)

    private fun abierto(): EstadoCrono = SesionCrono.abrir(portada(), receta, T0)

    @Test
    fun sinAbrirLaCocinaNoHayOngoing() {
        assertNull(ConstructorEnMarcha.construir(receta, portada(), T0))
    }

    @Test
    fun cuentaAtrasDelPasoQueAcabaAntesYUnaLineaPorCarril() {
        var e = MotorCrono.empezar(abierto(), receta, "f0", T0)
        e = MotorCrono.empezar(e, receta, "m", T0)
        val m = checkNotNull(ConstructorEnMarcha.construir(receta, e, T0 + 10_000L))
        assertEquals("Receta r", m.titulo)
        assertEquals(T0 + 2 * 60_000L, m.finAtMs)
        assertEquals("1:50", m.cortoCritico)
        assertEquals(2, m.lineas.size)
        assertTrue(m.lineas.contains("F0 · Sofrito · 4:50"))
        assertEquals("M · Cortar · 1:50", m.resumen)
    }

    @Test
    fun sinArrancarYVencido() {
        val m0 = checkNotNull(ConstructorEnMarcha.construir(receta, abierto(), T0))
        assertNull(m0.finAtMs)
        assertTrue(m0.lineas.all { it.endsWith("sin arrancar") })
        val e = MotorCrono.empezar(abierto(), receta, "m", T0)
        val vencido = checkNotNull(ConstructorEnMarcha.construir(receta, e, T0 + 3 * 60_000L))
        assertNull(vencido.finAtMs)
        assertEquals("¡Hecho!", vencido.cortoCritico)
        assertTrue(vencido.resumen.endsWith("¡listo!"))
    }

    @Test
    fun terminadoNoHayOngoing() {
        val e = abierto()
        val fin = e.copy(carriles = e.carriles.mapValues { EstadoCarril(fin = true) })
        assertNull(ConstructorEnMarcha.construir(receta, fin, T0))
    }

    @Test
    fun cadaCarrilTieneSuIdEstableYNoChocaConLasOtras() {
        assertEquals(IdsNotificacion.carril("f0"), IdsNotificacion.carril("f0"))
        assertFalse(IdsNotificacion.carril("f0") == IdsNotificacion.carril("m"))
        assertFalse(IdsNotificacion.carril("f0") == IdsNotificacion.ID_EN_MARCHA)
        assertEquals("carril:r", IdsNotificacion.tagCarril("r"))
    }

    @Test
    fun despiertaLaPantallaSoloConAvisosDeAccionNoAtrasados() {
        assertTrue(IdsNotificacion.despiertaPantalla(TipoAviso.FIN, false))
        assertTrue(IdsNotificacion.despiertaPantalla(TipoAviso.HITO, false))
        assertTrue(IdsNotificacion.despiertaPantalla(TipoAviso.TOCA_EMPEZAR, false))
        assertFalse(IdsNotificacion.despiertaPantalla(TipoAviso.DIEZ_S, false))
        assertFalse(IdsNotificacion.despiertaPantalla(TipoAviso.FIN, true))
    }

    @Test
    fun carrilesAvanzadosDetectaHechoYFin() {
        val antes = abierto()
        val hecho = MotorCrono.hecho(antes, receta, "f0", T0)
        val fin = MotorCrono.hecho(hecho, receta, "m", T0)
        assertEquals(setOf("f0"), IdsNotificacion.carrilesAvanzados(antes, hecho))
        assertEquals(setOf("m"), IdsNotificacion.carrilesAvanzados(hecho, fin))
        assertTrue(IdsNotificacion.carrilesAvanzados(antes, antes).isEmpty())
    }
}
