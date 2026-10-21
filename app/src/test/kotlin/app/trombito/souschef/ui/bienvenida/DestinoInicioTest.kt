package app.trombito.souschef.ui.bienvenida

import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.shell.Pestana
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DestinoInicioTest {
    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun `en el primer arranque sale la intro`() {
        val vm = DestinoInicioViewModel(FakeAjustesRepo())

        assertEquals(Rutas.BIENVENIDA, vm.destino.value)
    }

    @Test
    fun `con la intro vista se abre en Recetas`() {
        val repo = FakeAjustesRepo().apply { actual.value = Ajustes(introVista = true) }

        val vm = DestinoInicioViewModel(repo)

        assertEquals(Pestana.Recetas.ruta, vm.destino.value)
    }

    @Test
    fun `el destino no cambia al guardar un ajuste en mitad de la intro`() =
        runTest {
            val repo = FakeAjustesRepo()
            val vm = DestinoInicioViewModel(repo)
            assertEquals(Rutas.BIENVENIDA, vm.destino.value)

            // La intro marca "no vista" al entrar y el usuario elige cosas por el camino.
            repo.marcarIntroVista(false)
            repo.fijarTema(TemaApp.OSCURO)
            assertEquals(Rutas.BIENVENIDA, vm.destino.value)

            // Ni siquiera al acabar (la navegación a Recetas la hace la intro, no el destino de inicio).
            repo.marcarIntroVista(true)
            assertEquals(Rutas.BIENVENIDA, vm.destino.value)
        }

    @Test
    fun `la funcion pura decide por introVista`() {
        assertEquals(Rutas.BIENVENIDA, destinoInicial(Ajustes(introVista = false)))
        assertEquals(Pestana.Recetas.ruta, destinoInicial(Ajustes(introVista = true)))
    }
}
