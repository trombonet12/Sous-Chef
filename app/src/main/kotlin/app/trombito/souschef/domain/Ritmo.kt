package app.trombito.souschef.domain

import kotlin.math.abs
import kotlin.math.roundToInt

private const val COCINADAS_RITMO = 3
private const val COCINADAS_RITMO_MIN = 2
private const val MINUTOS_REALES_MAX = 720
private const val DESVIO_MINIMO_MIN = 3

/**
 * Cuánto se desvía Joan del plan en esta receta: mediana de `minutosReales - previsto` de las 3 cocinadas más
 * recientes con minutos válidos (0 < m ≤ 720). El previsto es el `previstoMin` guardado en la propia cocinada (así una
 * receta que cambió de duración no falsea el ritmo) o, en las anteriores a la v1.7, el [totalMin] actual. Positivo =
 * suele tardar más. Null con menos de 2 cocinadas o si el
 * desvío es menor de 3 min (no merece la pena avisar). Solo informa: no toca la receta ni el cronómetro.
 */
fun desvioHabitualMin(
    entradas: List<EntradaHistorial>,
    slug: String,
    totalMin: Int,
): Int? {
    val desvios =
        entradas
            .filter { it.slug == slug }
            .sortedWith(compareByDescending<EntradaHistorial> { it.fecha }.thenByDescending { it.id })
            .mapNotNull { e ->
                val previsto = e.previstoMin?.takeIf { it > 0 } ?: totalMin
                e.minutosReales?.takeIf { it in 1..MINUTOS_REALES_MAX }?.let { it - previsto }
            }.take(COCINADAS_RITMO)
    if (desvios.size < COCINADAS_RITMO_MIN) return null
    val ordenados = desvios.sorted()
    val mediana =
        if (ordenados.size % 2 == 1) {
            ordenados[ordenados.size / 2].toDouble()
        } else {
            (ordenados[ordenados.size / 2 - 1] + ordenados[ordenados.size / 2]) / 2.0
        }
    val redondeada = mediana.roundToInt()
    return redondeada.takeIf { abs(mediana) >= DESVIO_MINIMO_MIN }
}
