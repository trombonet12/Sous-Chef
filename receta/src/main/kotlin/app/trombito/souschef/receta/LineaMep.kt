package app.trombito.souschef.receta

/**
 * Una línea de mise en place (`mep[].l[]`) partida en cabecera, detalle y minuto de entrada.
 * Formato habitual: `<b>Cabecera</b> · detalle … minuto N.`
 *
 * El minuto es el de **entrada** (cuando la cosa pasa a la preparación o al fuego), no el primero que se nombre: en
 * «Se rasga en el minuto 12. Entra en la sartén en el minuto 30» es el 30.
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
        private val ENTRADA = Regex("\\b[Ee]ntran?\\b[^.;]*?minuto (\\d+)")

        /** Sin `<b>` inicial no hay cabecera y el detalle es toda la línea (sin etiquetas `<b>`). */
        fun parse(html: String): LineaMep {
            val cabecera = CABECERA.find(html)
            val resto = if (cabecera == null) html else html.substring(cabecera.range.last + 1)
            return LineaMep(
                cabecera = cabecera?.groupValues?.get(1)?.let { limpiar(it) },
                detalle = limpiar(resto.replaceFirst(SEPARADOR, "")),
                minuto = minutoDeEntrada(html),
            )
        }

        /** El último «entra(n) … minuto N» de la línea; si no hay, el último «minuto N» (el de la acción final). */
        private fun minutoDeEntrada(html: String): Int? {
            val entrada = ENTRADA.findAll(html).lastOrNull() ?: MINUTO.findAll(html).lastOrNull()
            return entrada?.groupValues?.get(1)?.toIntOrNull()
        }

        private fun limpiar(texto: String) = texto.replace(NEGRITAS, "").trim()
    }
}
