package app.trombito.souschef.ui.crono.voz

import app.trombito.souschef.receta.Texto

/** Comparación de nombres de ingrediente dichos en voz: sin tildes, por palabra y en singular o plural básico. */
internal object CoincidenciaNombres {
    private const val RANGO_PEDIDO_EN_NOMBRE = 1
    private const val RANGO_NOMBRE_EN_PEDIDO = 100
    private val noPalabra = Regex("[^a-z0-9]+")
    private val digitos = Regex("[0-9]+")
    private val articulos =
        setOf("de", "del", "la", "el", "los", "las", "un", "una", "y", "con", "en", "al", "a", "para")

    /** Palabras significativas del texto, normalizadas: sin artículos, preposiciones ni cifras. */
    fun tokens(texto: String): List<String> =
        Texto
            .normalizar(texto)
            .split(noPalabra)
            .filter { it.isNotEmpty() && it !in articulos && !digitos.matches(it) }
            .distinct()

    private fun formas(palabra: String): Set<String> =
        setOf(palabra, palabra.removeSuffix("s"), Texto.singular(palabra))

    fun mismaPalabra(
        a: String,
        b: String,
    ): Boolean = a == b || formas(a).any { it in formas(b) }

    private fun contenidas(
        menor: List<String>,
        mayor: List<String>,
    ): Boolean = menor.all { x -> mayor.any { y -> mismaPalabra(x, y) } }

    /** Menor es mejor: nombre igual, luego el pedido dentro del nombre y al revés; null si discrepan. */
    fun rango(
        pedido: List<String>,
        nombre: List<String>,
    ): Int? =
        when {
            pedido.isEmpty() || nombre.isEmpty() -> null
            contenidas(pedido, nombre) && contenidas(nombre, pedido) -> 0
            contenidas(pedido, nombre) -> RANGO_PEDIDO_EN_NOMBRE + nombre.size - pedido.size
            contenidas(nombre, pedido) -> RANGO_NOMBRE_EN_PEDIDO + pedido.size - nombre.size
            else -> null
        }
}
