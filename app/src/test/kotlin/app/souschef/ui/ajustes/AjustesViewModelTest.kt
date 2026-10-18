package app.souschef.ui.ajustes

import app.souschef.domain.Ajustes
import app.souschef.domain.AjustesRepo
import app.souschef.domain.DisposicionCrono
import app.souschef.domain.copia.ArchivoCopia
import app.souschef.domain.copia.GestorCopia
import app.souschef.ui.FakeHistorialRepo
import app.souschef.ui.FakePreferenciasRepo
import app.souschef.ui.crono.RelojFalso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private class AjustesFake : AjustesRepo {
        val actual = MutableStateFlow(Ajustes())

        override fun observar(): Flow<Ajustes> = actual

        override suspend fun fijarDisposicion(d: DisposicionCrono) {
            actual.value = actual.value.copy(disposicion = d)
        }

        override suspend fun fijarVoz(activa: Boolean) {
            actual.value = actual.value.copy(vozActiva = activa)
        }
    }

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun reflejaLosCambiosDelRepositorio() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = AjustesViewModel(AjustesFake(), copiasSinArchivos())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
            assertEquals(Ajustes(), vm.estado.value)

            vm.elegirDisposicion(DisposicionCrono.APILADA)
            vm.activarVoz(false)

            assertEquals(Ajustes(DisposicionCrono.APILADA, false), vm.estado.value)
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
