package app.trombito.souschef.domain

import app.trombito.souschef.receta.Momento

/** Hora de cenar por defecto (21:00), en minutos del día. */
const val CENAR_DEFECTO_MIN = 21 * 60

/** A partir de esta hora (17:00) una receta de comida o cena se sirve a la hora de cenar. */
const val CENA_DESDE_MIN = 17 * 60

/** Cuál de las dos horas de Ajustes se usa. */
enum class CualHora { COMER, CENAR }

/** Hora de servir elegida, en minutos del día, y de cuál de las dos horas de Ajustes sale. */
data class HoraUsada(
    val minutos: Int,
    val cual: CualHora,
)

/**
 * Hora de servir que corresponde a una receta con [momento] (`tags.momento`), mirada a las [ahora] (minutos del día):
 * - solo cena (con o sin otros momentos que no sean comida): la de cenar;
 * - comida y cena: la de cenar desde las 17:00 y la de comer antes;
 * - sin momento, o comida, postre, merienda y desayuno: la de comer. El desayuno no tiene modo «sin hora» en la app.
 */
fun horaServir(
    momento: List<Momento>?,
    ahora: Int,
    servirMin: Int,
    cenarMin: Int,
): HoraUsada {
    val m = momento.orEmpty()
    val cena = Momento.CENA in m
    val cenando =
        when {
            !cena -> false
            Momento.COMIDA in m -> ahora >= CENA_DESDE_MIN
            else -> true
        }
    return if (cenando) HoraUsada(cenarMin, CualHora.CENAR) else HoraUsada(servirMin, CualHora.COMER)
}

fun horaServir(
    momento: List<Momento>?,
    ahora: Int,
    ajustes: Ajustes,
): HoraUsada = horaServir(momento, ahora, ajustes.servirMin, ajustes.cenarMin)
