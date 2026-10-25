package app.trombito.souschef.receta.crono

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Regla única de «cocinada viva»: abierta, sin caducar y con algún carril sin terminar. */
class SesionCronoVivoTest {
    private val t0 = 1_700_000_000_000L

    private fun estado(
        inicioMs: Long? = t0,
        actualizadoMs: Long = t0,
        carriles: Map<String, EstadoCarril> = mapOf("A" to EstadoCarril(), "B" to EstadoCarril(fin = true)),
    ) = EstadoCrono(
        slug = "x",
        raciones = 2,
        servirMs = t0,
        carriles = carriles,
        inicioMs = inicioMs,
        actualizadoMs = actualizadoMs,
    )

    @Test
    fun abiertaYReciente() {
        assertTrue(SesionCrono.vivo(estado(), t0))
    }

    @Test
    fun sinArrancarNoEstaViva() {
        assertFalse(SesionCrono.vivo(estado(inicioMs = null), t0))
    }

    @Test
    fun enElLimiteExactoDeCaducidadSigueViva() {
        assertTrue(SesionCrono.vivo(estado(), t0 + SesionCrono.CADUCIDAD_MS))
    }

    @Test
    fun unMilisegundoDespuesDelLimiteHaCaducado() {
        assertFalse(SesionCrono.vivo(estado(), t0 + SesionCrono.CADUCIDAD_MS + 1))
    }

    @Test
    fun laCaducidadSeMideDesdeLaUltimaActualizacionNoDesdeElInicio() {
        val e = estado(actualizadoMs = t0 + SesionCrono.CADUCIDAD_MS)
        assertTrue(SesionCrono.vivo(e, t0 + 2 * SesionCrono.CADUCIDAD_MS))
        assertFalse(SesionCrono.vivo(e, t0 + 2 * SesionCrono.CADUCIDAD_MS + 1))
    }

    @Test
    fun conTodosLosCarrilesTerminadosNoEstaViva() {
        val e = estado(carriles = mapOf("A" to EstadoCarril(fin = true), "B" to EstadoCarril(fin = true)))
        assertFalse(SesionCrono.vivo(e, t0))
    }

    @Test
    fun conUnSoloCarrilPendienteSigueViva() {
        val e = estado(carriles = mapOf("A" to EstadoCarril(fin = true), "B" to EstadoCarril(i = 1)))
        assertTrue(SesionCrono.vivo(e, t0))
    }

    @Test
    fun laPausaNoLaMata() {
        val pausada = EstadoCarril(pausado = true, restanteS = 30)
        assertTrue(SesionCrono.vivo(estado(carriles = mapOf("A" to pausada)), t0))
    }

    @Test
    fun corriendoYVencidaSigueViva() {
        val vencida = EstadoCarril(finAtMs = t0 - 1_000L)
        assertTrue(SesionCrono.vivo(estado(carriles = mapOf("A" to vencida)), t0))
    }
}
