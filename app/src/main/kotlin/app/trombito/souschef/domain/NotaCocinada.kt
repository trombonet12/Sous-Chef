package app.trombito.souschef.domain

/** Nota «para la próxima vez» de una cocinada; [fecha] ISO yyyy-MM-dd. */
data class NotaCocinada(
    val fecha: String,
    val nota: String,
)

/** Notas no vacías de [slug], de la más reciente a la más antigua (fecha y luego id, descendentes). */
fun notasDe(
    entradas: List<EntradaHistorial>,
    slug: String,
): List<NotaCocinada> =
    entradas
        .filter { it.slug == slug }
        .sortedWith(compareByDescending<EntradaHistorial> { it.fecha }.thenByDescending { it.id })
        .mapNotNull { e ->
            val texto = e.nota.orEmpty().trim()
            if (texto.isEmpty()) null else NotaCocinada(e.fecha, texto)
        }
