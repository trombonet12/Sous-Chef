package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta

/** Avisos (§4.11) y cadena de alarmas (ADR-0005). Los flags del estado evitan avisar dos veces. */
object AvisosCrono {
    /** Pasado este tiempo vencido un aviso ya no suena: solo se anota. */
    const val SILENCIO_MS = 90_000L

    private class Pendiente(
        val evento: Evento,
        val texto: String,
        val hito: Int?,
    )

    /**
     * Próximo evento por venir (el más cercano de todos los carriles), o null si no queda ninguno. «Toca empezar» solo
     * cuenta si su hora prevista es firme (ADR-0018): una hora que se mueve con [ahoraMs] no arma alarma.
     */
    fun siguienteEvento(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
    ): Evento? {
        val plano = Plano(receta)
        val previstos = plano.previstos(e, ahoraMs)
        val deCarriles = plano.carriles.flatMap { pendientes(plano, e, it.id, previstos) }
        return (deCarriles.map { it.evento } + TemporizadoresLibres.eventos(e))
            .minWithOrNull(compareBy({ it.ms }, { it.tipo.ordinal }))
    }

    /** Como [siguienteEvento], pero ignorando el carril [excepto] (el que se está mirando). */
    fun siguienteEventoFuera(
        e: EstadoCrono,
        receta: Receta,
        excepto: String?,
        ahoraMs: Long,
    ): Evento? {
        val plano = Plano(receta)
        val previstos = plano.previstos(e, ahoraMs)
        val deCarriles =
            plano.carriles.filter { it.id != excepto }.flatMap { pendientes(plano, e, it.id, previstos) }
        return (deCarriles.map { it.evento } + TemporizadoresLibres.eventos(e))
            .minWithOrNull(compareBy({ it.ms }, { it.tipo.ordinal }))
    }

    /**
     * Avisos cuya hora ya llegó, más el estado con sus flags marcados. Los que llevan > 90 s vencidos salen
     * `silencioso`. Ya no hay aviso de 10 s: los estados viejos con `a10` se leen sin usarlo.
     */
    fun vencidos(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
    ): Vencidos {
        val plano = Plano(receta)
        val avisos = mutableListOf<Aviso>()
        var carriles = e.carriles
        // Las horas previstas no dependen de los flags `avisado`: se calculan una vez para todos los carriles.
        val previstos = plano.previstos(e, ahoraMs)
        for (c in plano.carriles) {
            val (ec, nuevos) = vencidosCarril(plano, e.copy(carriles = carriles), c.id, ahoraMs, previstos) ?: continue
            avisos += nuevos
            carriles = carriles + (c.id to ec)
        }
        val delCarriles = if (carriles != e.carriles) e.copy(carriles = carriles, actualizadoMs = ahoraMs) else e
        val libres = TemporizadoresLibres.vencidos(delCarriles, ahoraMs)
        val estado = libres.fold(delCarriles) { acc, l -> TemporizadoresLibres.marcarAvisado(acc, l.id, ahoraMs) }
        return Vencidos(
            (avisos + libres.map { TemporizadoresLibres.aviso(it, ahoraMs) }).sortedBy { it.instanteMs },
            estado,
        )
    }

    private fun vencidosCarril(
        plano: Plano,
        e: EstadoCrono,
        carril: String,
        ahoraMs: Long,
        previstos: Map<String, PasoPrevisto>,
    ): Pair<EstadoCarril, List<Aviso>>? {
        val ec0 = e.carriles[carril]
        val p = plano.pasoActual(e, carril)
        if (ec0 == null || p == null) return null
        var ec = rearmaToca(plano, e, ec0, p)
        val avisos = mutableListOf<Aviso>()
        val vencidos =
            pendientes(plano, e.copy(carriles = e.carriles + (carril to ec)), carril, previstos).filter {
                it.evento.ms <= ahoraMs
            }
        for (pe in vencidos) {
            ec = marcar(ec, pe)
            avisos += aviso(pe, ahoraMs)
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

    @Suppress("DEPRECATION")
    private fun marcar(
        ec: EstadoCarril,
        pe: Pendiente,
    ): EstadoCarril =
        when (pe.evento.tipo) {
            TipoAviso.HITO -> ec.copy(hitosAvisados = ec.hitosAvisados + checkNotNull(pe.hito))
            TipoAviso.DIEZ_S -> ec
            TipoAviso.FIN -> ec.copy(aFin = true)
            TipoAviso.TOCA_EMPEZAR -> ec.copy(avisado = true)
            TipoAviso.TEMPORIZADOR -> ec // los libres no son carriles: se marcan aparte
        }

    /** Eventos del paso actual del carril que aún no se han avisado, con su instante. */
    private fun pendientes(
        plano: Plano,
        e: EstadoCrono,
        carril: String,
        previstos: Map<String, PasoPrevisto>,
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
                // Solo con hora firme: si se moviera con «ahora», la alarma se re-armaría en bucle sin avisar.
                previstos[p.id]?.takeIf { it.firme }?.let {
                    listOf(
                        Pendiente(
                            Evento(it.inicioMs, TipoAviso.TOCA_EMPEZAR, carril, p.t),
                            OrdenPaso.de(p).avisoEmpezar(),
                            null,
                        ),
                    )
                } ?: emptyList()
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
        val textoFin = OrdenPaso.de(p).avisoHecho()
        val cero = if (ec.aFin) null else Pendiente(Evento(fin, TipoAviso.FIN, carril, p.t), textoFin, null)
        return hitos + listOfNotNull(cero)
    }
}
