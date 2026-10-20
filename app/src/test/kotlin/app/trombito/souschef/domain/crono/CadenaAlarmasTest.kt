package app.trombito.souschef.domain.crono

import app.trombito.souschef.receta.crono.AgrupadorAlarmas
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.MotorCrono
import app.trombito.souschef.receta.crono.SesionCrono
import app.trombito.souschef.receta.crono.TipoAviso
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.FakeAlarmaSistema
import app.trombito.souschef.ui.crono.FakeCronoEstadoRepo
import app.trombito.souschef.ui.crono.FakeNotificador
import app.trombito.souschef.ui.crono.RelojFalso
import app.trombito.souschef.ui.crono.T0
import app.trombito.souschef.ui.crono.recetaCrono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private val presencia = PresenciaCrono()
    private val cadena = CadenaAlarmas(almacen, recetas, reloj, sistema, notificador, presencia)

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

    /** Cocinada "a" con el fuego arrancado en T0 (acaba a los 300 s) y la mesa a los 200 s (acaba a los 320 s). */
    private suspend fun cocinandoConDosCarriles() {
        val receta = recetaCrono("a")
        var e = SesionCrono.inicial(receta, T0 + 15 * 60_000L, T0)
        e = SesionCrono.abrir(e, receta, T0)
        e = MotorCrono.empezar(e, receta, "f0", T0)
        e = MotorCrono.empezar(e, receta, "m", T0 + 200_000L)
        almacen.fijar(e)
    }

    @Test
    fun programaSoloElEventoMasProximoDeTodasLasCocinadas() =
        runTest {
            cocinando("b", T0 + 60_000L)
            cocinando("a", T0)
            cadena.reprogramar()
            // a: el paso de 300 s acaba en T0 + 300 s; b arranca un minuto después. Ya no hay aviso de 10 s.
            assertEquals(T0 + 300_000L, sistema.programada)
            assertEquals(TipoAviso.FIN, cadena.proximoEvento()?.tipo)
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
    fun alSonarEmiteLaAlarmaLoPersisteYArmaLaSiguiente() =
        runTest {
            cocinando("a", T0)
            // Un +30 s antes del cero mueve el cero; aquí solo dejamos pasar el tiempo hasta 1 s después del cero.
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            val alarma = notificador.alarmas.single()
            assertEquals("a", alarma.slug)
            assertEquals("Receta a", alarma.titulo)
            assertTrue(alarma.sonar)
            assertEquals(listOf("F0: Sofrito: hecho"), alarma.alarma.lineas.map { it.texto })
            val guardado = Json.decodeFromString(EstadoCrono.serializer(), repo.obtener("a")!!.json)
            assertTrue(guardado.carriles.getValue("f0").aFin)
            assertNull(sistema.programada)
        }

    @Test
    fun siLaAlarmaLlegaMuyTardeLosAvisosNoSuenanYSoloSeRepinta() =
        runTest {
            cocinando("a", T0)
            reloj.ms = T0 + 300_000L + 120_000L
            cadena.procesar()
            assertTrue(notificador.alarmas.isEmpty())
            assertEquals(2, notificador.carrilesPorSlug.getValue("a").size)
        }

    @Test
    fun yaNoHayAvisoDeDiezSegundos() =
        runTest {
            cocinando("a", T0)
            reloj.ms = T0 + 291_000L
            cadena.procesar()
            assertTrue(notificador.alarmas.isEmpty())
            assertEquals(
                setOf("f0", "m"),
                notificador.carrilesPorSlug
                    .getValue("a")
                    .map { it.carril }
                    .toSet(),
            )
        }

    @Test
    fun conLaCocinaEnPantallaNoPublicaNadaNiConsumeLosAvisos() =
        runTest {
            cocinando("a", T0)
            presencia.visible = "a"
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            assertTrue(notificador.alarmas.isEmpty())
            assertTrue("a" !in notificador.cocinas)
            // Los consume el tic del ViewModel: el flag sigue sin marcar y la alarma del sistema no se rearma.
            val guardado = Json.decodeFromString(EstadoCrono.serializer(), repo.obtener("a")!!.json)
            assertFalse(guardado.carriles.getValue("f0").aFin)
            assertNull(sistema.programada)
        }

    @Test
    fun conOtraCocinaEnPantallaLaNoVisibleSiAvisa() =
        runTest {
            cocinando("a", T0)
            cocinando("b", T0)
            presencia.visible = "a"
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            assertEquals(listOf("b"), notificador.alarmas.map { it.slug })
        }

    @Test
    fun loQueCoincideSeAgrupaEnLaMismaAlarmaSinVolverASonar() =
        runTest {
            cocinandoConDosCarriles()
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            reloj.ms = T0 + 321_000L
            cadena.procesar()
            val (primera, segunda) = notificador.alarmas
            assertTrue(primera.sonar)
            assertEquals(listOf("f0"), primera.alarma.lineas.map { it.carril })
            assertFalse(segunda.sonar)
            assertEquals(listOf("f0", "m"), segunda.alarma.lineas.map { it.carril })
            assertEquals(primera.alarma.inicioMs, segunda.alarma.inicioMs)
        }

    @Test
    fun pasadaLaVentanaLaAlarmaEmpiezaDeNuevoYVuelveASonar() =
        runTest {
            cocinandoConDosCarriles()
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            // La mesa acabó a los 320 s; se procesa a los 381 s: 80 s después de la alarma anterior (> 60 s).
            reloj.ms = T0 + 301_000L + AgrupadorAlarmas.VENTANA_MS + 20_000L
            cadena.procesar()
            assertEquals(2, notificador.alarmas.size)
            val ultima = notificador.alarmas.last()
            assertTrue(ultima.sonar)
            assertEquals(listOf("m"), ultima.alarma.lineas.map { it.carril })
        }

    @Test
    fun alDarUnPasoPorHechoSuLineaSaleDeLaAlarmaYSiNoQuedaNingunaSeQuita() =
        runTest {
            cocinandoConDosCarriles()
            val receta = recetaCrono("a")
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            reloj.ms = T0 + 321_000L
            cadena.procesar()
            val antes = almacen.leer("a")!!
            val tras = MotorCrono.hecho(antes, receta, "f0", reloj.ms)
            cadena.descartarObsoletos(antes, tras)
            val queda = notificador.alarmaActual.getValue("a")
            assertFalse(queda.sonar)
            assertEquals(listOf("m"), queda.alarma.lineas.map { it.carril })
            val ultimo = MotorCrono.hecho(tras, receta, "m", reloj.ms)
            cadena.descartarObsoletos(tras, ultimo)
            assertNull(notificador.alarmaActual["a"])
            assertEquals(listOf("a"), notificador.alarmasQuitadas)
        }

    @Test
    fun trasMorirElProcesoUnMandoQuitaLaAlarmaQueSigueSonando() =
        runTest {
            cocinandoConDosCarriles()
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            // Proceso nuevo: la memoria de alarmas publicadas está vacía, pero la de la bandeja sigue ahí.
            val nueva = CadenaAlarmas(almacen, recetas, reloj, sistema, notificador, presencia)
            val antes = almacen.leer("a")!!
            val tras = MotorCrono.hecho(antes, recetaCrono("a"), "f0", reloj.ms)
            nueva.descartarObsoletos(antes, tras)
            assertNull(notificador.alarmaActual["a"])
            assertEquals(listOf("a"), notificador.alarmasQuitadas)
        }

    @Test
    fun sinCarrilesAvanzadosNoQuitaNingunaAlarma() =
        runTest {
            cocinandoConDosCarriles()
            val estado = almacen.leer("a")!!
            cadena.descartarObsoletos(estado, estado)
            assertTrue(notificador.alarmasQuitadas.isEmpty())
        }

    @Test
    fun siguienteDaHechoAlVencidoQuitaSuLineaYRearmaSinBloquearse() =
        runTest {
            cocinandoConDosCarriles()
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            assertEquals("f0", cadena.siguiente("a"))
            assertNull(notificador.alarmaActual["a"])
            assertEquals(T0 + 320_000L, sistema.programada)
        }

    @Test
    fun losMetodosPublicosConcurrentesNoSeBloquean() =
        runBlocking {
            cocinandoConDosCarriles()
            reloj.ms = T0 + 301_000L
            val trabajos =
                List(20) { i ->
                    launch(Dispatchers.Default) {
                        when (i % 4) {
                            0 -> cadena.procesar()
                            1 -> cadena.silenciar("a")
                            2 -> cadena.repintar()
                            else -> cadena.reprogramar()
                        }
                    }
                }
            withTimeout(10_000) { trabajos.joinAll() }
        }

    @Test
    fun silenciarQuitaLaAlarmaYLaSiguienteSuenaDeNuevo() =
        runTest {
            cocinandoConDosCarriles()
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            cadena.silenciar("a")
            assertEquals(listOf("a"), notificador.alarmasQuitadas)
            assertNull(notificador.alarmaActual["a"])
            reloj.ms = T0 + 321_000L
            cadena.procesar()
            assertTrue(notificador.alarmas.last().sonar)
            assertEquals(
                listOf("m"),
                notificador.alarmas
                    .last()
                    .alarma.lineas
                    .map { it.carril },
            )
        }

    @Test
    fun conUnPasoVencidoLaCocinaPideHechoYLlevaElFinMasProximo() =
        runTest {
            cocinandoConDosCarriles()
            reloj.ms = T0 + 100_000L
            cadena.repintar()
            val corriendo = notificador.cocinas.getValue("a")
            assertNull(corriendo.urgente)
            assertEquals(T0 + 300_000L, corriendo.finProximoMs)
            assertTrue(corriendo.llegadaMs != null)
            reloj.ms = T0 + 310_000L
            cadena.repintar()
            val vencido = notificador.cocinas.getValue("a")
            assertEquals("f0", vencido.urgente)
            assertEquals(T0 + 320_000L, vencido.finProximoMs)
            assertEquals("Receta a", vencido.tituloReceta)
        }

    @Test
    fun alVerLaCocinaSeQuitanLasNotificacionesYAlSalirReapareceLaFija() =
        runTest {
            cocinandoConDosCarriles()
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            assertTrue("a" in notificador.cocinas)
            presencia.visible = "a"
            cadena.silenciar("a")
            assertTrue("a" !in notificador.cocinas)
            assertNull(notificador.alarmaActual["a"])
            presencia.visible = null
            cadena.reprogramar()
            assertTrue("a" in notificador.cocinas)
        }

    @Test
    fun conLaCocinaVisibleRepintarNoPublicaLaFija() =
        runTest {
            cocinando("a", T0)
            presencia.visible = "a"
            cadena.repintar()
            assertTrue(notificador.cocinas.isEmpty())
            assertEquals(listOf("a"), notificador.cocinasQuitadas)
        }

    @Test
    fun repintarPintaLaCocinaFijaConTodosLosCarrilesYLaQuitaAlCerrar() =
        runTest {
            cocinando("a", T0)
            cadena.repintar()
            assertEquals(2, notificador.carrilesPorSlug.getValue("a").size)
            assertNull(notificador.cocinas.getValue("a").urgente)
            cadena.cerrado("a")
            assertTrue(notificador.carrilesPorSlug.isEmpty())
        }

    @Test
    fun repintarSoloPintaLasCocinadasVivas() =
        runTest {
            cocinando("a", T0)
            almacen.fijar(SesionCrono.inicial(recetaCrono("b"), T0, T0))
            cadena.repintar()
            assertEquals(setOf("a"), notificador.carrilesPorSlug.keys)
        }

    @Test
    fun repintarQuitaLaCocinaDeUnaCocinadaCaducada() =
        runTest {
            cocinando("a", T0)
            cadena.repintar()
            reloj.ms = T0 + SesionCrono.CADUCIDAD_MS + 1
            cadena.repintar()
            assertTrue(notificador.carrilesPorSlug.isEmpty())
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
    fun hayVivaSoloConUnaCocinadaArrancada() =
        runTest {
            assertFalse(cadena.hayViva())
            almacen.fijar(SesionCrono.inicial(recetaCrono("b"), T0, T0))
            assertFalse(cadena.hayViva())
            cocinando("a", T0)
            assertTrue(cadena.hayViva())
        }

    @Test
    fun abandonarBorraElEstadoLimpiaYRearma() =
        runTest {
            cocinando("a", T0)
            cadena.reprogramar()
            cadena.abandonar("a")
            assertNull(repo.obtener("a"))
            assertEquals(listOf("a"), notificador.limpiados)
            assertTrue(notificador.carrilesPorSlug.isEmpty())
            assertNull(sistema.programada)
        }
}
