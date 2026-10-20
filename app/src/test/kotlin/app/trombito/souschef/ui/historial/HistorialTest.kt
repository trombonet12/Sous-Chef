package app.trombito.souschef.ui.historial

import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.receta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun entrada(
    id: String,
    fecha: String,
    base: String?,
    slug: String = id,
    nota: String? = null,
) = EntradaHistorial(id = id, slug = slug, titulo = "Plato $id", fecha = fecha, base = base, nota = nota)

class HistorialLogicaTest {
    private val lista =
        listOf(
            entrada("e3", "2026-10-10", "arroz"),
            entrada("e2", "2026-10-05", "arroz"),
            entrada("e1", "2026-10-01", "pasta"),
            entrada("e0", "2026-09-20", null),
        )

    @Test
    fun marcaDosSeguidasCuandoLaSiguienteComparteBase() {
        val filas = marcarRepeticiones(lista, setOf("e3"))
        assertEquals(listOf(true, false, false, false), filas.map { it.repite })
        assertEquals(listOf(true, false, false, false), filas.map { it.recetaExiste })
    }

    @Test
    fun laMarcaSeCalculaSobreLaListaCompletaAunqueSeFiltre() {
        val estado = construirEstadoHistorial(lista, emptySet(), "arroz", null)
        assertEquals(listOf("e3", "e2"), estado.filas.map { it.entrada.id })
        assertTrue(estado.filas.first().repite)
        assertEquals(4, estado.total)
    }

    @Test
    fun buscaEnNotaTecnicaSlugYBase() {
        val e =
            entrada(
                "x",
                "2026-10-01",
                "pasta",
                slug = "macarrones",
                nota = "Quedó jugosa",
            ).copy(tecnica = listOf("sofrito"))
        assertTrue(coincideHistorial(e, "JUGOSA"))
        assertTrue(coincideHistorial(e, "sofrito"))
        assertTrue(coincideHistorial(e, "macarr"))
        assertTrue(coincideHistorial(e, "pasta"))
        assertFalse(coincideHistorial(e, "pizza"))
    }

    @Test
    fun diaYMesSeparaElDiaSinCeroYElMesCorto() {
        assertEquals("14" to "oct", diaYMes("2026-10-14"))
        assertEquals("5" to "ene", diaYMes("2026-01-05"))
        assertEquals("30" to "dic", diaYMes("2026-12-30T21:15:00"))
    }

    @Test
    fun diaYMesDevuelveNuloSiLaFechaNoSeEntiende() {
        assertNull(diaYMes("ayer"))
        assertNull(diaYMes(""))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModelTest {
    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun borrarNecesitaUnSegundoToqueDentroDeTresSegundos() =
        runTest(UnconfinedTestDispatcher()) {
            val repo = FakeHistorialRepo(listOf(entrada("a", "2026-10-01", "x"), entrada("b", "2026-09-01", "y")))
            val vm = HistorialViewModel(repo, FakeRecetasRepo(listOf(receta("a"))), FakeAjustesRepo())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            vm.tocarBorrar("a")
            assertEquals("a", vm.estado.value.pendienteBorrar)
            assertEquals(2, repo.datos.value.size)

            advanceTimeBy(3_001)
            runCurrent()
            assertNull(vm.estado.value.pendienteBorrar)
            assertEquals(2, repo.datos.value.size)

            vm.tocarBorrar("a")
            advanceTimeBy(2_000)
            vm.tocarBorrar("a")
            assertNull(vm.estado.value.pendienteBorrar)
            assertEquals(listOf("b"), repo.datos.value.map { it.id })
        }

    @Test
    fun tocarOtraEntradaCambiaLaConfirmacionSinBorrar() =
        runTest(UnconfinedTestDispatcher()) {
            val repo = FakeHistorialRepo(listOf(entrada("a", "2026-10-01", "x"), entrada("b", "2026-09-01", "y")))
            val vm = HistorialViewModel(repo, FakeRecetasRepo(), FakeAjustesRepo())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            vm.tocarBorrar("a")
            vm.tocarBorrar("b")
            assertEquals("b", vm.estado.value.pendienteBorrar)
            assertEquals(2, repo.datos.value.size)
            assertFalse(
                vm.estado.value.filas
                    .first()
                    .recetaExiste,
            )
        }

    @Test
    fun laDisposicionDeAjustesLlegaAlEstado() =
        runTest(UnconfinedTestDispatcher()) {
            val ajustes = FakeAjustesRepo()
            val vm =
                HistorialViewModel(
                    FakeHistorialRepo(listOf(entrada("a", "2026-10-01", "x"))),
                    FakeRecetasRepo(),
                    ajustes,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            assertEquals(DisposicionLista.TINTE, vm.estado.value.disposicion)
            ajustes.fijarLista(DisposicionLista.FRANJA)
            assertEquals(DisposicionLista.FRANJA, vm.estado.value.disposicion)
        }
}
