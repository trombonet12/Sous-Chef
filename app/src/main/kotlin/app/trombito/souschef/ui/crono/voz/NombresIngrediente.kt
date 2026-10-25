package app.trombito.souschef.ui.crono.voz

import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Texto

/** Nombres de ingrediente de una receta tal como se dicen y se buscan en un paso. */
internal object NombresIngrediente {
    private const val MIN_EXTRA = 4
    private val medida = Regex("""\s+de\s+\d.*$""")

    /** El nombre sin la equivalencia entre paréntesis ni lo que va tras la coma. */
    fun base(nombre: String): String = nombre.substringBefore('(').substringBefore(',').trim()

    /** El nombre base sin la medida del corte («ajo picado de 1 mm» -> «ajo picado»). */
    fun corto(nombre: String): String = base(nombre).replace(medida, "").trim()

    /** Identifica al mismo ingrediente repetido en varias zonas. */
    fun clave(nombre: String): String = Texto.normalizar(base(nombre)).trim()

    /** ¿Nombra el texto del paso a este ingrediente? Por su palabra núcleo o por el nombre entero. */
    fun nombradoEn(
        textoPaso: String,
        nombre: String,
    ): Boolean =
        Texto.contienePalabra(textoPaso, Texto.nucleo(nombre)) || Texto.contienePalabra(textoPaso, base(nombre))

    /**
     * Si varios ingredientes distintos comparten núcleo («aceite de girasol», «aceite de oliva»), se quedan los que
     * además nombran el resto de su nombre en el paso ([enPaso]); si ninguno lo hace, todos.
     */
    fun desempatar(
        nombrados: List<Ingrediente>,
        enPaso: List<String>,
    ): List<Ingrediente> {
        val descartados = mutableSetOf<Ingrediente>()
        nombrados.groupBy { Texto.nucleo(it.n) }.values.forEach { grupo ->
            val propios = grupo.filter { extrasEn(it, enPaso) }
            if (grupo.map { clave(it.n) }.distinct().size > 1 && propios.isNotEmpty()) {
                descartados += grupo - propios.toSet()
            }
        }
        return nombrados.filterNot { it in descartados }
    }

    private fun extrasEn(
        i: Ingrediente,
        enPaso: List<String>,
    ): Boolean {
        val nucleo = CoincidenciaNombres.tokens(Texto.nucleo(i.n))
        val extras =
            CoincidenciaNombres
                .tokens(base(i.n))
                .filter { it.length >= MIN_EXTRA && nucleo.none { n -> CoincidenciaNombres.mismaPalabra(n, it) } }
        return extras.any { e -> enPaso.any { CoincidenciaNombres.mismaPalabra(e, it) } }
    }
}
