@file:Suppress("TooManyFunctions") // una función por regla del documento

package app.trombito.souschef.receta

private val ETIQUETA_B = Regex("</?b>")
private val DURACION_ESCRITA = Regex("(?<![\\w.,])\\d+(?:[.,]\\d+)?\\s*(?:min|minutos?|s|seg|segundos?)(?![\\p{L}\\d])")
private val MINUTO_N = Regex("\\bminutos?\\s+\\d")
private val MEDIDA_CORTE = Regex("\\d+(?:[.,]\\d+)?\\s*(?:mm|cm)(?![\\p{L}\\d])")
private val FIN_FRASE_NOTA = Regex("[.!?]+(?:\\s+|$)")
private val PARRAFOS = Regex("\\n[ \\t]*\\n")
private const val SEPARADOR_ORDEN = " + "
private val NO_ALFANUM = Regex("[^a-z0-9]+")

/** Largo como lo ve el usuario: `{N}` vale 1 carácter y `<b>` / `</b>` valen 0. */
internal fun largoVisible(texto: String): Int = Potencia.TOKEN.replace(ETIQUETA_B.replace(texto, ""), "#").length

private fun frases(nota: String): List<String> = nota.split(FIN_FRASE_NOTA).map { it.trim() }.filter { it.isNotEmpty() }

private fun parrafos(p: String): List<String> = p.split(PARRAFOS).map { it.trim() }.filter { it.isNotEmpty() }

private fun palabrasDeContenido(frase: String): List<String> =
    Texto
        .normalizar(Potencia.TOKEN.replace(ETIQUETA_B.replace(frase, ""), " "))
        .split(NO_ALFANUM)
        .filter { it.length >= TopesLegibilidad.LARGO_PALABRA_CONTENIDO }

/** Reglas 30-36. Lógica pura; las llama [Validador.legibilidad] (indicador `--legibilidad` de `sc validar`). */
internal fun hallazgosDeLegibilidad(r: Receta): List<Hallazgo> {
    val h = mutableListOf<Hallazgo>()

    fun aviso(
        regla: Int,
        texto: String,
    ) {
        h += Hallazgo(r.slug, regla, Nivel.AVISO, texto)
    }
    for (p in r.pasos) {
        pasoLegible(p, ::aviso)
    }
    textosGeneralesLegibles(r, ::aviso)
    h += ordenDeOff(r)
    return h
}

private fun pasoLegible(
    p: Paso,
    aviso: (Int, String) -> Unit,
) {
    regla30(p, aviso)
    p.nota?.let { regla31(p, it, aviso) }
    regla32(p, aviso)
    for (hito in p.hitos) {
        val n = largoVisible(hito.t)
        if (n > TopesLegibilidad.HITO_MAX) {
            aviso(33, "${p.id}: hito de $n caracteres (> ${TopesLegibilidad.HITO_MAX}): «${hito.t}»")
        }
    }
    p.p?.let { regla34P(p.id, it, aviso) }
    p.nota?.let { regla36(p, it, aviso) }
}

private fun regla30(
    p: Paso,
    aviso: (Int, String) -> Unit,
) {
    val n = largoVisible(p.t)
    if (n > TopesLegibilidad.T_MAX) aviso(30, "${p.id}: t de $n caracteres (> ${TopesLegibilidad.T_MAX})")
    val sinMarcas = Potencia.TOKEN.replace(ETIQUETA_B.replace(p.t, ""), "#")
    DURACION_ESCRITA.find(sinMarcas)?.let { aviso(30, "${p.id}: t lleva la duración escrita («${it.value}»)") }
}

private fun regla31(
    p: Paso,
    nota: String,
    aviso: (Int, String) -> Unit,
) {
    val n = largoVisible(nota)
    if (n > TopesLegibilidad.NOTA_MAX) aviso(31, "${p.id}: nota de $n caracteres (> ${TopesLegibilidad.NOTA_MAX})")
    val nFrases = frases(nota).size
    if (nFrases > TopesLegibilidad.NOTA_FRASES_MAX) {
        aviso(31, "${p.id}: nota de $nFrases frases (> ${TopesLegibilidad.NOTA_FRASES_MAX})")
    }
    if (MINUTO_N.containsMatchIn(Texto.normalizar(nota))) aviso(31, "${p.id}: la nota habla de un minuto de reloj")
    val f = p.f
    if (f != null && Potencia.TOKEN.findAll(nota).any { it.groupValues[1].toInt() == f }) {
        aviso(31, "${p.id}: la nota repite la potencia {$f} que ya pinta f")
    }
}

