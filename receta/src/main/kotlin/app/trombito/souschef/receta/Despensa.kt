package app.trombito.souschef.receta

/** Despensa: semilla, emparejamiento ingrediente↔entrada y regla 12 (todo ingrediente de `grupos` está en ella). */
object Despensa {
    /** Nombres de la semilla (`data/despensa-semilla.md`): líneas de lista bajo `##`, hasta el `---`. */
    fun parsearSemilla(md: String): Set<String> {
        val nombres = mutableSetOf<String>()
        var enSeccion = false
        for (linea in md.lineSequence()) {
            when {
                linea.trim() == "---" -> {
                    break
                }

                linea.startsWith("## ") -> {
                    enSeccion = true
                }

                enSeccion && linea.isNotBlank() -> {
                    linea
                        .split(" · ")
                        .flatMap { it.split(" o ") }
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { nombres += it }
                }
            }
        }
        return nombres
    }

    /** Casa por palabra completa en cualquiera de los dos sentidos (el ingrediente contiene la entrada o al revés). */
    fun casa(
        ingrediente: String,
        entrada: String,
    ): Boolean {
        val limpio = ingrediente.substringBefore(',').replace(Regex("\\([^)]*\\)"), " ").trim()
        return formas(entrada).any { Texto.contienePalabra(ingrediente, it) } ||
            listOf(limpio, Texto.nucleo(ingrediente)).any { Texto.contienePalabra(entrada, it) }
    }

    /** Siempre disponible aunque la semilla no lo liste. */
    private val implicitos = setOf("agua")

    fun esImplicito(ingrediente: String): Boolean = implicitos.any { casa(ingrediente, it) }

    /** Primera entrada de la despensa que casa con el ingrediente, o null. */
    fun entradaDe(
        ingrediente: String,
        entradas: Collection<String>,
    ): String? = entradas.firstOrNull { casa(ingrediente, it) }

    /** La entrada tal cual y en singular (la despensa dice «Ajos», «Huevos», «limones»; la receta, «diente de ajo»). */
    private fun formas(entrada: String): List<String> {
        val e = Texto.normalizar(entrada).trim()
        return listOfNotNull(
            e,
            e.removeSuffix("es").takeIf {
                e.endsWith("es")
            },
            e.removeSuffix("s").takeIf { e.endsWith("s") },
        )
    }

    /** Regla 12: ingrediente de `grupos` que no casa con ninguna entrada ni es implícito → ERROR. */
    fun comprobar(
        r: Receta,
        despensa: Set<String>,
    ): List<Hallazgo> =
        r.grupos
            .flatMap { it.items }
            .filter { i -> !esImplicito(i.n) && entradaDe(i.n, despensa) == null }
            .map { Hallazgo(r.slug, 12, Nivel.ERROR, "ingrediente '${it.n}' no está en la despensa") }
}
