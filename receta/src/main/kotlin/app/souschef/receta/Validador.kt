package app.souschef.receta

import kotlin.math.abs

enum class Nivel { ERROR, AVISO }

data class Hallazgo(
    val slug: String,
    val regla: Int,
    val nivel: Nivel,
    val texto: String,
)

private const val EPS = 1e-6
private const val MAX_FUEGOS = 3
private const val MIN_VIGILADO = 6.0
private const val TOLERANCIA_S = 30.0
private const val P_MIN_CHARS = 120
private const val F_MAX = 9

/** Validador (reglas 1-7, 10, 11, 16-18, 21 y 23 de docs/cocina/esquema-receta.md § Validación). Lógica pura. */
object Validador {
    /** Si [tecnicasExistentes] es null no se comprueba la regla 23. */
    fun receta(
        r: Receta,
        tecnicasExistentes: Set<String>? = null,
    ): List<Hallazgo> {
        val h = mutableListOf<Hallazgo>()
        val ctx = Ctx(r, h)
        ctx.regla1()
        ctx.regla2()
        ctx.regla3()
        ctx.regla4y5()
        ctx.regla6y23(tecnicasExistentes)
        ctx.regla7y10y17()
        ctx.regla11()
        ctx.regla16()
        ctx.regla18()
        ctx.regla21()
        return h
    }

    /** Ficha de técnica: slug, título y pasos (regla 0, sin número en el doc). */
    fun tecnica(t: Tecnica): List<Hallazgo> =
        buildList {
            if (t.slug.isBlank()) add(Hallazgo(t.slug, 0, Nivel.ERROR, "slug vacío"))
            if (t.titulo.isBlank()) add(Hallazgo(t.slug, 0, Nivel.ERROR, "titulo vacío"))
            if (t.pasos.isEmpty()) add(Hallazgo(t.slug, 0, Nivel.ERROR, "sin pasos"))
        }
}

