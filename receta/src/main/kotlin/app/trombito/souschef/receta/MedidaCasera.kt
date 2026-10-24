package app.trombito.souschef.receta

/**
 * Suma de equivalencias caseras («1 cucharada sopera», «2 cucharaditas», «1½ cucharaditas») para la lista de «Todo
 * junto». Solo sabe de cucharadas soperas y cucharaditas (1 cucharada sopera = 3 cucharaditas); cualquier otra cosa
 * («1 cebolla mediana», «una pizca», «1 cucharadita rasa») hace que no haya suma.
 */
object MedidaCasera {
    /** Doceavos de cucharadita: permite sumar mitades, tercios y cuartos sin errores de coma flotante. */
    private const val DOCEAVOS = 12
    private const val CDTAS_POR_CUCHARADA = 3
    private const val POR_CUCHARADA = DOCEAVOS * CDTAS_POR_CUCHARADA
    private const val CUARTO = 3
    private const val TERCIO = 4
    private const val MEDIA = 6
    private const val DOS_TERCIOS = 8
    private const val TRES_CUARTOS = 9

    private val FRACCIONES = mapOf('¼' to CUARTO, '⅓' to TERCIO, '½' to MEDIA, '⅔' to DOS_TERCIOS, '¾' to TRES_CUARTOS)
    private val GLIFOS = FRACCIONES.entries.associate { (g, v) -> v to g }
    private val PARTE =
        Regex("^(?:(una|un|media)|(\\d+)?\\s*([¼⅓½⅔¾])?)\\s*(?:de\\s+)?(cucharadas?\\s+soperas?|cucharaditas?)$")
    private val ENTRE_PARENTESIS = Regex("\\(([^()]*)\\)\\s*$")
    private val CONJUNCION = Regex("\\s+y\\s+")

    /** La equivalencia entre paréntesis al final de un nombre de ingrediente, o null si no la lleva. */
    fun equivalenciaDe(nombre: String): String? =
        ENTRE_PARENTESIS
            .find(nombre.trim())
            ?.groupValues
            ?.get(1)
            ?.trim()

    /**
     * Suma de [equivalencias] escrita como «1 cucharada sopera y 2 cucharaditas», o null si no hay ninguna, alguna
     * no es de cucharadas soperas y cucharaditas, o el resultado no se puede escribir con ½, ⅓, ¼, ⅔ o ¾.
     */
    fun sumar(equivalencias: List<String>): String? {
        val valores = equivalencias.map(::doceavos)
        return if (valores.isEmpty() || valores.any { it == null }) null else escribir(valores.filterNotNull().sum())
    }

    /** Valor en doceavos de cucharadita de «1 cucharada sopera y 1 cucharadita», o null si no se entiende. */
    private fun doceavos(equivalencia: String): Int? {
        val partes =
            equivalencia
                .trim()
                .lowercase()
                .split(CONJUNCION)
                .map { parte(it.trim()) }
        return if (partes.any { it == null }) null else partes.filterNotNull().sum().takeIf { it > 0 }
    }

    private fun parte(texto: String): Int? {
        val g = PARTE.matchEntire(texto)?.groupValues ?: return null
        val palabra = g[1]
        val entero = g[2]
        val fraccion = g[3]
        val cantidad =
            when {
                palabra == "media" -> MEDIA
                palabra.isNotEmpty() -> DOCEAVOS
                entero.isEmpty() && fraccion.isEmpty() -> null
                else -> (entero.toIntOrNull() ?: 0) * DOCEAVOS + (fraccion.firstOrNull()?.let { FRACCIONES[it] } ?: 0)
            }
        return cantidad?.let { if (g[4].startsWith("cucharadita")) it else it * CDTAS_POR_CUCHARADA }
    }

    private fun escribir(total: Int): String? {
        val cucharadas = total / POR_CUCHARADA
        val resto = total % POR_CUCHARADA
        val partes = mutableListOf<String>()
        if (cucharadas > 0) {
            partes += if (cucharadas == 1) "1 cucharada sopera" else "$cucharadas cucharadas soperas"
        }
        if (resto > 0) partes += cucharaditas(resto) ?: return null
        return partes.joinToString(" y ")
    }

    private fun cucharaditas(doceavos: Int): String? {
        val enteras = doceavos / DOCEAVOS
        val fraccion = doceavos % DOCEAVOS
        val glifo = if (fraccion == 0) "" else GLIFOS[fraccion]?.toString() ?: return null
        val cantidad = if (enteras == 0) glifo else "$enteras$glifo"
        return when {
            doceavos > DOCEAVOS -> "$cantidad cucharaditas"
            doceavos == DOCEAVOS || fraccion == MEDIA -> "$cantidad cucharadita"
            else -> "$cantidad de cucharadita"
        }
    }
}
