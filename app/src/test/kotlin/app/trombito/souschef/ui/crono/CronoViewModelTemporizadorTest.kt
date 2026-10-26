package app.trombito.souschef.ui.crono

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.crono.AlmacenCrono
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.TemporizadoresLibres
import app.trombito.souschef.receta.crono.TipoAviso
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.FakeTecnicasRepo
import app.trombito.souschef.ui.Rutas
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Temporizador libre en la cocina (ADR-0016): «Temporizador» y la voz llegan aquí. */
@OptIn(ExperimentalCoroutinesApi::class)
class CronoViewModelTemporizadorTest {
    private val creados = mutableListOf<CronoViewModel>()
    private val repo = FakeCronoEstadoRepo()
    private val reloj = RelojFalso()
    private val sistema = FakeAlarmaSistema()
    private val avisador = FakeAvisador()
    private val recetas = FakeRecetasRepo(listOf(recetaCrono()))
    private val notificador = FakeNotificador()
    private val cadena = CadenaAlarmas(AlmacenCrono(repo), recetas, reloj, sistema, notificador)

    @Before
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun TestScope.crear(): CronoViewModel {
        val args = mapOf(Rutas.ARG_SLUG to "r", Rutas.ARG_RACIONES to 0, Rutas.ARG_SERVIR to "12:15")
        return CronoViewModel(
            SavedStateHandle(args),
            recetas,
            FakeTecnicasRepo(),
            FakeHistorialRepo(),
            cadena,
            reloj,
            avisador,
            FakeAjustesRepo(),
            FakeEscucha(),
            FakeHabla(),
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

    private fun TestScope.asentar() {
        runCurrent()
        runCurrent()
    }

    @Test
    fun elTemporizadorLibreSePersisteYArmaLaAlarmaSinTocarLosCarriles() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            val antes = guardado().carriles
            assertTrue(vm.temporizador(5))
            asentar()
            val e = guardado()
            assertEquals(listOf(5), e.libres.map { it.minutos })
            assertEquals(T0, e.libres.single().inicioMs)
            assertEquals(antes, e.carriles)
            assertEquals(T0 + 300_000L, sistema.programada)
            assertEquals(e, vm.ui.value.estado)
        }

    @Test
    fun noAceptaDuracionesFueraDeRangoNiMasDeTres() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            assertFalse(vm.temporizador(0))
            assertFalse(vm.temporizador(181))
            repeat(3) {
                assertTrue(vm.temporizador(5))
                asentar()
            }
            assertFalse(vm.temporizador(5))
            asentar()
            assertEquals(3, guardado().libres.size)
        }

    @Test
    fun sinAbrirLaCocinaNoSePuedePonerUnTemporizador() =
        prueba {
            val vm = crear()
            assertFalse(vm.temporizador(5))
            asentar()
            assertTrue(guardado().libres.isEmpty())
        }

    @Test
    fun quitarElTemporizadorLoBorraYRearmaLaCadena() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.temporizador(5)
            asentar()
            vm.quitarTemporizador(guardado().libres.single().id)
            asentar()
            assertTrue(guardado().libres.isEmpty())
            assertNull(sistema.programada)
        }

    @Test
    fun enPrimerPlanoElTicAvisaDelTemporizadorYLoMarca() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.alVer()
            runCurrent()
            vm.temporizador(1)
            asentar()
            reloj.ms = T0 + 61_000L
            siguienteTic()
            assertEquals(listOf(TipoAviso.TEMPORIZADOR), avisador.sonados)
            assertTrue(guardado().libres.single().avisado)
            assertTrue(notificador.alarmas.isEmpty())
        }

    @Test
    fun fueraDeLaPantallaLoAvisaLaCadenaConLaAlarmaDelSistema() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.temporizador(1)
            asentar()
            vm.enSegundoPlano()
            asentar()
            reloj.ms = T0 + 61_000L
            siguienteTic()
            assertTrue(avisador.sonados.isEmpty())
            cadena.procesar()
            assertEquals(
                listOf("Temporizador: 1 min"),
                notificador.alarmaActual
                    .getValue("r")
                    .alarma.lineas
                    .map { it.texto },
            )
        }

    @Test
    fun unLibreNoEntraEnElResumenYMantieneVivaLaCocinadaCerrada() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.temporizador(5)
            asentar()
            listOf("f0", "m", "f0").forEach {
                vm.hecho(it)
                runCurrent()
                siguienteTic()
            }
            val resumen = checkNotNull(vm.ui.value.resumen)
            assertTrue(resumen.pasosReales.all { it.id in setOf("p1", "p2", "p3") })
            assertEquals(1, TemporizadoresLibres.pendientes(checkNotNull(vm.ui.value.estado)).size)
            assertTrue(cadena.vivas.hayViva())
        }

    @Test
    fun abandonarQuitaLosLibres() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.temporizador(5)
            asentar()
            vm.abandonar()
            asentar()
            assertNull(repo.obtener("r"))
            assertFalse(cadena.vivas.hayViva())
        }
}
