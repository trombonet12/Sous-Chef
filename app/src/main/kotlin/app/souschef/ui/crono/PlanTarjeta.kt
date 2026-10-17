package app.souschef.ui.crono

/** Qué cabe en una tarjeta según la altura que le toca. */
internal data class PlanTarjeta(
    val nota: Boolean,
    val metoSaco: Boolean,
    val tituloLineas: Int,
    val relojSp: Int,
)

// Altura fija de la tarjeta: márgenes, fila de título, barra + hito, fila final y separaciones.
private const val FIJO_DP = 12 + 48 + 40 + 48 + 16
private const val LINEA_TITULO_DP = 28
private const val METO_SACO_DP = 48
private const val NOTA_DP = 44
private const val RELOJ_MAX_SP = 88
private const val RELOJ_MIN_SP = 36
private const val RELOJ_COMODO_SP = 64
private const val ALTO_POR_SP = 1.2f
private const val CARRILES_SIN_METO = 4

/**
 * Reparte la altura: primero se quita la nota, luego METO/SACO (siempre con 4 o más carriles) y por último el título
 * pasa a una línea. El reloj ocupa lo que sobra, entre [RELOJ_MIN_SP] y [RELOJ_MAX_SP].
 */
internal fun planTarjeta(
    alturaDp: Float,
    carriles: Int,
    hayMetoSaco: Boolean,
    hayNota: Boolean,
): PlanTarjeta {
    val metoOk = hayMetoSaco && carriles < CARRILES_SIN_METO
    val candidatos =
        listOf(
            PlanTarjeta(hayNota, metoOk, 2, 0),
            PlanTarjeta(false, metoOk, 2, 0),
            PlanTarjeta(false, false, 2, 0),
            PlanTarjeta(false, false, 1, 0),
        )
    val libre = alturaDp - FIJO_DP
    val conReloj =
        candidatos.map {
            val meto = if (it.metoSaco) METO_SACO_DP else 0
            val nota = if (it.nota) NOTA_DP else 0
            val gastado = it.tituloLineas * LINEA_TITULO_DP + meto + nota
            it.copy(relojSp = ((libre - gastado) / ALTO_POR_SP).toInt())
        }
    val elegido = conReloj.firstOrNull { it.relojSp >= RELOJ_COMODO_SP } ?: conReloj.last()
    return elegido.copy(relojSp = elegido.relojSp.coerceIn(RELOJ_MIN_SP, RELOJ_MAX_SP))
}
