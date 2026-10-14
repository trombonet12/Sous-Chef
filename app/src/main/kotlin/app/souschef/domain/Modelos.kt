package app.souschef.domain

/** Paso realmente cronometrado al cocinar (segundos desde que se abrió el paso hasta Hecho). */
data class PasoReal(
    val id: String,
    val t: String,
    val segundos: Int,
)

/** Una vez que se cocinó una receta. `id` es estable (legado: `<slug>-<fecha>[-HHMMSS]`). */
data class EntradaHistorial(
    val id: String,
    val slug: String,
    val titulo: String,
    val fecha: String,
    val base: String? = null,
    val tecnica: List<String> = emptyList(),
    val raciones: Int? = null,
    val minutosReales: Int? = null,
    val nota: String? = null,
    val pasosReales: List<PasoReal> = emptyList(),
)

data class Preferencia(
    val slug: String,
    val favorito: Boolean = false,
    val raciones: Int? = null,
)

enum class EstadoDespensa { HAY, POCO, FALTA, VETADO }

data class ItemDespensa(
    val id: Long = 0,
    val nombre: String,
    val categoria: String,
    val estado: EstadoDespensa = EstadoDespensa.HAY,
    val nota: String? = null,
)

data class ItemCompra(
    val id: Long = 0,
    val texto: String,
    val zona: String = "",
    val origen: String? = null,
    val marcado: Boolean = false,
    val orden: Int = 0,
)

/** Estado serializado del cronómetro de una receta (el JSON lo interpreta el motor, no el repositorio). */
data class CronoEstado(
    val slug: String,
    val json: String,
    val actualizadoMs: Long,
)
