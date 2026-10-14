package app.souschef.receta

import java.text.Normalizer

/** Comparación de texto sin acentos ni mayúsculas, por palabra completa (reglas 8, 12 y 24 del validador). */
object Texto {
    private val marcas = Regex("\\p{Mn}+")
    private const val VOCALES = "aeiou"
    private const val CONSONANTES_ES = "lrndzsxyj"
    private const val MIN_NUCLEO = 3

    /** Minúsculas y sin diacríticos (ñ pasa a n en ambos lados de la comparación). */
    fun normalizar(s: String): String = marcas.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase()

    /**
     * ¿Aparece [termino] como palabra completa en [texto]? Plural: `-s` tras vocal y `-es` tras l,r,n,d,z,s,x,y,j
     * (cebolla/cebollas, pan/panes). Nunca por subcadena: «sal» no casa en «salsa».
     */
    fun contienePalabra(
        texto: String,
        termino: String,
    ): Boolean {
        val t = normalizar(termino).trim()
        if (t.isEmpty()) return false
        val sufijo =
            when {
                t.last() in VOCALES -> "s?"
                t.last() in CONSONANTES_ES -> "(?:es)?"
                else -> ""
            }
        val re = Regex("(?<![a-z0-9])" + Regex.escape(t) + sufijo + "(?![a-z0-9])")
        return re.containsMatchIn(normalizar(texto))
    }

    /** Singular tosco: quita `-es` tras consonante de la lista y, si no, una `s` final (tomates, patatas). */
    fun singular(palabra: String): String =
        when {
            palabra.endsWith(
                "es",
            ) && palabra.dropLast(2).lastOrNull()?.let { it in CONSONANTES_ES } == true -> palabra.dropLast(2)

            palabra.endsWith("s") && palabra.length > MIN_NUCLEO -> palabra.dropLast(1)

            else -> palabra
        }

    /** Palabra núcleo: sin paréntesis ni lo que sigue a la coma, la primera palabra de 3+ letras no genérica. */
    fun nucleo(nombre: String): String {
        val limpio = normalizar(nombre).substringBefore(',').replace(Regex("\\([^)]*\\)"), " ").trim()
        val palabras = limpio.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        return palabras.firstOrNull { it.length >= MIN_NUCLEO && it !in genericas } ?: limpio
    }

    private val genericas = setOf("salsa", "semillas", "semilla", "hojas", "hoja", "polvo", "zumo", "trozos")
    private const val MIN_CLAVE = 4

    /** Palabras del nombre (sin paréntesis ni coma) de 4 o más letras que no son genéricas; «aceite» sí cuenta. */
    fun palabrasClave(nombre: String): List<String> {
        val limpio = normalizar(nombre).substringBefore(',').replace(Regex("\\([^)]*\\)"), " ")
        return limpio
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= MIN_CLAVE && it !in genericas }
            .flatMap { listOf(it, singular(it)) }
            .distinct()
    }
}
