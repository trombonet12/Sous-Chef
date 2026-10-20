package app.trombito.souschef.receta

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Detector de números de potencia (0-9, vitrocerámica) en texto libre. Los marca como `{N}` para que la app los
 * traduzca a la cocina del usuario. Un falso positivo (un minuto, una tanda, una cantidad) es peor que uno sin
 * marcar, así que lo que no encaja claro va a `dudosos` y no se toca. Idempotente: lo ya marcado no se vuelve a marcar.
 */
object MarcaPotencia {
    /** Un número de potencia en `posicion` (índice del dígito) con ±40 caracteres de contexto. */
    data class Cambio(
        val posicion: Int,
        val numero: Int,
        val contexto: String,
    )

    data class Resultado(
        val texto: String,
        val cambios: List<Cambio>,
        val dudosos: List<Cambio>,
    )

    private const val CONTEXTO = 40

    /** Números de potencia seguros que todavía no llevan llaves. */
    fun candidatos(texto: String): List<Cambio> = analizar(texto).first

    fun marcar(texto: String): Resultado {
        val (seguros, dudosos) = analizar(texto)
        val sb = StringBuilder(texto)
        for (c in seguros.sortedByDescending { it.posicion }) {
            sb.insert(c.posicion + 1, '}')
            sb.insert(c.posicion, '{')
        }
        return Resultado(sb.toString(), seguros, dudosos)
    }

    private enum class Estado { SEGURO, DUDOSO, SILENCIO }

    private class Eval(
        val estado: Estado,
        val posiciones: List<Int>,
    )

    private const val NO_LETRA = "(?<![\\p{L}\\p{N}])"
    private const val FIN_PAL = "(?![\\p{L}\\p{N}])"
    private val UNIDAD =
        Regex(
            "\\s*(?:[%°ºª]|(?:min|mins|minutos?|seg|segundos?|s|h|horas?|cm|mm|g|gr|kg|ml|l|litros?|vez|veces|" +
                "tandas?|pasos?|raci[oó]n|raciones|uds?|huevos?|dientes?|cucharadas?|cucharaditas?|vasos?|personas?|" +
                "porciones?|piezas?|rodajas?|lonchas?|hojas?|trozos?|er)$FIN_PAL)",
            RegexOption.IGNORE_CASE,
        )
    private val DE = Regex("\\s+de\\s", RegexOption.IGNORE_CASE)
    private val AL = Regex("${NO_LETRA}al \\{?(\\d)", RegexOption.IGNORE_CASE)
    private val DEL_AL = Regex("${NO_LETRA}del \\{?(\\d)\\}? al \\{?(\\d)", RegexOption.IGNORE_CASE)
    private val DEL = Regex("${NO_LETRA}del \\{?(\\d)", RegexOption.IGNORE_CASE)
    private val ENTRE = Regex("${NO_LETRA}entre el \\{?(\\d)\\}? y el \\{?(\\d)", RegexOption.IGNORE_CASE)
    private val FUEGO = Regex("$NO_LETRA(?:fuego|potencia|posici[oó]n) \\{?(\\d)", RegexOption.IGNORE_CASE)
    private val PLURAL = Regex("${NO_LETRA}(?:potencias|escala) (?:del? )?\\{?(\\d)", RegexOption.IGNORE_CASE)
    private val SUBE_A =
        Regex("${NO_LETRA}(?:sub(?:e|es|ir|irlo)|baj(?:a|as|ar|arlo)) a \\{?(\\d)", RegexOption.IGNORE_CASE)
    private val MANTEN_EL =
        Regex("${NO_LETRA}(?:mantén|mantener|mantienes|sigue|queda) (?:en )?el \\{?(\\d)", RegexOption.IGNORE_CASE)
    private val ESCALA = Regex("(?:potencias|escala)[^.;:!?]*$", RegexOption.IGNORE_CASE)
    private val PALABRA_ANTES = Regex("([\\p{L}]+)\\s*$")

