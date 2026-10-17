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

/**
 * Validador (reglas 1-8, 10, 11, 13-21 y 23-25 de docs/cocina/esquema-receta.md § Validación). Lógica pura.
 * Fuera de aquí: la 12 (despensa, en `tools/`). Sin implementar: la 9 y la 22 (ver el documento).
 */
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
        ctx.regla8()
        ctx.regla11()
        ctx.regla13()
        ctx.regla14y15()
        ctx.regla16()
        ctx.regla18()
        ctx.regla19()
        ctx.regla20()
        ctx.regla21()
        ctx.regla24()
        ctx.regla25()
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

    /**
     * Regla 8. Heurística: el nombre `n` (sin paréntesis) casa si aparece entero en alguna línea de `mep` o,
     * si no, si aparece su palabra núcleo (primera palabra de 3 o más letras, o cualquier palabra clave
     * de 4+ letras no genérica; también en singular).
     * Palabra completa; ver [Texto.contienePalabra].
     */
    fun regla8() {
        if (r.mep.isEmpty()) return
        val lineas = r.mep.flatMap { it.l }.map { it.replace(Regex("</?b>"), "") }
        for (i in r.grupos.flatMap { it.items }) {
            val entero =
                Texto
                    .normalizar(i.n)
                    .substringBefore(',')
                    .replace(Regex("\\([^)]*\\)"), " ")
                    .trim()
            val nucleo = Texto.nucleo(i.n)
            val formas = (listOf(entero, nucleo, Texto.singular(nucleo)) + Texto.palabrasClave(i.n)).distinct()
            val ok = lineas.any { l -> formas.any { Texto.contienePalabra(l, it) } }
            if (!ok) err(8, "ingrediente '${i.n}' no aparece en el mep")
        }
    }

    fun regla13() {
        val prohibidas = listOf("al gusto", "chorrito", "c/s")
        for (i in r.grupos.flatMap { it.items }) {
            if (i.a == null && i.txt.isNullOrBlank()) err(13, "'${i.n}' sin cantidad (ni a ni txt)")
            val texto = Texto.normalizar("${i.n} ${i.txt.orEmpty()}")
            prohibidas
                .filter { Regex("(?<![a-z])" + Regex.escape(it) + "(?![a-z])").containsMatchIn(texto) }
                .forEach { err(13, "'${i.n}': cantidad vaga («$it»)") }
        }
    }

    fun regla14y15() {
        val mesa = r.pasos.filter { tipo(it.carril) == TipoCarril.MESA }
        if (r.cortes.isNotEmpty() && mesa.none { abs(it.off) <= EPS }) {
            err(14, "hay cortes pero ningún paso de mesa arranca en el minuto 0")
        }
        if (r.cortes.isEmpty()) {
            mesa
                .firstOrNull { CORTA.containsMatchIn(Texto.normalizar("${it.t} ${it.nota.orEmpty()}")) }
                ?.let { aviso(15, "${it.id}: corta algo en mesa y 'cortes' está vacío") }
        }
    }

    fun regla19() {
        for (c in r.cortes) {
            if (!MEDIDA.containsMatchIn("${c.t} ${c.p.orEmpty()}")) aviso(19, "corte '${c.t}' sin medida (mm o cm)")
        }
    }

    fun regla20() {
        val fuego = r.pasos.filter { tipo(it.carril) == TipoCarril.FUEGO }
        for ((carril, pasos) in fuego.groupBy { it.carril }) {
            for ((a, b) in pasos.sortedBy { it.off }.zipWithNext()) {
                val f = b.f ?: continue
                val texto = Texto.normalizar("${b.t} ${b.nota.orEmpty()} ${b.p}")
                val nombrada =
                    Regex("(?<![0-9])$f(?![0-9])").containsMatchIn(texto) ||
                        (f == 0 && APAGAR.containsMatchIn(texto))
                if (f != a.f && !nombrada) aviso(20, "${b.id}: pasa a potencia $f en '$carril' y el texto no la nombra")
            }
        }
    }

    /**
     * Regla 25. El verbo de potencia de `t`/`nota` («baja al N», «sube al N») casa con la transición real de `f`
     * del carril (prev → f). Los textos de `hitos` son condicionales: se comparan solo con `f`.
     */
    fun regla25() {
        for (pasos in r.pasos.groupBy { it.carril }.values) {
            var prev: Int? = null
            for (p in pasos.sortedBy { it.off }) {
                val f = p.f ?: continue
                listOfNotNull(p.t, p.nota).forEach { verbosDePotencia(p.id, it, prev ?: f, f) }
                p.hitos.forEach { verbosDePotencia(p.id, it.t, f, f) }
                prev = f
            }
        }
    }

    private fun verbosDePotencia(
        id: String,
        texto: String,
        previa: Int,
        f: Int,
    ) {
        for (frase in Texto.normalizar(texto).split(FIN_FRASE)) {
            for (m in BAJA_AL.findAll(frase)) {
                val n = m.groupValues[1].toInt()
                if (n >= maxOf(previa, f)) errPotencia(id, "baja", n, previa, f)
            }
            for (m in SUBE_AL.findAll(frase)) {
                val n = m.groupValues[1].toInt()
                if (n <= minOf(previa, f)) errPotencia(id, "sube", n, previa, f)
            }
            if (!VERBO_POTENCIA.containsMatchIn(frase)) continue
            for (m in DEL_AL.findAll(frase)) {
                val x = m.groupValues[1].toInt()
                if (x != previa && x != f) {
                    aviso(25, "paso '$id': dice «del $x al ${m.groupValues[2]}» pero la potencia pasa de $previa a $f")
                }
            }
        }
    }

    private fun errPotencia(
        id: String,
        verbo: String,
        n: Int,
        previa: Int,
        f: Int,
    ) = err(25, "paso '$id': dice «$verbo al $n» pero la potencia pasa de $previa a $f")

    fun regla24() {
        for (i in r.grupos.flatMap { it.items }) {
            val n = Texto.normalizar(i.n)
            VETOS
                .firstOrNull { Regex("(?<![a-z])$it").containsMatchIn(n) }
                ?.let { aviso(24, "ingrediente vetado: '${i.n}'") }
        }
    }
}

