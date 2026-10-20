package app.trombito.souschef.ui.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.TipoCarril
import app.trombito.souschef.receta.crono.ClaseEstado
import app.trombito.souschef.receta.crono.EstadoCarril
import app.trombito.souschef.ui.comun.nivel

/** Acción principal de un carril; el botón grande de la UI la ejecuta. */
enum class AccionCarril { EMPEZAR, PAUSAR, SEGUIR, HECHO, BLOQUEADO, TERMINADO }

/** Hito del paso sobre la barra: [fraccion] de 0 a 1, [avisado] si ya sonó. */
data class MarcaHito(
    val fraccion: Float,
    val texto: String,
    val avisado: Boolean,
)

/** Un paso que viene después en el carril: título y duración en segundos. */
data class PasoProximo(
    val titulo: String,
    val segundos: Int,
)

/** Pasos siguientes que se guardan en la vista (la voz dice como mucho estos). */
private const val MAX_PROXIMOS = 3

/** Modelo de vista puro de un carril (sin Compose): lo consumen las disposiciones de la cocina y la voz. */
data class VistaCarril(
    val id: String,
    val nombre: String,
    /** Color crudo del carril; la UI lo pasa por `colorDeCarril(hex, indice)`. */
    val colorHex: String,
    val indiceCarril: Int,
    /** 1-based; 0 si terminado. */
    val paso: Int,
    val pasos: Int,
    /** Título del paso actual; null = carril terminado. */
    val titulo: String?,
    /** Potencia canónica 0-9, solo en carril FUEGO con `f` definido; se pinta con [cocina] (`Cocina.nivel`). */
    val fuego: Int?,
    /** Campo `tapa` crudo; la UI usa [recursoTapa]. */
    val tapa: String?,
    /** Negativo = pasado de tiempo. */
    val quedanS: Int,
    val duracionS: Int,
    val progreso: Float,
    val hitos: List<MarcaHito>,
    val proximoHito: MarcaHito?,
    val meto: String?,
    val saco: String?,
    val nota: String?,
    /** Título del siguiente paso del carril (+ " · fuego X" si FUEGO, X en la [cocina]), o null si es el último. */
    val despues: String?,
    /** Duración en segundos del siguiente paso (solo pantalla; la voz no la dice), o null si es el último. */
    val despuesS: Int? = null,
    val accion: AccionCarril,
    /** "Escurrir (MESA)" si [accion] es BLOQUEADO. */
    val bloqueadoPor: String?,
    val clase: ClaseEstado,
    /** Hora prevista de empezar el paso actual (`servir − total + off`); null si terminado. */
    val sugeridoMs: Long?,
    /** Minutos que el paso acabará tarde (+) o pronto (−) respecto al plan; 0 si va en hora. */
    val desvioMin: Int = 0,
    /** Instante de fin si corre; null si no. */
    val finAtMs: Long?,
    /** El paso tiene `p` o la receta enlaza técnicas. */
    val tienePorque: Boolean,
    /** Texto `p` del paso actual (el porqué que la voz puede leer); null si no tiene. */
    val porque: String? = null,
    /** Hasta [MAX_PROXIMOS] pasos siguientes del carril, para «próximos pasos». */
    val proximos: List<PasoProximo> = emptyList(),
    /** Título del paso al que vuelve «deshacer» (el del último Hecho del carril); null si no hay nada que deshacer. */
    val deshacerA: String? = null,
    /** Este carril tiene el último Hecho de la cocina (el que deshace «deshaz» sin nombrar carril). */
    val ultimoHecho: Boolean = false,
    /** Cocina del usuario: la voz dice el [fuego] con ella (ADR-0014). */
    val cocina: Cocina = Cocina.VITRO_9,
) {
    val puedeDeshacer: Boolean get() = deshacerA != null

    val terminado: Boolean get() = accion == AccionCarril.TERMINADO

    /** Corriendo y con el cero ya pasado, esperando Hecho. */
    val vencido: Boolean get() = accion == AccionCarril.HECHO
}

/** Una vista por carril, en el orden de los carriles de la receta. */
fun vistasCarriles(datos: DatosCrono): List<VistaCarril> =
    datos.carriles.mapIndexed { indice, carril ->
        val ec = datos.ec(carril.id)
        val paso = datos.paso(carril.id)
        val vista =
            if (ec == null || paso == null) {
                vistaTerminada(datos, carril, indice)
            } else {
                vistaActiva(datos, carril, indice, ec, paso)
            }
        vista.copy(
            deshacerA = datos.estado.deshacer[carril.id]?.let { datos.cola(carril.id).getOrNull(it.i)?.t },
            ultimoHecho = datos.estado.ultimoHecho == carril.id,
            cocina = datos.cocina,
        )
    }

private fun vistaTerminada(
    datos: DatosCrono,
    carril: Carril,
    indice: Int,
) = VistaCarril(
    id = carril.id,
    nombre = carril.nombre,
    colorHex = carril.color,
    indiceCarril = indice,
    paso = 0,
    pasos = datos.cola(carril.id).size,
    titulo = null,
    fuego = null,
    tapa = null,
    quedanS = 0,
    duracionS = 0,
    progreso = 1f,
    hitos = emptyList(),
    proximoHito = null,
    meto = null,
    saco = null,
    nota = null,
    despues = null,
    accion = AccionCarril.TERMINADO,
    bloqueadoPor = null,
    clase = datos.vista(carril.id).clase,
    sugeridoMs = null,
    finAtMs = null,
    tienePorque = false,
    porque = null,
)

