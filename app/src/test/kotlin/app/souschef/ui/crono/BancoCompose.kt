package app.souschef.ui.crono

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Banco mínimo para probar composables en la JVM con Robolectric sin `ui-test`: monta el contenido en una actividad,
 * deja pasar los fotogramas y lee el árbol de semántica sin fusionar.
 */
@OptIn(InternalComposeUiApi::class)
class BancoCompose {
    private val controlador = Robolectric.buildActivity(ComponentActivity::class.java).setup()

    fun poner(contenido: @Composable () -> Unit) {
        controlador.get().setContent(content = contenido)
        reposo()
    }

    /** Deja correr composición, medida y dibujo pendientes. */
    fun reposo() {
        repeat(FOTOGRAMAS) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(MS_FOTOGRAMA)) }
    }

    fun raiz(): SemanticsNode {
        val vista = buscarRaiz(controlador.get().window.decorView)
        return checkNotNull(vista) { "No hay contenido Compose" }.semanticsOwner.unmergedRootSemanticsNode
    }

    fun nodos(): List<SemanticsNode> = aplanar(raiz())

    fun conEtiqueta(etiqueta: String): SemanticsNode =
        checkNotNull(nodos().firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == etiqueta }) {
            "No hay nodo con etiqueta $etiqueta"
        }

    fun etiquetas(prefijo: String): List<String> =
        nodos()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
            .filter { it.startsWith(prefijo) }

    /** Textos de un nodo y de todo lo que cuelga de él. */
    fun textos(nodo: SemanticsNode): List<String> =
        aplanar(nodo).flatMap { n ->
            n.config
                .getOrNull(SemanticsProperties.Text)
                ?.map { it.text }
                .orEmpty()
        }

    fun hayDesplazable(): Boolean =
        nodos().any {
            it.config.contains(SemanticsProperties.VerticalScrollAxisRange) ||
                it.config.contains(SemanticsProperties.HorizontalScrollAxisRange) ||
                it.config.contains(SemanticsActions.ScrollBy)
        }

    fun pulsar(nodo: SemanticsNode) {
        val accion = checkNotNull(nodo.config.getOrNull(SemanticsActions.OnClick)) { "El nodo no es pulsable" }
        accion.action?.invoke()
        reposo()
    }

    /** Primer nodo pulsable con ese texto dentro del nodo dado (o en todo el árbol). */
    fun pulsableConTexto(
        texto: String,
        dentroDe: SemanticsNode = raiz(),
    ): SemanticsNode =
        checkNotNull(
            aplanar(dentroDe).firstOrNull { n ->
                n.config.contains(SemanticsActions.OnClick) && textos(n).any { it == texto }
            },
        ) { "No hay botón con el texto $texto" }

    private fun aplanar(nodo: SemanticsNode): List<SemanticsNode> = listOf(nodo) + nodo.children.flatMap(::aplanar)

    private fun buscarRaiz(vista: View): RootForTest? =
        when {
            vista is RootForTest -> vista
            vista is ViewGroup -> (0 until vista.childCount).firstNotNullOfOrNull { buscarRaiz(vista.getChildAt(it)) }
            else -> null
        }

    private companion object {
        const val FOTOGRAMAS = 4
        const val MS_FOTOGRAMA = 20L
    }
}
