package app.souschef.data.seed

import app.souschef.domain.EstadoDespensa

data class ItemSemilla(
    val nombre: String,
    val categoria: String,
    val estado: EstadoDespensa = EstadoDespensa.HAY,
)

/** Parser de `data/despensa-semilla.md`: `## Categoría` y debajo `a · b · c`. Para en la primera línea `---`. */
object SemillaDespensa {
    const val CATEGORIA_VETADOS = "Vetados"

    /** Vetos fijos de CLAUDE.md / docs/cocina/reglas.md. */
    val VETOS =
        listOf(
            "garbanzos",
            "guisantes",
            "champiñones",
            "coliflor",
            "brócoli",
            "casquería",
            "anchoas",
            "alcaparras",
            "marisco",
        )

    fun parsear(markdown: String): List<ItemSemilla> {
        val items = mutableListOf<ItemSemilla>()
        var categoria: String? = null
        for (linea in markdown.lineSequence().map { it.trim() }) {
            when {
                linea == "---" -> {
                    break
                }

                linea.startsWith("## ") -> {
                    categoria = linea.removePrefix("## ").trim()
                }

                categoria != null && linea.isNotEmpty() -> {
                    linea.split('·').map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                        items += ItemSemilla(it.replaceFirstChar(Char::uppercase), categoria)
                    }
                }
            }
        }
        return items +
            VETOS.map { ItemSemilla(it.replaceFirstChar(Char::uppercase), CATEGORIA_VETADOS, EstadoDespensa.VETADO) }
    }
}