private fun regla32(
    p: Paso,
    aviso: (Int, String) -> Unit,
) {
    for ((campo, valor) in listOf("meto" to p.meto, "saco" to p.saco)) {
        for (elemento in valor
            .orEmpty()
            .split(SEPARADOR_ORDEN)
            .map { it.trim() }
            .filter { it.isNotEmpty() }) {
            val n = largoVisible(elemento)
            if (n > TopesLegibilidad.ELEMENTO_ORDEN_MAX) {
                val tope = TopesLegibilidad.ELEMENTO_ORDEN_MAX
                aviso(32, "${p.id}: $campo con un elemento de $n caracteres (> $tope): «$elemento»")
            }
            if (MEDIDA_CORTE.containsMatchIn(elemento)) {
                aviso(32, "${p.id}: $campo lleva la medida del corte: «$elemento»")
            }
        }
    }
}

private fun regla34P(
    id: String,
    p: String,
    aviso: (Int, String) -> Unit,
) {
    val n = largoVisible(p)
    if (n > TopesLegibilidad.P_MAX) aviso(34, "$id: p de $n caracteres (> ${TopesLegibilidad.P_MAX})")
    val trozos = parrafos(p)
    if (trozos.size > TopesLegibilidad.P_PARRAFOS_MAX) {
        aviso(34, "$id: p de ${trozos.size} párrafos (> ${TopesLegibilidad.P_PARRAFOS_MAX})")
    }
    trozos.map { largoVisible(it) }.filter { it > TopesLegibilidad.P_PARRAFO_MAX }.forEach {
        aviso(34, "$id: párrafo de p de $it caracteres (> ${TopesLegibilidad.P_PARRAFO_MAX})")
    }
}

private fun textosGeneralesLegibles(
    r: Receta,
    aviso: (Int, String) -> Unit,
) {
    val d = largoVisible(r.descripcion)
    if (d > TopesLegibilidad.DESCRIPCION_MAX) {
        aviso(34, "descripcion de $d caracteres (> ${TopesLegibilidad.DESCRIPCION_MAX})")
    }
    r.lectura?.let {
        val n = largoVisible(it)
        if (n > TopesLegibilidad.LECTURA_MAX) aviso(34, "lectura de $n caracteres (> ${TopesLegibilidad.LECTURA_MAX})")
        if (parrafos(it).size < 2) aviso(34, "lectura sin ningún salto de párrafo")
    }
    for (decision in r.porque) {
        val titulo = decision.firstOrNull().orEmpty()
        decision.drop(1).map { largoVisible(it) }.filter { it > TopesLegibilidad.PORQUE_MAX }.forEach {
            aviso(34, "porque «$titulo» de $it caracteres (> ${TopesLegibilidad.PORQUE_MAX})")
        }
    }
    for (b in r.mep) {
        val n = largoVisible(b.porq.orEmpty())
        if (n > TopesLegibilidad.PORQUE_MAX) {
            aviso(34, "mep «${b.t}»: porq de $n caracteres (> ${TopesLegibilidad.PORQUE_MAX})")
        }
    }
}

/** Regla 35 (ERROR): los pasos del array van en orden de `off` no decreciente. Un hallazgo por cada retroceso. */
private fun ordenDeOff(r: Receta): List<Hallazgo> =
    r.pasos.zipWithNext().filter { (a, b) -> b.off < a.off }.map { (a, b) ->
        val texto = "${b.id} (off ${b.off}) va después de ${a.id} (off ${a.off}): el array no sigue el orden de off"
        Hallazgo(r.slug, 35, Nivel.ERROR, texto)
    }

/** Regla 36: una frase de la nota cuyas palabras de contenido ya están en `p` en [TopesLegibilidad.REPETICION_MIN]. */
private fun regla36(
    p: Paso,
    nota: String,
    aviso: (Int, String) -> Unit,
) {
    val enP = palabrasDeContenido(p.p.orEmpty()).flatMap { listOf(it, Texto.singular(it)) }.toSet()
    for (frase in frases(nota)) {
        val palabras = palabrasDeContenido(frase)
        if (palabras.size < TopesLegibilidad.PALABRAS_CONTENIDO_MIN) continue
        val repetidas = palabras.count { it in enP || Texto.singular(it) in enP }
        if (repetidas.toDouble() / palabras.size >= TopesLegibilidad.REPETICION_MIN - EPS_REPETICION) {
            aviso(36, "${p.id}: la frase «$frase» de la nota repite p ($repetidas de ${palabras.size} palabras)")
        }
    }
}

private const val EPS_REPETICION = 1e-9
