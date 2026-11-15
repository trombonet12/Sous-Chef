package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Holgura
import app.trombito.souschef.receta.Receta

/** Consultas puras sobre el estado (§4.3, §4.6, §4.7, §4.9). Nada de reloj global: `ahoraMs` siempre por parámetro. */
@Suppress("TooManyFunctions") // fachada de consultas: una función por pregunta de la UI
object ConsultasCrono {
    private const val UMBRAL_ESPERA_MS = 3 * 60_000L
    private const val VENTANA_PARALELO_MIN = 12

    /** Carriles normalizados: declarados sin duplicados, el no declarado entra gris tipo mesa, sin pasos se quita. */
    fun carriles(receta: Receta): List<Carril> = Plano(receta).carriles

    fun todoTerminado(e: EstadoCrono): Boolean = e.carriles.values.all { it.fin }

    /** `quedan(c)`: 0 si terminado; negativo si el reloj pasó de cero. */
    fun quedan(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
    ): Int {
        val plano = Plano(receta)
        val ec = e.carriles[carril]
        val p = plano.pasoActual(e, carril)
        return if (ec == null || p == null) 0 else quedanS(ec, p, ahoraMs)
    }

    /** Hora prevista de cada paso (ADR-0018): sigue al cocinero, no al reloj de servir. Clave: id de paso. */
    fun previstos(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
    ): Map<String, PasoPrevisto> = Plano(receta).previstos(e, ahoraMs)

    /** Alguien ha arrancado o dado por hecho algún paso: desde ahí la hora de servir ya no mueve nada. */
    fun arrancada(e: EstadoCrono): Boolean = e.inicioRealMs.isNotEmpty() || e.finRealMs.isNotEmpty()

    /**
     * Hora prevista de empezar el paso actual del carril (ADR-0018); null si terminado o desconocido. Pasar
     * [previstos] evita recalcularlos cuando ya se tienen de este mismo `(e, ahoraMs)`.
     */
    fun sugerido(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
        previstos: Map<String, PasoPrevisto> = previstos(e, receta, ahoraMs),
    ): Long? = Plano(receta).pasoActual(e, carril)?.let { previstos[it.id]?.inicioMs }

    fun vista(
        e: EstadoCrono,
        receta: Receta,
        carril: String,
        ahoraMs: Long,
        previstos: Map<String, PasoPrevisto> = previstos(e, receta, ahoraMs),
    ): VistaEstado = vista(Plano(receta), e, carril, ahoraMs, previstos)

    internal fun vista(
        plano: Plano,
        e: EstadoCrono,
        carril: String,
        ahoraMs: Long,
        previstos: Map<String, PasoPrevisto>,
    ): VistaEstado {
        val ec = e.carriles[carril]
        val p = plano.pasoActual(e, carril)
        if (ec == null || p == null) return VistaEstado(ClaseEstado.FIN, null)
        val sug = previstos[p.id]?.inicioMs ?: ahoraMs
        val pendientes = plano.pendientes(e, p)
        val clase =
            when {
                ec.corriendo && quedanS(ec, p, ahoraMs) <= 0 -> ClaseEstado.ATENDER
                ec.pausado -> ClaseEstado.PAUSA
                ec.corriendo -> ClaseEstado.MARCHA
                pendientes.isNotEmpty() && !ec.salto -> ClaseEstado.BLOQ
                sug - ahoraMs > UMBRAL_ESPERA_MS -> ClaseEstado.ESPERA
                else -> ClaseEstado.YA
            }
        val bloqueo = if (clase == ClaseEstado.BLOQ) pendientes.map { it.t to it.carril } else emptyList()
        return VistaEstado(clase, sug, bloqueo)
    }

    /** Foco automático: el carril con menor rango accionable (atender, ya, marcha, pausa, espera); empates por hora. */
    fun siguienteAccionable(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
    ): String? {
        val plano = Plano(receta)
        val previstos = plano.previstos(e, ahoraMs)
        return plano.carriles
            .map { it.id to vista(plano, e, it.id, ahoraMs, previstos) }
            .filter { it.second.clase.rango <= ClaseEstado.ESPERA.rango }
            .minWithOrNull(compareBy({ it.second.clase.rango }, { it.second.sugeridoMs ?: Long.MAX_VALUE }))
            ?.first
    }

    fun plan(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
        previstos: Map<String, PasoPrevisto> = previstos(e, receta, ahoraMs),
    ): PlanCrono {
        val plano = Plano(receta)
        val vivos = plano.carriles.filter { e.carriles[it.id]?.fin == false }
        val vistas = vivos.associate { it.id to vista(plano, e, it.id, ahoraMs, previstos) }
        val retraso =
            vistas.values
                .filter { it.clase == ClaseEstado.YA }
                .maxOfOrNull { redondeaMin(ahoraMs - (it.sugeridoMs ?: ahoraMs)) } ?: 0
        val llegada = previstos.llegadaMs(ahoraMs)
        val difFin = redondeaMin(llegada - e.servirMs)
        return PlanCrono(
            retrasoMin = retraso,
            difFinMin = difFin,
            tarde = retraso > 2 || difFin > 1,
            llegadaMs = llegada,
            vencido = vivos.firstOrNull { vistas[it.id]?.clase == ClaseEstado.ATENDER }?.id,
            aguantan = plano.titulosDesdeActual(e, vivos, Holgura.FLEXIBLE),
            noEsperan = plano.titulosDesdeActual(e, vivos, Holgura.CRITICO),
        )
    }

    /** "Mientras esto corre": qué otros recipientes piden atención en ≤ 12 min. */
    fun paralelo(
        e: EstadoCrono,
        receta: Receta,
        carrilActual: String,
        ahoraMs: Long,
        previstos: Map<String, PasoPrevisto> = previstos(e, receta, ahoraMs),
    ): List<Paralelo> {
        val plano = Plano(receta)
        return plano.carriles
            .filter { it.id != carrilActual }
            .mapNotNull { c -> filaParalelo(plano, e, c.id, ahoraMs, previstos) }
            .filter { it.minutos <= VENTANA_PARALELO_MIN }
            .sortedBy { it.minutos }
    }

    private fun filaParalelo(
        plano: Plano,
        e: EstadoCrono,
        carril: String,
        ahoraMs: Long,
        previstos: Map<String, PasoPrevisto>,
    ): Paralelo? {
        val ec = e.carriles[carril]
        val p = plano.pasoActual(e, carril)
        val saltar =
            ec == null || p == null || (ec.corriendo && quedanS(ec, p, ahoraMs) > 0) ||
                (plano.bloqueado(e, p) && !ec.salto)
        if (saltar || ec == null || p == null) return null
        val m = redondeaMin((previstos[p.id]?.inicioMs ?: ahoraMs) - ahoraMs)
        val etiqueta =
            when {
                ec.corriendo -> EtiquetaParalelo.VENCIDO
                ec.pausado -> EtiquetaParalelo.PAUSA
                m <= 0 -> EtiquetaParalelo.AHORA
                else -> EtiquetaParalelo.EN_MIN
            }
        return Paralelo(carril, p.t, m, etiqueta)
    }
}
