package app.trombito.souschef.receta

import kotlin.math.roundToInt

enum class TipoCocina { VITRO, INDUCCION, GAS }

enum class Mando { DIGITAL, ANALOGICO }

/**
 * Cocina del usuario. `max` es el número más alto del mando; `null` = mando sin números (solo en ANALOGICO).
 * La potencia canónica de las recetas es vitrocerámica digital 0-9 (`Paso.f`).
 */
data class Cocina(
    val tipo: TipoCocina = TipoCocina.VITRO,
    val mando: Mando = Mando.DIGITAL,
    val max: Int? = 9,
) {
    val esCanonica: Boolean get() = tipo == TipoCocina.VITRO && mando == Mando.DIGITAL && max == 9

    /** Ajusta lo inválido: DIGITAL 9..20 (sin número → 9); ANALOGICO 4..12 o sin números. */
    fun valida(): Cocina =
        when {
            mando == Mando.DIGITAL -> copy(max = (max ?: DIGITAL_MIN).coerceIn(DIGITAL_MIN, DIGITAL_MAX))
            max == null -> this
            else -> copy(max = max.coerceIn(ANALOGICO_MIN, ANALOGICO_MAX))
        }

    companion object {
        const val DIGITAL_MIN = 9
        const val DIGITAL_MAX = 20
        const val ANALOGICO_MIN = 4
        const val ANALOGICO_MAX = 12
        val VITRO_9 = Cocina()
    }
}

/** Potencia ya traducida a la cocina del usuario. */
sealed interface NivelPotencia {
    data class Numero(
        val valor: Double,
    ) : NivelPotencia

    data class Palabra(
        val texto: String,
    ) : NivelPotencia

    data object Apagado : NivelPotencia
}

/** Traduce la potencia canónica 0-9 a la cocina del usuario y reescribe los `{N}` de los textos libres. */
object Potencia {
    private const val CANON_MAX = 9
    private const val APAGADO = "apagado"

    /** Marca de potencia canónica en texto libre: `{0}`..`{9}`. */
    val TOKEN: Regex = Regex("\\{(\\d)\\}")

    private val RANGO = Regex("(?<![\\p{L}\\p{N}])([dD])el? \\{(\\d)\\} al? \\{(\\d)\\}")
    private val SUELTO =
        Regex("(?<![\\p{L}\\p{N}])(?:([aA]) )?(?:([fF]uego|[pP]otencia) )?(?:([aA]l) )?\\{(\\d)\\}")

    fun nivel(
        f: Int,
        c: Cocina,
    ): NivelPotencia {
        val cocina = c.valida()
        val max = cocina.max
        val g = f.coerceAtMost(CANON_MAX)
        return when {
            g <= 0 -> {
                NivelPotencia.Apagado
            }

            max == null -> {
                NivelPotencia.Palabra(palabra(g))
            }

            cocina.mando == Mando.DIGITAL -> {
                NivelPotencia.Numero(maxOf(1, (g * max / CANON_MAX.toDouble()).roundToInt()).toDouble())
            }

            else -> {
                NivelPotencia.Numero(maxOf(1.0, (g * max / CANON_MAX.toDouble() * 2).roundToInt() / 2.0))
            }
        }
    }

    /** El número formateado ("3,5"), la palabra ("medio-alto") o "apagado". */
    fun texto(
        f: Int,
        c: Cocina,
    ): String = traducir(f, c)

    private fun traducir(
        f: Int,
        c: Cocina,
    ): String =
        when (val n = nivel(f, c)) {
            is NivelPotencia.Numero -> Escalado.numero(n.valor)
            is NivelPotencia.Palabra -> n.texto
            NivelPotencia.Apagado -> APAGADO
        }

    /**
     * Sustituye los `{N}` por la potencia en la cocina `c`. Con mando numérico queda el número ("{0}" → "0"); sin
     * números se reescribe con gramática ("al {7}" → "a fuego medio-alto"). Sin tokens devuelve la misma instancia.
     */
    fun reescribir(
        texto: String,
        c: Cocina,
    ): String {
        val cocina = c.valida()
        return when {
            !TOKEN.containsMatchIn(texto) -> {
                texto
            }

            cocina.max != null -> {
                TOKEN.replace(texto) { m ->
                    val f = m.groupValues[1].toInt()
                    if (f == 0) "0" else traducir(f, cocina)
                }
            }

            else -> {
                val sinRangos = RANGO.replace(texto) { rango(it, cocina) }
                SUELTO.replace(sinRangos) { suelto(it, sinRangos, cocina) }
            }
        }
    }

