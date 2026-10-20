package app.trombito.souschef.receta.crono

/** Una línea de la alarma única, por carril: «SARTÉN GRANDE: Primera fritura: hecho». */
data class LineaAlarma(
    val carril: String,
    val texto: String,
)

/** La alarma que está en la bandeja: cuándo empezó a sonar y qué líneas lleva (una por carril). */
data class AlarmaActiva(
    val inicioMs: Long,
    val lineas: List<LineaAlarma>,
)

/** Resultado de agrupar: la alarma a pintar y si debe sonar (false = se actualiza en silencio). */
data class Agrupado(
    val alarma: AlarmaActiva,
    val sonar: Boolean,
)

/** Agrupa en una sola notificación de alarma lo que coincide en el tiempo. Puro y sin reloj propio. */
object AgrupadorAlarmas {
    const val VENTANA_MS = 60_000L
    private const val SEPARADOR = " · "

    /** Tipos de aviso que piden hacer algo y, por tanto, suenan. */
    private val SONOROS = setOf(TipoAviso.FIN, TipoAviso.HITO, TipoAviso.TOCA_EMPEZAR)

    /**
     * Una línea por carril, en el orden en que cada carril aparece en [avisos]. Texto «NOMBRE: aviso»; si el carril
     * tiene varios avisos se unen con « · » en orden temporal. Ignora los silenciosos y los tipos que no suenan.
     * Si [nombres] no conoce el carril se usa su id.
     */
    fun lineasDe(
        avisos: List<Aviso>,
        nombres: Map<String, String>,
    ): List<LineaAlarma> =
        avisos
            .filter { !it.silencioso && it.tipo in SONOROS }
            .groupBy { it.carril }
            .map { (carril, delCarril) ->
                val texto = delCarril.sortedBy { it.instanteMs }.joinToString(SEPARADOR) { it.texto }
                LineaAlarma(carril, "${nombres[carril] ?: carril}: $texto")
            }

    /**
     * Sin [activa] o con la activa caducada (>= [VENTANA_MS]): alarma nueva con [nuevas] y `sonar = true`.
     * Si sigue viva: fusiona (la línea nueva de un carril sustituye a la vieja en su sitio; carriles nuevos al final),
     * mantiene `inicioMs` y `sonar = false`.
     * Con [nuevas] vacías no hay nada que avisar: devuelve [activa] tal cual (o una alarma vacía de [ahoraMs] si es
     * null) con `sonar = false`.
     */
    fun combinar(
        activa: AlarmaActiva?,
        nuevas: List<LineaAlarma>,
        ahoraMs: Long,
    ): Agrupado =
        when {
            nuevas.isEmpty() -> {
                Agrupado(activa ?: AlarmaActiva(ahoraMs, emptyList()), sonar = false)
            }

            activa == null || ahoraMs - activa.inicioMs >= VENTANA_MS -> {
                Agrupado(AlarmaActiva(ahoraMs, nuevas), sonar = true)
            }

            else -> {
                Agrupado(fusionar(activa, nuevas), sonar = false)
            }
        }

    private fun fusionar(
        activa: AlarmaActiva,
        nuevas: List<LineaAlarma>,
    ): AlarmaActiva {
        val porCarril = nuevas.associateBy { it.carril }
        val sustituidas = activa.lineas.map { porCarril[it.carril] ?: it }
        val presentes = activa.lineas.mapTo(HashSet()) { it.carril }
        val anadidas = nuevas.filter { it.carril !in presentes }
        return activa.copy(lineas = sustituidas + anadidas)
    }

    /** Quita las líneas de esos [carriles] (su paso se dio por hecho). null si no queda ninguna. */
    fun quitar(
        activa: AlarmaActiva?,
        carriles: Collection<String>,
    ): AlarmaActiva? {
        if (activa == null) return null
        val quedan = activa.lineas.filter { it.carril !in carriles }
        return if (quedan.isEmpty()) null else activa.copy(lineas = quedan)
    }
}
