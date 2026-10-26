package app.trombito.souschef.domain

import app.trombito.souschef.receta.Receta
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** Propuestas que enseña la tarjeta «¿Qué cocino hoy?». */
const val PROPUESTAS_MAX = 3

/** Días sin cocinar una receta para que vuelva a proponerse. */
const val DIAS_SIN_COCINAR = 14L

/** Margen (min) con el que una receta aún «llega»: la hora de empezar pudo pasar hace menos de esto. */
const val MARGEN_EMPEZAR_MIN = 5

private const val MIN_HORA = 60
private const val DIAS_MOTIVO_CENA = 21L
private const val BASES_RECIENTES = 2

/** Por qué se propone una receta; la tarjeta lo escribe en una frase corta. */
sealed interface MotivoPropuesta {
    /** Está marcada con «Quiero hacerla». */
    data object QuieresHacerla : MotivoPropuesta

    /** Nunca se ha cocinado. */
    data object SinEstrenar : MotivoPropuesta

    /** Hace [dias] que no se cocina (14 o más). */
    data class HaceTiempo(
        val dias: Int,
    ) : MotivoPropuesta

    /** Es de cena y llega a la hora de cenar. */
    data object LlegaParaCenar : MotivoPropuesta
}

/** Una receta propuesta, su motivo y los minutos del día a los que hay que empezar para servirla a su hora. */
data class Propuesta(
    val receta: Receta,
    val motivo: MotivoPropuesta,
    val empiezaMin: Int,
)

/**
 * Hasta [PROPUESTAS_MAX] recetas para cocinar hoy: la ventana número [desplazamiento] (el contador de «Otra») de
 * [candidatas]. La misma entrada da siempre las mismas propuestas: nada depende del azar, solo de [semilla].
 */
@Suppress("LongParameterList") // las seis entradas del dominio y el contador de «Otra»
fun propuestas(
    recetas: List<Receta>,
    historial: List<EntradaHistorial>,
    preferencias: Map<String, Preferencia>,
    ajustes: Ajustes,
    ahora: LocalDateTime,
    semilla: LocalDate,
    desplazamiento: Int = 0,
): List<Propuesta> {
    val todas = candidatas(recetas, historial, preferencias, ajustes, ahora, semilla)
    return ventana(todas, desplazamiento)
}

/**
 * Todas las recetas que se pueden proponer, de la más a la menos prioritaria:
 * - llegan a la hora (la de comer o la de cenar, según `horaServir`) con [MARGEN_EMPEZAR_MIN] de margen;
 * - sin cocinar en los últimos [DIAS_SIN_COCINAR] días;
 * - sin repetir la base de las 2 últimas cocinadas;
 * - sin las que se valoraron «No repetir» la última vez, ni las que no caben con el equipo.
 *
 * Primero «Quiero hacerla», luego «Nueva» y luego el resto; dentro de cada grupo el orden sale de [semilla].
 */
@Suppress("LongParameterList") // las seis entradas del dominio
fun candidatas(
    recetas: List<Receta>,
    historial: List<EntradaHistorial>,
    preferencias: Map<String, Preferencia>,
    ajustes: Ajustes,
    ahora: LocalDateTime,
    semilla: LocalDate,
): List<Propuesta> {
    val hoy = ahora.toLocalDate()
    val ahoraMin = ahora.hour * MIN_HORA + ahora.minute
    val recientes = historial.sortedWith(compareByDescending<EntradaHistorial> { it.fecha }.thenByDescending { it.id })
    val ultimaPorSlug = recientes.distinctBy { it.slug }.associateBy { it.slug }
    val basesRecientes = basesRecientes(recientes, recetas)
    return recetas
        .filter { r ->
            val ultima = ultimaPorSlug[r.slug]
            val dias = ultima?.let { diasDesde(it.fecha, hoy) }
            val sinRepetirBase = baseDe(r) !in basesRecientes
            val descansada = ultima == null || (dias != null && dias >= DIAS_SIN_COCINAR)
            val gustada = ultima?.valoracion != Valoracion.NO_REPETIR
            llegaAHora(r, ajustes, ahoraMin) &&
                descansada &&
                sinRepetirBase &&
                gustada &&
                cabeConMiEquipo(r, ajustes.equipo)
        }.map { r ->
            val ultima = ultimaPorSlug[r.slug]
            val quiere = preferencias[r.slug]?.quiereHacerla == true
            val prioridad =
                when {
                    quiere -> 0
                    esNueva(r, ultima?.fecha, hoy) -> 1
                    else -> 2
                }
            val cual = horaServir(r.tags.momento, ahoraMin, ajustes).cual
            val motivo = motivoDe(quiere, ultima, cual, hoy)
            val propuesta = Propuesta(r, motivo, empiezaMin(r, ajustes, ahoraMin))
            Triple(prioridad, propuesta, desempate(r.slug, semilla))
        }.sortedWith(compareBy({ it.first }, { it.third }, { it.second.receta.slug }))
        .map { it.second }
}

/** Minutos del día a los que hay que empezar [r] para servirla a su hora (la de comer o la de cenar). */
private fun empiezaMin(
    r: Receta,
    ajustes: Ajustes,
    ahoraMin: Int,
): Int = horaServir(r.tags.momento, ahoraMin, ajustes).minutos - ceil(r.totalEfectivo).toInt()

private fun llegaAHora(
    r: Receta,
    ajustes: Ajustes,
    ahoraMin: Int,
): Boolean = empiezaMin(r, ajustes, ahoraMin) >= ahoraMin - MARGEN_EMPEZAR_MIN

private fun motivoDe(
    quiere: Boolean,
    ultima: EntradaHistorial?,
    cual: CualHora,
    hoy: LocalDate,
): MotivoPropuesta {
    val dias = ultima?.let { diasDesde(it.fecha, hoy) }
    return when {
        quiere -> MotivoPropuesta.QuieresHacerla
        dias == null -> MotivoPropuesta.SinEstrenar
        cual == CualHora.CENAR && dias < DIAS_MOTIVO_CENA -> MotivoPropuesta.LlegaParaCenar
        else -> MotivoPropuesta.HaceTiempo(dias.toInt())
    }
}

/** La ventana [desplazamiento] de [PROPUESTAS_MAX] propuestas; con más candidatas que cabe, da la vuelta al final. */
fun ventana(
    candidatas: List<Propuesta>,
    desplazamiento: Int,
): List<Propuesta> {
    val n = candidatas.size
    if (n <= PROPUESTAS_MAX) return candidatas
    val inicio = (desplazamiento.toLong() * PROPUESTAS_MAX).mod(n.toLong()).toInt()
    return List(PROPUESTAS_MAX) { candidatas[(inicio + it) % n] }
}

private fun diasDesde(
    fecha: String,
    hoy: LocalDate,
): Long? = runCatching { ChronoUnit.DAYS.between(LocalDate.parse(fecha.take(LONGITUD_FECHA)), hoy) }.getOrNull()

private fun baseDe(receta: Receta): String? = normalizarBase(receta.tags.base)

private fun normalizarBase(base: String?): String? =
    base
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotEmpty() }

/** Las bases de las [BASES_RECIENTES] últimas cocinadas (la de la entrada o, si no la trae, la de su receta). */
private fun basesRecientes(
    recientes: List<EntradaHistorial>,
    recetas: List<Receta>,
): Set<String> {
    val porSlug = recetas.associateBy { it.slug }
    return recientes
        .take(BASES_RECIENTES)
        .mapNotNull { e -> normalizarBase(e.base ?: porSlug[e.slug]?.tags?.base) }
        .toSet()
}

private const val LONGITUD_FECHA = 10
