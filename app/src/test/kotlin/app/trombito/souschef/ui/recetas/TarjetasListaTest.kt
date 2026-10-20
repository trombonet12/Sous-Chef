package app.trombito.souschef.ui.recetas

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
import app.trombito.souschef.ui.crono.BancoCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class TarjetasListaTest {
    private val lasana =
        Receta(
            slug = "lasana",
            titulo = "Lasaña de carne",
            descripcion = "Capas de pasta, boloñesa y bechamel gratinadas al horno.",
            totalMin = 47.0,
            tags = Tags(base = "pasta", proteina = "pollo"),
            carriles = listOf(Carril("f1", tipo = "fuego"), Carril("f2", tipo = "fuego"), Carril("h", tipo = "horno")),
            pasos = listOf(Paso("p1", "Hacer la salsa", "f1", 0.0, 10.0)),
        )
    private val tarjeta =
        TarjetaReceta(
            receta = lasana,
            favorita = false,
            fuegos = 2,
            ultimaFecha = "2026-09-01",
            hace = Hace(UnidadHace.MESES, 1),
            conHorno = true,
            hora = 13 * 60 + 19,
        )

    private fun pintar(
        disposicion: DisposicionLista,
        onFavorita: () -> Unit = {},
        onAbrir: () -> Unit = {},
    ): BancoCompose {
        val banco = BancoCompose()
        banco.poner { MaterialTheme { TarjetaLista(tarjeta, disposicion, onFavorita, onAbrir) } }
        return banco
    }

    private fun BancoCompose.todosLosTextos(): List<String> = textos(raiz())

    private fun BancoCompose.descripciones(): List<String> =
        nodos().flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }

    @Test
    fun `tinte muestra tiempo, hora de empezar, base y proteina, fuegos y horno`() {
        val banco = pintar(DisposicionLista.TINTE)
        val textos = banco.todosLosTextos()
        assertTrue(textos.contains("Lasaña de carne"))
        assertTrue(textos.contains("47"))
        assertTrue(textos.contains("13:19"))
        assertTrue(textos.contains("Pasta · pollo"))
        assertTrue(textos.contains("2 fuegos · horno · hace 1 mes"))
        assertTrue(textos.contains(lasana.descripcion))
        assertTrue(banco.descripciones().contains("Marcar Lasaña de carne como favorita"))
    }

    @Test
    fun `franja junta tiempo y hora de empezar y el detalle en una linea`() {
        val banco = pintar(DisposicionLista.FRANJA)
        val textos = banco.todosLosTextos()
        assertTrue(textos.contains("Lasaña de carne"))
        assertTrue(textos.contains("47 min · empieza 13:19"))
        assertTrue(textos.contains("Pasta · pollo · 2 fuegos · horno · hace 1 mes"))
        assertTrue(textos.contains(lasana.descripcion))
        assertTrue(banco.descripciones().contains("Marcar Lasaña de carne como favorita"))
    }

    @Test
    fun `clasica conserva su pie con la hora de empezar`() {
        val banco = pintar(DisposicionLista.CLASICA)
        val textos = banco.todosLosTextos()
        assertTrue(textos.contains("Lasaña de carne"))
        assertTrue(textos.contains(lasana.descripcion))
        assertTrue(textos.contains("47 min · empieza 13:19 · 2 fuegos · hace 1 mes"))
        assertTrue(banco.descripciones().contains("Marcar Lasaña de carne como favorita"))
    }

    @Test
    fun `tocar la tarjeta la abre y tocar la estrella solo marca la favorita`() {
        DisposicionLista.entries.forEach { disposicion ->
            var abiertas = 0
            var favoritas = 0
            val banco = pintar(disposicion, { favoritas++ }, { abiertas++ })

            banco.pulsar(banco.pulsableConTexto("Lasaña de carne"))
            assertEquals(disposicion.name, 1, abiertas)
            assertEquals(disposicion.name, 0, favoritas)

            banco.pulsar(banco.estrella())
            assertEquals(disposicion.name, 1, abiertas)
            assertEquals(disposicion.name, 1, favoritas)
        }
    }

    /** El botón de la estrella: el último nodo pulsable (el más profundo) que contiene su descripción. */
    private fun BancoCompose.estrella(): SemanticsNode =
        nodos().last { nodo -> nodo.config.contains(SemanticsActions.OnClick) && contieneEstrella(nodo) }

    private fun contieneEstrella(nodo: SemanticsNode): Boolean =
        nodo.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.contains("favorita") } == true ||
            nodo.children.any(::contieneEstrella)

    @Test
    fun `la proteina ninguna no se escribe y la primera letra va en mayuscula`() {
        assertEquals("Arroz", baseYProteina(Tags(base = "arroz", proteina = "ninguna")))
        assertEquals("Arroz · huevo", baseYProteina(Tags(base = "arroz", proteina = "huevo")))
        assertEquals("Pollo", baseYProteina(Tags(base = null, proteina = "pollo")))
        assertEquals("", baseYProteina(Tags()))
    }

    @Test
    fun `en modo acabas la franja dice acabas`() {
        val banco = BancoCompose()
        val acabas = tarjeta.copy(hora = 16 * 60 + 2, modoHora = HoraTarjeta.ACABAR)
        banco.poner { MaterialTheme { TarjetaLista(acabas, DisposicionLista.FRANJA, {}, {}) } }
        assertTrue(banco.todosLosTextos().contains("47 min · acabas 16:02"))
    }
}
