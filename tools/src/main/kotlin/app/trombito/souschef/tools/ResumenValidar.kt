package app.trombito.souschef.tools

import app.trombito.souschef.receta.Hallazgo

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

/** `--estricto` convierte los avisos en fallo; los errores fallan siempre (`--legibilidad` no cambia nada). */
internal fun validarFalla(
    errores: Int,
    avisos: Int,
    estricto: Boolean,
): Boolean = errores > 0 || (estricto && avisos > 0)

private val LEGIBILIDAD = 30..36

internal fun lineaLegibilidad(hallazgos: List<Hallazgo>): String =
    "legibilidad por regla: ${resumenPorRegla(hallazgos, LEGIBILIDAD).joinToString(", ").ifEmpty { "nada" }}"
