package app.trombito.souschef.ui.buzon

import androidx.lifecycle.SavedStateHandle
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.Valoracion
import app.trombito.souschef.domain.VersionFija
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakePreferenciasRepo
import app.trombito.souschef.ui.Rutas
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class BuzonViewModelTest {
    private val hoy = LocalDate.of(2026, 10, 25)
    private val reloj = Reloj { hoy.atTime(12, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000 }

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun crear(
        repo: FakeAjustesRepo = FakeAjustesRepo(),
        historial: FakeHistorialRepo = FakeHistorialRepo(),
        preferencias: FakePreferenciasRepo = FakePreferenciasRepo(),
        estado: SavedStateHandle = SavedStateHandle(),
    ) = BuzonViewModel(estado, repo, VersionFija("1.7.0"), historial, preferencias, reloj)

    @Test
    fun editarCambiaElFormularioYLosAjustesLlegan() =
        runTest(UnconfinedTestDispatcher()) {
            val repo = FakeAjustesRepo()
            val vm = crear(repo)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ajustes.collect {} }
            assertEquals(FormularioBuzon(), vm.formulario.value)

            vm.editar { it.copy(tipo = TipoBuzon.FALLO) }
            vm.editar { it.copy(queOcurrio = "Se cierra") }

            assertEquals(FormularioBuzon(TipoBuzon.FALLO, queOcurrio = "Se cierra"), vm.formulario.value)

            repo.actual.value = repo.actual.value.copy(cocina = Cocina(tipo = TipoCocina.GAS))
            assertEquals(TipoCocina.GAS, vm.ajustes.value.cocina.tipo)
        }

    @Test
    fun elResumenVieneDesmarcadoYSeCalculaConElHistorial() =
        runTest(UnconfinedTestDispatcher()) {
            val historial =
                FakeHistorialRepo(
                    listOf(
                        EntradaHistorial("a", "sopa", "Sopa", "2026-10-20", valoracion = Valoracion.REPETIR),
                        EntradaHistorial("b", "tortilla", "Tortilla", "2026-07-01", nota = "privada"),
                    ),
                )
            val preferencias = FakePreferenciasRepo()
            preferencias.datos.value = mapOf("sopa" to Preferencia("sopa", quiereHacerla = true))
            val vm = crear(historial = historial, preferencias = preferencias)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.resumen.collect {} }

            assertFalse(vm.formulario.value.adjuntarResumen)
            val r = vm.resumen.value
            assertEquals(2, r?.cocinadas)
            assertEquals(1, r?.cocinadasUltimoMes)
            assertEquals(2, r?.recetasDistintas)
            assertEquals(1, r?.repetir)
            assertEquals(1, r?.quieroHacerla)
            assertEquals("1.7.0", r?.version)
        }

    @Test
    fun elPlatoDeLaRutaRellenaElFormularioDePedirUnaReceta() {
        val vm = crear(estado = SavedStateHandle(mapOf(Rutas.ARG_PLATO to "paella de verduras")))
        assertEquals(FormularioBuzon(TipoBuzon.RECETA, plato = "paella de verduras"), vm.formulario.value)
    }

    @Test
    fun sinPlatoEnLaRutaElFormularioEstaVacioYUnPlatoLargoSeCorta() {
        assertEquals(FormularioBuzon(), crear().formulario.value)
        val largo = "x".repeat(Rutas.PLATO_MAX + 50)
        val vm = crear(estado = SavedStateHandle(mapOf(Rutas.ARG_PLATO to largo)))
        assertEquals(Rutas.PLATO_MAX, vm.formulario.value.plato.length)
        assertNull(crear().resumen.value)
    }
}
