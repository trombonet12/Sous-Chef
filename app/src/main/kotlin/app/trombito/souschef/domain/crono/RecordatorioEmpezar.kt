package app.trombito.souschef.domain.crono

import java.time.Instant
import java.time.ZoneId

/**
 * El recordatorio «Avísame para empezar» (ADR-0016): uno solo, en reloj de pared. No es una cocinada: no tiene
 * `EstadoCrono` y no cuenta como viva.
 */
data class Recordatorio(
    val slug: String,
    val empezarMs: Long,
    val servirMs: Long,
)

/** Lo que hay que hacer con el recordatorio guardado cuando la app lo relee (disparo, reinicio, cambio de hora). */
sealed interface AccionRecordatorio {
    /** No hay nada guardado. */
    data object Nada : AccionRecordatorio

    /** Aún no es la hora: se vuelve a armar en [ms]. */
    data class Armar(
        val ms: Long,
    ) : AccionRecordatorio

    /** Es la hora, o pasó hace poco: se avisa ya y se borra. */
    data object Avisar : AccionRecordatorio

    /** Pasó hace demasiado: se borra sin avisar. */
    data object Borrar : AccionRecordatorio
}

/** El chip de la ficha: «Avísame a las HH:mm» ([puesto] false) o «Aviso a las HH:mm · Quitar» ([puesto] true). */
data class ChipAviso(
    val horaMin: Int,
    val puesto: Boolean,
)

/** Reglas puras del recordatorio: cálculo de la hora, persistencia como texto, regla del reinicio y el chip. */
object RecordatorioEmpezar {
    /** Pasado este tiempo desde la hora, tras un reinicio el aviso ya no tiene sentido y se borra. */
    const val GRACIA_MS = 10 * 60_000L

    private const val MS_MINUTO = 60_000L
    private const val MIN_HORA = 60
    private const val MIN_DIA = 24 * MIN_HORA
    private const val SEPARADOR = "|"
    private const val PARTES = 3

    /** La próxima ocurrencia de HH:mm de hoy ([empezarMin] desde medianoche), o null si ya pasó (o es ahora mismo). */
    fun proximoMs(
        empezarMin: Int,
        ahoraMs: Long,
        zona: ZoneId,
    ): Long? {
        val hoy = Instant.ofEpochMilli(ahoraMs).atZone(zona).toLocalDate()
        val ms =
            hoy
                .atTime(empezarMin.mod(MIN_DIA) / MIN_HORA, empezarMin.mod(MIN_DIA) % MIN_HORA)
                .atZone(zona)
                .toInstant()
                .toEpochMilli()
        return ms.takeIf { it > ahoraMs }
    }

    /**
     * El recordatorio para empezar a las [empezarMin] y servir a las [servirMin], o null si la hora de empezar ya
     * pasó. La hora de servir es la que sigue a la de empezar, aunque cruce la medianoche.
     */
    fun crear(
        slug: String,
        empezarMin: Int,
        servirMin: Int,
        ahoraMs: Long,
        zona: ZoneId,
    ): Recordatorio? {
        val empezarMs = proximoMs(empezarMin, ahoraMs, zona) ?: return null
        val hastaServirMin = (servirMin - empezarMin).mod(MIN_DIA)
        return Recordatorio(slug, empezarMs, empezarMs + hastaServirMin * MS_MINUTO)
    }

    /**
     * Regla común del disparo, el reinicio y los cambios de hora: antes de la hora se vuelve a armar; si pasó hace
     * [GRACIA_MS] o menos se avisa; si pasó hace más se borra sin avisar.
     */
    fun alReleer(
        r: Recordatorio?,
        ahoraMs: Long,
    ): AccionRecordatorio =
        when {
            r == null -> AccionRecordatorio.Nada
            r.empezarMs > ahoraMs -> AccionRecordatorio.Armar(r.empezarMs)
            ahoraMs - r.empezarMs <= GRACIA_MS -> AccionRecordatorio.Avisar
            else -> AccionRecordatorio.Borrar
        }

    /**
     * Qué chip enseña la ficha de [slug]. Si el recordatorio puesto es de esta receta, siempre se puede quitar. Si no,
     * se ofrece con la hora [empezarMin] que enseña la ficha (la del ritmo), salvo que ya haya pasado, que haya una
     * cocinada viva o que la receta no esté cargada ([empezarMin] null).
     */
    @Suppress("LongParameterList") // el chip depende de la ficha, del recordatorio, de las cocinadas y del reloj
    fun chip(
        slug: String,
        empezarMin: Int?,
        puesto: Recordatorio?,
        hayViva: Boolean,
        ahoraMs: Long,
        zona: ZoneId,
    ): ChipAviso? =
        when {
            puesto?.slug == slug -> ChipAviso(minutoDelDia(puesto.empezarMs, zona), puesto = true)
            hayViva || empezarMin == null -> null
            proximoMs(empezarMin, ahoraMs, zona) == null -> null
            else -> ChipAviso(empezarMin.mod(MIN_DIA), puesto = false)
        }

    private fun minutoDelDia(
        ms: Long,
        zona: ZoneId,
    ): Int {
        val t = Instant.ofEpochMilli(ms).atZone(zona)
        return t.hour * MIN_HORA + t.minute
    }

    /** «slug|empezarMs|servirMs»; el texto vacío es «no hay recordatorio». */
    fun codificar(r: Recordatorio?): String =
        r?.let { listOf(it.slug, it.empezarMs, it.servirMs).joinToString(SEPARADOR) }.orEmpty()

    /** Lo contrario de [codificar]; un valor ausente o mal formado cuenta como «no hay recordatorio». */
    fun decodificar(texto: String?): Recordatorio? {
        val partes = texto?.split(SEPARADOR)?.takeIf { it.size == PARTES && it[0].isNotBlank() }
        val empezar = partes?.get(1)?.toLongOrNull()
        val servir = partes?.get(2)?.toLongOrNull()
        if (partes == null || empezar == null || servir == null) return null
        return Recordatorio(partes[0], empezar, servir)
    }
}
