package app.trombito.souschef.domain

import java.time.Instant
import java.time.ZoneId

/** Reloj de pared inyectable: el cronómetro y la sync nunca leen la hora directamente (tests deterministas). */
fun interface Reloj {
    fun ahoraMs(): Long

    companion object {
        val Sistema = Reloj { System.currentTimeMillis() }
    }
}

private const val MINUTOS_HORA = 60

/** Hora actual en minutos del día, en la zona del móvil. */
fun Reloj.ahoraMin(zona: ZoneId = ZoneId.systemDefault()): Int {
    val t = Instant.ofEpochMilli(ahoraMs()).atZone(zona)
    return t.hour * MINUTOS_HORA + t.minute
}
