package app.trombito.souschef.ui.ajustes

import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.domain.copia.ArchivoCopia
import app.trombito.souschef.domain.copia.GestorCopia
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakePreferenciasRepo
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AjustesViewModelTest {
    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun reflejaLosCambiosDelRepositorio() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = AjustesViewModel(FakeAjustesRepo(), copiasSinArchivos())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
            assertEquals(Ajustes(), vm.estado.value)

            vm.elegirDisposicion(DisposicionCrono.APILADA)
            vm.activarVoz(false)

            assertEquals(Ajustes(DisposicionCrono.APILADA, false), vm.estado.value)

            vm.elegirLista(DisposicionLista.CLASICA)
            assertEquals(DisposicionLista.CLASICA, vm.estado.value.lista)
        }

    @Test
    fun elegirTemaYHoraTarjetaSeGuardan() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = AjustesViewModel(FakeAjustesRepo(), copiasSinArchivos())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            vm.elegirTema(TemaApp.OSCURO)
            vm.elegirHoraTarjeta(HoraTarjeta.ACABAR)

            assertEquals(TemaApp.OSCURO, vm.estado.value.tema)
            assertEquals(HoraTarjeta.ACABAR, vm.estado.value.horaTarjeta)
        }

    @Test
    fun ajustarServirMueveDeQuinceEnQuinceYDaLaVueltaAMedianoche() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = AjustesViewModel(FakeAjustesRepo(), copiasSinArchivos())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
            assertEquals(15 * 60 + 30, vm.estado.value.servirMin)

            vm.ajustarServir(15)
            assertEquals(15 * 60 + 45, vm.estado.value.servirMin)

            // 15:45 - 63 pasos de 15 min (15 h 45 min) = 00:00; un paso más vuelve a 23:45.
            vm.ajustarServir(-15 * 63)
            assertEquals(0, vm.estado.value.servirMin)
            vm.ajustarServir(-15)
            assertEquals(23 * 60 + 45, vm.estado.value.servirMin)
        }

    private fun copiasSinArchivos() =
        GestorCopia(
            FakeHistorialRepo(),
            FakePreferenciasRepo(),
            object : ArchivoCopia {
                override suspend fun escribir(
                    destino: String,
                    texto: String,
                ) = true

                override suspend fun leer(origen: String): String? = null
            },
            RelojFalso(),
        )
}
