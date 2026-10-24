package app.trombito.souschef.domain

import app.trombito.souschef.receta.Receta
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Días que una receta sin cocinar sigue contando como «Nueva». */
const val DIAS_NUEVA = 30L

/**
 * «Nueva»: nunca se ha cocinado ([ultimaCocinada] null) y su `creada` (ISO con hora, p. ej.
 * `2026-10-24T00:00:00Z`) es de hace menos de [DIAS_NUEVA] días respecto a [hoy]. Sin `creada`, ilegible o en el
 * futuro, no es nueva.
 */
fun esNueva(
    receta: Receta,
    ultimaCocinada: String?,
    hoy: LocalDate,
): Boolean {
    if (ultimaCocinada != null) return false
    val creada = receta.creada?.let { runCatching { LocalDate.parse(it.take(LONGITUD_FECHA)) }.getOrNull() }
    val dias = creada?.let { ChronoUnit.DAYS.between(it, hoy) }
    return dias != null && dias in 0 until DIAS_NUEVA
}

private const val LONGITUD_FECHA = 10
