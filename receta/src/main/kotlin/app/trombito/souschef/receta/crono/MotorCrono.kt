package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta

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
                EstadoCarril(i = ec.i + 1, inicioPasoMs = ahoraMs)
            }
        val base =
            e.copy(
                carriles = e.carriles + (carril to nuevo),
                pasosReales = e.pasosReales + (p.id to real),
                finRealMs = e.finRealMs + (p.id to ahoraMs),
                actualizadoMs = ahoraMs,
                // Se reinserta al final: el orden del mapa es el orden de los Hecho (para «deshaz» sin carril).
                deshacer = e.deshacer - carril + (carril to ec),
                ultimoHecho = carril,
            )
        return if (siguiente == null) {
            base.copy(foco = ConsultasCrono.siguienteAccionable(base, receta, ahoraMs) ?: base.foco)
        } else {
            // Avisado si su hora (ya con este Hecho) es firme y ha llegado: el cocinero está delante y no hay que
            // avisarle. Una hora flotante no cuenta: al fijarse después (arranca el compañero) su aviso debe sonar.
            val prev = plano.previstos(base, ahoraMs)[siguiente.id]
            val avisado = prev != null && prev.firme && prev.inicioMs <= ahoraMs
            base.copy(carriles = base.carriles + (carril to nuevo.copy(avisado = avisado)))
        }
    }

    /**
     * Deshace el último Hecho de [carril] (o, sin carril, el último Hecho de la cocina): el carril vuelve tal cual
     * estaba, con su reloj, pausa e hitos avisados, y se olvida el tiempo real que se guardó. Un solo nivel por carril;
     * sin nada que deshacer devuelve el estado tal cual.
     */
    fun deshacer(
        e: EstadoCrono,
        receta: Receta,
        carril: String?,
        ahoraMs: Long,
    ): EstadoCrono {
        val id = carril ?: e.ultimoHecho
        val previo = id?.let { e.deshacer[it] }
        if (id == null || previo == null) return e
        val paso = Plano(receta).colas[id]?.getOrNull(previo.i)
        return e.sinDeshacer(id).copy(
            carriles = e.carriles + (id to previo),
            pasosReales = paso?.let { e.pasosReales - it.id } ?: e.pasosReales,
            finRealMs = paso?.let { e.finRealMs - it.id } ?: e.finRealMs,
            actualizadoMs = ahoraMs,
        )
    }

    private fun arrancar(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
        forzar: Boolean,
    ): EstadoCrono {
        val plano = Plano(receta)
        val nuevo =
            modificar(e, receta, carril, ahoraMs) { ec, p ->
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
        // Primer arranque del paso (ADR-0018): reanudar una pausa no lo pisa.
        val primero =
            plano.pasoActual(e, carril)?.takeIf { e.carriles[carril]?.pausado != true && it.id !in e.inicioRealMs }
        return if (nuevo != e && primero != null) {
            nuevo.copy(inicioRealMs = nuevo.inicioRealMs + (primero.id to ahoraMs))
        } else {
            nuevo
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
        // Tocar el carril (empezar, pausa, prórroga) confirma el Hecho anterior: deshacerlo ya perdería ese trabajo.
        return if (nuevo == ec) {
            e
        } else {
            e.sinDeshacer(carril).copy(carriles = e.carriles + (carril to nuevo), actualizadoMs = ahoraMs)
        }
    }
}
