package app.souschef.ui.crono

import app.souschef.receta.Carril
import app.souschef.receta.Paso
import app.souschef.receta.TipoCarril
import app.souschef.receta.crono.ClaseEstado
import app.souschef.receta.crono.EstadoCarril

/** Acción principal de un carril; el botón grande de la UI la ejecuta. */
enum class AccionCarril { EMPEZAR, PAUSAR, SEGUIR, HECHO, BLOQUEADO, TERMINADO }

/** Hito del paso sobre la barra: [fraccion] de 0 a 1, [avisado] si ya sonó. */
data class MarcaHito(
    val fraccion: Float,
    val texto: String,
    val avisado: Boolean,
)

/** Modelo de vista puro de un carril (sin Compose): lo consumen las dos disposiciones de la cocina. */
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
    /** Potencia 0-9, solo en carril FUEGO con `f` definido. */
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
    /** Título del siguiente paso del carril (+ " · fuego N" si FUEGO), o null si es el último. */
    val despues: String?,
    val accion: AccionCarril,
    /** "Escurrir (MESA)" si [accion] es BLOQUEADO. */
    val bloqueadoPor: String?,
    val clase: ClaseEstado,
    val sugeridoMs: Long?,
    /** Instante de fin si corre; null si no. */
    val finAtMs: Long?,
    /** El paso tiene `p` o la receta enlaza técnicas. */
    val tienePorque: Boolean,
    /** Texto `p` del paso actual (el porqué que la voz puede leer); null si no tiene. */
    val porque: String? = null,
) {
    val terminado: Boolean get() = accion == AccionCarril.TERMINADO

    /** Corriendo y con el cero ya pasado, esperando Hecho. */
    val vencido: Boolean get() = accion == AccionCarril.HECHO
}

/** Una vista por carril, en el orden de los carriles de la receta. */
fun vistasCarriles(datos: DatosCrono): List<VistaCarril> =
    datos.carriles.mapIndexed { indice, carril ->
        val ec = datos.ec(carril.id)
        val paso = datos.paso(carril.id)
        if (ec == null || paso == null) {
            vistaTerminada(datos, carril, indice)
        } else {
            vistaActiva(datos, carril, indice, ec, paso)
        }
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
    val pendientes = requisitos(datos.receta, datos.estado, paso).pendientes
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
        despues = datos.cola(carril.id).getOrNull(ec.i + 1)?.let { despuesDe(it, esFuego) },
        accion = accion,
        bloqueadoPor =
            pendientes.firstOrNull()?.takeIf { accion == AccionCarril.BLOQUEADO }?.let {
                "${it.t} (${datos.nombre(it.carril)})"
            },
        clase = vista.clase,
        sugeridoMs = vista.sugeridoMs,
        finAtMs = ec.finAtMs,
        tienePorque = !paso.p.isNullOrBlank() || tecnicas.isNotEmpty(),
        porque = paso.p?.takeIf { it.isNotBlank() },
    )
}

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
): String = if (esFuego && siguiente.f != null) "${siguiente.t} · fuego ${siguiente.f}" else siguiente.t

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
