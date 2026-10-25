package app.trombito.souschef.ui.recetas

import androidx.lifecycle.SavedStateHandle
import app.trombito.souschef.domain.ContadorPropuestas
import app.trombito.souschef.domain.OtraDelDia
import app.trombito.souschef.domain.PropuestasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakePreferenciasRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.FakeAjustesRepo
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RecetasViewModelPropuestasTest {
    private val hoy = LocalDate.of(2026, 10, 25)
    private val reloj =
        Reloj {
            LocalDateTime
                .of(2026, 10, 25, 10, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private class PropuestasFalso : PropuestasRepo {
        val otra = MutableStateFlow<OtraDelDia?>(null)
        private var otraGuardada: String? = null
        var mostradas: String? = null

        override fun observarOtra(): Flow<OtraDelDia?> = otra

        override suspend fun otra(hoy: LocalDate) {
            otraGuardada = ContadorPropuestas.siguienteOtra(otraGuardada, hoy)
            otra.value = ContadorPropuestas.otraDe(otraGuardada)
        }

        override suspend fun mostrar(
            hoy: LocalDate,
            slugs: List<String>,
        ) {
            mostradas = ContadorPropuestas.mostrar(mostradas, hoy, slugs)
        }

        override fun observarCocinadas(): Flow<Int> = MutableStateFlow(0)
    }

    private val recetas =
        (1..7).map { Receta(slug = "r$it", titulo = "R$it", totalMin = 60.0, tags = Tags(base = "b$it")) }

    private fun viewModel(repo: PropuestasFalso) =
        RecetasViewModel(
            SavedStateHandle(),
            FakeRecetasRepo(recetas),
            FakePreferenciasRepo(),
            FakeHistorialRepo(),
            FakeAjustesRepo(),
            reloj,
            repo,
        )

    private fun RecetasViewModel.propuestos(): List<String> =
        estado.value.propuestas
            .map { it.receta.slug }

    @Test
    fun ensenaTresPropuestasLasAnotaYOtraEnsenaLasSiguientes() =
        runTest(UnconfinedTestDispatcher()) {
            val repo = PropuestasFalso()
            val vm = viewModel(repo)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            val primeras = vm.propuestos()
            assertEquals(3, primeras.size)
            assertTrue(vm.estado.value.hayOtraPropuesta)
            assertEquals(hoy, vm.estado.value.diaPropuestas)
            assertEquals(primeras.toSet(), ContadorPropuestas.mostradasDe(repo.mostradas, hoy))

            vm.otraPropuesta()

            val siguientes = vm.propuestos()
            assertEquals(3, siguientes.size)
            assertTrue(primeras.intersect(siguientes.toSet()).isEmpty())
            assertEquals((primeras + siguientes).toSet(), ContadorPropuestas.mostradasDe(repo.mostradas, hoy))
        }

    @Test
    fun alBuscarOFiltrarLaTarjetaDesaparece() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = viewModel(PropuestasFalso())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
            assertFalse(vm.propuestos().isEmpty())

            vm.buscar("r1")
            assertTrue(vm.propuestos().isEmpty())

            vm.buscar("")
            assertNotEquals(0, vm.propuestos().size)

            vm.alternarFiltro(OpcionFiltro.Base(FamiliaBase.PASTA))
            assertTrue(vm.propuestos().isEmpty())
        }
}
