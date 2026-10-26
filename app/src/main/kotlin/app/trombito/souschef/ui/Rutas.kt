package app.trombito.souschef.ui

import java.net.URLEncoder

/**
 * Rutas de navegación. Contrato del cronómetro: `crono/{slug}?raciones={n}&servir={HH:mm}`
 * (`raciones` 0 = usar las de la receta; `servir` vacío = la de comer o la de cenar de Ajustes, según la receta).
 */
object Rutas {
    const val ARG_SLUG = "slug"
    const val ARG_RACIONES = "raciones"
    const val ARG_SERVIR = "servir"
    const val ARG_FAMILIA = "familia"
    const val ARG_CATEGORIA = "categoria"

    /** Argumento opcional del Buzón: el plato que se pide (lo buscado en Recetas sin resultados). */
    const val ARG_PLATO = "plato"

    /** Largo máximo de [ARG_PLATO]: lo que sobra se corta. */
    const val PLATO_MAX = 100

    /** Intro de primer arranque: sin barras, y al acabar limpia la pila y va a Recetas. */
    const val BIENVENIDA = "bienvenida"

    /** La misma intro abierta desde Ajustes: al acabar vuelve atrás. */
    const val BIENVENIDA_REPETIR = "bienvenida/repetir"

    const val FICHA = "receta/{$ARG_SLUG}"
    const val AJUSTES = "ajustes"
    const val BUZON = "buzon"

    /** Patrón del destino del Buzón: `plato` opcional; se navega a [BUZON] o a [buzon]. */
    const val BUZON_PATRON = "buzon?$ARG_PLATO={$ARG_PLATO}"
    const val AJUSTES_COCINA = "ajustes/cocina"
    const val AJUSTES_APARIENCIA = "ajustes/apariencia"
    const val AJUSTES_HORARIO = "ajustes/horario"
    const val AJUSTES_CRONO = "ajustes/crono"
    const val AJUSTES_HISTORIAL = "ajustes/historial"
    const val AJUSTES_ACERCA = "ajustes/acerca"
    const val AJUSTES_NOVEDADES = "ajustes/novedades"
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

    /** El Buzón con el plato ya escrito (codificado: lleva espacios, tildes o signos). */
    fun buzon(plato: String): String {
        val limpio = plato.trim().take(PLATO_MAX)
        if (limpio.isEmpty()) return BUZON
        return "$BUZON?$ARG_PLATO=" + URLEncoder.encode(limpio, Charsets.UTF_8.name()).replace("+", "%20")
    }

    /** El cronómetro de una cocinada viva, sin argumentos: raciones y hora salen de lo guardado. */
    fun cronoViva(slug: String) = "crono/$slug"

    fun crono(
        slug: String,
        raciones: Int,
        servir: String,
    ) = "crono/$slug?$ARG_RACIONES=$raciones&$ARG_SERVIR=$servir"
}
