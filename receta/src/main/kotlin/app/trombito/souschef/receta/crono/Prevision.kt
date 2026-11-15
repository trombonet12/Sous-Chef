package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Paso

/**
 * Hora prevista de un paso, calculada desde lo que ha pasado de verdad (ADR-0018 «el plan sigue al cocinero»).
 *
 * - Pendiente: [inicioMs] es la hora a la que toca empezar (sin recortar a «ahora»; puede estar en el pasado si se va
 *   tarde) y [finMs] cuándo acabaría si empieza en cuanto pueda (`máx(inicioMs, ahora) + duración`).
 * - En marcha o en pausa: [inicioMs] es su arranque real y [finMs] su fin (en pausa o vencido crece con «ahora»).
 * - Hecho: arranque y fin reales; [hecho] = true.
 *
 * [firme]: la hora de inicio no depende de «ahora» (solo cambia con un mando): se puede armar una alarma a ella.
 * [finFirme]: lo mismo para [finMs]; los pasos que cuelgan de este lo heredan.
 */
data class PasoPrevisto(
    val inicioMs: Long,
    val finMs: Long,
    val firme: Boolean,
    val finFirme: Boolean = firme,
    val hecho: Boolean = false,
)

/** Hora a la que se acaba todo: el fin más tardío de los pasos no hechos; [ahoraMs] si no queda ninguno. */
internal fun Map<String, PasoPrevisto>.llegadaMs(ahoraMs: Long): Long =
    values.filterNot { it.hecho }.maxOfOrNull { it.finMs } ?: ahoraMs

internal fun Plano.previstos(
    e: EstadoCrono,
    ahoraMs: Long,
): Map<String, PasoPrevisto> = Prevision(this, e, ahoraMs).calcular()

/**
 * Hora prevista de todos los pasos en [ahoraMs]: función pura de `(estado, receta, ahora)`, sin reloj propio.
 * Sin desvíos coincide con `servir − total + off`; ver el ADR-0018 para la fórmula y los respaldos.
 */
internal class Prevision(
    private val plano: Plano,
    private val e: EstadoCrono,
    private val ahoraMs: Long,
) {
    private enum class Situacion { HECHO, ARRANCADO, PENDIENTE }

    /** Valores efectivos de un paso: los que heredan los que cuelgan de él (ADR-0018 § Fórmula). */
    private class Nodo(
        val inicioMs: Long,
        val finMs: Long,
        val finFirme: Boolean,
        val situacion: Situacion,
        val previstoMs: Long = inicioMs,
        val previstoFirme: Boolean = true,
    ) {
        /** Un arranque real no se mueve; el de un pendiente es tan firme como su fin. */
        val inicioFirme: Boolean get() = situacion != Situacion.PENDIENTE || finFirme

        fun publico() = PasoPrevisto(previstoMs, finMs, previstoFirme, finFirme, situacion == Situacion.HECHO)
    }

    private val oPlan = e.servirMs - plano.totalMs
    private val nodos = HashMap<String, Nodo>()
    private val actuales = HashSet<String>()

    fun calcular(): Map<String, PasoPrevisto> {
        fijos()
        // Arranque de la cocina: el primer paso de `off` 0 que ya ha arrancado (o se dio por hecho).
        val arranque =
            plano.receta.pasos
                .filter { off(it) == 0L }
                .mapNotNull { nodos[it.id]?.inicioMs }
                .minOrNull()
        for (p in plano.grafo.orden) {
            if (p.id !in nodos) nodos[p.id] = pendiente(p, arranque)
        }
        return plano.receta.pasos.associate { it.id to nodos.getValue(it.id).publico() }
    }

    /** Hechos, en marcha y en pausa: sus horas salen del estado, no del grafo. */
    private fun fijos() {
        for ((carril, cola) in plano.colas) {
            val ec = e.carriles[carril]
            cola.forEachIndexed { k, p ->
                when {
                    ec == null || ec.fin || k < ec.i -> nodos[p.id] = hecho(ec, cola, k)
                    k == ec.i && ec.corriendo -> nodos[p.id] = enMarcha(ec, p)
                    k == ec.i && ec.pausado -> nodos[p.id] = enPausa(ec, p)
                    k == ec.i -> actuales += p.id
                }
            }
        }
    }

    private fun hecho(
        ec: EstadoCarril?,
        cola: List<Paso>,
        k: Int,
    ): Nodo {
        val p = cola[k]
        val real =
            e.finRealMs[p.id]
                ?: ec?.takeIf { !it.fin && it.i == k + 1 }?.inicioPasoMs
                ?: (oPlan + finPlan(p))
        val inicio = e.inicioRealMs[p.id] ?: (real - dur(p))
        return Nodo(inicio, real, finFirme = true, Situacion.HECHO)
    }

    private fun enMarcha(
        ec: EstadoCarril,
        p: Paso,
    ): Nodo {
        val finAt = checkNotNull(ec.finAtMs)
        val inicio = e.inicioRealMs[p.id] ?: (finAt - dur(p))
        return Nodo(inicio, maxOf(finAt, ahoraMs), finFirme = finAt >= ahoraMs, Situacion.ARRANCADO)
    }

    private fun enPausa(
        ec: EstadoCarril,
        p: Paso,
    ): Nodo {
        val inicio = e.inicioRealMs[p.id] ?: ec.inicioPasoMs ?: ahoraMs
        val quedan = (ec.restanteS ?: p.segundos) * MS_S
        return Nodo(inicio, ahoraMs + quedan, finFirme = false, Situacion.ARRANCADO)
    }

    private fun pendiente(
        p: Paso,
        arranque: Long?,
    ): Nodo {
        val terminos = terminos(p, arranque)
        val previsto = terminos.maxOf { it.first }
        val firme = terminos.all { it.second }
        val inicio = maxOf(previsto, ahoraMs)
        // El paso actual cuenta la prórroga hecha antes de arrancar (`restanteS`); uno futuro, su duración de receta.
        // Fin firme solo si aún no toca (previsto > ahora): con previsto == ahora su arranque depende del cocinero.
        val duracion = if (p.id in actuales) (e.carriles[p.carril]?.restanteS ?: p.segundos) * MS_S else dur(p)
        return Nodo(inicio, inicio + duracion, firme && previsto > ahoraMs, Situacion.PENDIENTE, previsto, firme)
    }

    /** Un término por arista (cola, `dep`, `junto`), con su firmeza; sin aristas, el del origen del plan. */
    private fun terminos(
        p: Paso,
        arranque: Long?,
    ): List<Pair<Long, Boolean>> {
        val g = plano.grafo
        val deFin =
            (listOfNotNull(g.cola(p)) + g.dep(p)).map { q ->
                val n = nodos.getValue(q.id)
                (n.finMs + off(p) - finPlan(q)) to n.finFirme
            }
        val deJunto =
            listOfNotNull(g.junto(p)).map { j ->
                val n = nodos.getValue(j.id)
                (n.inicioMs + off(p) - off(j)) to n.inicioFirme
            }
        val aristas = deFin + deJunto
        return when {
            aristas.isNotEmpty() -> aristas
            arranque != null -> listOf((arranque + off(p)) to true)
            off(p) == 0L -> listOf(oPlan to true)
            else -> listOf((maxOf(oPlan, ahoraMs) + off(p)) to (ahoraMs <= oPlan))
        }
    }

    private fun off(p: Paso): Long = Plano.medioMinutoMs(p.off)

    private fun dur(p: Paso): Long = p.segundos * MS_S

    private fun finPlan(p: Paso): Long = off(p) + dur(p)

    private companion object {
        const val MS_S = 1000L
    }
}
