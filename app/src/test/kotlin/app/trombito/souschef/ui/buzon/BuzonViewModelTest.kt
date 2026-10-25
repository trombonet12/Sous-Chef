package app.trombito.souschef.ui.buzon

import app.trombito.souschef.domain.VersionFija
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.TipoCocina
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuzonViewModelTest {
    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun editarCambiaElFormularioYLosAjustesLlegan() =
        runTest(UnconfinedTestDispatcher()) {
            val repo = FakeAjustesRepo()
            val vm = BuzonViewModel(repo, VersionFija("1.7.0"))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ajustes.collect {} }
            assertEquals(FormularioBuzon(), vm.formulario.value)

            vm.editar { it.copy(tipo = TipoBuzon.FALLO) }
            vm.editar { it.copy(queOcurrio = "Se cierra") }

            assertEquals(FormularioBuzon(TipoBuzon.FALLO, queOcurrio = "Se cierra"), vm.formulario.value)

            repo.actual.value = repo.actual.value.copy(cocina = Cocina(tipo = TipoCocina.GAS))
            assertEquals(TipoCocina.GAS, vm.ajustes.value.cocina.tipo)
        }
}
