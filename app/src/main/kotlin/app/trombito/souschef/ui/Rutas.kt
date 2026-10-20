package app.trombito.souschef.ui

/**
 * Rutas de navegación. Contrato del cronómetro: `crono/{slug}?raciones={n}&servir={HH:mm}`
 * (`raciones` 0 = usar las de la receta; `servir` vacío = la hora de servir de Ajustes).
 */
object Rutas {
    const val ARG_SLUG = "slug"
    const val ARG_RACIONES = "raciones"
    const val ARG_SERVIR = "servir"
    const val ARG_FAMILIA = "familia"
    const val ARG_CATEGORIA = "categoria"

    const val FICHA = "receta/{$ARG_SLUG}"
    const val AJUSTES = "ajustes"
    const val AJUSTES_APARIENCIA = "ajustes/apariencia"
    const val AJUSTES_HORARIO = "ajustes/horario"
    const val AJUSTES_CRONO = "ajustes/crono"
    const val AJUSTES_HISTORIAL = "ajustes/historial"
    const val AJUSTES_ACERCA = "ajustes/acerca"
    const val TECNICA = "tecnica/{$ARG_SLUG}"
    const val CRONO = "crono/{$ARG_SLUG}?$ARG_RACIONES={$ARG_RACIONES}&$ARG_SERVIR={$ARG_SERVIR}"

    /** Recetas de una familia de base; `familia` es el nombre del enum en minúsculas (p. ej. `pasta`). */
    const val RECETAS_FAMILIA = "recetas/familia/{$ARG_FAMILIA}"

    /** Técnicas de una categoría; `categoria` es el nombre del enum en minúsculas (p. ej. `salsa`). */
    const val TECNICAS_CATEGORIA = "tecnicas/categoria/{$ARG_CATEGORIA}"

    fun ficha(slug: String) = "receta/$slug"

    fun recetasFamilia(familia: String) = "recetas/familia/$familia"

    fun tecnicasCategoria(categoria: String) = "tecnicas/categoria/$categoria"

    fun tecnica(slug: String) = "tecnica/$slug"

    fun crono(
        slug: String,
        raciones: Int,
        servir: String,
    ) = "crono/$slug?$ARG_RACIONES=$raciones&$ARG_SERVIR=$servir"
}