private fun vistaActiva(
    datos: DatosCrono,
    carril: Carril,
    indice: Int,
    ec: EstadoCarril,
    paso: Paso,
): VistaCarril {
    val quedan = datos.quedan(carril.id)
    val pendientes = requisitos(datos.recetaUi, datos.estado, paso).pendientes
    val accion = accionDe(ec, quedan, pendientes.isNotEmpty())
    val hitos = hitosDe(paso, ec)
    val vista = datos.vista(carril.id)
    val esFuego = carril.tipoCarril == TipoCarril.FUEGO
    val tecnicas = datos.receta.tags.tecnica
    return VistaCarril(
        id = carril.id,
        nombre = carril.nombre,
        colorHex = carril.color,
        indiceCarril = indice,
        paso = ec.i + 1,
        pasos = datos.cola(carril.id).size,
        titulo = paso.t,
        fuego = if (esFuego) paso.f else null,
        tapa = paso.tapa,
        quedanS = quedan,
        duracionS = paso.segundos,
        progreso = progreso(paso.segundos, quedan),
        hitos = hitos,
        proximoHito = hitos.firstOrNull { !it.avisado },
        meto = paso.meto,
        saco = paso.saco,
        nota = paso.nota,
        despues = datos.cola(carril.id).getOrNull(ec.i + 1)?.let { despuesDe(it, esFuego, datos.cocina) },
        despuesS = datos.cola(carril.id).getOrNull(ec.i + 1)?.segundos,
        accion = accion,
        bloqueadoPor =
            pendientes.firstOrNull()?.takeIf { accion == AccionCarril.BLOQUEADO }?.let {
                "${it.t} (${datos.nombre(it.carril)})"
            },
        clase = vista.clase,
        sugeridoMs = vista.sugeridoMs,
        desvioMin =
            vista.sugeridoMs?.let {
                desvioMin(it, paso.segundos, finPrevistoMs(it, quedan, ec.pausado, ec.finAtMs, datos.ahoraMs))
            } ?: 0,
        finAtMs = ec.finAtMs,
        tienePorque = !paso.p.isNullOrBlank() || tecnicas.isNotEmpty(),
        porque = paso.p?.takeIf { it.isNotBlank() },
        proximos =
            datos
                .cola(carril.id)
                .drop(ec.i + 1)
                .take(MAX_PROXIMOS)
                .map { PasoProximo(it.t, it.segundos) },
    )
}

/**
 * Fin previsto del paso actual. Sin empezar, cuenta desde ahora o desde el sugerido si aún no ha llegado (esperar no
 * es ir pronto); en pausa, desde ahora con lo que queda; corriendo, el fin real (o ahora, si ya pasó del cero).
 */
internal fun finPrevistoMs(
    sugeridoMs: Long,
    quedanS: Int,
    pausado: Boolean,
    finAtMs: Long?,
    ahoraMs: Long,
): Long =
    when {
        finAtMs != null -> maxOf(finAtMs, ahoraMs)
        pausado -> ahoraMs + quedanS * MS_POR_S
        else -> maxOf(ahoraMs, sugeridoMs) + quedanS * MS_POR_S
    }

/** Desvío frente al plan, en minutos redondeados: fin previsto − (sugerido + duración). Prórrogas y pausas suman. */
internal fun desvioMin(
    sugeridoMs: Long,
    duracionS: Int,
    finPrevistoMs: Long,
): Int = Math.round((finPrevistoMs - (sugeridoMs + duracionS * MS_POR_S)) / MS_POR_MIN.toDouble()).toInt()

private const val MS_POR_S = 1000L
private const val MS_POR_MIN = 60_000L

private fun accionDe(
    ec: EstadoCarril,
    quedan: Int,
    hayPendientes: Boolean,
): AccionCarril =
    when {
        ec.corriendo && quedan > 0 -> AccionCarril.PAUSAR
        ec.corriendo -> AccionCarril.HECHO
        ec.pausado -> AccionCarril.SEGUIR
        hayPendientes && !ec.salto -> AccionCarril.BLOQUEADO
        else -> AccionCarril.EMPEZAR
    }

private fun hitosDe(
    paso: Paso,
    ec: EstadoCarril,
): List<MarcaHito> =
    paso.hitosValidos.mapIndexed { i, h ->
        MarcaHito((h.en / paso.segundos).toFloat(), h.t, i in ec.hitosAvisados)
    }

private fun despuesDe(
    siguiente: Paso,
    esFuego: Boolean,
    cocina: Cocina,
): String = siguiente.f?.takeIf { esFuego }?.let { "${siguiente.t} · fuego ${cocina.nivel(it)}" } ?: siguiente.t

/**
 * Carril en foco. Prioridad: 1) HECHO vencido con el fin más antiguo (gana incluso a un [fijado] de otro carril);
 * 2) [fijado] si sigue vivo; 3) corriendo con menor `finAtMs`; 4) clase YA/ATENDER; 5) el primero no terminado.
 */
fun elegirFoco(
    vistas: List<VistaCarril>,
    fijado: String?,
): String? {
    val vivas = vistas.filter { !it.terminado }
    return vivas
        .filter { it.accion == AccionCarril.HECHO }
        .minByOrNull { it.finAtMs ?: Long.MAX_VALUE }
        ?.id
        ?: vivas.firstOrNull { it.id == fijado }?.id
        ?: vivas
            .filter { it.finAtMs != null }
            .minByOrNull { it.finAtMs ?: Long.MAX_VALUE }
            ?.id
        ?: vivas.firstOrNull { it.clase == ClaseEstado.YA || it.clase == ClaseEstado.ATENDER }?.id
        ?: vivas.firstOrNull()?.id
}
