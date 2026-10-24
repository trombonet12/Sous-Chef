package app.trombito.souschef.ui.novedades

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.domain.LineaNovedad
import app.trombito.souschef.domain.Novedades
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class DialogoNovedadesTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val banco = BancoCompose()

    private val v16 =
        Novedades(
            "1.6",
            "Sous Chef 1.6 ya está en tu cocina",
            listOf(
                LineaNovedad("Seis recetas nuevas", "recetas"),
                LineaNovedad("Receta del mes", "receta:costillar-bbq"),
                LineaNovedad("Cronómetro más claro"),
            ),
        )
    private val v15 =
        Novedades("1.5", "Sous Chef 1.5 ya está en tu cocina", listOf(LineaNovedad("Comparte ingredientes")))

    private fun poner(
        lista: List<Novedades>,
        confeti: Boolean = true,
        onCerrar: () -> Unit = {},
        onReceta: (String) -> Unit = {},
    ) = banco.poner { MaterialTheme { ContenidoNovedades(lista, confeti, onCerrar, onReceta) } }

    @Test
    fun `con dos versiones sale el titulo nuevo y la anterior queda plegada`() {
        poner(listOf(v16, v15))

        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "Sous Chef 1.6 ya está en tu cocina" in textos)
        assertTrue(textos.toString(), "Seis recetas nuevas" in textos)
        assertTrue(textos.toString(), "También en 1.5" in textos)
        assertFalse(textos.toString(), "Comparte ingredientes" in textos)
        assertFalse(textos.toString(), "Sous Chef 1.5 ya está en tu cocina" in textos)
    }

    @Test
    fun `tocar tambien en despliega las lineas anteriores`() {
        poner(listOf(v16, v15))

        banco.pulsar(banco.conEtiqueta("novedades-tambien-1.5"))

        assertTrue(banco.textos(banco.raiz()).toString(), "Comparte ingredientes" in banco.textos(banco.raiz()))
    }

    @Test
    fun `A cocinar cierra`() {
        var cerrado = 0
        poner(listOf(v16), onCerrar = { cerrado++ })

        banco.pulsar(banco.pulsableConTexto("A cocinar"))

        assertEquals(1, cerrado)
    }

    @Test
    fun `una linea con destino receta abre la ficha y recetas no navega`() {
        val abiertas = mutableListOf<String>()
        poner(listOf(v16), onReceta = { abiertas += it })

        banco.pulsar(banco.pulsableConTexto("Receta del mes"))
        val raiz = banco.raiz()
        val sinNavegar = banco.nodos().filter { "Seis recetas nuevas" in banco.textos(it) }
        sinNavegar.forEach { assertFalse(it.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick)) }

        assertEquals(listOf("costillar-bbq"), abiertas)
        assertTrue("Cronómetro más claro" in banco.textos(raiz))
    }

    @Test
    fun `el titulo es encabezado y el panel lleva su titulo`() {
        poner(listOf(v16))

        val titulo = banco.conEtiqueta("novedades-titulo")
        assertTrue(titulo.config.contains(SemanticsProperties.Heading))
        val panel = banco.nodos().mapNotNull { it.config.getOrNull(SemanticsProperties.PaneTitle) }
        assertEquals(listOf("Sous Chef 1.6 ya está en tu cocina"), panel)
    }

    @Test
    fun `el confeti no tiene semantica`() {
        poner(listOf(v16), confeti = true)

        val confeti = banco.conEtiqueta("novedades-confeti")
        assertTrue(confeti.config.isClearingSemantics)
        assertTrue(confeti.children.isEmpty())
        assertTrue(banco.textos(confeti).isEmpty())
        assertEquals(null, confeti.config.getOrNull(SemanticsProperties.ContentDescription))
    }

    @Test
    fun `sin confeti no se pinta la capa`() {
        poner(listOf(v16), confeti = false)

        assertTrue(banco.etiquetas("novedades-confeti").isEmpty())
    }
}
