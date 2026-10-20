package app.trombito.souschef.ui.tecnicas

import androidx.lifecycle.SavedStateHandle
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.receta.PasoTecnica
import app.trombito.souschef.receta.Tecnica
import app.trombito.souschef.ui.FakeTecnicasRepo
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.comun.TonoPaleta
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.receta
import app.trombito.souschef.ui.shell.Icono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TecnicasTest {
    private val todas =
        listOf(
            Tecnica("b", "Reducción", "Concentrar salsas", "salsa", listOf(PasoTecnica("Hervir", "A fuego vivo"))),
            Tecnica("a", "Árbol de cortes", "Juliana", "corte", consejos = listOf("Cuchillo afilado")),
            Tecnica("c", "Glaseado", "Brillo", "salsa"),
            Tecnica("d", "Sofrito", "Base", "cocción"),
        )

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    @Test
    fun ordenAlfabeticoSinAcentos() {
        assertEquals(listOf("a", "c", "b", "d"), construirEstadoTecnicas(todas, "", null).tecnicas.map { it.slug })
    }

    @Test
    fun buscaEnPasosYConsejos() {
        assertEquals(listOf("b"), construirEstadoTecnicas(todas, "fuego VIVO", null).tecnicas.map { it.slug })
        assertEquals(listOf("a"), construirEstadoTecnicas(todas, "afilado", null).tecnicas.map { it.slug })
    }

    @Test
    fun `la categoria se reconoce sin tildes ni mayusculas y la desconocida es otra`() {
        assertEquals(CategoriaTecnica.COCCION, CategoriaTecnica.de("cocción"))
        assertEquals(CategoriaTecnica.COCCION, CategoriaTecnica.de("Coccion"))
        assertEquals(CategoriaTecnica.SALSA, CategoriaTecnica.de(" SALSA "))
        assertEquals(CategoriaTecnica.OTRA, CategoriaTecnica.de("horneado"))
        assertEquals(CategoriaTecnica.OTRA, CategoriaTecnica.de(""))
    }

    @Test
    fun `cada categoria tiene su tono y su icono`() {
        assertEquals(TonoPaleta.AZUL, CategoriaTecnica.COCCION.tono)
        assertEquals(TonoPaleta.AMBAR, CategoriaTecnica.SALSA.tono)
        assertEquals(TonoPaleta.PIEDRA, CategoriaTecnica.OTRA.tono)
        assertEquals(Icono.Coccion, CategoriaTecnica.COCCION.icono)
        assertEquals(Icono.Plato, CategoriaTecnica.OTRA.icono)
        CategoriaTecnica.entries.forEach { c -> assertEquals(c.name, c, CategoriaTecnica.de(c.name)) }
    }

    @Test
    fun `las categorias salen en el orden del enum con su recuento sobre todas las tecnicas`() {
        val e = construirEstadoTecnicas(todas, "glaseado", null)
        assertEquals(
            listOf(CategoriaTecnica.COCCION to 1, CategoriaTecnica.CORTE to 1, CategoriaTecnica.SALSA to 2),
            e.categorias,
        )
        assertEquals(listOf("c"), e.tecnicas.map { it.slug })
    }

    @Test
    fun `el nombre visible es la cadena de los datos con mayuscula inicial`() {
        val e = construirEstadoTecnicas(todas, "", null)
        assertEquals("Cocción", e.nombres[CategoriaTecnica.COCCION])
        assertEquals("Salsa", e.nombres[CategoriaTecnica.SALSA])
        assertNull(e.nombres[CategoriaTecnica.OTRA])
    }

    @Test
    fun `la categoria fija filtra antes del buscador`() {
        val salsas = construirEstadoTecnicas(todas, "", CategoriaTecnica.SALSA)
        assertEquals(listOf("c", "b"), salsas.tecnicas.map { it.slug })
        assertEquals(CategoriaTecnica.SALSA, salsas.categoriaFija)
        assertEquals(2, salsas.total)
        val sinJuliana = construirEstadoTecnicas(todas, "juliana", CategoriaTecnica.SALSA)
        assertEquals(emptyList<String>(), sinJuliana.tecnicas.map { it.slug })
        val hervir = construirEstadoTecnicas(todas, "hervir", CategoriaTecnica.SALSA)
        assertEquals(listOf("b"), hervir.tecnicas.map { it.slug })
    }

    @Test
    fun `el viewmodel lee la categoria de la ruta y la disposicion de ajustes`() =
        runTest(UnconfinedTestDispatcher()) {
            val ajustes = FakeAjustesRepo()
            ajustes.actual.value = ajustes.actual.value.copy(lista = DisposicionLista.FRANJA)
            val vm =
                TecnicasViewModel(
                    SavedStateHandle(mapOf(Rutas.ARG_CATEGORIA to "coccion")),
                    FakeTecnicasRepo(todas),
                    ajustes,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            val e = vm.estado.value
            assertEquals(CategoriaTecnica.COCCION, e.categoriaFija)
            assertEquals(listOf("d"), e.tecnicas.map { it.slug })
            assertEquals(DisposicionLista.FRANJA, e.disposicion)
        }

    @Test
    fun `sin argumento la pestana no tiene categoria fija`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = TecnicasViewModel(SavedStateHandle(), FakeTecnicasRepo(todas), FakeAjustesRepo())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            val e = vm.estado.value
            assertNull(e.categoriaFija)
            assertEquals(4, e.tecnicas.size)
            assertEquals(DisposicionLista.TINTE, e.disposicion)
        }

    @Test
    fun enlacesInversosDeRecetasPorTitulo() {
        val rs =
            listOf(
                receta("z", "Zarzuela", tecnicas = listOf("x")),
                receta("m", "Migas", tecnicas = listOf("x", "y")),
                receta("n", "Nada"),
            )
        assertEquals(listOf("m", "z"), recetasConTecnica("x", rs).map { it.slug })
    }
}
