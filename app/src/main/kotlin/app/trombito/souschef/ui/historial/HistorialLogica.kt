package app.trombito.souschef.ui.historial

import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.ui.comun.fechaCorta
import app.trombito.souschef.ui.comun.normalizado
import java.time.LocalDate

data class FilaHistorial(
    val entrada: EntradaHistorial,
    /** La entrada siguiente (más vieja) tiene la misma base: se cocinó dos veces seguidas. */
    val repite: Boolean,
    val recetaExiste: Boolean,
)

data class HistorialUiState(
    val cargando: Boolean = true,
    val consulta: String = "",
    val total: Int = 0,
    val filas: List<FilaHistorial> = emptyList(),
    /** Id de la entrada que espera el segundo toque para borrarse. */
    val pendienteBorrar: String? = null,
    /** Diseño de tarjeta elegido en Ajustes. */
    val disposicion: DisposicionLista = DisposicionLista.TINTE,
)

/** "2026-10-14" -> ("14", "oct"); null si la fecha no se entiende. */
fun diaYMes(fecha: String): Pair<String, String>? {
    runCatching { LocalDate.parse(fecha.take(10)) }.getOrNull() ?: return null
    val partes = fechaCorta(fecha).split(' ', limit = 2)
    return if (partes.size == 2) partes[0] to partes[1] else null
}

/** La marca de repetición se calcula sobre la lista completa, no sobre la filtrada. */
fun marcarRepeticiones(
    entradas: List<EntradaHistorial>,
    slugsExistentes: Set<String>,
): List<FilaHistorial> =
    entradas.mapIndexed { i, e ->
        val siguiente = entradas.getOrNull(i + 1)
        FilaHistorial(
            entrada = e,
            repite = e.base != null && siguiente?.base == e.base,
            recetaExiste = e.slug in slugsExistentes,
        )
    }

/** Busca en titulo, slug, base, nota y técnicas. */
fun coincideHistorial(
    e: EntradaHistorial,
    consulta: String,
): Boolean {
    val q = consulta.trim().normalizado()
    return q.isEmpty() ||
        (listOf(e.titulo, e.slug, e.base.orEmpty(), e.nota.orEmpty()) + e.tecnica)
            .joinToString(" ")
            .normalizado()
            .contains(q)
}

fun construirEstadoHistorial(
    entradas: List<EntradaHistorial>,
    slugsExistentes: Set<String>,
    consulta: String,
    pendienteBorrar: String?,
    disposicion: DisposicionLista = DisposicionLista.TINTE,
): HistorialUiState =
    HistorialUiState(
        cargando = false,
        consulta = consulta,
        total = entradas.size,
        filas = marcarRepeticiones(entradas, slugsExistentes).filter { coincideHistorial(it.entrada, consulta) },
        pendienteBorrar = pendienteBorrar,
        disposicion = disposicion,
    )
