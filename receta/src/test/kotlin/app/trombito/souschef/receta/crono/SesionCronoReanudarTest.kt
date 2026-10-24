package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.leerReceta
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Decisión de reanudar al entrar en la cocina: nueva, tal cual (abierta) o portada con ajustes nuevos. */
class SesionCronoReanudarTest {
    private val ensalada =
        leerReceta(File("../data/recetas/ensalada-de-noquis-con-atun-y-feta.json").readText())

    private val ahora = 1_700_000_000_000L
    private val servirNuevo = ahora + 3_600_000L
    private val servirViejo = ahora + 7_200_000L

    private fun portada() =
        SesionCrono
            .inicial(ensalada, servirViejo, ahora - 60_000L, raciones = 2)
            .copy(marcados = setOf("mep-1"))

    @Test
    fun sinGuardadoCreaLaCocinadaNuevaConLasRacionesYLaHoraPedidas() {
        val e = SesionCrono.reanudar(null, ensalada, raciones = 4, servirMs = servirNuevo, ahoraMs = ahora)
        assertEquals(SesionCrono.inicial(ensalada, servirNuevo, ahora, 4), e)
        assertNull(e.inicioMs)
    }

    @Test
    fun portadaGuardadaConservaElProgresoYTomaRacionesYHoraNuevas() {
        val e = SesionCrono.reanudar(portada(), ensalada, raciones = 4, servirMs = servirNuevo, ahoraMs = ahora)
        assertEquals(4, e.raciones)
        assertEquals(servirNuevo, e.servirMs)
        assertEquals(ahora, e.actualizadoMs)
        assertEquals(setOf("mep-1"), e.marcados)
        assertNull(e.inicioMs)
    }

    @Test
    fun cocinadaAbiertaSeReanudaTalCualSinPisarlaConLaFicha() {
        val abierta = SesionCrono.abrir(portada(), ensalada, ahora - 30_000L)
        val e = SesionCrono.reanudar(abierta, ensalada, raciones = 4, servirMs = servirNuevo, ahoraMs = ahora)
        assertSame(abierta, e)
        assertEquals(2, e.raciones)
        assertEquals(servirViejo, e.servirMs)
    }

    @Test
    fun estadoCaducadoSeDescartaYEmpiezaDeNuevo() {
        val viejo = portada().copy(actualizadoMs = ahora - SesionCrono.CADUCIDAD_MS - 1)
        val e = SesionCrono.reanudar(viejo, ensalada, raciones = 3, servirMs = servirNuevo, ahoraMs = ahora)
        assertEquals(SesionCrono.inicial(ensalada, servirNuevo, ahora, 3), e)
    }

    @Test
    fun estadoDeOtraRecetaSeDescarta() {
        val ajeno = portada().copy(slug = "otra")
        val e = SesionCrono.reanudar(ajeno, ensalada, raciones = 2, servirMs = servirNuevo, ahoraMs = ahora)
        assertEquals(SesionCrono.inicial(ensalada, servirNuevo, ahora, 2), e)
    }
}
