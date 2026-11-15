package app.trombito.souschef.tools

import app.trombito.souschef.receta.Hallazgo
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Validador

/** Recuento por regla de los hallazgos del [rango] (p. ej. 30..36), en orden numérico: «R30: 12». */
internal fun resumenPorRegla(
    hallazgos: List<Hallazgo>,
    rango: IntRange,
): List<String> =
    hallazgos
        .filter { it.regla in rango }
        .groupingBy { it.regla }
        .eachCount()
        .toSortedMap()
        .map { (regla, n) -> "R$regla: $n" }

/** `--estricto` y `--legibilidad` convierten los avisos en fallo; los errores fallan siempre. */
internal fun validarFalla(
    errores: Int,
    avisos: Int,
    estricto: Boolean,
    legibilidad: Boolean,
): Boolean = errores > 0 || ((estricto || legibilidad) && avisos > 0)

private val LEGIBILIDAD = 30..36

/** Las reglas 30-36 solo corren con `--legibilidad`. */
internal fun legible(
    r: Receta,
    activa: Boolean,
): List<Hallazgo> = if (activa) Validador.legibilidad(r) else emptyList()

internal fun lineaLegibilidad(hallazgos: List<Hallazgo>): String =
    "legibilidad por regla: ${resumenPorRegla(hallazgos, LEGIBILIDAD).joinToString(", ").ifEmpty { "nada" }}"
