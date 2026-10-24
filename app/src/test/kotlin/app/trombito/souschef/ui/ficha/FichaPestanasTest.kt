package app.trombito.souschef.ui.ficha

import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.receta.BloqueMep
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Corte
import app.trombito.souschef.receta.Grupo
import app.trombito.souschef.receta.Hito
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
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

/** La ficha en capas en la JVM: pestañas, filtro de pasos con su hoja, detalle de la preparación y raciones fijas. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-xxhdpi")
class FichaPestanasTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val receta =
        Receta(
            slug = "sopa",
            titulo = "Sopa",
            descripcion = "Una sopa de las de antes",
            raciones = 2,
            totalMin = 20.0,
            carriles =
                listOf(
                    Carril("f0", nombre = "SARTÉN", color = "#E4572E", tipo = "fuego"),
                    Carril("m", nombre = "MESA", color = "#3E8FD1", tipo = "mesa"),
                ),
            grupos = listOf(Grupo("Base", listOf(Ingrediente("Cebolla", a = 100.0, u = "g")))),
            notaEscala = "La sal no sube igual",
            pasos =
                listOf(
                    Paso(
                        "p1",
                        "Sofrito",
                        "f0",
                        0.0,
                        8.0,
                        f = 5,
                        tapa = "PUESTA",
                        nota = "Remueve",
                        hitos = listOf(Hito(150.0, "Dale la vuelta")),
                        meto = "Cebolla",
                        saco = "Nada",
                        p = "Hasta que esté transparente",
                    ),
                    Paso("p2", "Picar", "m", 0.0, 4.0, p = "Cubos pequeños"),
                    Paso("p3", "Emplatar", "m", 8.0, 2.0),
                ),
            mep =
                listOf(
                    BloqueMep(
                        "Cortar",
                        l = listOf("<b>Cebolla</b> · picada fina, en cubos de medio centímetro minuto 3.", "Pan"),
                        porq = "Para que se haga a la vez",
                    ),
                ),
            cortes = listOf(Corte("Brunoise", p = "Cubos de tres milímetros")),
            porque = listOf(listOf("Por qué tapada", "Guarda el vapor")),
            pie = "Pie de la ficha",
        )

    private fun estado(permitidas: List<Int> = listOf(2, 4)) =
        FichaUiState(cargando = false, receta = receta, racionesPermitidas = permitidas)

    private fun pintar(estado: FichaUiState = estado()): BancoFicha {
        val banco = BancoFicha()
        banco.poner {
            MaterialTheme {
                FichaContenido(estado, AccionesFicha({}, {}, { _, _ -> }, {}, {}, {}))
            }
        }
        return banco
    }

    private fun BancoFicha.cuerpo() = etiquetas("ficha-").filter { !it.startsWith("ficha-pestana-") }

    private fun BancoFicha.ir(pestana: String) = pulsar(conEtiqueta("ficha-pestana-$pestana"))

    @Test
    fun `hay cinco pestanas y cada una cambia el contenido`() {
        val banco = pintar()
        PestanaFicha.entries.forEach { banco.conEtiqueta("ficha-pestana-${it.name.lowercase()}") }
        assertEquals(listOf("ficha-resumen"), banco.cuerpo())
        assertTrue(banco.textos().contains("Una sopa de las de antes"))

        banco.ir("ingredientes")
        assertEquals(listOf("ficha-ingredientes"), banco.cuerpo())
        assertTrue(banco.textos().toString(), banco.textos().contains("Cebolla"))

        banco.ir("preparacion")
        assertEquals(listOf("ficha-preparacion"), banco.cuerpo())
        assertTrue(banco.textos().contains("Cortar"))

        banco.ir("pasos")
        assertTrue(banco.cuerpo().toString(), "ficha-pasos" in banco.cuerpo())

        banco.ir("porque")
        assertEquals(listOf("ficha-porque"), banco.cuerpo())
        assertTrue(banco.textos().contains("Brunoise"))
        assertTrue(banco.textos().contains("Pie de la ficha"))
    }

    @Test
    fun `el filtro por carril reduce la lista y tocar un paso abre la hoja con el p`() {
        val banco = pintar()
        banco.ir("pasos")
        assertEquals(listOf("ficha-paso-p1", "ficha-paso-p2", "ficha-paso-p3"), banco.etiquetas("ficha-paso-"))

        banco.pulsar(banco.conEtiqueta("ficha-filtro-m"))
        assertEquals(listOf("ficha-paso-p2", "ficha-paso-p3"), banco.etiquetas("ficha-paso-"))

        banco.pulsar(banco.conEtiqueta("ficha-filtro-todos"))
        assertEquals(3, banco.etiquetas("ficha-paso-").size)

        assertFalse(banco.textos().contains("Hasta que esté transparente"))
        banco.pulsar(banco.conEtiqueta("ficha-paso-p1"))
        val textos = banco.textos()
        assertTrue(textos.toString(), textos.contains("Hasta que esté transparente"))
        assertTrue(textos.toString(), textos.contains("+2:30 Dale la vuelta"))
        assertTrue(textos.toString(), textos.contains("Entra: Cebolla"))
        assertTrue(textos.toString(), textos.contains("Sale: Nada"))
    }

    @Test
    fun `tocar una fila de la preparacion muestra su detalle`() {
        val banco = pintar()
        banco.ir("preparacion")
        val detalle = "picada fina, en cubos de medio centímetro minuto 3."
        assertFalse(banco.textos().contains(detalle))
        assertTrue(banco.textos().contains("min 3"))

        banco.pulsar(banco.pulsableConTexto("Cebolla"))

        assertTrue(banco.textos().toString(), banco.textos().contains(detalle))
    }

    @Test
    fun `con raciones fijas no hay stepper y con rango si`() {
        assertFalse(pintar(estado(listOf(2))).descripciones().contains("Una ración más"))
        assertTrue(pintar(estado(listOf(2, 4))).descripciones().contains("Una ración más"))
    }

    @Test
    fun `la cabecera sigue a la vista al cambiar de pestana`() {
        val banco = pintar()
        banco.ir("porque")
        assertTrue(banco.textos().contains("Servir a"))
        assertTrue(banco.textos().contains("Empezar a cocinar"))
    }
}

/** Como [BancoCompose], pero también lee las ventanas aparte (la hoja del paso). */
@OptIn(InternalComposeUiApi::class)
class BancoFicha {
    private val base = BancoCompose()

    fun poner(contenido: @Composable () -> Unit) = base.poner(contenido)

    fun pulsar(nodo: SemanticsNode) = base.pulsar(nodo)

    fun pulsableConTexto(texto: String) = base.pulsableConTexto(texto)

    fun conEtiqueta(etiqueta: String) = base.conEtiqueta(etiqueta)

    fun etiquetas(prefijo: String) = base.etiquetas(prefijo)

    /** Todo el texto visible, también el de ventanas aparte. */
    fun textos(): List<String> = todos().flatMap { base.textos(it) }

    fun descripciones(): List<String> = todos().flatMap { descripcionesDe(it) }

    private fun descripcionesDe(nodo: SemanticsNode): List<String> =
        nodo.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            nodo.children.flatMap { descripcionesDe(it) }

    private fun todos(): List<SemanticsNode> =
        vistas().mapNotNull { buscar(it) }.distinct().map { it.semanticsOwner.unmergedRootSemanticsNode }

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
}
