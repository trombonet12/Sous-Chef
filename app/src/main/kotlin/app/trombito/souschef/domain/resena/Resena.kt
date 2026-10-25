package app.trombito.souschef.domain.resena

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.HistorialRepo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex

/** Qué pasó al pedir la reseña a Play. */
enum class ResultadoResena {
    /** El flujo de Play terminó (Play decide si enseña o no el diálogo; no dice si el usuario valoró). */
    PEDIDA,

    /** La app no viene de Play (debug, Obtainium...): no hay a quién pedirle nada. */
    NO_DISPONIBLE,

    /** La API de Play falló: no se reintenta en esta sesión. */
    FALLO,
}

/** Pide la reseña a Play (In-App Review, F14). La implementación real vive en `data/resena`. */
fun interface LanzadorResena {
    suspend fun lanzar(): ResultadoResena
}

/** Sin Play: lo que usan debug y los tests que no miran la reseña. */
object SinResena : LanzadorResena {
    override suspend fun lanzar(): ResultadoResena = ResultadoResena.NO_DISPONIBLE
}

/** Guarda en `meta` (`play.resena.pedida`) que la reseña ya se pidió; es para siempre. */
interface ResenaRepo {
    suspend fun yaPedida(): Boolean

    suspend fun marcarPedida()
}

/** Reglas puras de cuándo pedir la reseña. */
object ReglaResena {
    /** Se pide tras la 3.ª cocinada cerrada. */
    const val COCINADAS_MINIMAS = 3

    /** Clave de `meta` que guarda que ya se pidió. */
    const val CLAVE_PEDIDA = "play.resena.pedida"

    /** Cocinadas cerradas: las del historial con duración real (las anotadas a mano no la tienen). */
    fun cerradas(historial: List<EntradaHistorial>): Int = historial.count { (it.minutosReales ?: 0) > 0 }

    fun debePedir(
        cerradas: Int,
        yaPedida: Boolean,
        hayViva: Boolean,
        falloEnSesion: Boolean,
    ): Boolean = !yaPedida && !falloEnSesion && !hayViva && cerradas >= COCINADAS_MINIMAS
}

/**
 * Pide la reseña una sola vez. [alVolverDelCrono] se llama cuando la app vuelve a Recetas o a la Ficha tras el
 * cronómetro; nunca se llama desde el cronómetro. Sin botón ni texto propio.
 */
class GestorResena(
    private val historial: HistorialRepo,
    private val repo: ResenaRepo,
    private val hayViva: suspend () -> Boolean,
    private val lanzador: LanzadorResena,
) {
    private val ocupado = Mutex()

    @Volatile private var falloEnSesion = false

    @Volatile private var sinPlayEnSesion = false

    suspend fun alVolverDelCrono() {
        // Una sola petición a la vez, y nada de esperar: si ya hay una en marcha, esta sobra.
        if (sinPlayEnSesion || !ocupado.tryLock()) return
        try {
            val cerradas = ReglaResena.cerradas(historial.observarTodo().first())
            val pedir =
                ReglaResena.debePedir(
                    cerradas = cerradas,
                    yaPedida = repo.yaPedida(),
                    hayViva = hayViva(),
                    falloEnSesion = falloEnSesion,
                )
            if (!pedir) return
            when (lanzador.lanzar()) {
                ResultadoResena.PEDIDA -> repo.marcarPedida()
                ResultadoResena.NO_DISPONIBLE -> sinPlayEnSesion = true
                ResultadoResena.FALLO -> falloEnSesion = true
            }
        } finally {
            ocupado.unlock()
        }
    }
}
