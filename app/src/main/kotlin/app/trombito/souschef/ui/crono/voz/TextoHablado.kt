package app.trombito.souschef.ui.crono.voz

private const val SEG_MINUTO = 60
private const val SEG_MEDIO_MINUTO = 30
private const val DECENA = 10
private const val PRIMERA_DECENA = 3
private const val MAX_MINUTOS_EN_LETRA = 30
private const val MAX_SEGUNDOS_EN_LETRA = 59
private val etiquetas = Regex("<[^>]*>")
private val espacios = Regex("""\s+""")

// El índice es el valor; «un» y «veintiún» porque siempre acompañan a minutos o segundos.
private val unidadesEnLetra =
    (
        "cero un dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince dieciséis " +
            "diecisiete dieciocho diecinueve veinte veintiún veintidós veintitrés veinticuatro veinticinco " +
            "veintiséis veintisiete veintiocho veintinueve"
    ).split(' ')
private val decenasEnLetra = listOf("treinta", "cuarenta", "cincuenta")

/** Quita las etiquetas (`<b>`) y los espacios sobrantes: la voz lee solo texto. */
internal fun sinMarcas(texto: String): String = etiquetas.replace(texto, "").replace(espacios, " ").trim()

/** «Treinta segundos», «Dos minutos», «Un minuto y medio»: números en letra hasta 30 minutos. */
internal fun duracionHablada(segundos: Int): String {
    val m = segundos / SEG_MINUTO
    val r = segundos % SEG_MINUTO
    val frase =
        when {
            m > MAX_MINUTOS_EN_LETRA -> "$m minutos"
            r == 0 -> "${enLetra(m)} ${nombreUnidad(m, "minuto")}"
            m == 0 -> "${enLetra(r)} ${nombreUnidad(r, "segundo")}"
            r == SEG_MEDIO_MINUTO -> "${enLetra(m)} ${nombreUnidad(m, "minuto")} y medio"
            else -> "${enLetra(m)} ${nombreUnidad(m, "minuto")} y ${enLetra(r)} ${nombreUnidad(r, "segundo")}"
        }
    return frase.replaceFirstChar { it.titlecase() }
}

private fun nombreUnidad(
    n: Int,
    nombre: String,
): String = if (n == 1) nombre else "${nombre}s"

private fun enLetra(n: Int): String =
    when {
        n < unidadesEnLetra.size -> unidadesEnLetra[n]
        n > MAX_SEGUNDOS_EN_LETRA -> n.toString()
        n % DECENA == 0 -> decenasEnLetra[n / DECENA - PRIMERA_DECENA]
        else -> "${decenasEnLetra[n / DECENA - PRIMERA_DECENA]} y ${unidadesEnLetra[n % DECENA]}"
    }
