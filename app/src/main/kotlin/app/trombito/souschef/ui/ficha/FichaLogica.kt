package app.trombito.souschef.ui.ficha

import app.trombito.souschef.domain.NotaCocinada
import app.trombito.souschef.domain.ResumenReceta
import app.trombito.souschef.domain.SERVIR_DEFECTO_MIN
import app.trombito.souschef.domain.crono.ChipAviso
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Equipo
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tecnica
import java.util.Locale
import kotlin.math.ceil

/** Hora de servir en minutos desde medianoche; la de partida es la de Ajustes (`Ajustes.servirMin`). */
object Servir {
    const val DEFECTO = SERVIR_DEFECTO_MIN
    const val PASO = 15
    private const val DIA = 24 * 60

    fun ajustar(
        minutos: Int,
        delta: Int,
    ): Int = (minutos + delta).mod(DIA)

    /** Hora de empezar: servir − total de la receta, redondeado hacia arriba para no ir justo. */
    fun inicio(
        servirMin: Int,
        receta: Receta,
    ): Int = ajustar(servirMin, -ceil(receta.totalEfectivo).toInt())

    /** La hora de empezar que enseña la ficha: la del plan, adelantada [ritmoMin] min si Joan suele tardar más. */
    fun inicioConRitmo(
        inicio: Int,
        ritmoMin: Int?,
    ): Int = ajustar(inicio, -(ritmoMin ?: 0))

    fun formato(minutos: Int): String = String.format(Locale.ROOT, "%02d:%02d", minutos / 60, minutos % 60)
}

data class TecnicaEnlace(
    val slug: String,
    val titulo: String,
)

data class FichaUiState(
    val cargando: Boolean = true,
    /** Receta ya traducida a [cocina] (`Receta.conCocina`): para pintar e imprimir, no para el motor. */
    val receta: Receta? = null,
    val cocina: Cocina = Cocina.VITRO_9,
    val raciones: Int = 2,
    /** Raciones que admite la receta (su `escala`, o 1..20); el stepper solo recorre estas. */
    val racionesPermitidas: List<Int> = (Escalado.RACIONES_MIN..Escalado.RACIONES_MAX).toList(),
    /** El defecto global de Ajustes no cabía en el rango de la receta y se ajustó a la más cercana. */
    val ajustadoAlRango: Boolean = false,
    val servirMin: Int = Servir.DEFECTO,
    /** [servirMin] es la hora de cenar de Ajustes (receta de cena) y nadie la ha movido en esta ficha. */
    val paraCenar: Boolean = false,
    val favorita: Boolean = false,
    /** «Quiero hacerla»: la receta está marcada para cocinarla pronto. */
    val quiereHacerla: Boolean = false,
    val tecnicas: List<TecnicaEnlace> = emptyList(),
    /** Carriles aún en marcha si esta receta tiene una cocinada viva; null si no la tiene. */
    val cocinando: Int? = null,
    /** Notas «para la próxima vez» de cocinadas anteriores de esta receta, la más reciente primero. */
    val notas: List<NotaCocinada> = emptyList(),
    /** Minutos que Joan suele desviarse del plan en esta receta (+ tarda más); null si no hay ritmo claro. */
    val ritmoMin: Int? = null,
    /** Aparatos que tiene el usuario (Ajustes → Tu equipo); la ficha avisa de los que la receta pide y no tiene. */
    val equipo: Set<Equipo> = Equipo.entries.toSet(),
    /** Lo que el historial cuenta de esta receta (veces, última vez, ritmo y valoración); null si nunca se cocinó. */
    val historial: ResumenReceta? = null,
    /** El chip «Avísame a las HH:mm» (o «Aviso a las HH:mm · Quitar»); null si no se ofrece. */
    val aviso: ChipAviso? = null,
) {
    /** Las raciones no son las de la receta: los gramos de pasos y preparación siguen siendo los de la base. */
    val fueraDeBase: Boolean get() = receta != null && raciones != receta.raciones
}

/** Enlaces solo a las técnicas de `tags.tecnica` que existen. */
fun enlacesTecnicas(
    receta: Receta,
    existentes: List<Tecnica>,
): List<TecnicaEnlace> =
    receta.tags.tecnica.mapNotNull { slug ->
        existentes.firstOrNull { it.slug == slug }?.let { TecnicaEnlace(it.slug, it.titulo) }
    }

/** Hito en segundos -> "+m:ss". */
fun formatoHito(segundos: Double): String {
    val total = Math.round(segundos).toInt()
    return String.format(Locale.ROOT, "+%d:%02d", total / 60, total % 60)
}
