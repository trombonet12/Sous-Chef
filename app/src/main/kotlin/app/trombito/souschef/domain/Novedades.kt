package app.trombito.souschef.domain

/** Una línea de las novedades; [destino] (opcional) es a dónde lleva (p. ej. «recetas» o una pantalla de Ajustes). */
data class LineaNovedad(
    val texto: String,
    val destino: String? = null,
)

/** Novedades de una versión `major.minor` (en `distribution/novedades/<version>.json`). */
data class Novedades(
    val version: String,
    val titulo: String,
    val destacar: Boolean,
    val lineas: List<LineaNovedad>,
)

/** Origen de las novedades empaquetadas. */
interface FuenteNovedades {
    /** Todas las que se pudieron leer, de la más nueva a la más vieja. Una entrada rota se ignora. */
    suspend fun todas(): List<Novedades>
}

/** Versión instalada, tal como la da el sistema (`major.minor.patch`, con posible sufijo). */
interface VersionApp {
    val nombre: String
}
