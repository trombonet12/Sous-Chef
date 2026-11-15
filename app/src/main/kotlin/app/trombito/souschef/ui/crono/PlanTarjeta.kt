package app.trombito.souschef.ui.crono

/** Qué cabe en una tarjeta según la altura que le toca. */
internal data class PlanTarjeta(
    val nota: Boolean,
    val metoSaco: Boolean,
    val tituloLineas: Int,
    val relojSp: Int,
    /** Alto máximo (dp, a letra 1,0×) de la pila de texto SEÑAL/METO/SACO; lo que no cabe se corta o se omite. */
    val textoDp: Int = 0,
)

// Altura fija de la tarjeta: márgenes, fila de cabecera, fila de hora prevista y chips, barra + próximo aviso (hasta 2
// líneas), fila final y separaciones.
private const val FIJO_DP = 12 + 48 + 32 + 52 + 48 + 20
private const val LINEA_TITULO_DP = 28

// METO y SACO, hasta 2 líneas cada uno (24 dp por línea) y una separación.
private const val METO_SACO_DP = 100

// Nota a 16 sp (interlineado 28 dp con la letra grande del tema), hasta 2 líneas, y una separación.
private const val NOTA_DP = 60
private const val RELOJ_MAX_SP = 88
private const val RELOJ_MIN_SP = 36
private const val ALTO_POR_SP = 1.2f

/**
 * Si la tarjeta no da para su parte fija, una línea de título, la nota, METO/SACO y el reloj mínimo (todo crece con la
 * letra del sistema), se pinta compacta: cabecera, reloj con mandos y debajo la pila de textos por prioridad, que mide
 * lo que cabe entero (ver [PilaPrioritaria]). [hayNota] y [hayMetoSaco] son si el paso los tiene; sin saberlo se
 * cuentan los dos (lo más estricto).
 */
internal fun tarjetaCompacta(
    alturaDp: Float,
    escalaLetra: Float,
    hayNota: Boolean = true,
    hayMetoSaco: Boolean = true,
): Boolean {
    val nota = if (hayNota) NOTA_DP else 0
    val metoSaco = if (hayMetoSaco) METO_SACO_DP else 0
    val minimo = FIJO_DP + LINEA_TITULO_DP + nota + metoSaco + RELOJ_MIN_SP * ALTO_POR_SP
    return alturaDp < minimo * maxOf(1f, escalaLetra)
}

/**
 * Reparte la altura: título a 2 líneas, nota y METO/SACO (hasta 2 líneas cada uno) van por delante del reloj, que
 * ocupa lo que sobra entre [RELOJ_MIN_SP] y [RELOJ_MAX_SP]. El reloj baja antes que nada; si ni con una línea de título
 * caben la nota y METO/SACO con el reloj mínimo, [tarjetaCompacta] ya habrá pasado la tarjeta a compacta.
 */
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
internal fun planTarjeta(
    alturaDp: Float,
    carriles: Int,
    hayMetoSaco: Boolean,
    hayNota: Boolean,
): PlanTarjeta {
    // [carriles] no oculta nada: con poca altura la tarjeta pasa a compacta, que reparte mejor los textos.
    val candidatos =
        listOf(
            PlanTarjeta(hayNota, hayMetoSaco, 2, 0),
            PlanTarjeta(hayNota, hayMetoSaco, 1, 0),
        )
    val libre = alturaDp - FIJO_DP
    val conReloj =
        candidatos.map {
            val meto = if (it.metoSaco) METO_SACO_DP else 0
            val nota = if (it.nota) NOTA_DP else 0
            val gastado = it.tituloLineas * LINEA_TITULO_DP + meto + nota
            it.copy(relojSp = ((libre - gastado) / ALTO_POR_SP).toInt())
        }
    val elegido = conReloj.firstOrNull { it.relojSp >= RELOJ_MIN_SP } ?: conReloj.last()
    val textoDp = (if (elegido.nota) NOTA_DP else 0) + (if (elegido.metoSaco) METO_SACO_DP else 0)
    return elegido.copy(relojSp = elegido.relojSp.coerceIn(RELOJ_MIN_SP, RELOJ_MAX_SP), textoDp = textoDp)
}
