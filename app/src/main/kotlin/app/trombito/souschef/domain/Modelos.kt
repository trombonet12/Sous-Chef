package app.trombito.souschef.domain

import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Equipo

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
    /** Cómo salió, para decidir si repetirla; null si no se valoró. */
    val valoracion: Valoracion? = null,
    /** Total previsto (min) de la receta escalada en esa cocinada; null en las anteriores a la v1.7. */
    val previstoMin: Int? = null,
)

/** Valoración de una cocinada. `codigo` es lo que se guarda en Room y en la copia; no cambiarlo. */
enum class Valoracion(
    val codigo: String,
) {
    REPETIR("repetir"),
    AJUSTAR("ajustar"),
    NO_REPETIR("no_repetir"),
    ;

    companion object {
        /** La valoración de [codigo], o null si es null o no se conoce (un código futuro no tumba la lectura). */
        fun deCodigo(codigo: String?): Valoracion? = entries.firstOrNull { it.codigo == codigo }
    }
}

data class Preferencia(
    val slug: String,
    val favorito: Boolean = false,
    val raciones: Int? = null,
    /** «Quiero hacerla»: la receta queda marcada para cocinarla pronto. */
    val quiereHacerla: Boolean = false,
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
    /** Hora de cenar (por defecto 21:00): la de las recetas que son de cena (`horaServir`). */
    val cenarMin: Int = CENAR_DEFECTO_MIN,
    val horaTarjeta: HoraTarjeta = HoraTarjeta.EMPEZAR,
    /** Cocina del usuario (ADR-0014); las recetas se traducen a ella al mostrarse. */
    val cocina: Cocina = Cocina.VITRO_9,
    /** Fuegos de la cocina, 1..[FUEGOS_MAX]. */
    val fuegos: Int = FUEGOS_DEFECTO,
    val horno: Boolean = true,
    /**
     * Aparatos que tiene el usuario («Tu equipo»); por defecto todos, para no esconder nada a quien no lo configura.
     * El horno refleja [horno], que sigue siendo su fuente.
     */
    val equipo: Set<Equipo> = Equipo.entries.toSet(),
    /** Raciones con las que se abre una receta sin elección previa, 1..[RACIONES_MAX]. */
    val racionesDefecto: Int = RACIONES_DEFECTO,
    /** La intro de primer uso ya se vio. */
    val introVista: Boolean = false,
    /** La guía del cronómetro ya se vio. */
    val guiaCronoVista: Boolean = false,
    /** `major.minor` de las últimas novedades enseñadas; null = ninguna (viene de antes de las novedades). */
    val novedadesVistas: String? = null,
)

const val FUEGOS_MIN = 1
const val FUEGOS_MAX = 6
const val FUEGOS_DEFECTO = 3
const val RACIONES_MIN = 1
const val RACIONES_MAX = 6
const val RACIONES_DEFECTO = 2

/** Estado serializado del cronómetro de una receta (el JSON lo interpreta el motor, no el repositorio). */
data class CronoEstado(
    val slug: String,
    val json: String,
    val actualizadoMs: Long,
)
