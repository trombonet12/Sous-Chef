package app.trombito.souschef.ui.tecnicas

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.receta.PasoTecnica
import app.trombito.souschef.receta.Tecnica
import app.trombito.souschef.ui.crono.BancoCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class TecnicasPantallaTest {
    private val todas =
        listOf(
            Tecnica(
                "b",
                "Reducción",
                "Concentrar salsas",
                "salsa",
                listOf(PasoTecnica("Hervir", "A fuego vivo"), PasoTecnica("Colar", "Con colador")),
            ),
            Tecnica("a", "Árbol de cortes", "Juliana", "corte"),
            Tecnica("d", "Sofrito", "Base de muchos guisos", "cocción"),
        )

    private fun pintar(
        disposicion: DisposicionLista,
        categoriaFija: CategoriaTecnica? = null,
        onAtras: (() -> Unit)? = null,
        onAbrir: (String) -> Unit = {},
        onCategoria: (String) -> Unit = {},
    ): BancoCompose {
        val banco = BancoCompose()
        val estado = construirEstadoTecnicas(todas, "", categoriaFija, disposicion)
        banco.poner { MaterialTheme { TecnicasContenido(estado, onAtras, {}, onAbrir, onCategoria) } }
        return banco
    }

    private fun BancoCompose.todosLosTextos(): List<String> = textos(raiz())

    @Test
    fun `la pestana pinta un cuadradito por categoria y el titulo de todas`() {
        val banco = pintar(DisposicionLista.TINTE)
        assertEquals(
            listOf("categoria-coccion", "categoria-corte", "categoria-salsa"),
            banco.etiquetas("categoria-"),
        )
        val textos = banco.todosLosTextos()
        assertTrue(textos.contains("Todas las técnicas"))
        assertTrue(textos.contains("Cocción"))
        assertTrue(textos.contains("1 técnica"))
    }

    @Test
    fun `tocar un cuadradito abre su categoria`() {
        val abiertas = mutableListOf<String>()
        val banco = pintar(DisposicionLista.TINTE, onCategoria = { abiertas += it })

        banco.pulsar(banco.conEtiqueta("categoria-salsa"))

        assertEquals(listOf("salsa"), abiertas)
    }

    @Test
    fun `las tres disposiciones pintan el titulo y tocar la tarjeta la abre`() {
        DisposicionLista.entries.forEach { disposicion ->
            val abiertas = mutableListOf<String>()
            val banco = pintar(disposicion, onAbrir = { abiertas += it })

            assertTrue(disposicion.name, banco.todosLosTextos().contains("Reducción"))
            banco.pulsar(banco.pulsableConTexto("Reducción"))
            assertEquals(disposicion.name, listOf("b"), abiertas)
        }
    }

    @Test
    fun `tinte lleva los pasos en el bloque y franja los junta con la categoria`() {
        assertTrue(pintar(DisposicionLista.TINTE).todosLosTextos().contains("2 pasos"))
        assertTrue(pintar(DisposicionLista.FRANJA).todosLosTextos().contains("Salsa · 2 pasos"))
    }

    @Test
    fun `con categoria fija no hay cuadraditos y el titulo es la categoria`() {
        val banco = pintar(DisposicionLista.TINTE, CategoriaTecnica.SALSA, onAtras = {})
        assertEquals(emptyList<String>(), banco.etiquetas("categoria-"))
        val textos = banco.todosLosTextos()
        assertTrue(textos.contains("Salsa"))
        assertTrue(textos.contains("Reducción"))
        assertTrue(!textos.contains("Sofrito"))
        assertTrue(!textos.contains("Todas las técnicas"))
    }
}
