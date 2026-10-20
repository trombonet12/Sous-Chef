package app.trombito.souschef.ui.tecnicas

import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tecnica
import app.trombito.souschef.ui.comun.normalizado

data class TecnicasUiState(
    val cargando: Boolean = true,
    val consulta: String = "",
    /** Categorías con al menos una técnica (sobre todas, sin buscador), en el orden del enum, con su recuento. */
    val categorias: List<Pair<CategoriaTecnica, Int>> = emptyList(),
    /** Nombre visible de cada categoría cuando los datos dan uno solo (p. ej. «Cocción»). */
    val nombres: Map<CategoriaTecnica, String> = emptyMap(),
    /** Categoría de la pantalla de una categoría; null en la pestaña. */
    val categoriaFija: CategoriaTecnica? = null,
    val disposicion: DisposicionLista = DisposicionLista.TINTE,
    /** Técnicas que hay antes del buscador (tras el prefiltro de categoría). */
    val total: Int = 0,
    val tecnicas: List<Tecnica> = emptyList(),
) {
    val mostrarRejilla: Boolean get() = categoriaFija == null && categorias.size > 1
}

/** Busca en titulo, resumen, categoria y en `t`/`p` de pasos y consejos. */
fun textoBuscableTecnica(t: Tecnica): String =
    buildList {
        add(t.titulo)
        add(t.resumen)
        add(t.categoria)
        t.pasos.forEach {
            add(it.t)
            add(it.p)
        }
        addAll(t.consejos)
    }.joinToString(" ").normalizado()

fun construirEstadoTecnicas(
    todas: List<Tecnica>,
    consulta: String,
    categoriaFija: CategoriaTecnica?,
    disposicion: DisposicionLista = DisposicionLista.TINTE,
): TecnicasUiState {
    val q = consulta.trim().normalizado()
    val delPrefiltro = todas.filter { categoriaFija == null || CategoriaTecnica.de(it.categoria) == categoriaFija }
    return TecnicasUiState(
        cargando = false,
        consulta = consulta,
        categorias = categoriasDe(todas),
        nombres = nombresDe(todas),
        categoriaFija = categoriaFija,
        disposicion = disposicion,
        total = delPrefiltro.size,
        tecnicas =
            delPrefiltro
                .filter { q.isEmpty() || textoBuscableTecnica(it).contains(q) }
                .sortedBy { it.titulo.normalizado() },
    )
}

private fun categoriasDe(todas: List<Tecnica>): List<Pair<CategoriaTecnica, Int>> {
    val cuentas = todas.groupingBy { CategoriaTecnica.de(it.categoria) }.eachCount()
    return CategoriaTecnica.entries.mapNotNull { c -> cuentas[c]?.let { c to it } }
}

/** El nombre de la categoría sale de los datos si todas sus técnicas coinciden en una sola cadena no vacía. */
private fun nombresDe(todas: List<Tecnica>): Map<CategoriaTecnica, String> =
    todas
        .groupBy { CategoriaTecnica.de(it.categoria) }
        .mapNotNull { (categoria, tecnicas) ->
            val cadenas =
                tecnicas
                    .map {
                        it.categoria.trim()
                    }.filter { it.isNotEmpty() }
                    .distinctBy { it.normalizado() }
            cadenas.singleOrNull()?.let { categoria to it.conInicialMayuscula() }
        }.toMap()

/** Recetas que citan la técnica en `tags.tecnica`, por título. */
fun recetasConTecnica(
    slug: String,
    recetas: List<Receta>,
): List<Receta> = recetas.filter { slug in it.tags.tecnica }.sortedBy { it.titulo.normalizado() }