@Suppress("TooManyFunctions") // una función por regla del documento
private class Ctx(
    val r: Receta,
    val h: MutableList<Hallazgo>,
) {
    fun err(
        regla: Int,
        texto: String,
    ) {
        h += Hallazgo(r.slug, regla, Nivel.ERROR, texto)
    }

    fun aviso(
        regla: Int,
        texto: String,
    ) {
        h += Hallazgo(r.slug, regla, Nivel.AVISO, texto)
    }

    fun tipo(id: String): TipoCarril? = r.carril(id)?.tipoCarril

    fun regla1() {
        if (r.pasos.isEmpty()) return err(1, "sin pasos")
        val fin = r.pasos.maxOf { it.fin }
        if (abs(fin - r.totalMin) > EPS) err(1, "el paso más tardío acaba en $fin y totalMin es ${r.totalMin}")
        if (r.pasos.none { abs(it.off) <= EPS }) err(1, "ningún paso arranca en el minuto 0")
    }

    fun regla2() {
        for (p in r.pasos) {
            for (d in p.dep) {
                val o = r.paso(d)
                when {
                    o == null -> {
                        err(2, "${p.id}: dep '$d' no existe")
                    }

                    o.carril == p.carril -> {
                        err(2, "${p.id}: dep '$d' es del mismo carril")
                    }

                    o.fin > p.off + EPS -> {
                        err(2, "${p.id}: dep '$d' termina (${o.fin}) después de que empiece (${p.off})")
                    }
                }
            }
            val j = p.junto
            if (j != null) {
                val o = r.paso(j)
                when {
                    o == null -> err(2, "${p.id}: junto '$j' no existe")
                    o.carril == p.carril -> err(2, "${p.id}: junto '$j' es del mismo carril")
                }
            }
        }
        ciclo()?.let { err(2, "ciclo en dep: $it") }
    }

    private fun ciclo(): String? {
        val estado = mutableMapOf<String, Int>() // 1 en curso, 2 hecho

        fun visita(
            id: String,
            ruta: List<String>,
        ): String? {
            val previo = estado[id]
            var hallado: String? = null
            if (previo == 1) {
                hallado = (ruta + id).joinToString(" -> ")
            } else if (previo == null) {
                estado[id] = 1
                hallado =
                    r
                        .paso(id)
                        ?.dep
                        .orEmpty()
                        .firstNotNullOfOrNull { visita(it, ruta + id) }
                estado[id] = 2
            }
            return hallado
        }
        for (p in r.pasos) visita(p.id, emptyList())?.let { return it }
        return null
    }

    fun regla3() {
        val fuego = r.pasos.filter { tipo(it.carril) == TipoCarril.FUEGO }
        for (p in fuego) {
            val activos =
                fuego
                    .filter { it.off <= p.off + EPS && p.off < it.off + it.min - EPS }
                    .map { it.carril }
                    .toSet()
            if (activos.size > MAX_FUEGOS) {
                err(3, "${activos.size} fuegos a la vez en el minuto ${p.off}")
                return
            }
        }
    }

    fun regla4y5() {
        for (p in r.pasos) {
            val t = tipo(p.carril)
            if (t == null) err(5, "${p.id}: carril '${p.carril}' no declarado")
            val f = p.f
            if (f != null && t != null && t != TipoCarril.FUEGO) err(4, "${p.id}: f=$f en carril que no es fuego")
            if (f != null && f !in 0..F_MAX) err(4, "${p.id}: f=$f fuera de 0-9")
        }
        val usados = r.pasos.map { it.carril }.toSet()
        r.carriles.filter { it.id !in usados }.forEach { err(5, "carril '${it.id}' declarado y sin usar") }
    }

    fun regla6y23(existentes: Set<String>?) {
        if (r.tags.base.isNullOrBlank()) err(6, "tags.base vacío")
        if (r.tags.tecnica.isEmpty()) err(6, "tags.tecnica vacío")
        if (existentes == null) return
        r.tags.tecnica
            .filter { it !in existentes }
            .forEach { aviso(23, "tag de técnica '$it' sin ficha") }
    }

    fun regla7y10y17() {
        for (p in r.pasos) {
            val ens = p.hitos.map { it.en }
            if (ens.zipWithNext().any { (a, b) -> b < a }) err(7, "${p.id}: hitos desordenados")
            if (ens.any { it >= p.segundos }) err(7, "${p.id}: un hito llega a la duración del paso (${p.segundos} s)")
            val vigilado = tipo(p.carril).let { it == TipoCarril.FUEGO || it == TipoCarril.HORNO }
            val sinAvisos = p.hitos.isEmpty() && p.sinHitos.isNullOrBlank()
            if (vigilado && p.min >= MIN_VIGILADO && sinAvisos) {
                err(10, "${p.id}: ${p.min} min en fuego/horno sin hitos ni sinHitos")
            }
            val s = p.s
            if (s != null && abs(s - p.min * 60) > TOLERANCIA_S) err(17, "${p.id}: s=$s incoherente con min=${p.min}")
        }
    }

    fun regla11() {
        val ultimo = r.pasos.lastOrNull() ?: return
        if (ultimo.saco.isNullOrBlank()) err(11, "el último paso (${ultimo.id}) no declara saco")
    }

    fun regla16() {
        for ((carril, pasos) in r.pasos.groupBy { it.carril }) {
            pasos.sortedBy { it.off }.zipWithNext().forEach { (a, b) ->
                if (a.fin > b.off + EPS) err(16, "solape en '$carril' entre ${a.id} y ${b.id}")
            }
        }
    }

    fun regla18() {
        val otra = Regex("<(?!/?b>)")
        for (b in r.mep) {
            for (l in b.l) if (otra.containsMatchIn(l)) err(18, "mep '${b.t}': solo se permite <b>: $l")
        }
    }

    fun regla21() {
        for (p in r.pasos) {
            val n = p.p?.length ?: 0
            if (n < P_MIN_CHARS) aviso(21, "${p.id}: p de $n caracteres (< $P_MIN_CHARS)")
        }
    }
}
