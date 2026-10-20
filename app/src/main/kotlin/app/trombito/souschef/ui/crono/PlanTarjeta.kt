package app.trombito.souschef.ui.crono

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
 * Si la tarjeta no da ni para su parte fija, una línea de título y el reloj mínimo (todo crece con la letra del
 * sistema), se pinta compacta: dos filas, sin «Después», METO/SACO ni nota.
 */
internal fun tarjetaCompacta(
    alturaDp: Float,
    escalaLetra: Float,
): Boolean = alturaDp < (FIJO_DP + LINEA_TITULO_DP + RELOJ_MIN_SP * ALTO_POR_SP) * maxOf(1f, escalaLetra)

// Tarjeta compacta: márgenes y botones (dp fijos), fila de cabecera y cada línea extra (crecen con la letra).
private const val COMPACTA_FIJO_DP = 8 + 48 + 8
private const val COMPACTA_CABECERA_DP = 28
private const val COMPACTA_LINEA_DP = 19

/** Cuántas líneas extra (hito, después, nota, en ese orden) caben en una tarjeta compacta. */
internal fun lineasExtraCompacta(
    alturaDp: Float,
    escalaLetra: Float,
): Int {
    val escala = maxOf(1f, escalaLetra)
    val libre = alturaDp - COMPACTA_FIJO_DP - COMPACTA_CABECERA_DP * escala
    return (libre / (COMPACTA_LINEA_DP * escala)).toInt().coerceIn(0, 3)
}

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
