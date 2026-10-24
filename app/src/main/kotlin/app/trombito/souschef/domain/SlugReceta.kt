package app.trombito.souschef.domain

/** Forma válida de un slug de receta. Se comprueba antes de actuar sobre cualquier slug que llegue de fuera. */
object SlugReceta {
    private val FORMA = Regex("^[a-z0-9-]{1,64}$")

    fun valido(slug: String?): Boolean = slug != null && FORMA.matches(slug)
}
