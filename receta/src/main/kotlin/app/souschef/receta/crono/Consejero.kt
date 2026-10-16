package app.souschef.receta.crono

import app.souschef.receta.Receta

enum class TipoConsejo { PRONTO, OTRO_CARRIL, ADELANTA }

/** Un consejo corto para el compañero del cronómetro. */
data class Consejo(
    val texto: String,
    val tipo: TipoConsejo,
)

/** Qué le diría Trombito ahora mismo al cocinero. Puro: el reloj llega por parámetro. */
object Consejero {
    private const val PRONTO_MS = 60_000L
    private const val VENTANA_MS = 5 * 60_000L
    private const val MAX_CHARS = 60
    private val etiquetas = Regex("</?[a-zA-Z][^>]*>")

    fun consejo(
        receta: Receta,
        estado: EstadoCrono,
        ahoraMs: Long,
    ): Consejo? =
        pronto(receta, estado, ahoraMs)
            ?: otroCarril(receta, estado, ahoraMs)
            ?: adelanta(receta, estado)

    private fun pronto(
        receta: Receta,
        estado: EstadoCrono,
        ahoraMs: Long,
    ): Consejo? {
        val evento = AvisosCrono.siguienteEvento(estado, receta) ?: return null
        return if (evento.ms - ahoraMs in 0 until PRONTO_MS) {
            Consejo("En nada toca: ${descripcion(receta, evento)}", TipoConsejo.PRONTO)
        } else {
            null
        }
    }

    private fun otroCarril(
        receta: Receta,
        estado: EstadoCrono,
        ahoraMs: Long,
    ): Consejo? {
        val evento = AvisosCrono.siguienteEventoFuera(estado, receta, estado.foco)
        val falta = (evento?.ms ?: Long.MAX_VALUE) - ahoraMs
        return if (evento != null && falta in 0 until VENTANA_MS) {
            val min = Math.floorDiv(falta + PRONTO_MS - 1, PRONTO_MS)
            Consejo("En $min min: ${descripcion(receta, evento)}", TipoConsejo.OTRO_CARRIL)
        } else {
            null
        }
    }

    private fun descripcion(
        receta: Receta,
        evento: Evento,
    ): String = "${receta.carril(evento.carril)?.nombre ?: evento.carril} · ${evento.paso.corto()}"

    private fun adelanta(
        receta: Receta,
        estado: EstadoCrono,
    ): Consejo? {
        val linea =
            receta.mep
                .flatMap { it.l }
                .firstOrNull { SesionCrono.idCasilla(it) !in estado.marcados && it.corto().isNotEmpty() }
        return linea?.let { Consejo("Ve adelantando: ${it.corto()}", TipoConsejo.ADELANTA) }
    }

    /** Sin etiquetas, cortado en el primer " · " o ",", y a [MAX_CHARS] como mucho. */
    private fun String.corto(): String {
        val t = replace(etiquetas, "").substringBefore(" · ").substringBefore(",").trim()
        return if (t.length > MAX_CHARS) t.take(MAX_CHARS - 1).trimEnd() + "…" else t
    }
}
