package app.trombito.souschef.ui.ficha

import androidx.lifecycle.SavedStateHandle
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.crono.AlmacenCrono
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Escala
import app.trombito.souschef.receta.Grupo
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Tecnica
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.receta.crono.SesionCrono
import app.trombito.souschef.ui.FakePreferenciasRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.FakeTecnicasRepo
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.crono.FakeAlarmaSistema
import app.trombito.souschef.ui.crono.FakeCronoEstadoRepo
import app.trombito.souschef.ui.crono.FakeNotificador
import app.trombito.souschef.ui.crono.RelojFalso
import app.trombito.souschef.ui.receta
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
}

@OptIn(ExperimentalCoroutinesApi::class)
class FichaViewModelTest {
    private val prefs = FakePreferenciasRepo()
    private val recetas =
        FakeRecetasRepo(
            listOf(
                receta(
                    "sopa",
                    tecnicas = listOf("plancha"),
                ).copy(grupos = listOf(Grupo("Base", listOf(Ingrediente("pan", a = 100.0, u = "g"))))),
            ),
        )

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private val repoCrono = FakeCronoEstadoRepo()
    private val reloj = RelojFalso()
    private val almacen = AlmacenCrono(repoCrono)
    private val cadena = CadenaAlarmas(almacen, recetas, reloj, FakeAlarmaSistema(), FakeNotificador())
    private val ajustes = FakeAjustesRepo()

