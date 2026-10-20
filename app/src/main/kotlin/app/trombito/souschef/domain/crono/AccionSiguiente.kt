package app.trombito.souschef.domain.crono

import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.ConsultasCrono
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.MotorCrono

/** Resultado de "Siguiente": estado nuevo y carril afectado (null si no había nada que hacer). */
data class ResultadoSiguiente(
    val estado: EstadoCrono,
    val carril: String?,
)

/**
 * Acción "Siguiente" de la notificación: Hecho del paso más urgente y arranque del siguiente de ese carril.
 * Más urgente = el vencido esperando Hecho; si no, el que acaba antes. Si nada corre, arranca el próximo accionable.
 */
object AccionSiguiente {
    fun aplicar(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
    ): ResultadoSiguiente {
        val urgente =
            e.carriles
                .filter { (_, ec) -> !ec.fin && ec.finAtMs != null }
                .minByOrNull { it.value.finAtMs ?: Long.MAX_VALUE }
                ?.key
        return if (urgente ==
            null
        ) {
            arrancarAccionable(e, receta, ahoraMs)
        } else {
            hechoYSiguiente(e, receta, urgente, ahoraMs)
        }
    }

    private fun arrancarAccionable(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
    ): ResultadoSiguiente {
        val accionable = ConsultasCrono.siguienteAccionable(e, receta, ahoraMs)
        val nuevo = accionable?.let { MotorCrono.empezar(e, receta, it, ahoraMs) } ?: e
        return ResultadoSiguiente(nuevo, accionable.takeIf { nuevo != e })
    }

    private fun hechoYSiguiente(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
    ): ResultadoSiguiente {
        val hecho = MotorCrono.hecho(e, receta, carril, ahoraMs)
        val sigue = hecho.carriles[carril]?.fin == false
        return ResultadoSiguiente(if (sigue) MotorCrono.empezar(hecho, receta, carril, ahoraMs) else hecho, carril)
    }
}
