package app.trombito.souschef.ui.novedades

import androidx.compose.ui.graphics.Color
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.ComprobarNovedades
import app.trombito.souschef.domain.EstadoActualizacionFijo
import app.trombito.souschef.domain.FuenteNovedadesFija
import app.trombito.souschef.domain.VersionFija
import app.trombito.souschef.domain.novedad
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NovedadesViewModelTest {
    private val ajustes = FakeAjustesRepo()

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        ajustes.actual.value = Ajustes(introVista = true, novedadesVistas = "1.5")
    }

    @After
    fun despues() = Dispatchers.resetMain()

    private val play = EstadoActualizacionFijo(false)

    private fun vm(): NovedadesViewModel {
        val fuente = FuenteNovedadesFija(listOf(novedad("1.6"), novedad("1.5")))
        val caso = ComprobarNovedades(ajustes, fuente, VersionFija("1.6.0")) { false }
        return NovedadesViewModel(caso, play, CoroutineScope(UnconfinedTestDispatcher()))
    }

    @Test
    fun `espera a saber si Play tiene una actualizacion inmediata`() {
        play.inmediataEnCurso.value = null
        val vm = vm()

        vm.alLlegarARecetas(entradaDirecta = false)
        assertNull(vm.pendientes.value)

        play.inmediataEnCurso.value = false
        assertEquals(listOf("1.6"), vm.pendientes.value?.map { it.version })
    }

    @Test
    fun `con el flujo inmediato de Play en curso no enseña nada`() {
        play.inmediataEnCurso.value = true
        val vm = vm()

        vm.alLlegarARecetas(entradaDirecta = false)

        assertNull(vm.pendientes.value)
        assertEquals("1.5", ajustes.actual.value.novedadesVistas)
    }

    @Test
    fun `al llegar a Recetas expone las novedades pero aun no las marca`() {
        val vm = vm()

        vm.alLlegarARecetas(entradaDirecta = false)

        assertEquals(listOf("1.6"), vm.pendientes.value?.map { it.version })
        assertEquals("1.5", ajustes.actual.value.novedadesVistas)
    }

    @Test
    fun `marcarVistas se llama al mostrarse y no al cerrar`() {
        val vm = vm()
        vm.alLlegarARecetas(entradaDirecta = false)

        vm.alMostrarse()
        assertEquals("1.6", ajustes.actual.value.novedadesVistas)

        ajustes.actual.value = ajustes.actual.value.copy(novedadesVistas = "1.5")
        vm.cerrar()
        assertNull(vm.pendientes.value)
        assertEquals("1.5", ajustes.actual.value.novedadesVistas)
    }

    @Test
    fun `una entrada directa no enseña nada y no se vuelve a comprobar en el proceso`() {
        val vm = vm()

        vm.alLlegarARecetas(entradaDirecta = true)
        assertNull(vm.pendientes.value)

        vm.alLlegarARecetas(entradaDirecta = false)
        assertNull(vm.pendientes.value)
        assertEquals("1.5", ajustes.actual.value.novedadesVistas)
    }

    @Test
    fun `solo el slug valido de un destino receta es navegable`() {
        assertEquals("costillar-bbq", slugDeDestino("receta:costillar-bbq"))
        assertNull(slugDeDestino("recetas"))
        assertNull(slugDeDestino(null))
        assertNull(slugDeDestino("receta:../etc"))
    }

    @Test
    fun `el confeti no pasa de 60 piezas`() {
        val colores = listOf(Color.Red, Color.Blue)

        assertEquals(MAX_PARTICULAS, generarParticulas(500, colores).size)
        assertEquals(14, generarParticulas(14, colores).size)
        assertTrue(generarParticulas(10, emptyList()).isEmpty())
        assertTrue(DURACION_CONFETI_MS <= 3_000)
    }
}
