package app.trombito.souschef.ui.recetas

import androidx.lifecycle.SavedStateHandle
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakePreferenciasRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.crono.RelojFalso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecetasViewModelTiempoTest {
    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun viewModel(ajustes: FakeAjustesRepo = FakeAjustesRepo()) =
        RecetasViewModel(
            SavedStateHandle(),
            FakeRecetasRepo(listOf(Receta(slug = "a", titulo = "A", totalMin = 30.0))),
            FakePreferenciasRepo(),
            FakeHistorialRepo(),
            ajustes,
            RelojFalso(),
        )

    @Test
    fun activarTomaLaHoraDeServirDeAjustesYSalirLoApaga() =
        runTest(UnconfinedTestDispatcher()) {
            val ajustes = FakeAjustesRepo()
            ajustes.actual.value = ajustes.actual.value.copy(servirMin = 14 * 60)
            val vm = viewModel(ajustes)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
            assertNull(vm.estado.value.tiempo)

            vm.activarTiempo()
            assertEquals(ModoTiempo(14 * 60), vm.estado.value.tiempo)
            assertEquals(
                1,
                vm.estado.value.tarjetas
                    .count { it.llegada != null },
            )

            vm.salirTiempo()
            assertNull(vm.estado.value.tiempo)
            assertNull(
                vm.estado.value.tarjetas
                    .single()
                    .llegada,
            )
        }

    @Test
    fun moverCambiaLaHoraDeQuinceEnQuinceYSoloLlegoSeEnciende() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = viewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
            vm.moverServir(15)
            assertNull(vm.estado.value.tiempo)

            vm.activarTiempo()
            val inicial =
                vm.estado.value.tiempo!!
                    .servirMin
            vm.moverServir(15)
            vm.moverServir(15)
            vm.moverServir(-15)
            vm.soloLlego(true)

            assertEquals(ModoTiempo(inicial + 15, soloLlego = true), vm.estado.value.tiempo)
        }
}
