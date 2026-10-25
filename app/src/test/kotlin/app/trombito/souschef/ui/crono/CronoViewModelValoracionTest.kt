package app.trombito.souschef.ui.crono

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.Valoracion
import app.trombito.souschef.domain.crono.AlmacenCrono
import app.trombito.souschef.domain.crono.CadenaAlarmas
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * F4: la valoración del cierre llega a la entrada que se inserta al confirmar (también si se elige durante la ventana
 * de deshacer) y, ya confirmada, se actualiza con `valorar` sin volver a insertar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CronoViewModelValoracionTest {
    private val historial = FakeHistorialRepo()
    private val reloj = RelojFalso()
    private val ajustes = FakeAjustesRepo()
    private val recetas = FakeRecetasRepo(listOf(recetaCrono()))
    private val cadena =
        CadenaAlarmas(AlmacenCrono(FakeCronoEstadoRepo()), recetas, reloj, FakeAlarmaSistema(), FakeNotificador())
    private val creados = mutableListOf<CronoViewModel>()

    @Before
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    /** El bucle de tic es infinito: hay que parar cada ViewModel o `runTest` no termina. */
    private fun prueba(bloque: suspend TestScope.() -> Unit) =
        runTest {
            try {
                bloque()
            } finally {
                creados.forEach { it.viewModelScope.cancel() }
            }
        }

    /** Un ViewModel con la cocina ya terminada: listo para cerrar. */
    private fun TestScope.cocinaTerminada(): CronoViewModel {
        val args = mapOf(Rutas.ARG_SLUG to "r", Rutas.ARG_RACIONES to 0, Rutas.ARG_SERVIR to "12:15")
        val vm =
            CronoViewModel(
                SavedStateHandle(args),
                recetas,
                FakeTecnicasRepo(),
                historial,
                cadena,
                reloj,
                FakeAvisador(),
                ajustes,
                FakeEscucha(),
                FakeHabla(),
            )
        creados += vm
        runCurrent()
        vm.abrirCocina()
        runCurrent()
        listOf("m", "f0", "f0").forEach {
            vm.hecho(it)
            runCurrent()
            advanceTimeBy(1_001)
            runCurrent()
        }
        return vm
    }

    private fun guardada() = historial.datos.value.single()

    private fun TestScope.vencerLaVentana() {
        advanceTimeBy(VENTANA_DESHACER_MS + 100)
        runCurrent()
    }

    @Test
    fun laValoracionElegidaAntesDeCerrarYElPrevistoLlegaALaEntrada() =
        prueba {
            val vm = cocinaTerminada()
            vm.valorar(Valoracion.REPETIR)
            vm.cerrar("")
            runCurrent()
            vencerLaVentana()
            val entrada = guardada()
            assertEquals(Valoracion.REPETIR, entrada.valoracion)
            assertEquals(15, entrada.previstoMin)
        }

    @Test
    fun laValoracionElegidaDuranteLaVentanaDeDeshacerLlegaALaEntradaInsertada() =
        prueba {
            val vm = cocinaTerminada()
            vm.cerrar("")
            runCurrent()
            vm.valorar(Valoracion.AJUSTAR)
            assertTrue(historial.datos.value.isEmpty())
            vencerLaVentana()
            assertEquals(Valoracion.AJUSTAR, guardada().valoracion)
        }

    @Test
    fun laUltimaValoracionDeLaVentanaGanaYGuardarYaTambienLaLleva() =
        prueba {
            val vm = cocinaTerminada()
            vm.valorar(Valoracion.REPETIR)
            vm.cerrar("")
            runCurrent()
            vm.valorar(Valoracion.NO_REPETIR)
            vm.confirmarCierre()
            runCurrent()
            assertEquals(Valoracion.NO_REPETIR, guardada().valoracion)
        }

    @Test
    fun quitarLaValoracionEnLaVentanaDejaLaEntradaSinValorar() =
        prueba {
            val vm = cocinaTerminada()
            vm.valorar(Valoracion.REPETIR)
            vm.cerrar("")
            runCurrent()
            vm.valorar(null)
            vencerLaVentana()
            assertNull(guardada().valoracion)
        }

    @Test
    fun valorarDespuesDeConfirmarUsaValorarYNoVuelveAInsertar() =
        prueba {
            val vm = cocinaTerminada()
            vm.cerrar("")
            runCurrent()
            vencerLaVentana()
            assertNull(guardada().valoracion)

            vm.valorar(Valoracion.AJUSTAR)
            runCurrent()

            assertEquals(1, historial.datos.value.size)
            assertEquals(Valoracion.AJUSTAR, guardada().valoracion)
        }

    @Test
    fun deshacerElCierreConservaLaValoracionYNoGuardaNada() =
        prueba {
            val vm = cocinaTerminada()
            vm.cerrar("")
            runCurrent()
            vm.valorar(Valoracion.AJUSTAR)
            vm.deshacerCierre()
            vencerLaVentana()
            assertEquals(Valoracion.AJUSTAR, vm.ui.value.valoracion)
            assertTrue(historial.datos.value.isEmpty())
        }
}
