package app.souschef.ui.recetas

import app.souschef.domain.Preferencia
import app.souschef.domain.Reloj
import app.souschef.receta.Grupo
import app.souschef.receta.Ingrediente
import app.souschef.ui.FakeHistorialRepo
import app.souschef.ui.FakePreferenciasRepo
import app.souschef.ui.FakeRecetasRepo
import app.souschef.ui.receta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class HaceTest {
    private val hoy = LocalDate.of(2026, 10, 14)

    private fun hace(dias: Long) = Hace.de(hoy.minusDays(dias).toString(), hoy)

    @Test
    fun nuncaCocinadaEsSinEstrenarResaltado() {
        val h = Hace.de(null, hoy)
        assertEquals(UnidadHace.SIN_ESTRENAR, h.unidad)
        assertTrue(h.resaltado)
    }

    @Test
    fun hoyAyerYDias() {
        assertEquals(UnidadHace.HOY, hace(0).unidad)
        assertEquals(UnidadHace.AYER, hace(1).unidad)
        assertEquals(Hace(UnidadHace.DIAS, 6), hace(6))
    }

    @Test
    fun semanasMesesYAnos() {
        assertEquals(Hace(UnidadHace.SEMANAS, 1), hace(7))
        assertEquals(Hace(UnidadHace.SEMANAS, 4), hace(29))
        assertEquals(Hace(UnidadHace.MESES, 1, false), hace(30))
        assertEquals(Hace(UnidadHace.MESES, 2, true), hace(60))
        assertEquals(Hace(UnidadHace.ANOS, 1, true), hace(365))
    }

    @Test
    fun resaltaDesdeSesentaDias() {
        assertFalse(hace(59).resaltado)
        assertTrue(hace(60).resaltado)
    }

    @Test
    fun fechaFuturaCuentaComoHoy() {
        assertEquals(UnidadHace.HOY, hace(-3).unidad)
    }
}

class RecetasLogicaTest {
    private val hoy = LocalDate.of(2026, 10, 14)

    private fun estado(
        recetas: List<app.souschef.receta.Receta>,
        consulta: String = "",
        base: String? = null,
        prefs: Map<String, Preferencia> = emptyMap(),
        fechas: Map<String, String> = emptyMap(),
    ) = construirEstadoRecetas(recetas, prefs, fechas, FiltroRecetas(consulta, base), hoy)

    @Test
    fun buscaIgnorandoAcentosEnIngredientesYPasos() {
        val con = receta("a", "Tortilla").copy(grupos = listOf(Grupo("g", listOf(Ingrediente("Pimiento rojo")))))
        val otra = receta("b", "Otra")
        assertEquals(listOf("a"), estado(listOf(con, otra), "PIMIENTO").tarjetas.map { it.receta.slug })
        assertEquals(listOf("a", "b"), estado(listOf(con, otra), "pícar").tarjetas.map { it.receta.slug }.sorted())
        assertEquals(listOf("b"), estado(listOf(con, otra), "otr").tarjetas.map { it.receta.slug })
    }

    @Test
    fun chipsSoloSiHayMasDeUnaBaseTrasElFiltroDeTexto() {
        val rs = listOf(receta("a", "Arroz", base = "arroz"), receta("b", "Pasta", base = "pasta"))
        assertTrue(estado(rs).mostrarChips)
        assertFalse(estado(rs, "arroz").mostrarChips)
        assertEquals(listOf(ChipBase("arroz", 1), ChipBase("pasta", 1)), estado(rs).bases)
    }

    @Test
    fun filtraPorBaseYIgnoraLaBaseQueYaNoEstaVisible() {
        val rs = listOf(receta("a", "Arroz", base = "arroz"), receta("b", "Pasta", base = "pasta"))
        assertEquals(listOf("b"), estado(rs, base = "pasta").tarjetas.map { it.receta.slug })
        val e = estado(rs, consulta = "arroz", base = "pasta")
        assertEquals(null, e.baseActiva)
        assertEquals(listOf("a"), e.tarjetas.map { it.receta.slug })
    }

    @Test
    fun favoritasPrimeroYLuegoActualizadaDescendente() {
        val rs =
            listOf(
                receta("vieja").copy(actualizada = "2026-01-01T00:00:00Z"),
                receta("nueva").copy(actualizada = "2026-09-01T00:00:00Z"),
                receta("fav").copy(actualizada = "2025-01-01T00:00:00Z"),
            )
        val e = estado(rs, prefs = mapOf("fav" to Preferencia("fav", favorito = true)))
        assertEquals(listOf("fav", "nueva", "vieja"), e.tarjetas.map { it.receta.slug })
    }

    @Test
    fun cuentaFuegosSinElHornoYCalculaHace() {
        val rs = listOf(receta("a", fuegos = 2), receta("b"))
        val e = estado(rs, fechas = mapOf("a" to "2026-07-01"))
        val a = e.tarjetas.first { it.receta.slug == "a" }
        val b = e.tarjetas.first { it.receta.slug == "b" }
        assertEquals(2, a.fuegos)
        assertEquals(Hace(UnidadHace.MESES, 3, true), a.hace)
        assertEquals(UnidadHace.SIN_ESTRENAR, b.hace.unidad)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecetasViewModelTest {
    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun favoritaSubeAlPrincipioYElHistorialDaElHace() =
        runTest(UnconfinedTestDispatcher()) {
            val prefs = FakePreferenciasRepo()
            val historial = FakeHistorialRepo().also { it.ultimas.value = mapOf("b" to "2026-10-13") }
            val reloj =
                Reloj {
                    LocalDate
                        .of(2026, 10, 14)
                        .atStartOfDay()
                        .toInstant(java.time.ZoneOffset.UTC)
                        .toEpochMilli()
                }
            val recetas = FakeRecetasRepo(listOf(receta("a", base = "x"), receta("b", base = "y")))
            val vm = RecetasViewModel(recetas, prefs, historial, reloj)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            assertEquals(2, vm.estado.value.tarjetas.size)
            assertEquals(
                UnidadHace.AYER,
                vm.estado.value.tarjetas
                    .first { it.receta.slug == "b" }
                    .hace.unidad,
            )

            vm.alternarFavorita("b", true)
            assertEquals(
                "b",
                vm.estado.value.tarjetas
                    .first()
                    .receta.slug,
            )

            vm.buscar("zzz")
            assertTrue(
                vm.estado.value.tarjetas
                    .isEmpty(),
            )
            vm.buscar("")
            vm.elegirBase("x")
            assertEquals(
                listOf("a"),
                vm.estado.value.tarjetas
                    .map { it.receta.slug },
            )
        }
}
