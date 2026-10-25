package app.trombito.souschef.data.resena

import android.app.Activity
import app.trombito.souschef.data.actualizacion.DecisionActualizacion
import app.trombito.souschef.domain.resena.LanzadorResena
import app.trombito.souschef.domain.resena.ResultadoResena
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.CancellationException

/**
 * Pide la reseña con In-App Review (F14). Solo si la app se instaló desde Play: en debug y con Obtainium no hace nada.
 * No pide permisos ni red propia: habla con la app de Play del móvil, como In-App Updates (ADR-0013).
 */
class ResenaPlay(
    private val manager: ReviewManager,
    private val instalador: () -> String?,
    private val actividad: () -> Activity?,
) : LanzadorResena {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun lanzar(): ResultadoResena {
        if (instalador() != DecisionActualizacion.INSTALADOR_PLAY) return ResultadoResena.NO_DISPONIBLE
        return try {
            val info = manager.requestReview()
            // Si la actividad se fue mientras Play contestaba, no hay dónde pintar: fallo de esta sesión.
            val pantalla = actividad()
            if (pantalla == null) {
                ResultadoResena.FALLO
            } else {
                manager.launchReview(pantalla, info)
                ResultadoResena.PEDIDA
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoResena.FALLO
        }
    }
}
