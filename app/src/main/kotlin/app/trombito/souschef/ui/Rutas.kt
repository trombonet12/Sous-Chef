package app.trombito.souschef.ui

/**
 * Rutas de navegación. Contrato del cronómetro: `crono/{slug}?raciones={n}&servir={HH:mm}`
 * (`raciones` 0 = usar las de la receta; `servir` vacío = la hora de servir de Ajustes).
 */
object Rutas {
    const val ARG_SLUG = "slug"
    const val ARG_RACIONES = "raciones"
    const val ARG_SERVIR = "servir"

    const val FICHA = "receta/{$ARG_SLUG}"
    const val AJUSTES = "ajustes"
    const val AJUSTES_APARIENCIA = "ajustes/apariencia"
    const val AJUSTES_HORARIO = "ajustes/horario"
    const val AJUSTES_CRONO = "ajustes/crono"
    const val AJUSTES_HISTORIAL = "ajustes/historial"
    const val TECNICA = "tecnica/{$ARG_SLUG}"
    const val CRONO = "crono/{$ARG_SLUG}?$ARG_RACIONES={$ARG_RACIONES}&$ARG_SERVIR={$ARG_SERVIR}"

    fun ficha(slug: String) = "receta/$slug"

    fun tecnica(slug: String) = "tecnica/$slug"

    fun crono(
        slug: String,
        raciones: Int,
        servir: String,
    ) = "crono/$slug?$ARG_RACIONES=$raciones&$ARG_SERVIR=$servir"
}
