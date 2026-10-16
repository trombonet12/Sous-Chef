package app.souschef.domain.crono

import app.souschef.receta.crono.EstadoCrono
import app.souschef.receta.crono.MotorCrono
import app.souschef.receta.crono.SesionCrono
import app.souschef.receta.crono.TipoAviso
import app.souschef.ui.FakeRecetasRepo
import app.souschef.ui.crono.FakeAlarmaSistema
import app.souschef.ui.crono.FakeCronoEstadoRepo
import app.souschef.ui.crono.FakeNotificador
import app.souschef.ui.crono.RelojFalso
import app.souschef.ui.crono.T0
import app.souschef.ui.crono.recetaCrono
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CadenaAlarmasTest {
    private val repo = FakeCronoEstadoRepo()
    private val reloj = RelojFalso()
    private val sistema = FakeAlarmaSistema()
    private val notificador = FakeNotificador()
    private val almacen = AlmacenCrono(repo)
    private val recetas = FakeRecetasRepo(listOf(recetaCrono("a"), recetaCrono("b")))
    private val cadena = CadenaAlarmas(almacen, recetas, reloj, sistema, notificador)

    /** Cocinada abierta en T0 con el fuego f0 arrancado en [arranqueMs]. */
    private suspend fun cocinando(
        slug: String,
        arranqueMs: Long,
    ) {
        val receta = recetaCrono(slug)
        var e = SesionCrono.inicial(receta, T0 + 15 * 60_000L, T0)
        e = SesionCrono.abrir(e, receta, T0)
        e = MotorCrono.empezar(e, receta, "f0", arranqueMs)
        almacen.fijar(e)
    }

    @Test
    fun programaSoloElEventoMasProximoDeTodasLasCocinadas() =
        runTest {
            cocinando("b", T0 + 60_000L)
            cocinando("a", T0)
            cadena.reprogramar()
            // a: quedan 10 s del paso de 300 s -> T0 + 290 s; b arranca un minuto después.
            assertEquals(T0 + 290_000L, sistema.programada)
            assertEquals(TipoAviso.DIEZ_S, cadena.proximoEvento()?.tipo)
        }

    @Test
    fun sinCocinadasAbiertasCancelaLaAlarma() =
        runTest {
            almacen.fijar(SesionCrono.inicial(recetaCrono("a"), T0, T0))
            sistema.programada = 123L
            cadena.reprogramar()
            assertNull(sistema.programada)
            assertEquals(1, sistema.cancelaciones)
        }

    @Test
    fun ignoraLasCocinadasCaducadas() =
        runTest {
            cocinando("a", T0)
            reloj.ms = T0 + SesionCrono.CADUCIDAD_MS + 1
            cadena.reprogramar()
            assertNull(sistema.programada)
        }

    @Test
    fun alSonarEmiteLoVencidoLoPersisteYArmaLaSiguiente() =
        runTest {
            cocinando("a", T0)
            // Un +30 s antes del cero mueve el cero; aquí solo dejamos pasar el tiempo hasta 1 s después del cero.
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            assertEquals(listOf(TipoAviso.FIN), notificador.publicados.map { it.aviso.tipo })
            assertEquals("a", notificador.publicados.single().slug)
            val guardado = Json.decodeFromString(EstadoCrono.serializer(), repo.obtener("a")!!.json)
            assertTrue(guardado.carriles.getValue("f0").aFin)
            assertNull(sistema.programada)
        }

    @Test
    fun siLaAlarmaLlegaMuyTardeLosAvisosSalenSilenciosos() =
        runTest {
            cocinando("a", T0)
            reloj.ms = T0 + 300_000L + 120_000L
            cadena.procesar()
            assertTrue(notificador.publicados.isEmpty())
            assertEquals(2, notificador.carrilesPorSlug.getValue("a").size)
        }

    @Test
    fun elAvisoDeDiezSegundosSoloRepintaElCarrilSinSonar() =
        runTest {
            cocinando("a", T0)
            reloj.ms = T0 + 291_000L
            cadena.procesar()
            assertTrue(notificador.publicados.isEmpty())
            assertEquals(
                setOf("f0", "m"),
                notificador.carrilesPorSlug
                    .getValue("a")
                    .map { it.carril }
                    .toSet(),
            )
        }

    @Test
    fun repintarPintaUnaNotificacionPorCarrilYLaQuitaAlCerrar() =
        runTest {
            cocinando("a", T0)
            cadena.repintar()
            assertEquals(2, notificador.carrilesPorSlug.getValue("a").size)
            cadena.cerrado("a")
            assertTrue(notificador.carrilesPorSlug.isEmpty())
        }

    @Test
    fun repintarSoloPintaLasCocinadasVivas() =
        runTest {
            cocinando("a", T0)
            almacen.fijar(SesionCrono.inicial(recetaCrono("b"), T0, T0))
            cadena.repintar()
            assertEquals(setOf("a"), notificador.enMarchaPorSlug.keys)
        }

    @Test
    fun repintarQuitaLaOngoingDeUnaCocinadaCaducada() =
        runTest {
            cocinando("a", T0)
            cadena.repintar()
            reloj.ms = T0 + SesionCrono.CADUCIDAD_MS + 1
            cadena.repintar()
            assertTrue(notificador.enMarchaPorSlug.isEmpty())
        }

    @Test
    fun otraVivaIgnoraLaPropiaYLasSinArrancar() =
        runTest {
            cocinando("a", T0)
            almacen.fijar(SesionCrono.inicial(recetaCrono("b"), T0, T0))
            assertNull(cadena.otraViva("a"))
            assertEquals("a", cadena.otraViva("b")?.slug)
        }

    @Test
    fun abandonarBorraElEstadoLimpiaYRearma() =
        runTest {
            cocinando("a", T0)
            cadena.reprogramar()
            cadena.abandonar("a")
            assertNull(repo.obtener("a"))
            assertEquals(listOf("a"), notificador.limpiados)
            assertTrue(notificador.enMarchaPorSlug.isEmpty())
            assertNull(sistema.programada)
        }
}
