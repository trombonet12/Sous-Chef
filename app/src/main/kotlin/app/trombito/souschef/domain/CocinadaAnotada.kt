package app.trombito.souschef.domain

import app.trombito.souschef.receta.Receta
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Paso de la hora al anotar una cocinada sin cronómetro, en minutos. */
const val PASO_HORA_ANOTAR_MIN = 15L

/**
 * La entrada de historial de una cocinada hecha sin la app («Anótala sin cronómetro»): sin pasos reales ni duración
 * real, y con el `previstoMin` del plan de la receta. Cuenta para «hace…» y para dejar de ser «Nueva», pero no para el
 * ritmo, que solo mira cocinadas con minutos. El id es el mismo `slug-yyyy-MM-dd-HHmmss` que el del cierre del
 * cronómetro, calculado con [momento] (hora local).
 */
fun entradaAnotada(
    receta: Receta,
    raciones: Int,
    momento: LocalDateTime,
    nota: String,
): EntradaHistorial =
    EntradaHistorial(
        id = "${receta.slug}-${SELLO_ID.format(momento)}",
        slug = receta.slug,
        titulo = receta.titulo,
        fecha = DateTimeFormatter.ISO_LOCAL_DATE.format(momento),
        base = receta.tags.base,
        tecnica = receta.tags.tecnica,
        raciones = raciones,
        minutosReales = null,
        nota = normalizarNota(nota),
        pasosReales = emptyList(),
        previstoMin = previstoMinDe(receta),
    )

/**
 * Mueve el momento de la cocinada anotada: [dias] días (con el signo que sea) y [minutos] dentro del día, que dan la
 * vuelta a medianoche sin cambiar la fecha. Nunca pasa de [ahora]: no se anota una cocinada del futuro.
 */
fun ajustarMomento(
    actual: LocalDateTime,
    ahora: LocalDateTime,
    dias: Long = 0,
    minutos: Long = 0,
): LocalDateTime {
    val fecha = actual.toLocalDate().plusDays(dias)
    val hora = actual.toLocalTime().plusMinutes(minutos)
    val movido = LocalDateTime.of(fecha, hora)
    return if (movido.isAfter(ahora)) ahora else movido
}
