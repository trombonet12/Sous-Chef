package app.souschef.ui.despensa

import app.souschef.domain.EstadoDespensa
import app.souschef.domain.ItemCompra
import app.souschef.domain.ItemDespensa

enum class VistaDespensa { DESPENSA, COMPRA }

const val CATEGORIA_POR_DEFECTO = "Otros"

/** Grupo de la lista de despensa. [esVetados] es el grupo especial "Vetados" (siempre el último). */
data class GrupoDespensa(
    val categoria: String,
    val items: List<ItemDespensa>,
    val esVetados: Boolean = false,
)

/** Grupo de la lista de compra; [clave] vacía = items sin zona ni origen. */
data class GrupoCompra(
    val clave: String,
    val items: List<ItemCompra>,
)

data class DespensaUiState(
    val vista: VistaDespensa = VistaDespensa.DESPENSA,
    val cargando: Boolean = true,
    val consulta: String = "",
    val filtro: EstadoDespensa? = null,
    val totalDespensa: Int = 0,
    val grupos: List<GrupoDespensa> = emptyList(),
    val categorias: List<String> = emptyList(),
    val totalCompra: Int = 0,
    val grupoCompra: List<GrupoCompra> = emptyList(),
    val textoCompra: String = "",
    val hayMarcados: Boolean = false,
    /** Nº de elementos pasados a la compra en la última acción (null = sin aviso). */
    val avisoPasados: Int? = null,
)

/** HAY -> POCO -> FALTA -> HAY. VETADO no cicla. */
fun siguienteEstado(estado: EstadoDespensa): EstadoDespensa =
    when (estado) {
        EstadoDespensa.HAY -> EstadoDespensa.POCO
        EstadoDespensa.POCO -> EstadoDespensa.FALTA
        EstadoDespensa.FALTA -> EstadoDespensa.HAY
        EstadoDespensa.VETADO -> EstadoDespensa.VETADO
    }

private fun normalizar(texto: String): String = texto.trim().lowercase()

fun agruparDespensa(
    items: List<ItemDespensa>,
    consulta: String,
    filtro: EstadoDespensa?,
): List<GrupoDespensa> {
    val q = normalizar(consulta)
    val visibles =
        items
            .filter { filtro == null || it.estado == filtro }
            .filter { q.isEmpty() || normalizar(it.nombre).contains(q) || normalizar(it.categoria).contains(q) }
            .sortedBy { normalizar(it.nombre) }
    val (vetados, resto) = visibles.partition { it.estado == EstadoDespensa.VETADO }
    val normales =
        resto
            .groupBy { it.categoria.trim().ifEmpty { CATEGORIA_POR_DEFECTO } }
            .toSortedMap(compareBy { normalizar(it) })
            .map { (c, l) -> GrupoDespensa(c, l) }
    return if (vetados.isEmpty()) normales else normales + GrupoDespensa("", vetados, esVetados = true)
}

fun categoriasExistentes(items: List<ItemDespensa>): List<String> =
    items
        .filter { it.estado != EstadoDespensa.VETADO }
        .map { it.categoria.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { normalizar(it) }
        .sortedBy { normalizar(it) }

/** Nombres de FALTA y POCO que aún no están en la compra (comparación sin mayúsculas ni espacios extremos). */
fun textosParaCompra(
    despensa: List<ItemDespensa>,
    compra: List<ItemCompra>,
): List<String> {
    val presentes = compra.map { normalizar(it.texto) }.toMutableSet()
    return despensa
        .filter { it.estado == EstadoDespensa.FALTA || it.estado == EstadoDespensa.POCO }
        .map { it.nombre.trim() }
        .filter { it.isNotEmpty() && presentes.add(normalizar(it)) }
}

fun agruparCompra(items: List<ItemCompra>): List<GrupoCompra> =
    items
        .sortedBy { it.orden }
        .groupBy { it.zona.trim().ifEmpty { it.origen?.trim().orEmpty() } }
        .map { (k, l) -> GrupoCompra(k, l) }
        .sortedBy { it.clave.isEmpty() }

/** Texto plano de lo pendiente (sin marcar), con la zona/origen como cabecera. */
fun textoCompra(grupos: List<GrupoCompra>): String =
    grupos
        .mapNotNull { g ->
            val lineas = g.items.filterNot { it.marcado }.map { "- ${it.texto}" }
            when {
                lineas.isEmpty() -> null
                g.clave.isEmpty() -> lineas.joinToString("\n")
                else -> (listOf("${g.clave}:") + lineas).joinToString("\n")
            }
        }.joinToString("\n\n")
