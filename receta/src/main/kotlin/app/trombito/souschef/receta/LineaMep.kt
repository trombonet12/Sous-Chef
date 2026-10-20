package app.trombito.souschef.receta

/**
 * Una línea de mise en place (`mep[].l[]`) partida en cabecera, detalle y minuto de entrada.
 * Formato habitual: `<b>Cabecera</b> · detalle … minuto N.`
 */
data class LineaMep(
    val cabecera: String?,
    val detalle: String,
    val minuto: Int?,
) {
    companion object {
        private val CABECERA = Regex("^\\s*<b>(.*?)</b>", RegexOption.DOT_MATCHES_ALL)
        private val SEPARADOR = Regex("^\\s*·\\s*")
        private val NEGRITAS = Regex("</?b>")
        private val MINUTO = Regex("minuto (\\d+)")

        /** Sin `<b>` inicial no hay cabecera y el detalle es toda la línea (sin etiquetas `<b>`). */
        fun parse(html: String): LineaMep {
            val cabecera = CABECERA.find(html)
            val resto = if (cabecera == null) html else html.substring(cabecera.range.last + 1)
            return LineaMep(
                cabecera = cabecera?.groupValues?.get(1)?.let { limpiar(it) },
                detalle = limpiar(resto.replaceFirst(SEPARADOR, "")),
                minuto =
                    MINUTO
                        .find(html)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull(),
            )
        }

        private fun limpiar(texto: String) = texto.replace(NEGRITAS, "").trim()
    }
}
