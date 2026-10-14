package app.souschef.ui.crono

import app.souschef.receta.Paso
import app.souschef.receta.Receta
import app.souschef.receta.crono.EstadoCrono
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/** "HH:mm" de hoy (según [ahoraMs]) en epoch ms; si no se entiende, las 15:30. */
fun servirDeHoy(
    texto: String,
    ahoraMs: Long,
    zona: ZoneId,
): Long {
    val hora = runCatching { LocalTime.parse(texto) }.getOrDefault(LocalTime.of(15, 30))
    val hoy = Instant.ofEpochMilli(ahoraMs).atZone(zona).toLocalDate()
    return hoy
        .atTime(hora)
        .atZone(zona)
        .toInstant()
        .toEpochMilli()
}

/** "mm:ss" (o "h:mm:ss"); pasado de cero, "+mm:ss". */
fun formatoReloj(segundos: Int): String {
    val abs = kotlin.math.abs(segundos)
    val h = abs / 3600
    val m = abs % 3600 / 60
    val s = abs % 60
    val cuerpo =
        if (h > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", m, s)
        }
    return if (segundos < 0) "+$cuerpo" else cuerpo
}

fun formatoHora(
    ms: Long,
    zona: ZoneId = ZoneId.systemDefault(),
): String {
    val t = Instant.ofEpochMilli(ms).atZone(zona)
    return String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)
}

/** Cola de un carril: sus pasos en el orden del array. */
fun colaDe(
    receta: Receta,
    carril: String,
): List<Paso> = receta.pasos.filter { it.carril == carril }

fun pasoActual(
    receta: Receta,
    estado: EstadoCrono,
    carril: String,
): Paso? {
    val ec = estado.carriles[carril] ?: return null
    return if (ec.fin) null else colaDe(receta, carril).getOrNull(ec.i)
}

/** Un paso está hecho si su carril terminó o ya pasó de su posición. */
fun pasoHecho(
    receta: Receta,
    estado: EstadoCrono,
    idPaso: String,
): Boolean {
    val paso = receta.paso(idPaso)
    val ec = paso?.let { estado.carriles[it.carril] }
    return paso == null || ec == null || ec.fin || ec.i > colaDe(receta, paso.carril).indexOfFirst { it.id == idPaso }
}

/** Requisitos del paso: los de `dep` aún sin terminar y los ya cumplidos. */
data class Requisitos(
    val pendientes: List<Paso>,
    val cumplidos: List<Paso>,
)

fun requisitos(
    receta: Receta,
    estado: EstadoCrono,
    paso: Paso,
): Requisitos {
    val deps = paso.dep.mapNotNull { receta.paso(it) }
    val (hechos, pendientes) = deps.partition { pasoHecho(receta, estado, it.id) }
    return Requisitos(pendientes, hechos)
}

/** Duración corta de un paso: "5 min", "1 min 30 s"... */
fun minutosPaso(paso: Paso): String {
    val m = paso.segundos / 60
    val s = paso.segundos % 60
    return when {
        m == 0 -> "$s s"
        s == 0 -> "$m min"
        else -> "$m min $s s"
    }
}

/** Fracción consumida del paso (0..1) para la barra. */
fun progreso(
    segundos: Int,
    quedan: Int,
): Float = if (segundos <= 0) 1f else ((segundos - quedan).toFloat() / segundos).coerceIn(0f, 1f)
