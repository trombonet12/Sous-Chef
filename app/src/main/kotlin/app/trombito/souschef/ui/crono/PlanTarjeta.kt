package app.trombito.souschef.ui.crono

/** Qué cabe en una tarjeta según la altura que le toca. */
internal data class PlanTarjeta(
    val nota: Boolean,
    val metoSaco: Boolean,
    val tituloLineas: Int,
    val relojSp: Int,
)

// Altura fija de la tarjeta: márgenes, fila de título, barra + próximo aviso (hasta 2 líneas), fila final y
// separaciones.
private const val FIJO_DP = 12 + 48 + 52 + 48 + 16
private const val LINEA_TITULO_DP = 28

// METO y SACO, hasta 2 líneas cada uno: se cuentan unas 3 líneas de 20 dp y una separación.
private const val METO_SACO_DP = 68

// Nota a 16 sp (interlineado 24 dp), hasta 2 líneas, y una separación.
private const val NOTA_DP = 52
private const val RELOJ_MAX_SP = 88
private const val RELOJ_MIN_SP = 36
private const val ALTO_POR_SP = 1.2f

/**
 * Si la tarjeta no da para su parte fija, una línea de título, la nota, METO/SACO y el reloj mínimo (todo crece con la
 * letra del sistema), se pinta compacta: dos filas y debajo el texto repartido por prioridad, que es lo que se lee de
 * un vistazo. [hayNota] y [hayMetoSaco] son si el paso los tiene; sin saberlo se cuentan los dos (lo más estricto).
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

// Tarjeta compacta: márgenes y botones (dp fijos), fila de cabecera (nombre y título a 2 líneas) y cada línea de texto
// (todo crece con la letra).
private const val COMPACTA_FIJO_DP = 8 + 48 + 8
private const val COMPACTA_CABECERA_DP = 40
private const val COMPACTA_LINEA_DP = 20
private const val COMPACTA_LINEAS_MAX = 8

/** Cuántas líneas de texto (nota, METO, SACO, aviso y «Después») caben en una tarjeta compacta. */
internal fun lineasExtraCompacta(
    alturaDp: Float,
    escalaLetra: Float,
): Int {
    val escala = maxOf(1f, escalaLetra)
    val libre = alturaDp - COMPACTA_FIJO_DP - COMPACTA_CABECERA_DP * escala
    return (libre / (COMPACTA_LINEA_DP * escala)).toInt().coerceIn(0, COMPACTA_LINEAS_MAX)
}

/** Líneas que se dan a cada texto de la tarjeta compacta (0 = no se pinta). */
internal data class LineasCompacta(
    val nota: Int,
    val junto: Int,
    val meto: Int,
    val saco: Int,
    val hito: Int,
    val despues: Int,
)

private const val CARACTERES_POR_LINEA = 42
private const val LINEAS_POR_TEXTO = 2

/**
 * Reparte [presupuesto] líneas por prioridad: nota, «a la vez que» / «Antes» ([hayJunto]), METO, SACO, próximo aviso y
 * «Después». Cada texto pide 1 o 2 líneas según su longitud (una longitud de 0 es que no hay texto); si queda menos,
 * recibe lo que quede.
 */
@Suppress("LongParameterList")
internal fun repartoLineasCompacta(
    presupuesto: Int,
    longNota: Int,
    longMeto: Int,
    longSaco: Int,
    hayHito: Boolean,
    hayJunto: Boolean = false,
): LineasCompacta {
    var libres = presupuesto

    fun dar(quiere: Int): Int = minOf(quiere, libres).also { libres -= it }

    fun lineasDe(longitud: Int): Int =
        if (longitud <= 0) {
            0
        } else {
            ((longitud + CARACTERES_POR_LINEA - 1) / CARACTERES_POR_LINEA).coerceIn(1, LINEAS_POR_TEXTO)
        }
    val nota = dar(lineasDe(longNota))
    val junto = dar(if (hayJunto) 1 else 0)
    val meto = dar(lineasDe(longMeto))
    val saco = dar(lineasDe(longSaco))
    val hito = dar(if (hayHito) 1 else 0)
    val despues = dar(LINEAS_POR_TEXTO)
    return LineasCompacta(nota, junto, meto, saco, hito, despues)
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
    return elegido.copy(relojSp = elegido.relojSp.coerceIn(RELOJ_MIN_SP, RELOJ_MAX_SP))
}
