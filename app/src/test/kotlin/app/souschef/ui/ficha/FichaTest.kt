package app.souschef.ui.ficha

import androidx.lifecycle.SavedStateHandle
import app.souschef.domain.EstadoDespensa
import app.souschef.domain.ItemDespensa
import app.souschef.domain.Preferencia
import app.souschef.receta.Grupo
import app.souschef.receta.Ingrediente
import app.souschef.receta.Tecnica
import app.souschef.ui.FakeCompraRepo
import app.souschef.ui.FakePreferenciasRepo
import app.souschef.ui.FakeRecetasRepo
import app.souschef.ui.FakeTecnicasRepo
import app.souschef.ui.Rutas
import app.souschef.ui.despensa.FakeDespensaRepo
import app.souschef.ui.receta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FichaLogicaTest {
    @Test
    fun servirDaLaVueltaAlDia() {
        assertEquals("15:30", Servir.formato(Servir.DEFECTO))
        assertEquals("00:05", Servir.formato(Servir.ajustar(23 * 60 + 50, 15)))
        assertEquals("23:45", Servir.formato(Servir.ajustar(0, -15)))
    }

    @Test
    fun inicioEsServirMenosElTotalRedondeadoArriba() {
        assertEquals("14:43", Servir.formato(Servir.inicio(Servir.DEFECTO, receta("a").copy(totalMin = 47.0))))
        assertEquals("14:42", Servir.formato(Servir.inicio(Servir.DEFECTO, receta("a").copy(totalMin = 47.5))))
        assertEquals("23:50", Servir.formato(Servir.inicio(20, receta("a").copy(totalMin = 30.0))))
    }

    @Test
    fun hitosComoMasMinutosSegundos() {
        assertEquals("+1:05", formatoHito(65.0))
        assertEquals("+0:30", formatoHito(30.0))
    }

    @Test
    fun enlacesSoloALasTecnicasQueExisten() {
        val r = receta("a", tecnicas = listOf("plancha", "no-existe"))
        val enlaces = enlacesTecnicas(r, listOf(Tecnica("plancha", "A la plancha")))
        assertEquals(listOf(TecnicaEnlace("plancha", "A la plancha")), enlaces)
    }

    private fun conGrupo(vararg items: Ingrediente) = receta("a").copy(grupos = listOf(Grupo("Base", items.toList())))

    private fun d(
        nombre: String,
        estado: EstadoDespensa,
    ) = ItemDespensa(nombre = nombre, categoria = "", estado = estado)

    @Test
    fun loQueHayEnLaDespensaNoSale() {
        val r = conGrupo(Ingrediente("arroz", a = 100.0, u = "g"))
        assertEquals(emptyList<Falta>(), faltas(r, 2, listOf(d("Arroz", EstadoDespensa.HAY))))
    }

    @Test
    fun pocoYFaltaSalenConSuEstado() {
        val r = conGrupo(Ingrediente("arroz", a = 100.0, u = "g"), Ingrediente("sal", a = 2.0, u = "g"))
        val despensa = listOf(d("Arroz", EstadoDespensa.POCO), d("Sal", EstadoDespensa.FALTA))
        assertEquals(
            listOf(Falta("100 g arroz", EstadoDespensa.POCO), Falta("2 g sal", EstadoDespensa.FALTA)),
            faltas(r, 2, despensa),
        )
    }

    @Test
    fun ingredienteSinEntradaSaleConEstadoNulo() {
        val r = conGrupo(Ingrediente("azafrán", a = 1.0, u = "g"))
        assertEquals(listOf(Falta("1 g azafrán", null)), faltas(r, 2, listOf(d("Arroz", EstadoDespensa.HAY))))
    }

    @Test
    fun vetadoNoSaleYElAguaTampoco() {
        val r = conGrupo(Ingrediente("agua", a = 200.0, u = "ml"), Ingrediente("coliflor", a = 100.0, u = "g"))
        assertEquals(emptyList<Falta>(), faltas(r, 2, listOf(d("Coliflor", EstadoDespensa.VETADO))))
    }

    @Test
    fun lasCantidadesSeEscalanSinAsterisco() {
        val r =
            conGrupo(
                Ingrediente("sal", a = 2.0, u = "g", esc = "sub"),
                Ingrediente("arroz", a = 100.0, u = "g"),
            )
        val despensa = listOf(d("Sal", EstadoDespensa.FALTA), d("Arroz", EstadoDespensa.FALTA))
        assertEquals(listOf("3,4 g sal", "200 g arroz"), faltas(r, 4, despensa).map { it.linea })
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FichaViewModelTest {
    private val prefs = FakePreferenciasRepo()
    private val compra = FakeCompraRepo()
    private val recetas =
        FakeRecetasRepo(
            listOf(
                receta(
                    "sopa",
                    tecnicas = listOf("plancha"),
                ).copy(grupos = listOf(Grupo("Base", listOf(Ingrediente("pan", a = 100.0, u = "g"))))),
            ),
        )
    private val despensa =
        FakeDespensaRepo(listOf(ItemDespensa(1, "Pan", "", EstadoDespensa.FALTA)))

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun TestScope.crear(): FichaViewModel {
        val vm =
            FichaViewModel(
                SavedStateHandle(mapOf(Rutas.ARG_SLUG to "sopa")),
                recetas,
                FakeTecnicasRepo(listOf(Tecnica("plancha", "A la plancha"))),
                prefs,
                compra,
                despensa,
                backgroundScope,
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    @Test
    fun restauraLasRacionesGuardadasYLaHoraPorDefecto() =
        runTest(UnconfinedTestDispatcher()) {
            prefs.datos.value = mapOf("sopa" to Preferencia("sopa", raciones = 5))
            val vm = crear()
            assertEquals(5, vm.estado.value.raciones)
            assertEquals("15:30", Servir.formato(vm.estado.value.servirMin))
            assertEquals(
                listOf("plancha"),
                vm.estado.value.tecnicas
                    .map { it.slug },
            )
        }

    @Test
    fun guardaLasRacionesTras600msSinTocar() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = crear()
            vm.cambiarRaciones(3)
            vm.cambiarRaciones(4)
            assertEquals(4, vm.estado.value.raciones)
            advanceTimeBy(599)
            runCurrent()
            assertEquals(emptyList<Pair<String, Int>>(), prefs.racionesGuardadas)
            advanceTimeBy(2)
            runCurrent()
            assertEquals(listOf("sopa" to 4), prefs.racionesGuardadas)
        }

    @Test
    fun alSalirAntesDelDebounceGuardaLoPendiente() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = crear()
            vm.cambiarRaciones(6)
            advanceTimeBy(100)
            vm.guardarPendiente()
            runCurrent()
            assertEquals(listOf("sopa" to 6), prefs.racionesGuardadas)
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(listOf("sopa" to 6), prefs.racionesGuardadas)
        }

    @Test
    fun racionesQuedanEntre1Y20() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = crear()
            vm.cambiarRaciones(0)
            assertEquals(1, vm.estado.value.raciones)
            vm.cambiarRaciones(99)
            assertEquals(20, vm.estado.value.raciones)
        }

    @Test
    fun anadirALaCompraUsaLasRacionesEscaladas() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = crear()
            vm.cambiarRaciones(4)
            vm.anadirALaCompra()
            assertEquals(listOf(listOf("200 g pan") to "sopa"), compra.anadidos)
            assertEquals(MensajeFicha.ANADIDA, vm.estado.value.mensaje)
            advanceTimeBy(3_001)
            runCurrent()
            assertNull(vm.estado.value.mensaje)
        }

    @Test
    fun noAnadeNadaSiLaDespensaLoCubre() =
        runTest(UnconfinedTestDispatcher()) {
            despensa.datos.value = listOf(ItemDespensa(1, "Pan", "", EstadoDespensa.HAY))
            val vm = crear()
            vm.anadirALaCompra()
            assertEquals(emptyList<Pair<List<String>, String?>>(), compra.anadidos)
        }

    @Test
    fun textoDeCompraParaElPortapapeles() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = crear()
            vm.cambiarRaciones(1)
            assertEquals("50 g pan", vm.textoCompra())
            despensa.datos.value = listOf(ItemDespensa(1, "Pan", "", EstadoDespensa.HAY))
            assertNull(vm.textoCompra())
        }
}
