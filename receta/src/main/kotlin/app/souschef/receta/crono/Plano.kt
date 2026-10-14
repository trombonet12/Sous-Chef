package app.souschef.receta.crono

import app.souschef.receta.Carril
import app.souschef.receta.Holgura
import app.souschef.receta.Paso
import app.souschef.receta.Receta

/** Vista derivada de la receta: carriles normalizados (§4.1), colas en orden del array y sugeridos en ms. */
internal class Plano(
    val receta: Receta,
) {
    val carriles: List<Carril>
    val colas: Map<String, List<Paso>> = receta.pasos.groupBy { it.carril }
    private val ubicacion: Map<String, Pair<String, Int>>
    val totalMs: Long = medioMinutoMs(receta.totalEfectivo)

    init {
        val declarados = receta.carriles.distinctBy { it.id }
        val extra =
            receta.pasos
                .map { it.carril }
                .distinct()
                .filter { id -> declarados.none { it.id == id } }
        carriles = (declarados + extra.map { Carril(id = it) }).filter { colas.containsKey(it.id) }
        ubicacion = colas.flatMap { (c, ps) -> ps.mapIndexed { i, p -> p.id to (c to i) } }.toMap()
    }

    fun carril(id: String): Carril? = carriles.firstOrNull { it.id == id }

    fun pasoActual(
        e: EstadoCrono,
        carril: String,
    ): Paso? {
        val ec = e.carriles[carril]
        return if (ec == null || ec.fin) null else colas[carril]?.getOrNull(ec.i)
    }

    /** `sugerido(off) = servir − (total − off)`, en ms y redondeado a 0,5 min (sin truncar). */
    fun sugeridoMs(
        e: EstadoCrono,
        p: Paso,
    ): Long = e.servirMs - totalMs + medioMinutoMs(p.off)

    private fun hecho(
        e: EstadoCrono,
        id: String,
    ): Boolean {
        val (c, i) = ubicacion[id] ?: return true
        val ec = e.carriles[c]
        return ec == null || ec.fin || ec.i > i
    }

    /** Pasos de `dep` aún sin terminar, como (id, carril). */
    fun pendientes(
        e: EstadoCrono,
        p: Paso,
    ): List<Paso> = p.dep.filterNot { hecho(e, it) }.mapNotNull { id -> receta.paso(id) }

    fun bloqueado(
        e: EstadoCrono,
        p: Paso,
    ): Boolean = pendientes(e, p).isNotEmpty()

    /** `restante(c) = max(0, quedan) + Σ s` de los pasos siguientes del carril. */
    fun restanteCarrilS(
        e: EstadoCrono,
        carril: String,
        ahoraMs: Long,
    ): Int {
        val ec = e.carriles.getValue(carril)
        val cola = colas.getValue(carril)
        return maxOf(0, quedanS(ec, cola[ec.i], ahoraMs)) + cola.drop(ec.i + 1).sumOf { it.segundos }
    }

    /** Títulos únicos de los pasos con esa holgura, desde el índice actual de cada carril. */
    fun titulosDesdeActual(
        e: EstadoCrono,
        carriles: List<Carril>,
        holgura: Holgura,
    ): List<String> =
        carriles
            .flatMap { c -> colas.getValue(c.id).drop(e.carriles.getValue(c.id).i) }
            .filter { it.holguraTipo == holgura }
            .map { it.t }
            .distinct()

    companion object {
        fun medioMinutoMs(min: Double): Long = Math.round(min * 2) * 30_000L
    }
}

internal fun quedanS(
    ec: EstadoCarril,
    p: Paso,
    ahoraMs: Long,
): Int =
    when {
        ec.fin -> 0
        ec.finAtMs != null -> (-Math.floorDiv(-(ec.finAtMs - ahoraMs), 1000L)).toInt()
        else -> ec.restanteS ?: p.segundos
    }

internal fun redondeaMin(ms: Long): Int = Math.floorDiv(ms + 30_000L, 60_000L).toInt()

internal fun redondeaS(ms: Long): Long = Math.floorDiv(ms + 500L, 1000L)
