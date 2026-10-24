package app.trombito.souschef.ui.crono

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.crono.AlmacenCrono
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.FakeTecnicasRepo
import app.trombito.souschef.ui.Rutas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** ADR-0014: lo que se guarda en el historial es canónico (con `{N}`); se traduce al mostrarlo. */
@OptIn(ExperimentalCoroutinesApi::class)
class CronoViewModelHistorialTest {
    private val historial = FakeHistorialRepo()
    private val reloj = RelojFalso()
    private val ajustes = FakeAjustesRepo()
    private val recetas = FakeRecetasRepo(listOf(recetaCrono()))
    private val cadena =
        CadenaAlarmas(AlmacenCrono(FakeCronoEstadoRepo()), recetas, reloj, FakeAlarmaSistema(), FakeNotificador())

    @Before
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun elHistorialGuardaLosTitulosDeLosPasosCanonicosAunqueLaCocinaNoLoSea() =
        runTest {
            ajustes.fijarCocina(Cocina(TipoCocina.GAS, Mando.ANALOGICO, null))
            recetas.datos.value =
                listOf(
                    recetaCrono().copy(
                        pasos = recetaCrono().pasos.map { if (it.id == "p1") it.copy(t = "Baja al {3}") else it },
                    ),
                )
            val args = mapOf(Rutas.ARG_SLUG to "r", Rutas.ARG_RACIONES to 0, Rutas.ARG_SERVIR to "12:15")
            val vm =
                CronoViewModel(
                    SavedStateHandle(args),
                    recetas,
                    FakeTecnicasRepo(),
                    historial,
                    cadena,
                    reloj,
                    FakeAvisador(),
                    ajustes,
                    FakeEscucha(),
                    FakeHabla(),
                )
            try {
                runCurrent()
                vm.abrirCocina()
                runCurrent()
                listOf("m", "f0", "f0").forEach {
                    vm.hecho(it)
                    runCurrent()
                    advanceTimeBy(1_001)
                    runCurrent()
                }
                vm.cerrar("")
                runCurrent()
                advanceTimeBy(VENTANA_DESHACER_MS + 100)
                runCurrent()
                val entrada = historial.datos.value.single()
                assertEquals("Baja al {3}", entrada.pasosReales.first { it.id == "p1" }.t)
            } finally {
                vm.viewModelScope.cancel()
            }
        }
}
