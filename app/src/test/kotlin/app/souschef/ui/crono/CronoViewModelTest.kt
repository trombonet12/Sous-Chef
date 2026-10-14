package app.souschef.ui.crono

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.souschef.domain.crono.AlmacenCrono
import app.souschef.domain.crono.CadenaAlarmas
import app.souschef.receta.crono.EstadoCrono
import app.souschef.receta.crono.TipoAviso
import app.souschef.ui.FakeHistorialRepo
import app.souschef.ui.FakeRecetasRepo
import app.souschef.ui.FakeTecnicasRepo
import app.souschef.ui.Rutas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CronoViewModelTest {
    private val creados = mutableListOf<CronoViewModel>()
    private val repo = FakeCronoEstadoRepo()
    private val historial = FakeHistorialRepo()
    private val reloj = RelojFalso()
    private val sistema = FakeAlarmaSistema()
    private val avisador = FakeAvisador()
    private val almacen = AlmacenCrono(repo)
    private val recetas = FakeRecetasRepo(listOf(recetaCrono()))
    private val notificador = FakeNotificador()
    private val cadena = CadenaAlarmas(almacen, recetas, reloj, sistema, notificador)

    @Before
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun TestScope.crear(
        raciones: Int = 0,
        servir: String = "12:15",
    ): CronoViewModel {
        val args = mapOf(Rutas.ARG_SLUG to "r", Rutas.ARG_RACIONES to raciones, Rutas.ARG_SERVIR to servir)
        return CronoViewModel(
            SavedStateHandle(args),
            recetas,
            FakeTecnicasRepo(),
            historial,
            cadena,
            reloj,
            avisador,
        ).also {
            creados += it
            runCurrent()
        }
    }

    /** El bucle de tic es infinito: hay que parar cada ViewModel o `runTest` no termina. */
    private fun prueba(bloque: suspend TestScope.() -> Unit) =
        runTest {
            try {
                bloque()
            } finally {
                creados.forEach { it.viewModelScope.cancel() }
            }
        }

    private suspend fun guardado(): EstadoCrono =
        Json.decodeFromString(EstadoCrono.serializer(), checkNotNull(repo.obtener("r")).json)

    private fun TestScope.siguienteTic() {
        advanceTimeBy(1_001)
        runCurrent()
    }

    @Test
    fun alEntrarCreaYPersisteLaPortadaConLasRacionesYLaHoraDeServir() =
        prueba {
            val vm = crear(raciones = 4)
            val e = guardado()
            assertNull(e.inicioMs)
            assertEquals(4, e.raciones)
            assertEquals(T0 + 15 * 60_000L, e.servirMs)
            assertEquals(e, vm.ui.value.estado)
            assertNull(sistema.programada)
        }

    @Test
    fun abrirLaCocinaFijaElInicioYLosMandosSePersistenYArmanLaAlarma() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            assertEquals(T0, guardado().inicioMs)

            vm.alternar("f0")
            runCurrent()
            val f0 = guardado().carriles.getValue("f0")
            assertEquals(T0 + 300_000L, f0.finAtMs)
            // Próximo evento: el aviso de "quedan 10 s".
            assertEquals(T0 + 290_000L, sistema.programada)

            vm.masTreinta("f0")
            runCurrent()
            assertEquals(T0 + 330_000L, guardado().carriles.getValue("f0").finAtMs)
            assertEquals(T0 + 320_000L, sistema.programada)
        }

    @Test
    fun elDobleToqueDeHechoSoloCuentaUnaVezHastaElSiguienteTic() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.hecho("f0")
            vm.hecho("f0")
            runCurrent()
            assertEquals(1, guardado().carriles.getValue("f0").i)
            siguienteTic()
            vm.hecho("f0")
            runCurrent()
            assertTrue(guardado().carriles.getValue("f0").fin)
        }

    @Test
    fun enPrimerPlanoElTicSuenaElFinYLoMarca() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.alternar("f0")
            runCurrent()
            reloj.ms = T0 + 301_000L
            siguienteTic()
            assertEquals(listOf(TipoAviso.FIN), avisador.sonados)
            assertTrue(guardado().carriles.getValue("f0").aFin)
            assertEquals(T0 + 301_000L, vm.ui.value.ahoraMs)
        }

    @Test
    fun alTerminarTodoElCierreEscribeElHistorialYBorraElEstado() =
        prueba {
            val vm = crear(raciones = 3)
            vm.abrirCocina()
            runCurrent()
            listOf("f0", "m").forEach {
                vm.hecho(it)
                runCurrent()
                siguienteTic()
            }
            vm.hecho("f0")
            runCurrent()
            assertNotNull(vm.ui.value.resumen)
            assertEquals(1, avisador.cierres)

            vm.cerrar("  menos sal  ")
            runCurrent()
            assertTrue(vm.ui.value.cierrePendiente)
            assertTrue(historial.datos.value.isEmpty())
            assertNotNull(repo.obtener("r"))
            assertEquals(listOf("r"), notificador.limpiados)
            advanceTimeBy(VENTANA_DESHACER_MS + 100)
            runCurrent()
            val entrada = historial.datos.value.single()
            assertEquals("r", entrada.slug)
            assertEquals("Receta r", entrada.titulo)
            assertEquals(3, entrada.raciones)
            assertEquals("menos sal", entrada.nota)
            assertEquals(setOf("p1", "p2", "p3"), entrada.pasosReales.map { it.id }.toSet())
            assertTrue(entrada.id.startsWith("r-2026-10-14-"))
            assertEquals("2026-10-14", entrada.fecha)
            assertNull(repo.obtener("r"))
            assertTrue(vm.ui.value.salida)
            assertNull(sistema.programada)
        }

    @Test
    fun deshacerElCierreNoEscribeNadaYVuelveAlResumen() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            listOf("m", "f0", "f0").forEach {
                vm.hecho(it)
                runCurrent()
                siguienteTic()
            }
            vm.cerrar("")
            runCurrent()
            vm.deshacerCierre()
            advanceTimeBy(VENTANA_DESHACER_MS + 100)
            runCurrent()
            assertFalse(vm.ui.value.cierrePendiente)
            assertFalse(vm.ui.value.salida)
            assertTrue(historial.datos.value.isEmpty())
            assertNotNull(repo.obtener("r"))
            assertNotNull(vm.ui.value.resumen)
        }

    @Test
    fun salirDuranteLaVentanaConfirmaElCierreAlMomento() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            listOf("m", "f0", "f0").forEach {
                vm.hecho(it)
                runCurrent()
                siguienteTic()
            }
            vm.cerrar("")
            runCurrent()
            vm.enSegundoPlano()
            runCurrent()
            assertEquals(1, historial.datos.value.size)
            assertNull(repo.obtener("r"))
        }

    @Test
    fun hechoCancelaLosAvisosDelCarrilYLaOngoingSeRepinta() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            assertEquals("Receta r", notificador.enMarchaPorSlug.getValue("r").titulo)
            vm.hecho("f0")
            runCurrent()
            assertEquals(listOf("r" to setOf("f0")), notificador.canceladas)
        }

    @Test
    fun reiniciarNecesitaDosToquesYVuelveALaPortada() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.hecho("m")
            runCurrent()
            vm.reiniciar()
            assertTrue(vm.ui.value.reiniciarArmado)
            assertTrue(guardado().carriles.getValue("m").fin)

            vm.reiniciar()
            runCurrent()
            assertFalse(vm.ui.value.reiniciarArmado)
            val e = guardado()
            assertNull(e.inicioMs)
            assertFalse(e.carriles.getValue("m").fin)
        }

    @Test
    fun unaCocinadaEnMarchaSeReanudaSinPisarlaConLaHoraDeLaFicha() =
        prueba {
            val primero = crear()
            primero.abrirCocina()
            runCurrent()
            val vm = crear(servir = "18:00")
            assertEquals(
                T0 + 15 * 60_000L,
                vm.ui.value.estado
                    ?.servirMs,
            )
            assertEquals(
                T0,
                vm.ui.value.estado
                    ?.inicioMs,
            )
        }

    @Test
    fun laHoraDeServirEsDeHoyEnLaZonaLocal() {
        val ms = servirDeHoy("15:30", T0, ZoneId.systemDefault())
        assertEquals(3 * 60 * 60_000L + 30 * 60_000L, ms - T0)
    }
}
