package app.trombito.souschef.domain.crono

import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.MotorCrono
import app.trombito.souschef.receta.crono.SesionCrono
import app.trombito.souschef.ui.crono.T0
import app.trombito.souschef.ui.crono.recetaCrono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdsNotificacionTest {
    private val receta = recetaCrono()

    private fun portada(): EstadoCrono = SesionCrono.inicial(receta, T0 + 15 * 60_000L, T0, 2)

    private fun abierto(): EstadoCrono = SesionCrono.abrir(portada(), receta, T0)

    @Test
    fun unaCocinaYUnaAlarmaPorCocinadaConIdsDistintosYTagDelSlug() {
        assertFalse(IdsNotificacion.ID_COCINA == IdsNotificacion.ID_ALARMA)
        assertEquals("cocina:r", IdsNotificacion.tagCocina("r"))
        assertEquals("alarma:r", IdsNotificacion.tagAlarma("r"))
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

    private fun carril(
        id: String,
        indice: Int,
        total: Int,
        fin: Long?,
    ) = CarrilNotificacion(
        "r",
        "Receta",
        id,
        id.uppercase(),
        "#112233",
        indice,
        total,
        "t",
        if (fin != null) SituacionCarril.CORRIENDO else SituacionCarril.SIN_ARRANCAR,
        fin,
        if (fin != null) 125 else 0,
    )

    @Test
    fun seAsciendeElCarrilQueAcabaAntes() {
        val cs = listOf(carril("a", 1, 2, 9_000L), carril("b", 1, 2, 5_000L), carril("c", 1, 2, null))
        assertEquals("b", CarrilNotificacion.promocionado(cs))
    }

    @Test
    fun sinRelojSePromocionaElPrimeroYSinCarrilesNinguno() {
        assertEquals("a", CarrilNotificacion.promocionado(listOf(carril("a", 1, 2, null), carril("b", 1, 2, null))))
        assertNull(CarrilNotificacion.promocionado(emptyList()))
    }

    @Test
    fun progresoDePasoIDeNYChipCorto() {
        val c = carril("a", 1, 4, 5_000L)
        assertEquals(0, c.progreso)
        assertEquals(2 * CarrilNotificacion.PASO, carril("a", 3, 4, null).progreso)
        assertEquals("2:05", c.cortoCritico)
        assertEquals("A", carril("a", 1, 4, null).cortoCritico)
    }
}
