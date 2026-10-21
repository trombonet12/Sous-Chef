package app.trombito.souschef.ui.recetas

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import app.trombito.souschef.ui.receta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RecetasPantallaTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val recetas =
        listOf(
            receta("a", "Macarrones", base = "pasta"),
            receta("b", "Lasaña", base = "pasta"),
            receta("c", "Risotto", base = "arroz"),
        )

    private fun estado(familia: FamiliaBase? = null) =
        construirEstadoRecetas(
            recetas,
            emptyMap(),
            emptyMap(),
            FiltroRecetas(familiaFija = familia),
            ContextoLista(LocalDate.of(2026, 10, 14)),
        ).copy(orden = OrdenRecetas.ALFABETICO)

    private fun pintar(
        estado: RecetasUiState,
        onCategoria: (String) -> Unit = {},
        onAtras: () -> Unit = {},
    ): BancoCompose {
        val banco = BancoCompose()
        val acciones = AccionesFiltro({}, {}, {})
        banco.poner {
            MaterialTheme {
                RecetasContenido(
                    estado,
                    {},
                    acciones,
                    AccionesLista({ _, _ -> }, {}, onCategoria, onAtras),
                )
            }
        }
        return banco
    }

    private fun BancoCompose.buscadorEstaSobre(etiqueta: String): Boolean {
        val buscador = nodos().first { it.config.contains(SemanticsProperties.EditableText) }
        return buscador.boundsInRoot.top < conEtiqueta(etiqueta).boundsInRoot.top
    }

    @Test
    fun `la pestana pinta buscador, cuadraditos y tarjetas, sin titulo de la lista completa`() {
        val banco = pintar(estado())
        assertEquals(listOf("categoria-pasta", "categoria-arroz"), banco.etiquetas("categoria-"))
        assertTrue(banco.buscadorEstaSobre("categoria-pasta"))
        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), !textos.contains("Todas las recetas"))
        assertTrue(textos.toString(), textos.contains("Macarrones"))
    }

    @Test
    fun `tocar un cuadradito llama a onCategoria con el nombre de la familia en minusculas`() {
        val abiertas = mutableListOf<String>()
        val banco = pintar(estado(), onCategoria = { abiertas += it })

        banco.pulsar(banco.conEtiqueta("categoria-arroz"))

        assertEquals(listOf("arroz"), abiertas)
    }

    @Test
    fun `al buscar desaparecen los cuadraditos`() {
        val buscando = estado().copy(consulta = "risotto")
        assertTrue(pintar(buscando).etiquetas("categoria-").isEmpty())
    }

    @Test
    fun `con una sola familia no hay cuadraditos`() {
        val banco = pintar(estado().copy(categorias = estado().categorias.take(1)))
        assertTrue(banco.etiquetas("categoria-").isEmpty())
    }

    @Test
    fun `con familia fija hay titulo con la familia y sin cuadraditos ni titulo de todas`() {
        var atras = 0
        val banco = pintar(estado(FamiliaBase.PASTA), onAtras = { atras++ })
        assertTrue(banco.etiquetas("categoria-").isEmpty())
        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), textos.contains("Pasta"))
        assertTrue(textos.toString(), textos.contains("Lasaña"))
        assertTrue(textos.toString(), !textos.contains("Todas las recetas"))
        assertTrue(textos.toString(), !textos.contains("Risotto"))
        assertEquals(0, atras)
    }
}
