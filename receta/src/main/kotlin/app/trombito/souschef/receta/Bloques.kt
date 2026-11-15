package app.trombito.souschef.receta

/** Un bloque de la prosa estructurada: párrafo o lista (el texto puede llevar `<b>`). */
sealed interface Bloque {
    data class Parrafo(
        val texto: String,
    ) : Bloque

    data class Lista(
        val elementos: List<String>,
    ) : Bloque
}

/**
 * Mini formato de la prosa de las recetas (ADR-0017): una línea en blanco abre párrafo nuevo, una línea que empieza
 * por «- » es un elemento de lista (las seguidas forman la misma lista) y un salto simple dentro de un párrafo o de
 * un elemento se une con un espacio. Nada más: ni Markdown ni HTML (`<b>` lo trata quien pinta).
 */
object Bloques {
    private const val VINETA = "- "

    fun analizar(texto: String): List<Bloque> {
        val bloques = mutableListOf<Bloque>()
        val parrafo = mutableListOf<String>()
        val elementos = mutableListOf<String>()

        fun cerrar() {
            if (parrafo.isNotEmpty()) bloques += Bloque.Parrafo(parrafo.joinToString(" "))
            if (elementos.isNotEmpty()) bloques += Bloque.Lista(elementos.toList())
            parrafo.clear()
            elementos.clear()
        }
        texto.lines().map { it.trim() }.forEach { linea ->
            when {
                linea.isEmpty() -> cerrar()

                linea.startsWith(VINETA) -> {
                    if (parrafo.isNotEmpty()) cerrar()
                    elementos += linea.removePrefix(VINETA).trim()
                }

                elementos.isNotEmpty() -> elementos[elementos.lastIndex] += " $linea"

                else -> parrafo += linea
            }
        }
        cerrar()
        return bloques
    }
}