    private fun analizar(t: String): Pair<List<Cambio>, List<Cambio>> {
        val acc = Acumulador(t)
        acc.gruposDelAl()
        acc.gruposEntre()
        acc.simples(PLURAL, dudoso = true)
        acc.simples(FUEGO, dudoso = false)
        acc.simplesAl()
        acc.simples(DEL, dudoso = true)
        acc.simples(SUBE_A, dudoso = true)
        acc.simples(MANTEN_EL, dudoso = true)
        return acc.resultado()
    }

    /** Recoge posiciones de dígitos ya clasificadas; una posición se decide una sola vez (la primera regla manda). */
    private class Acumulador(
        val t: String,
    ) {
        private val vistos = mutableSetOf<Int>()
        private val seguros = sortedSetOf<Int>()
        private val dudosos = sortedSetOf<Int>()

        fun registra(
            evals: List<Eval>,
            fuerzaDudoso: Boolean = false,
        ) {
            val todas = evals.flatMap { it.posiciones }
            if (todas.any { it in vistos }) return
            vistos += todas
            val estados = evals.map { it.estado }
            when {
                Estado.SILENCIO in estados -> Unit
                fuerzaDudoso || Estado.DUDOSO in estados -> dudosos += todas
                else -> seguros += todas
            }
        }

        private fun pos(
            m: MatchResult,
            g: Int,
        ) = m.groups[g]!!.range.first

        fun gruposDelAl() {
            for (m in DEL_AL.findAll(t)) {
                val a = pos(m, 1)
                val b = pos(m, 2)
                val escala = (t[a] == '0' && t[b] == '9') || ESCALA.containsMatchIn(t.substring(0, m.range.first))
                registra(listOf(evalua(t, a), evalua(t, b)), fuerzaDudoso = escala)
            }
        }

        fun gruposEntre() {
            for (m in ENTRE.findAll(t)) registra(listOf(evalua(t, pos(m, 1)), evalua(t, pos(m, 2))))
        }

        fun simples(
            regex: Regex,
            dudoso: Boolean,
        ) {
            for (m in regex.findAll(t)) registra(listOf(evalua(t, pos(m, 1))), fuerzaDudoso = dudoso)
        }

        /** «al N»; tras «paso»/«tanda» ya no es una potencia clara. */
        fun simplesAl() {
            for (m in AL.findAll(t)) {
                val palabra = palabraAnterior(t.substring(0, m.range.first))
                registra(listOf(evalua(t, pos(m, 1))), fuerzaDudoso = palabra == "paso" || palabra == "tanda")
            }
        }

        fun resultado(): Pair<List<Cambio>, List<Cambio>> {
            fun sinLlaves(p: Int) = !(p > 0 && t[p - 1] == '{' && t.getOrNull(p + 1) == '}')

            fun cambio(p: Int) =
                Cambio(
                    p,
                    t[p] - '0',
                    t.substring(
                        maxOf(0, p - CONTEXTO),
                        minOf(
                            t.length,
                            p + 1 + CONTEXTO,
                        ),
                    ),
                )
            return seguros.filter(::sinLlaves).map(::cambio) to dudosos.filter(::sinLlaves).map(::cambio)
        }
    }

    /** Valora el dígito de `pos` según lo que le sigue; sigue un rango `N-M` (el último dígito manda). */
    private fun evalua(
        t: String,
        pos: Int,
    ): Eval {
        var i = pos + 1
        if (t.getOrNull(i) == '}') i++
        val j = segundoDeRango(t, i)
        if (j != null) {
            val resto = evalua(t, j)
            return Eval(resto.estado, listOf(pos) + resto.posiciones)
        }
        return Eval(estadoDeCola(t, i), listOf(pos))
    }

    /** Posición del dígito M en `N-M` / `N–M` (con o sin llaves), si `i` apunta al guion. */
    private fun segundoDeRango(
        t: String,
        i: Int,
    ): Int? {
        if (t.getOrNull(i) != '-' && t.getOrNull(i) != '–') return null
        val j = if (t.getOrNull(i + 1) == '{') i + 2 else i + 1
        return j.takeIf { t.getOrNull(it)?.isDigit() == true }
    }

