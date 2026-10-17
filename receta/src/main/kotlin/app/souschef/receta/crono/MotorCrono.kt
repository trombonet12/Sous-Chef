package app.souschef.receta.crono

import app.souschef.receta.Paso
import app.souschef.receta.Receta

/**
 * Mandos del cronómetro (§4.10): funciones puras `(estado, receta, carril, ahoraMs) -> estado'`.
 * Un mando sobre un carril terminado o inexistente devuelve el estado tal cual.
 */
object MotorCrono {
    private const val EXTRA_S = 30

    /** Play: arranca el paso. Si hay dependencias sin terminar no hace nada (usar [empezarIgual]). */
    fun empezar(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
    ): EstadoCrono = arrancar(e, receta, carril, ahoraMs, forzar = false)

    /** "Empezar igual": arranca saltándose las dependencias pendientes (`salto = true`). */
    fun empezarIgual(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
    ): EstadoCrono = arrancar(e, receta, carril, ahoraMs, forzar = true)

    fun pausar(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
    ): EstadoCrono =
        modificar(e, receta, carril, ahoraMs) { ec, p ->
            if (!ec.corriendo) ec else ec.copy(finAtMs = null, restanteS = quedanS(ec, p, ahoraMs), pausado = true)
        }

    fun reanudar(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
    ): EstadoCrono = if (e.carriles[carril]?.pausado == true) arrancar(e, receta, carril, ahoraMs, forzar = true) else e

    /** +30 s: atajo de [prorrogar]. */
    fun masTreinta(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
    ): EstadoCrono = prorrogar(e, receta, carril, EXTRA_S, ahoraMs)

    /** +[segundos]: vale corriendo, parado y vencido; rearma `aFin` si queda > 0 y `a10` si queda > 10. */
    fun prorrogar(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        segundos: Int,
        ahoraMs: Long,
    ): EstadoCrono =
        modificar(e, receta, carril, ahoraMs) { ec, p ->
            val nuevo =
                if (ec.corriendo) {
                    ec.copy(finAtMs = ec.finAtMs?.plus(segundos * 1000L))
                } else {
                    ec.copy(restanteS = (ec.restanteS ?: p.segundos) + segundos)
                }
            val q = quedanS(nuevo, p, ahoraMs)
            nuevo.copy(aFin = nuevo.aFin && q <= 0, a10 = nuevo.a10 && q <= 10)
        }

    /**
     * Hecho: guarda los segundos reales (mín. 1) y pasa al siguiente paso del carril o lo da por terminado.
     * Se puede pulsar sin haber arrancado el reloj.
     */
    fun hecho(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
    ): EstadoCrono {
        val plano = Plano(receta)
        val ec = e.carriles[carril]
        val p = plano.pasoActual(e, carril)
        if (ec == null || p == null) return e
        val real = maxOf(1L, redondeaS(ahoraMs - (ec.inicioPasoMs ?: ahoraMs))).toInt()
        val siguiente = plano.colas.getValue(carril).getOrNull(ec.i + 1)
        val nuevo =
            if (siguiente == null) {
                EstadoCarril(i = ec.i, fin = true)
            } else {
                EstadoCarril(
                    i = ec.i + 1,
                    avisado = plano.sugeridoMs(e, siguiente) <= ahoraMs,
                    inicioPasoMs = ahoraMs,
                )
            }
        val base =
            e.copy(
                carriles = e.carriles + (carril to nuevo),
                pasosReales = e.pasosReales + (p.id to real),
                actualizadoMs = ahoraMs,
            )
        return if (siguiente == null) {
            base.copy(foco = ConsultasCrono.siguienteAccionable(base, receta, ahoraMs) ?: base.foco)
        } else {
            base
        }
    }

    private fun arrancar(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
        forzar: Boolean,
    ): EstadoCrono {
        val plano = Plano(receta)
        return modificar(e, receta, carril, ahoraMs) { ec, p ->
            val bloqueado = !forzar && !ec.pausado && !ec.salto && plano.bloqueado(e, p)
            if (ec.corriendo || bloqueado) {
                ec
            } else {
                ec.copy(
                    finAtMs = ahoraMs + (ec.restanteS ?: p.segundos) * 1000L,
                    restanteS = null,
                    pausado = false,
                    salto = true,
                )
            }
        }
    }

    private fun modificar(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
        cambio: (EstadoCarril, Paso) -> EstadoCarril,
    ): EstadoCrono {
        val ec = e.carriles[carril]
        val p = Plano(receta).pasoActual(e, carril)
        if (ec == null || p == null) return e
        val nuevo = cambio(ec, p)
        return if (nuevo == ec) e else e.copy(carriles = e.carriles + (carril to nuevo), actualizadoMs = ahoraMs)
    }
}
