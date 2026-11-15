package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Holgura
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta

/**
 * Vista derivada de la receta: carriles normalizados (§4.1), colas en orden del array y el grafo de precedencias
 * (colas, `dep` y `junto`) ya ordenado. Las horas previstas están en `Prevision.kt` (ADR-0018).
 */
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

    /** Grafo ac de colas, `dep` y `junto`, ordenado topológicamente (ADR-0018). */
    val grafo: GrafoPlano by lazy { GrafoPlano.de(receta, colas) }

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

/**
 * Precedencias de un paso: su [cola] (el anterior de su carril), sus [dep] y su [junto]. Solo entran las aristas que no
 * cierran un ciclo (el validador los prohíbe; por defensa se ignoran, empezando por las últimas, que son los `junto`).
 */
internal class GrafoPlano(
    val orden: List<Paso>,
    private val cola: Map<String, Paso>,
    private val dep: Map<String, List<Paso>>,
    private val junto: Map<String, Paso>,
) {
    fun cola(p: Paso): Paso? = cola[p.id]

    fun dep(p: Paso): List<Paso> = dep[p.id].orEmpty()

    fun junto(p: Paso): Paso? = junto[p.id]

    companion object {
        fun de(
            receta: Receta,
            colas: Map<String, List<Paso>>,
        ): GrafoPlano {
            val ac = Aciclico(receta.pasos)
            val cola = HashMap<String, Paso>()
            for (ps in colas.values) ps.zipWithNext { a, b -> ac.une(a, b)?.let { cola[b.id] = it } }
            val dep = HashMap<String, List<Paso>>()
            for (p in receta.pasos) dep[p.id] = p.dep.mapNotNull { id -> ac.une(receta.paso(id), p) }
            val junto = HashMap<String, Paso>()
            for (p in receta.pasos) p.junto?.let { id -> ac.une(receta.paso(id), p)?.let { junto[p.id] = it } }
            return GrafoPlano(ac.ordenado(), cola, dep, junto)
        }
    }
}

/** Precedencias aceptadas una a una: se rechaza la que cerraría un ciclo. */
private class Aciclico(
    private val pasos: List<Paso>,
) {
    private val antes: Map<String, MutableSet<String>> = pasos.associate { it.id to mutableSetOf() }

    private fun depende(
        desde: String,
        objetivo: String,
        visto: MutableSet<String> = mutableSetOf(),
    ): Boolean = desde == objetivo || (visto.add(desde) && antes[desde].orEmpty().any { depende(it, objetivo, visto) })

    /** Une `previo -> p` y lo devuelve, salvo que no exista o que `previo` ya dependa de `p`. */
    fun une(
        previo: Paso?,
        p: Paso,
    ): Paso? {
        val unible = previo != null && previo.id in antes && !depende(previo.id, p.id)
        return if (unible) previo?.also { antes.getValue(p.id) += it.id } else null
    }

    /** Orden topológico estable (a igual precedencia, el del array). */
    fun ordenado(): List<Paso> {
        val porId = pasos.associateBy { it.id }
        val orden = LinkedHashSet<String>()

        fun visita(id: String) {
            if (orden.contains(id)) return
            antes[id].orEmpty().forEach { visita(it) }
            orden += id
        }
        pasos.forEach { visita(it.id) }
        return orden.mapNotNull { porId[it] }
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
