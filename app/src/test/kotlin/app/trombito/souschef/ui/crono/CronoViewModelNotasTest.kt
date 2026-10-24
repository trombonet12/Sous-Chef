package app.trombito.souschef.ui.crono

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.NotaCocinada
import app.trombito.souschef.domain.crono.AlmacenCrono
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.FakeTecnicasRepo
import app.trombito.souschef.ui.Rutas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Las notas de cocinadas anteriores de la receta llegan al estado de la cocina. */
@OptIn(ExperimentalCoroutinesApi::class)
class CronoViewModelNotasTest {
    private val reloj = RelojFalso()
    private val ajustes = FakeAjustesRepo()
    private val recetas = FakeRecetasRepo(listOf(recetaCrono()))
    private val cadena =
        CadenaAlarmas(AlmacenCrono(FakeCronoEstadoRepo()), recetas, reloj, FakeAlarmaSistema(), FakeNotificador())

    @Before
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun entrada(
        id: String,
        slug: String,
        fecha: String,
        nota: String?,
    ) = EntradaHistorial(id = id, slug = slug, titulo = "Receta $slug", fecha = fecha, nota = nota)

    private fun crearVm(historial: FakeHistorialRepo) =
        CronoViewModel(
            SavedStateHandle(mapOf(Rutas.ARG_SLUG to "r", Rutas.ARG_RACIONES to 0, Rutas.ARG_SERVIR to "12:15")),
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

    @Test
    fun notasSoloDeEstaRecetaYDeLaMasRecienteALaMasAntigua() =
        runTest {
            val historial =
                FakeHistorialRepo(
                    listOf(
                        entrada("r-1", "r", "2026-09-01", "Menos sal"),
                        entrada("otra-1", "otra", "2026-10-01", "Nota de otra receta"),
                        entrada("r-2", "r", "2026-10-05", "Más tiempo al horno"),
                        entrada("r-3", "r", "2026-10-10", null),
                    ),
                )
            val vm = crearVm(historial)
            try {
                runCurrent()
                assertEquals(
                    listOf(
                        NotaCocinada("2026-10-05", "Más tiempo al horno"),
                        NotaCocinada("2026-09-01", "Menos sal"),
                    ),
                    vm.ui.value.notas,
                )
            } finally {
                vm.viewModelScope.cancel()
            }
        }

    @Test
    fun lasNotasSeMantienenAlDiaMientrasViveElViewModel() =
        runTest {
            val historial = FakeHistorialRepo()
            val vm = crearVm(historial)
            try {
                runCurrent()
                assertEquals(emptyList<NotaCocinada>(), vm.ui.value.notas)
                historial.agregar(entrada("r-1", "r", "2026-10-12", "Más ajo"))
                runCurrent()
                assertEquals(listOf(NotaCocinada("2026-10-12", "Más ajo")), vm.ui.value.notas)
            } finally {
                vm.viewModelScope.cancel()
            }
        }
}
