package app.trombito.souschef.ui.crono.guia

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo

/** Puntos de la cocina que explica la guía, en el orden en que se explican. */
object GuiaIds {
    const val CARRIL = "carril"
    const val HECHO = "hecho"
    const val PRORROGA = "prorroga"
    const val DESHACER = "deshacer"
    const val MICRO = "micro"
    const val MENU = "menu"

    val ORDEN: List<String> = listOf(CARRIL, HECHO, PRORROGA, DESHACER, MICRO, MENU)
}

/**
 * Límites (en la raíz de la composición) de los controles marcados con [guia]. Varios controles pueden compartir id (un
 * "Hecho" por carril): para la guía vale el más alto y, a igual altura, el más a la izquierda.
 */
@Stable
class GuiaEstado {
    private val puntos = mutableStateMapOf<Any, Pair<String, Rect>>()

    internal fun registrar(
        clave: Any,
        id: String,
        limites: Rect,
    ) {
        val nuevo = id to limites
        if (puntos[clave] != nuevo) puntos[clave] = nuevo
    }

    internal fun quitar(clave: Any) {
        puntos.remove(clave)
    }

    /** Ids con algún control a la vista. */
    fun ids(): Set<String> = puntos.values.mapTo(mutableSetOf()) { it.first }

    fun limite(id: String): Rect? =
        puntos.values
            .filter { it.first == id }
            .map { it.second }
            .minWithOrNull(compareBy({ it.top }, { it.left }))
}

val LocalGuia = compositionLocalOf<GuiaEstado?> { null }

/** Marca este control como punto [id] de la guía de la cocina (no hace nada si no hay guía en la composición). */
fun Modifier.guia(id: String): Modifier = this then GuiaElemento(id)

private data class GuiaElemento(
    val id: String,
) : ModifierNodeElement<GuiaNodo>() {
    override fun create() = GuiaNodo(id)

    override fun update(node: GuiaNodo) {
        node.id = id
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "guia"
        properties["id"] = id
    }
}

private class GuiaNodo(
    var id: String,
) : Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var estado: GuiaEstado? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val destino = currentValueOf(LocalGuia) ?: return
        estado = destino
        if (coordinates.isAttached) destino.registrar(this, id, coordinates.boundsInRoot())
    }

    override fun onDetach() {
        estado?.quitar(this)
        estado = null
    }
}
