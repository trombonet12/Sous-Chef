package app.trombito.souschef.ui.recetas

import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.ficha.Servir
import kotlin.math.ceil

/**
 * El modo «¿Qué me da tiempo?» de la lista: a qué hora se sirve y si solo se enseñan las recetas a las que se llega.
 * Vive en el filtro del ViewModel y no se guarda: al cerrar la pantalla vuelve a estar apagado.
 */
data class ModoTiempo(
    /** Hora de servir en minutos del día; arranca en la de Ajustes y se mueve de 15 en 15. */
    val servirMin: Int,
    val soloLlego: Boolean = false,
)

/** Qué le pasa a una receta si se quiere servir a una hora: [llega], cuándo empezar y, si no llega, cuándo acabaría. */
data class Llegada(
    val slug: String,
    val llega: Boolean,
    /** Minutos del día en que hay que empezar para servir a la hora elegida (`servir − total`, redondeado arriba). */
    val empiezaMin: Int,
    /** Minutos del día en que acabaría si se empezara ahora; null si llega. */
    val acabariaMin: Int?,
    /** Duración de la receta en minutos, redondeada hacia arriba. */
    val minutos: Int,
)

object QueMeDaTiempo {
    /** Si la hora de servir ya pasó, solo cuenta como «mañana» cuando faltan menos de 12 h. */
    const val HORIZONTE_MIN = 12 * 60
    private const val DIA = 24 * 60

    /**
     * Minutos que faltan desde [ahoraMin] hasta servir a [servirMin] (ambos minutos del día), o null si no hay hora
     * útil.
     *
     * La regla: si la hora de servir es la de ahora o está por delante, es la de hoy. Si ya pasó, se toma la del día
     * siguiente solo cuando faltan menos de 12 h (p. ej. son las 23:00 y se sirve a las 02:00); si faltan 12 h o más
     * (son las 20:00 y se sirve a las 15:30) se entiende que la hora de hoy ya se fue y no hay margen, así que no llega
     * ninguna receta.
     */
    fun margen(
        ahoraMin: Int,
        servirMin: Int,
    ): Int? {
        val directo = servirMin - ahoraMin
        val siguiente = directo + DIA
        return when {
            directo >= 0 -> directo
            siguiente < HORIZONTE_MIN -> siguiente
            else -> null
        }
    }

    /**
     * Para cada receta, en el mismo orden, si da tiempo de servir a [servirMin] empezando ahora ([ahoraMin], minutos
     * del día). Una receta llega si su duración (redondeada arriba, como en la ficha) cabe en [margen].
     */
    fun evaluar(
        recetas: List<Receta>,
        ahoraMin: Int,
        servirMin: Int,
    ): List<Llegada> {
        val margen = margen(ahoraMin, servirMin)
        return recetas.map { r ->
            val minutos = ceil(r.totalEfectivo).toInt()
            val llega = margen != null && minutos <= margen
            Llegada(
                slug = r.slug,
                llega = llega,
                empiezaMin = Servir.ajustar(servirMin, -minutos),
                acabariaMin = if (llega) null else Servir.ajustar(ahoraMin, minutos),
                minutos = minutos,
            )
        }
    }
}

/**
 * Pone a cada tarjeta su [Llegada], con la hora que enseña (empezar si llega, acabar si no), quita las que no llegan si
 * [ModoTiempo.soloLlego] y ordena: primero las que llegan, de la que se empieza más tarde (la más corta) a la que se
 * empieza antes, y luego las que no, en el orden que ya traían. Sin modo devuelve la lista tal cual.
 */
internal fun aplicarTiempo(
    tarjetas: List<TarjetaReceta>,
    modo: ModoTiempo?,
    ahoraMin: Int,
): List<TarjetaReceta> {
    if (modo == null) return tarjetas
    val llegadas = QueMeDaTiempo.evaluar(tarjetas.map { it.receta }, ahoraMin, modo.servirMin).associateBy { it.slug }
    val conLlegada =
        tarjetas.mapNotNull { t ->
            val l = llegadas.getValue(t.receta.slug)
            if (!l.llega && modo.soloLlego) return@mapNotNull null
            t.copy(
                llegada = l,
                hora = l.acabariaMin ?: l.empiezaMin,
                modoHora = if (l.llega) HoraTarjeta.EMPEZAR else HoraTarjeta.ACABAR,
                // La hora es la del plan: el ritmo no cuenta aquí.
                conRitmo = false,
            )
        }
    // sortedWith es estable: dentro de cada grupo se respeta el orden elegido en Ajustes.
    return conLlegada.sortedWith(
        compareBy<TarjetaReceta> { it.llegada?.llega != true }.thenBy { t ->
            t.llegada?.takeIf { it.llega }?.minutos ?: 0
        },
    )
}
