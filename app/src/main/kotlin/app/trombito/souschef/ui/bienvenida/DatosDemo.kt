package app.trombito.souschef.ui.bienvenida

import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Gantt
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.conCocina
import app.trombito.souschef.receta.crono.ConsultasCrono
import app.trombito.souschef.receta.crono.MotorCrono
import app.trombito.souschef.receta.crono.SesionCrono
import app.trombito.souschef.receta.voz.CarrilVoz
import app.trombito.souschef.ui.crono.DatosCrono
import app.trombito.souschef.ui.recetas.ContextoLista
import app.trombito.souschef.ui.recetas.EquipoReceta
import app.trombito.souschef.ui.recetas.Hace
import app.trombito.souschef.ui.recetas.RasgosReceta
import app.trombito.souschef.ui.recetas.TarjetaReceta
import java.time.LocalDate
import kotlin.math.ceil

/** Receta de la que salen la cocina de muestra y la primera tarjeta (varios fuegos, mesa y horno). */
internal const val SLUG_COCINA = "pollo-almendras"

/** Receta de rango 1-2 raciones: enseña cómo se ajusta la lista de ingredientes. */
internal const val SLUG_RACIONES = "tacos-de-carne"

/** Instante fijo de la cocina de muestra: la pantalla de la intro no cambia entre un vistazo y otro. */
internal const val AHORA_DEMO_MS = 1_800_000_000_000L

private const val MS_MINUTO = 60_000L
private const val SEGUNDOS_TRANSCURRIDOS = 90_000L
private const val DIAS_ULTIMA_VEZ = 3L

/** Recetas reales del repositorio con las que se hacen los ejemplos; `null` si aún no han llegado o no existen. */
data class DatosDemo(
    val cocina: Receta? = null,
    val raciones: Receta? = null,
) {
    /** Las recetas disponibles, para las tarjetas de ejemplo. */
    val tarjetas: List<Receta> get() = listOfNotNull(cocina, raciones)

    /** Los carriles de la receta de la cocina, tal como los nombra la voz. */
    val carrilesVoz: List<CarrilVoz>
        get() =
            cocina
                ?.let(ConsultasCrono::carriles)
                .orEmpty()
                .mapIndexed { i, c -> CarrilVoz(c.id, c.nombre, i + 1) }
}

/**
 * La cocina de muestra: la receta con la cocina abierta y los carriles que pueden arrancar ya en marcha, 90 s después,
 * en un instante fijo. [receta] es la canónica (la del motor) y los textos salen traducidos a [cocina].
 */
internal fun cocinaDemo(
    receta: Receta,
    cocina: Cocina,
): DatosCrono {
    val servirMs = AHORA_DEMO_MS + ceil(receta.totalEfectivo).toLong() * MS_MINUTO
    var estado = SesionCrono.abrir(SesionCrono.inicial(receta, servirMs, AHORA_DEMO_MS), receta, AHORA_DEMO_MS)
    ConsultasCrono.carriles(receta).forEach {
        estado = MotorCrono.empezar(estado, receta, it.id, AHORA_DEMO_MS)
    }
    return DatosCrono(receta, estado, AHORA_DEMO_MS + SEGUNDOS_TRANSCURRIDOS, cocina, receta.conCocina(cocina))
}

/**
 * Tarjeta de lista de [receta] tal como la pintaría la lista con los [ajustes] de ahora: la hora según el modo, y
 * «pide X fuegos» u «horno» si la receta pide más equipo del que hay. [ahoraMin]: hora actual en minutos del día.
 */
internal fun tarjetaDemo(
    receta: Receta,
    ajustes: Ajustes,
    ahoraMin: Int,
    hoy: LocalDate = LocalDate.now(),
): TarjetaReceta {
    val contexto = ContextoLista(hoy, ahoraMin, ajustes.servirMin, ajustes.horaTarjeta, ajustes.fuegos, ajustes.horno)
    val ultima = hoy.minusDays(DIAS_ULTIMA_VEZ).toString()
    val fuegos = Gantt.fuegos(receta)
    val hace = Hace.de(ultima, hoy)
    val conHorno = EquipoReceta.CON_HORNO in RasgosReceta.de(receta, false, fuegos, hace).equipo
    return TarjetaReceta(
        receta = receta,
        favorita = false,
        fuegos = fuegos,
        ultimaFecha = ultima,
        hace = hace,
        conHorno = conHorno,
        hora = contexto.hora(receta),
        modoHora = ajustes.horaTarjeta,
        pideFuegos = fuegos.takeIf { it > ajustes.fuegos } ?: 0,
        pideHorno = conHorno && !ajustes.horno,
    )
}
