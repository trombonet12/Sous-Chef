package app.trombito.souschef.ui.recetas

import androidx.lifecycle.SavedStateHandle
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.ui.CLAVE_ATAJO_TIEMPO
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakePreferenciasRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** El atajo «¿Qué me da tiempo?» (F17) llega como una marca de una sola vez en el estado de la entrada Recetas. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecetasViewModelAtajoTest {
    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun viewModel(estado: SavedStateHandle) =
        RecetasViewModel(
            estado,
            FakeRecetasRepo(emptyList()),
            FakePreferenciasRepo(),
            FakeHistorialRepo(),
            FakeAjustesRepo(),
            Reloj { 0L },
        )

    @Test
    fun `la marca del atajo enciende el modo y se gasta`() =
        runTest(UnconfinedTestDispatcher()) {
            val estado = SavedStateHandle()
            val vm = viewModel(estado)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
            assertNull(vm.estado.value.tiempo)

            estado[CLAVE_ATAJO_TIEMPO] = true

            assertNotNull(vm.estado.value.tiempo)
            assertEquals(false, estado.get<Boolean>(CLAVE_ATAJO_TIEMPO))
        }

    @Test
    fun `sin marca el modo no se enciende y salir del modo no lo vuelve a encender`() =
        runTest(UnconfinedTestDispatcher()) {
            val estado = SavedStateHandle()
            val vm = viewModel(estado)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
            assertNull(vm.estado.value.tiempo)

            estado[CLAVE_ATAJO_TIEMPO] = true
            vm.salirTiempo()

            assertNull(vm.estado.value.tiempo)
        }
}
