package app.trombito.souschef.ui.bienvenida

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Escala con la que se pinta el contenido del marco: más pequeño que el real para que quepa una pantalla entera. */
internal const val ESCALA_DEMO = 0.85f

private val FORMA_MOVIL = RoundedCornerShape(28.dp)
private val GROSOR_MARCO = 3.dp

/**
 * Marco con borde de móvil que pinta composables **reales** de la app a [ESCALA_DEMO] y a todo el ancho: el contenido
 * se mide en un lienzo más grande (ancho / escala) y se reduce, así los textos y las proporciones son los de la
 * pantalla. Es solo para mirar: el marco se come los toques (el dedo que lo cruza sigue desplazando la página) y
 * para TalkBack es un único elemento con [descripcion].
 *
 * [alto] es el alto visible del marco; [fondo] lo que se ve tras el contenido.
 */
@Composable
internal fun MarcoDemo(
    descripcion: String,
    alto: Dp,
    modifier: Modifier = Modifier,
    fondo: Color = MaterialTheme.colorScheme.background,
    contenido: @Composable () -> Unit,
) {
    Surface(
        modifier.fillMaxWidth().height(alto).testTag("marco-demo"),
        shape = FORMA_MOVIL,
        color = fondo,
        border = BorderStroke(GROSOR_MARCO, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.clearAndSetSemantics { contentDescription = descripcion }) {
            LienzoReducido(ESCALA_DEMO, contenido)
            // Capa que recibe los toques sin consumirlos: nada del contenido se pulsa, el scroll de la página sí.
            Box(Modifier.matchParentSize().pointerInput(Unit) {})
        }
    }
}

/** Mide [contenido] a `ancho / escala` por `alto / escala` y lo dibuja reducido a [escala]. */
@Composable
private fun LienzoReducido(
    escala: Float,
    contenido: @Composable () -> Unit,
) {
    Layout(contenido) { medibles, restricciones ->
        val ancho = restricciones.maxWidth
        val alto = restricciones.maxHeight
        val interior = Constraints.fixed((ancho / escala).roundToInt(), (alto / escala).roundToInt())
        val colocables = medibles.map { it.measure(interior) }
        layout(ancho, alto) {
            colocables.forEach {
                it.placeWithLayer(0, 0) {
                    scaleX = escala
                    scaleY = escala
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }
}
