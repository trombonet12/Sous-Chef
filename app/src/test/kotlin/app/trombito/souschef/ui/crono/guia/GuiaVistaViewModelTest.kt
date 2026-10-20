package app.trombito.souschef.ui.crono.guia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.crono.AlmacenCrono
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.FakeTecnicasRepo
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.crono.CronoViewModel
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.crono.FakeAlarmaSistema
import app.trombito.souschef.ui.crono.FakeAvisador
import app.trombito.souschef.ui.crono.FakeCronoEstadoRepo
import app.trombito.souschef.ui.crono.FakeEscucha
import app.trombito.souschef.ui.crono.FakeHabla
import app.trombito.souschef.ui.crono.FakeNotificador
import app.trombito.souschef.ui.crono.RelojFalso
import app.trombito.souschef.ui.crono.recetaCrono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** El ViewModel del cronómetro expone la guía vista de Ajustes y la marca al acabarla o saltarla. */
@OptIn(ExperimentalCoroutinesApi::class)
class GuiaVistaViewModelTest {
    private val ajustes = FakeAjustesRepo()
    private val recetas = FakeRecetasRepo(listOf(recetaCrono()))
    private val reloj = RelojFalso()
    private val cadena =
        CadenaAlarmas(
            AlmacenCrono(FakeCronoEstadoRepo()),
            recetas,
            reloj,
            FakeAlarmaSistema(),
            FakeNotificador(),
        )

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
            FakeAvisador(),
            ajustes,
            FakeEscucha(),
            FakeHabla(),
        ).also { runCurrent() }
    }

    private fun prueba(bloque: suspend TestScope.(CronoViewModel) -> Unit) =
        runTest {
            val vm = crear()
            try {
                bloque(vm)
            } finally {
                vm.viewModelScope.cancel()
            }
        }

    @Test
    fun `con la guia sin ver la cocina la muestra`() =
        prueba { vm ->
            assertFalse(vm.ui.value.guiaVista)
        }

    @Test
    fun `con la guia vista no la muestra`() =
        prueba { vm ->
            ajustes.actual.value = ajustes.actual.value.copy(guiaCronoVista = true)
            runCurrent()
            assertTrue(vm.ui.value.guiaVista)
        }

    @Test
    fun `acabar o saltar la guia la marca vista en Ajustes`() =
        prueba { vm ->
            vm.marcarGuiaVista()
            runCurrent()

            assertTrue(vm.ui.value.guiaVista)
            assertEquals(true, ajustes.actual.value.guiaCronoVista)
        }
}