    private fun TestScope.crear(): FichaViewModel {
        val vm =
            FichaViewModel(
                SavedStateHandle(mapOf(Rutas.ARG_SLUG to "sopa")),
                recetas,
                FakeTecnicasRepo(listOf(Tecnica("plancha", "A la plancha"))),
                prefs,
                cadena,
                ajustes,
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
    fun laHoraDeServirParteDeAjustesYElMasMenosNoLosToca() =
        runTest(UnconfinedTestDispatcher()) {
            ajustes.actual.value = ajustes.actual.value.copy(servirMin = 14 * 60)
            val vm = crear()
            assertEquals("14:00", Servir.formato(vm.estado.value.servirMin))
            // Sin tocar la ficha, sigue al ajuste.
            ajustes.actual.value = ajustes.actual.value.copy(servirMin = 13 * 60)
            assertEquals("13:00", Servir.formato(vm.estado.value.servirMin))
            vm.ajustarServir(Servir.PASO)
            assertEquals("13:15", Servir.formato(vm.estado.value.servirMin))
            assertEquals(13 * 60, ajustes.actual.value.servirMin)
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

    private fun conEscala(
        escala: Escala?,
        base: Int = 2,
    ) {
        val actual = recetas.datos.value.first()
        recetas.datos.value = listOf(actual.copy(raciones = base, escala = escala))
    }

    @Test
    fun conRangoLaGuardadaManda() =
        runTest(UnconfinedTestDispatcher()) {
            conEscala(Escala(2, 8, 2))
            prefs.datos.value = mapOf("sopa" to Preferencia("sopa", raciones = 6))
            val vm = crear()
            assertEquals(6, vm.estado.value.raciones)
            assertEquals(listOf(2, 4, 6, 8), vm.estado.value.racionesPermitidas)
            assertFalse(vm.estado.value.ajustadoAlRango)
        }

    @Test
    fun siLaGuardadaNoCabeSeUsaElDefectoDeAjustes() =
        runTest(UnconfinedTestDispatcher()) {
            conEscala(Escala(2, 8, 2))
            prefs.datos.value = mapOf("sopa" to Preferencia("sopa", raciones = 3))
            ajustes.actual.value = ajustes.actual.value.copy(racionesDefecto = 4)
            assertEquals(4, crear().estado.value.raciones)
        }

    @Test
    fun elDefectoGlobalSeAjustaAlRangoDeLaReceta() =
        runTest(UnconfinedTestDispatcher()) {
            conEscala(Escala(1, 4, 1))
            ajustes.actual.value = ajustes.actual.value.copy(racionesDefecto = 6)
            val estado = crear().estado.value
            assertEquals(4, estado.raciones)
            assertTrue(estado.ajustadoAlRango)
        }

    @Test
    fun unaRecetaFijaAbreConSuUnicoValorYNoDejaMoverse() =
        runTest(UnconfinedTestDispatcher()) {
            conEscala(Escala(2, 2, 1))
            ajustes.actual.value = ajustes.actual.value.copy(racionesDefecto = 5)
            val vm = crear()
            assertEquals(2, vm.estado.value.raciones)
            assertEquals(listOf(2), vm.estado.value.racionesPermitidas)
            vm.cambiarRaciones(7)
            assertEquals(2, vm.estado.value.raciones)
        }

    @Test
    fun cambiarRacionesSoloSeMueveDentroDeLasPermitidas() =
        runTest(UnconfinedTestDispatcher()) {
            conEscala(Escala(2, 6, 2))
            val vm = crear()
            vm.cambiarRaciones(99)
            assertEquals(6, vm.estado.value.raciones)
            vm.cambiarRaciones(0)
            assertEquals(2, vm.estado.value.raciones)
            vm.cambiarRaciones(5)
            assertEquals(4, vm.estado.value.raciones)
        }

    @Test
    fun fueraDeBaseSoloSiLasRacionesNoSonLasDeLaReceta() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = crear()
            assertEquals(2, vm.estado.value.raciones)
            assertFalse(vm.estado.value.fueraDeBase)
            vm.cambiarRaciones(4)
            assertTrue(vm.estado.value.fueraDeBase)
            vm.cambiarRaciones(2)
            assertFalse(vm.estado.value.fueraDeBase)
        }

    @Test
    fun laRecetaLlegaTraducidaALaCocinaSinNumeros() =
        runTest(UnconfinedTestDispatcher()) {
            val canonica = recetas.datos.value.first()
            recetas.datos.value =
                listOf(canonica.copy(pasos = listOf(Paso("p1", "Baja al {7}", "m", 0.0, 5.0, f = 7))))
            ajustes.actual.value = ajustes.actual.value.copy(cocina = Cocina(TipoCocina.GAS, Mando.ANALOGICO, null))
            val estado = crear().estado.value
            val paso = estado.receta?.pasos?.firstOrNull()
            assertEquals("Baja a fuego medio-alto", paso?.t)
            // El fuego numérico y los ids no cambian: la identidad canónica es del motor.
            assertEquals(7, paso?.f)
            assertEquals("p1", paso?.id)
            assertEquals(TipoCocina.GAS, estado.cocina.tipo)
        }

    @Test
    fun enVitroCanonicaElTextoNoCambia() =
        runTest(UnconfinedTestDispatcher()) {
            val canonica = recetas.datos.value.first()
            recetas.datos.value =
                listOf(canonica.copy(pasos = listOf(Paso("p1", "Baja al {3}", "m", 0.0, 5.0, f = 3))))
            val estado = crear().estado.value
            val paso = estado.receta?.pasos?.firstOrNull()
            assertEquals("Baja al 3", paso?.t)
        }

    @Test
    fun conLaCocinaAbiertaLaFichaSabeQueSeEstaCocinandoYAlSalirSeAbandona() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = crear()
            assertNull(vm.estado.value.cocinando)
            val receta = recetas.datos.value.first()
            val servir = reloj.ms + 60 * 60_000L
            almacen.fijar(SesionCrono.abrir(SesionCrono.inicial(receta, servir, reloj.ms), receta, reloj.ms))
            runCurrent()
            assertEquals(1, vm.estado.value.cocinando)

            vm.abandonarCocinada()
            runCurrent()

            assertNull(almacen.leer("sopa"))
            assertNull(vm.estado.value.cocinando)
        }
}
