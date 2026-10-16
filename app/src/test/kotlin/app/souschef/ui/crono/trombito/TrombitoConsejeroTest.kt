package app.souschef.ui.crono.trombito

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.souschef.receta.crono.Consejo
import app.souschef.receta.crono.TipoConsejo
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Render sin la librería de test de Compose: se leen las semánticas del árbol ya compuesto. */
@RunWith(RobolectricTestRunner::class)
class TrombitoConsejeroTest {
    private fun textos(consejo: Consejo?): List<String> {
        val actividad = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        actividad.setContent { TrombitoConsejero(consejo) }
        shadowOf(android.os.Looper.getMainLooper()).idle()
        val raiz = actividad.window.decorView.buscaRaiz() ?: error("sin árbol de Compose")
        val salida = mutableListOf<String>()

        fun recorre(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { salida += it.text }
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.let { salida += it }
            n.children.forEach(::recorre)
        }
        recorre(raiz.semanticsOwner.unmergedRootSemanticsNode)
        return salida
    }

    private fun View.buscaRaiz(): RootForTest? =
        when {
            this is RootForTest -> this
            this is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).buscaRaiz() }
            else -> null
        }

    @Test
    fun muestraLaMascotaYElTextoDelConsejo() {
        val t = textos(Consejo("En nada toca: Añadir arroz", TipoConsejo.PRONTO))
        assertTrue(t.toString(), t.any { it.contains("Trombito chef") })
        assertTrue(t.toString(), "En nada toca: Añadir arroz" in t)
        assertTrue(t.toString(), "Cerrar consejo" in t)
    }

    @Test
    fun sinConsejoSoloHayMascota() {
        val t = textos(null)
        assertTrue(t.toString(), t.any { it.contains("Trombito chef") })
        assertTrue(t.toString(), "Cerrar consejo" !in t)
    }
}
