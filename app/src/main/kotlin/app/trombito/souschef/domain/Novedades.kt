package app.trombito.souschef.domain

import kotlinx.coroutines.flow.StateFlow

/** Una línea de las novedades; [destino] (opcional) es a dónde lleva (p. ej. «recetas» o una pantalla de Ajustes). */
data class LineaNovedad(
    val texto: String,
    val destino: String? = null,
)

/** Novedades de una versión `major.minor` (en `distribution/novedades/<version>.json`). */
data class Novedades(
    val version: String,
    val titulo: String,
    val lineas: List<LineaNovedad>,
)

/** Origen de las novedades empaquetadas. */
interface FuenteNovedades {
    /** Todas las que se pudieron leer, de la más nueva a la más vieja. Una entrada rota se ignora. */
    suspend fun todas(): List<Novedades>
}

/**
 * Si Play tiene una actualización inmediata en curso (su pantalla tapa la app). `null` mientras se comprueba; `false`
 * sin Play o sin actualización. Las novedades esperan a que deje de ser `null`.
 */
interface EstadoActualizacion {
    val inmediataEnCurso: StateFlow<Boolean?>
}

/** Versión instalada, tal como la da el sistema (`major.minor.patch`, con posible sufijo). */
interface VersionApp {
    val nombre: String
}
