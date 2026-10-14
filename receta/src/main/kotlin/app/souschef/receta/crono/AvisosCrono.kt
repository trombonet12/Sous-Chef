package app.souschef.receta.crono

import app.souschef.receta.Paso
import app.souschef.receta.Receta

/** Avisos (§4.11) y cadena de alarmas (ADR-0005). Los flags del estado evitan avisar dos veces. */
object AvisosCrono {
    /** Pasado este tiempo vencido un aviso ya no suena: solo se anota. */
    const val SILENCIO_MS = 90_000L
    private const val DIEZ_S_MS = 10_000L

    private class Pendiente(
        val evento: Evento,
        val texto: String,
        val hito: Int?,
    )

    /** Próximo evento por venir (el más cercano de todos los carriles), o null si no queda ninguno. */
    fun siguienteEvento(
        e: EstadoCrono,
        receta: Receta,
    ): Evento? {
        val plano = Plano(receta)
        return plano.carriles
            .flatMap { pendientes(plano, e, it.id) }
            .map { it.evento }
            .minWithOrNull(compareBy({ it.ms }, { it.tipo.ordinal }))
    }

    /**
     * Avisos cuya hora ya llegó, más el estado con sus flags marcados. Los que llevan > 90 s vencidos salen
     * `silencioso`. El `DIEZ_S` que se salta (el cero ya pasó) se marca sin avisar.
     */
    fun vencidos(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
    ): Vencidos {
        val plano = Plano(receta)
        val avisos = mutableListOf<Aviso>()
        var carriles = e.carriles
        for (c in plano.carriles) {
            val (ec, nuevos) = vencidosCarril(plano, e.copy(carriles = carriles), c.id, ahoraMs) ?: continue
            avisos += nuevos
            carriles = carriles + (c.id to ec)
        }
        return Vencidos(
            avisos.sortedBy { it.instanteMs },
            if (carriles != e.carriles) e.copy(carriles = carriles, actualizadoMs = ahoraMs) else e,
        )
    }

    private fun vencidosCarril(
        plano: Plano,
        e: EstadoCrono,
        carril: String,
        ahoraMs: Long,
    ): Pair<EstadoCarril, List<Aviso>>? {
        val ec0 = e.carriles[carril]
        val p = plano.pasoActual(e, carril)
        if (ec0 == null || p == null) return null
        var ec = rearmaToca(plano, e, ec0, p)
        val avisos = mutableListOf<Aviso>()
        val vencidos =
            pendientes(plano, e.copy(carriles = e.carriles + (carril to ec)), carril).filter {
                it.evento.ms <=
                    ahoraMs
            }
        for (pe in vencidos) {
            ec = marcar(ec, pe)
            val superado = pe.evento.tipo == TipoAviso.DIEZ_S && ahoraMs >= (ec.finAtMs ?: Long.MAX_VALUE)
            if (!superado) avisos += aviso(pe, ahoraMs)
        }
        return ec to avisos
    }

    // El aviso "toca empezar" se rearma mientras el paso está bloqueado.
    private fun rearmaToca(
        plano: Plano,
        e: EstadoCrono,
        ec: EstadoCarril,
        p: Paso,
    ): EstadoCarril =
        if (ec.avisado && sinArrancar(ec) && plano.bloqueado(e, p)) {
            ec.copy(avisado = false)
        } else {
            ec
        }

    private fun sinArrancar(ec: EstadoCarril): Boolean = !ec.corriendo && !ec.pausado && !ec.salto

    private fun aviso(
        pe: Pendiente,
        ahoraMs: Long,
    ): Aviso =
        Aviso(
            tipo = pe.evento.tipo,
            carril = pe.evento.carril,
            paso = pe.evento.paso,
            texto = pe.texto,
            instanteMs = pe.evento.ms,
            silencioso = ahoraMs - pe.evento.ms > SILENCIO_MS,
        )

    private fun marcar(
        ec: EstadoCarril,
        pe: Pendiente,
    ): EstadoCarril =
        when (pe.evento.tipo) {
            TipoAviso.HITO -> ec.copy(hitosAvisados = ec.hitosAvisados + checkNotNull(pe.hito))
            TipoAviso.DIEZ_S -> ec.copy(a10 = true)
            TipoAviso.FIN -> ec.copy(aFin = true)
            TipoAviso.TOCA_EMPEZAR -> ec.copy(avisado = true)
        }

    /** Eventos del paso actual del carril que aún no se han avisado, con su instante. */
    private fun pendientes(
        plano: Plano,
        e: EstadoCrono,
        carril: String,
    ): List<Pendiente> {
        val ec = e.carriles[carril]
        val p = plano.pasoActual(e, carril)
        if (ec == null || p == null) return emptyList()
        val fin = ec.finAtMs
        return when {
            fin != null -> {
                pendientesCorriendo(ec, p, carril, fin)
            }

            !ec.pausado && !ec.avisado && (ec.salto || !plano.bloqueado(e, p)) -> {
                listOf(
                    Pendiente(
                        Evento(plano.sugeridoMs(e, p), TipoAviso.TOCA_EMPEZAR, carril, p.t),
                        "Toca empezar: ${p.t}",
                        null,
                    ),
                )
            }

            else -> {
                emptyList()
            }
        }
    }

    private fun pendientesCorriendo(
        ec: EstadoCarril,
        p: Paso,
        carril: String,
        fin: Long,
    ): List<Pendiente> {
        val hitos =
            p.hitosValidos.mapIndexedNotNull { i, h ->
                if (i in ec.hitosAvisados) {
                    null
                } else {
                    val ms = fin - (p.segundos - h.en.toLong()) * 1000L
                    Pendiente(Evento(ms, TipoAviso.HITO, carril, p.t), h.t, i)
                }
            }
        val diez =
            if (ec.a10) {
                null
            } else {
                Pendiente(
                    Evento(fin - DIEZ_S_MS, TipoAviso.DIEZ_S, carril, p.t),
                    "${p.t}: quedan 10 s",
                    null,
                )
            }
        val cero = if (ec.aFin) null else Pendiente(Evento(fin, TipoAviso.FIN, carril, p.t), "${p.t}: hecho", null)
        return hitos + listOfNotNull(diez, cero)
    }
}
