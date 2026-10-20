package app.trombito.souschef.ui.tecnicas

import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tecnica
import app.trombito.souschef.ui.comun.contarPor
import app.trombito.souschef.ui.comun.normalizado

data class TecnicasUiState(
    val cargando: Boolean = true,
    val consulta: String = "",
    val categoriaActiva: String? = null,
    val categorias: List<Pair<String, Int>> = emptyList(),
    val coincidentes: Int = 0,
    val total: Int = 0,
    val tecnicas: List<Tecnica> = emptyList(),
) {
    val mostrarChips: Boolean get() = categorias.size > 1
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
    categoriaElegida: String?,
): TecnicasUiState {
    val q = consulta.trim().normalizado()
    val porTexto = todas.filter { q.isEmpty() || textoBuscableTecnica(it).contains(q) }
    val categorias = porTexto.contarPor { it.categoria.ifBlank { null } }
    val categoriaActiva = categoriaElegida?.takeIf { c -> categorias.any { it.first == c } }
    return TecnicasUiState(
        cargando = false,
        consulta = consulta,
        categoriaActiva = categoriaActiva,
        categorias = categorias,
        coincidentes = porTexto.size,
        total = todas.size,
        tecnicas =
            porTexto
                .filter { categoriaActiva == null || it.categoria == categoriaActiva }
                .sortedBy { it.titulo.normalizado() },
    )
}

/** Recetas que citan la técnica en `tags.tecnica`, por título. */
fun recetasConTecnica(
    slug: String,
    recetas: List<Receta>,
): List<Receta> = recetas.filter { slug in it.tags.tecnica }.sortedBy { it.titulo.normalizado() }
