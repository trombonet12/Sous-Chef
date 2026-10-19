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

/**
 * Cómo se muestran los carriles del cronómetro: uno en foco y el resto compacto, todos apilados, o la clásica de antes
 * de la v0.6 (pestañas, un panel por recipiente con scroll y mandos abajo).
 */
enum class DisposicionCrono { FOCO, APILADA, CLASICA }

/** Tarjetas de la lista de recetas: tinte de la base, franja de color o la clásica con mini-gantt. */
enum class DisposicionLista { TINTE, FRANJA, CLASICA }

/**
 * Orden de la lista de recetas: favoritas y luego las más recientes, las que hace más que no se cocinan (sin estrenar
 * primero), alfabético o la más rápida primero.
 */
enum class OrdenRecetas { FAVORITAS, OLVIDADAS, ALFABETICO, RAPIDA }

/** Tema de la app (y del cronómetro): el del sistema, claro u oscuro. */
enum class TemaApp {
    AUTOMATICO,
    CLARO,
    OSCURO,
    ;

    fun oscuro(sistemaOscuro: Boolean): Boolean =
        when (this) {
            AUTOMATICO -> sistemaOscuro
            CLARO -> false
            OSCURO -> true
        }
}

/** Qué hora enseñan las tarjetas de la lista: cuándo empezar para servir a tiempo, o cuándo acabas si empiezas ya. */
enum class HoraTarjeta { EMPEZAR, ACABAR }

/** Hora de servir por defecto (15:30), en minutos del día. */
const val SERVIR_DEFECTO_MIN = 15 * 60 + 30

/** Preferencias globales de la app. */
data class Ajustes(
    val disposicion: DisposicionCrono = DisposicionCrono.APILADA,
    val vozActiva: Boolean = true,
    val lista: DisposicionLista = DisposicionLista.TINTE,
    val orden: OrdenRecetas = OrdenRecetas.FAVORITAS,
    val tema: TemaApp = TemaApp.AUTOMATICO,
    /** Punto de partida de la ficha y del cronómetro, en minutos del día. */
    val servirMin: Int = SERVIR_DEFECTO_MIN,
    val horaTarjeta: HoraTarjeta = HoraTarjeta.EMPEZAR,
)

/** Estado serializado del cronómetro de una receta (el JSON lo interpreta el motor, no el repositorio). */
data class CronoEstado(
    val slug: String,
    val json: String,
    val actualizadoMs: Long,
)