    private fun palabra(f: Int): String =
        when (f) {
            1 -> "mínimo"
            2, 3 -> "bajo"
            4 -> "medio-bajo"
            5 -> "medio"
            6, 7 -> "medio-alto"
            8 -> "alto"
            else -> "máximo"
        }

    /** "del {4} al {5}" → "de fuego medio-bajo a medio"; si los dos extremos coinciden, "a fuego X". */
    private fun rango(
        m: MatchResult,
        c: Cocina,
    ): String {
        val (d, a, b) = m.destructured
        val desde = traducir(a.toInt(), c)
        val hasta = traducir(b.toInt(), c)
        return when {
            desde != hasta -> "${d}e fuego $desde a $hasta"
            d == "D" -> "A fuego $desde"
            else -> "a fuego $desde"
        }
    }

    /** `{N}` con sus palabras previas opcionales ("a", "fuego"/"potencia", "al"): nunca duplica "fuego". */
    private fun suelto(
        m: MatchResult,
        fuente: String,
        c: Cocina,
    ): String {
        val (pre, ref, al) = m.destructured
        val f = m.groupValues[4].toInt()
        val resultado =
            if (f == 0) {
                val conservaA = pre.isNotEmpty() && ref.isEmpty() && al.isEmpty()
                (if (conservaA) "$pre " else "") + APAGADO
            } else {
                val ruta =
                    if (pre.isNotEmpty()) {
                        "$pre "
                    } else if (al.isNotEmpty() && ref.isEmpty()) {
                        "a "
                    } else {
                        ""
                    }
                val fuego = if (ruta.isEmpty() && ref.startsWith("F")) "Fuego" else "fuego"
                "$ruta$fuego ${traducir(f, c)}"
            }
        val previo = fuente.substring(0, m.range.first).trimEnd()
        val abreFrase = previo.isEmpty() || previo.last() in ".!?"
        val mayuscula = abreFrase || m.value.first().isUpperCase()
        return if (mayuscula) resultado.replaceFirstChar { it.uppercaseChar() } else resultado
    }
}

/** Copia con la potencia de todos los textos libres traducida a la cocina `c`. No toca `f` ni ids. */
fun Receta.conCocina(c: Cocina): Receta {
    val r = { s: String -> Potencia.reescribir(s, c) }
    val copia =
        copy(
            descripcion = r(descripcion),
            notaEscala = notaEscala?.let(r),
            lectura = lectura?.let(r),
            pie = pie?.let(r),
            porque = porque.map { fila -> fila.map(r) },
            cortes = cortes.map { it.copy(p = it.p?.let(r)) },
            mep = mep.map { b -> b.copy(porq = b.porq?.let(r), l = b.l.map(r)) },
            pasos =
                pasos.map { p ->
                    p.copy(
                        t = r(p.t),
                        nota = p.nota?.let(r),
                        p = p.p?.let(r),
                        meto = p.meto?.let(r),
                        saco = p.saco?.let(r),
                        sinHitos = p.sinHitos?.let(r),
                        hitos = p.hitos.map { h -> h.copy(t = r(h.t)) },
                    )
                },
        )
    return if (copia == this) this else copia
}

/** Copia de la técnica con la potencia de sus textos libres traducida a la cocina `c`. */
fun Tecnica.conCocina(c: Cocina): Tecnica {
    val r = { s: String -> Potencia.reescribir(s, c) }
    val copia =
        copy(
            resumen = r(resumen),
            pasos = pasos.map { it.copy(t = r(it.t), p = r(it.p)) },
            consejos = consejos.map(r),
            cortes = cortes.map { it.copy(p = it.p?.let(r)) },
            pie = pie?.let(r),
        )
    return if (copia == this) this else copia
}
