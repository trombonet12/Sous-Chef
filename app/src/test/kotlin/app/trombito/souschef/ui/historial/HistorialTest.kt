package app.trombito.souschef.ui.historial

import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.ResumenMes
import app.trombito.souschef.domain.Valoracion
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.crono.RelojFalso
import app.trombito.souschef.ui.receta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

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
        val estado = construirEstadoHistorial(lista, emptySet(), "arroz", null, LocalDate.of(2026, 10, 24))
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

    private fun TestScope.crear(
        repo: FakeHistorialRepo,
        recetas: FakeRecetasRepo = FakeRecetasRepo(),
        reloj: RelojFalso = RelojFalso(),
    ): HistorialViewModel {
        val vm = HistorialViewModel(repo, recetas, FakeAjustesRepo(), reloj)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    @Test
    fun borrarDeUnToqueGuardaLaEntradaParaDeshacer() =
        runTest(UnconfinedTestDispatcher()) {
            val a = entrada("a", "2026-10-01", "x")
            val repo = FakeHistorialRepo(listOf(a, entrada("b", "2026-09-01", "y")))
            val vm = crear(repo, FakeRecetasRepo(listOf(receta("a"))))

            vm.borrar(a)

            assertEquals(listOf("b"), repo.datos.value.map { it.id })
            assertEquals(a, vm.estado.value.borrada)
        }

    @Test
    fun deshacerReinsertaLaMismaEntradaConSuValoracion() =
        runTest(UnconfinedTestDispatcher()) {
            val a = entrada("a", "2026-10-01", "x").copy(valoracion = Valoracion.AJUSTAR, nota = "más sal")
            val repo = FakeHistorialRepo(listOf(a))
            val vm = crear(repo)

            vm.borrar(a)
            vm.deshacerBorrado()

            assertEquals(listOf(a), repo.datos.value)
            assertNull(vm.estado.value.borrada)
        }

    @Test
    fun descartarElBorradoYaNoSePuedeDeshacer() =
        runTest(UnconfinedTestDispatcher()) {
            val a = entrada("a", "2026-10-01", "x")
            val repo = FakeHistorialRepo(listOf(a))
            val vm = crear(repo)

            vm.borrar(a)
            vm.descartarBorrado()
            vm.deshacerBorrado()

            assertEquals(emptyList<EntradaHistorial>(), repo.datos.value)
            assertNull(vm.estado.value.borrada)
        }

    @Test
    fun soloSePuedeDeshacerElUltimoBorrado() =
        runTest(UnconfinedTestDispatcher()) {
            val a = entrada("a", "2026-10-01", "x")
            val b = entrada("b", "2026-09-01", "y")
            val repo = FakeHistorialRepo(listOf(a, b))
            val vm = crear(repo)

            vm.borrar(a)
            vm.borrar(b)
            vm.deshacerBorrado()

            assertEquals(listOf("b"), repo.datos.value.map { it.id })
        }

    @Test
    fun editarNotaCambiaSoloLaNotaYLaVaciaLaQuita() =
        runTest(UnconfinedTestDispatcher()) {
            val a = entrada("a", "2026-10-01", "x").copy(valoracion = Valoracion.REPETIR, nota = "antes")
            val repo = FakeHistorialRepo(listOf(a, entrada("b", "2026-09-01", "y", nota = "intacta")))
            val vm = crear(repo)

            fun guardada(id: String) = repo.datos.value.first { it.id == id }

            vm.editarNota("a", "  ahora con limón ")
            assertEquals(a.copy(nota = "ahora con limón"), guardada("a"))
            assertEquals("intacta", guardada("b").nota)

            vm.editarNota("a", "   ")
            assertNull(guardada("a").nota)
            assertEquals(Valoracion.REPETIR, guardada("a").valoracion)
        }

    @Test
    fun elResumenDelMesSaleDelRelojYNoDelFiltro() =
        runTest(UnconfinedTestDispatcher()) {
            val repo =
                FakeHistorialRepo(
                    listOf(
                        entrada("a", "2026-10-12", "x"),
                        entrada("a2", "2026-10-02", "x", slug = "a"),
                        entrada("b", "2026-09-01", "y"),
                    ),
                )
            val vm = crear(repo, FakeRecetasRepo(listOf(receta("a"), receta("b"), receta("c"))))

            val esperado = ResumenMes(cocinadas = 2, recetasDistintas = 1, sinEstrenar = 1, totalRecetas = 3)
            assertEquals(esperado, vm.estado.value.mes)
            vm.buscar("no-existe")
            assertEquals(2, vm.estado.value.mes.cocinadas)
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
                    RelojFalso(),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            assertEquals(DisposicionLista.TINTE, vm.estado.value.disposicion)
            ajustes.fijarLista(DisposicionLista.FRANJA)
            assertEquals(DisposicionLista.FRANJA, vm.estado.value.disposicion)
        }
}