private val CORTA =
    Regex(
        "\\b(cort(a|ar|ado|ados|ada|adas|amos|e|es)|pic(a|ar|ado|ados|ada|adas|amos)|lamin(a|ar|ado|ados|ada|adas)|" +
            "troce(a|ar|ado|ados|ada|adas)|ralla(r|do|dos|da|das)?|dados?)\\b",
    )
private const val V_BAJA = "(?:baja|bajas|bajar|bajalo)"
private const val V_SUBE = "(?:sube|subes|subir|subelo)"
private const val OTRO_VERBO = "(?!\\b(?:$V_BAJA|$V_SUBE)\\b)"
private val FIN_FRASE = Regex("[.;!?\\n]+")
private val VERBO_POTENCIA = Regex("\\b(?:$V_BAJA|$V_SUBE)\\b")
private val BAJA_AL = Regex("\\b$V_BAJA\\b(?:$OTRO_VERBO.)*?\\bal (\\d)\\b")
private val SUBE_AL = Regex("\\b$V_SUBE\\b(?:$OTRO_VERBO.)*?\\bal (\\d)\\b")
private val DEL_AL = Regex("\\bdel (\\d) al (\\d)\\b")
private val APAGAR = Regex("apag|retira|fuera del fuego|fuera de la placa|reposo")
private val MEDIDA = Regex("\\d+([.,]\\d+)?\\s*(mm|cm)\\b|por la mitad")

// Raíces normalizadas (sin acentos); se buscan al inicio de palabra.
private val VETOS =
    listOf(
        "garbanzo",
        "guisante",
        "champinon",
        "coliflor",
        "brocoli",
        "casqueria",
        "higado",
        "rinon",
        "callos",
        "sesos",
        "molleja",
        "anchoa",
        "alcaparra",
        "marisco",
        "gamba",
        "langostino",
        "mejillon",
        "calamar",
        "pulpo",
        "sepia",
        "almeja",
        "ostra",
        "vieira",
        "cigala",
        "berberecho",
        "navaja",
    )
