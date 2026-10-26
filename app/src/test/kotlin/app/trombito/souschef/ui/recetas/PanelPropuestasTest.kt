package app.trombito.souschef.ui.recetas

import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.domain.MotivoPropuesta
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.domain.Propuesta
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import app.trombito.souschef.ui.receta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.time.LocalDate

/**
 * El chip «¿Qué cocino hoy?» y su panel flotante (un `Dialog`, otra ventana: se lee de todas las ventanas abiertas, no
 * solo de la actividad), y que la rejilla de la pestaña son solo las familias.
 */
@OptIn(InternalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PanelPropuestasTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val recetas =
        listOf(
            receta("a", "Macarrones", base = "pasta"),
            receta("b", "Risotto", base = "arroz"),
            receta("c", "Tortilla", base = "patata"),
        )

    private fun propuesta(
        slug: String,
        motivo: MotivoPropuesta,
    ) = Propuesta(recetas.first { it.slug == slug }, motivo, 14 * 60 + 30)

    private val tres =
        listOf(
            propuesta("a", MotivoPropuesta.SinEstrenar),
            propuesta("b", MotivoPropuesta.QuieresHacerla),
            propuesta("c", MotivoPropuesta.LlegaParaCenar),
        )

    private fun estado(
        todas: List<Receta> = recetas,
        propuestas: List<Propuesta> = tres,
    ) = construirEstadoRecetas(
        todas,
        emptyMap(),
        emptyMap(),
        FiltroRecetas(orden = OrdenRecetas.ALFABETICO),
        ContextoLista(LocalDate.of(2026, 10, 14)),
    ).copy(propuestas = propuestas)

    private class Registro {
        val abiertas = mutableListOf<String>()
        var otras = 0
        var vistas = 0
    }

    private fun pintar(
        estado: RecetasUiState,
        registro: Registro = Registro(),
    ): BancoCompose {
        val banco = BancoCompose()
        val lista =
            AccionesLista(
                favorita = { _, _ -> },
                abrir = { registro.abiertas += it },
                otra = { registro.otras++ },
                vistas = { registro.vistas++ },
            )
        banco.poner { MaterialTheme { RecetasContenido(estado, {}, AccionesFiltro({}, {}, {}), lista) } }
        return banco
    }

    // Todas las ventanas abiertas (la actividad y los diálogos), cada una con su árbol de semántica sin fusionar.

    @Suppress("UNCHECKED_CAST")
    private fun vistas(): List<View> {
        val clase = Class.forName("android.view.WindowManagerGlobal")
        val global = clase.getMethod("getInstance").invoke(null)
        val campo = clase.getDeclaredField("mViews").apply { isAccessible = true }
        return campo.get(global) as List<View>
    }

    private fun buscar(vista: View): RootForTest? =
        when {
            vista is RootForTest -> vista
            vista is ViewGroup -> (0 until vista.childCount).firstNotNullOfOrNull { buscar(vista.getChildAt(it)) }
            else -> null
        }

    private fun aplanar(nodo: SemanticsNode): List<SemanticsNode> = listOf(nodo) + nodo.children.flatMap(::aplanar)

    private fun todos(): List<SemanticsNode> =
        vistas()
            .mapNotNull { buscar(it) }
            .distinct()
            .flatMap { aplanar(it.semanticsOwner.unmergedRootSemanticsNode) }

    private fun etiqueta(nodo: SemanticsNode) = nodo.config.getOrNull(SemanticsProperties.TestTag)

    private fun nodoConEtiqueta(nombre: String) = todos().firstOrNull { etiqueta(it) == nombre }

    private fun hayPanel() = nodoConEtiqueta("panel-propuestas") != null

    private fun textos(): List<String> = todos().flatMap { n -> textosDe(n) }

    private fun textosDe(nodo: SemanticsNode): List<String> {
        val textos = nodo.config.getOrNull(SemanticsProperties.Text) ?: return emptyList()
        return textos.map { it.text }
    }

    private fun descripciones(): List<String> =
        todos().flatMap { n -> n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }

    private fun BancoCompose.pulsarEtiqueta(nombre: String) = pulsar(checkNotNull(nodoConEtiqueta(nombre)) { nombre })

    // Tests

    @Test
    fun `el chip abre el panel y la tarjeta fija de arriba ya no existe`() {
        val banco = pintar(estado())
        assertTrue(textos().contains("¿Qué cocino hoy?"))
        assertFalse(hayPanel())
        assertTrue(banco.etiquetas("propuesta-").isEmpty())
        assertTrue(banco.etiquetas("propuestas").isEmpty())

        banco.pulsarEtiqueta("chip-propuestas")

        assertTrue(hayPanel())
        assertTrue(textos().containsAll(listOf("Macarrones", "Risotto", "Tortilla")))
        assertTrue(textos().any { it.contains("empieza 14:30") })
        val titulo = checkNotNull(nodoConEtiqueta("panel-propuestas"))
        assertEquals("¿Qué cocino hoy?", titulo.config.getOrNull(SemanticsProperties.PaneTitle))
    }

    @Test
    fun `el chip sale junto al de que me da tiempo y solo en la pestana`() {
        val pestana = pintar(estado())
        assertNotNull(nodoConEtiqueta("chip-propuestas"))
        assertNotNull(nodoConEtiqueta("chip-tiempo"))
        assertEquals(1, pestana.etiquetas("chip-propuestas").size)

        val familia = pintar(estado().copy(familiaFija = FamiliaBase.PASTA))
        assertTrue(familia.etiquetas("chip-propuestas").isEmpty())
    }

    @Test
    fun `abrir el panel avisa de las propuestas vistas y pintar la lista no`() {
        val registro = Registro()
        val banco = pintar(estado(), registro)
        assertEquals(0, registro.vistas)

        banco.pulsarEtiqueta("chip-propuestas")

        assertTrue(registro.vistas >= 1)
    }

    @Test
    fun `el icono de otras propuestas llama a otra y deja el panel abierto`() {
        val registro = Registro()
        val banco = pintar(estado(), registro)
        banco.pulsarEtiqueta("chip-propuestas")
        assertTrue(descripciones().contains("Otras propuestas"))
        assertFalse(textos().contains("Otra"))

        banco.pulsarEtiqueta("propuestas-otra")

        assertEquals(1, registro.otras)
        assertTrue(hayPanel())
    }

    @Test
    fun `tocar una propuesta abre su ficha y cierra el panel`() {
        val registro = Registro()
        val banco = pintar(estado(), registro)
        banco.pulsarEtiqueta("chip-propuestas")

        banco.pulsarEtiqueta("propuesta-b")

        assertEquals(listOf("b"), registro.abiertas)
        assertFalse(hayPanel())
    }

    @Suppress("DEPRECATION") // Dialog.onBackPressed: es el gesto Atrás del diálogo
    @Test
    fun `Atras cierra el panel sin abrir nada`() {
        val registro = Registro()
        val banco = pintar(estado(), registro)
        banco.pulsarEtiqueta("chip-propuestas")
        assertTrue(hayPanel())

        checkNotNull(ShadowDialog.getLatestDialog()).onBackPressed()
        banco.reposo()

        assertFalse(hayPanel())
        assertTrue(registro.abiertas.isEmpty())
    }

    @Test
    fun `sin propuestas el panel lo dice y el icono de otras sigue`() {
        val banco = pintar(estado(propuestas = emptyList()))
        banco.pulsarEtiqueta("chip-propuestas")

        assertTrue(hayPanel())
        assertTrue(textos().contains("Hoy no hay nada que llegue a tiempo"))
        assertNotNull(nodoConEtiqueta("propuestas-otra"))
    }

    @Test
    fun `la rejilla de la pestana son las nueve familias en tres por tres, sin cenas rapidas`() {
        val bases =
            listOf("pasta", "arroz", "patata", "pescado", "ensalada", "hojaldre", "salteado", "asado", "postre")
        val todas = bases.mapIndexed { i, base -> receta("r$i", "Receta $i", base = base) }
        val banco = pintar(estado(todas, propuestas = emptyList()))

        val cuadraditos = banco.etiquetas("categoria-")
        assertEquals(9, cuadraditos.size)
        assertEquals(
            listOf("pasta", "arroz", "patata", "pescado", "verdura", "masas", "salteado", "carnes", "postres"),
            cuadraditos.map { it.removePrefix("categoria-") },
        )
        assertFalse(cuadraditos.contains("categoria-cenas-rapidas"))
        assertFalse(banco.textos(banco.raiz()).contains("Cenas rápidas"))
        // Tres columnas: los de una fila comparten la parte de arriba y cada fila empieza más abajo que la anterior.
        val arriba = cuadraditos.map { banco.conEtiqueta(it).boundsInRoot.top }
        assertEquals(arriba[0], arriba[2], 1f)
        assertEquals(arriba[3], arriba[5], 1f)
        assertTrue(arriba[3] > arriba[0])
        assertTrue(arriba[6] > arriba[3])
    }
}
