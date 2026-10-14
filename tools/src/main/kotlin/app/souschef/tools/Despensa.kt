package app.souschef.tools

import app.souschef.receta.Hallazgo
import app.souschef.receta.Nivel
import app.souschef.receta.Receta
import app.souschef.receta.Texto

/** Regla 12: todo ingrediente de `grupos` está en la despensa o en `compra`. */
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
    private fun casa(
        nombre: String,
        entrada: String,
    ): Boolean {
        val limpio = nombre.substringBefore(',').replace(Regex("\\([^)]*\\)"), " ").trim()
        return formas(entrada).any { Texto.contienePalabra(nombre, it) } ||
            listOf(limpio, Texto.nucleo(nombre)).any { Texto.contienePalabra(entrada, it) }
    }

    /** Siempre disponible aunque la semilla no lo liste. */
    private val implicitos = setOf("agua")

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

    fun comprobar(
        r: Receta,
        despensa: Set<String>,
    ): List<Hallazgo> {
        val compra = r.compra.map { it.n }
        return r.grupos
            .flatMap { it.items }
            .filter { i -> (despensa + compra + implicitos).none { casa(i.n, it) } }
            .map { Hallazgo(r.slug, 12, Nivel.ERROR, "ingrediente '${it.n}' no está en la despensa ni en compra") }
    }
}