    private fun estadoDeCola(
        t: String,
        i: Int,
    ): Estado {
        val c = t.getOrNull(i)
        val decimal = c != null && c in ".,/" && t.getOrNull(i + 1)?.isDigit() == true
        return when {
            c?.isDigit() == true || decimal || UNIDAD.matchesAt(t, i) -> Estado.SILENCIO
            DE.matchesAt(t, i) -> Estado.DUDOSO
            else -> Estado.SEGURO
        }
    }

    private fun palabraAnterior(antes: String): String? =
        PALABRA_ANTES.find(antes)?.let { it.groupValues[1].lowercase() }

    private fun Regex.matchesAt(
        t: String,
        i: Int,
    ): Boolean = i <= t.length && matchAt(t, i) != null
}

/** Recorre los campos de texto libre de una receta o técnica (los únicos donde puede haber potencias). */
object CamposDeTexto {
    private val CAMPOS_RECETA =
        listOf("descripcion", "notaEscala", "lectura", "pie").map { listOf(it) } +
            listOf(
                listOf("porque", "[]", "[0]"),
                listOf("porque", "[]", "[1]"),
                listOf("cortes", "[]", "p"),
                listOf("mep", "[]", "porq"),
                listOf("mep", "[]", "l", "[]"),
            ) +
            listOf("t", "nota", "p", "meto", "saco", "sinHitos").map { listOf("pasos", "[]", it) } +
            listOf(listOf("pasos", "[]", "hitos", "[]", "t"))

    private val CAMPOS_TECNICA =
        listOf(
            listOf("resumen"),
            listOf("pie"),
            listOf("pasos", "[]", "t"),
            listOf("pasos", "[]", "p"),
            listOf("consejos", "[]"),
            listOf("cortes", "[]", "p"),
        )

    /**
     * Aplica `f(campo, texto)` a los campos de texto libre de una receta o técnica (`campo` es una ruta legible como
     * `pasos[p3].nota`) y devuelve el documento con solo esas cadenas sustituidas; el resto del JSON queda intacto.
     */
    fun transformar(
        doc: JsonObject,
        receta: Boolean,
        f: (String, String) -> String,
    ): JsonObject {
        var actual: JsonElement = doc
        for (ruta in if (receta) CAMPOS_RECETA else CAMPOS_TECNICA) actual = reescribe(actual, ruta, 0, "", f)
        return actual as JsonObject
    }

    private fun reescribe(
        e: JsonElement,
        ruta: List<String>,
        i: Int,
        campo: String,
        f: (String, String) -> String,
    ): JsonElement {
        if (i == ruta.size) {
            return if (e is JsonPrimitive && e.isString) JsonPrimitive(f(campo, e.content)) else e
        }
        val paso = ruta[i]
        return when {
            e is JsonArray && paso == "[]" -> {
                JsonArray(e.mapIndexed { n, x -> reescribe(x, ruta, i + 1, "$campo[${etiqueta(x, n)}]", f) })
            }

            e is JsonArray && paso.startsWith("[") -> {
                val n = paso.removeSurrounding("[", "]").toInt()
                JsonArray(e.mapIndexed { k, x -> if (k == n) reescribe(x, ruta, i + 1, "$campo[$n]", f) else x })
            }

            e is JsonObject && paso in e -> {
                val ruta2 = if (campo.isEmpty()) paso else "$campo.$paso"
                JsonObject(e + (paso to reescribe(e.getValue(paso), ruta, i + 1, ruta2, f)))
            }

            else -> {
                e
            }
        }
    }

    private fun etiqueta(
        x: JsonElement,
        n: Int,
    ): String = ((x as? JsonObject)?.get("id") as? JsonPrimitive)?.takeIf { it.isString }?.content ?: n.toString()
}
