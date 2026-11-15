package app.trombito.souschef.ui.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.TipoCarril
import app.trombito.souschef.receta.crono.ClaseEstado
import app.trombito.souschef.receta.crono.EstadoCarril
import app.trombito.souschef.receta.crono.OrdenPaso
import app.trombito.souschef.ui.comun.nivel

/** Acción principal de un carril; el botón grande de la UI la ejecuta. */
enum class AccionCarril { EMPEZAR, PAUSAR, SEGUIR, HECHO, BLOQUEADO, TERMINADO }

/** Hito del paso sobre la barra: [fraccion] de 0 a 1, [avisado] si ya sonó. */
data class MarcaHito(
    val fraccion: Float,
    val texto: String,
    val avisado: Boolean,
)

/** Qué le toca a la tarjeta de un carril en este momento: decide qué parte de la orden del paso se enseña. */
enum class MomentoPaso {
    /** Antes de empezar (toca ya, espera su hora o espera a otro carril): METE, fuego, tapa y duración. */
    POR_EMPEZAR,

    /** Corriendo o en pausa: el reloj, el próximo aviso y la señal. */
    EN_MARCHA,

    /** El reloj pasó de cero y espera Hecho: SACA y qué viene luego. */
    VENCIDO,

    TERMINADO,
}

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
    /** Minutos que faltan para la hora prevista de empezar (negativo si ya pasó); null si terminado. */
    val faltanMin: Int? = null,
    /** Número (1-based, el de la insignia) del carril cuyo paso bloquea a este; solo si [accion] es BLOQUEADO. */
    val bloqueadoPorN: Int? = null,
    /** Número (1-based) del carril del paso con el que este va «a la vez» (`junto`); null si no tiene. */
    val juntoN: Int? = null,
    /** Potencia canónica del siguiente paso del carril (solo carril FUEGO): «Luego» no la repite si es la misma. */
    val despuesF: Int? = null,
    /** La orden del paso compuesta con sus datos (ADR-0017): METE, fuego «4 → 5», tapa, duración, SEÑAL y SACA. */
    val orden: OrdenPaso =
        OrdenPaso(
            titulo = titulo.orEmpty(),
            meto = meto,
            fuego = fuego,
            tapa = tapa,
            segundos = duracionS.takeIf { it > 0 },
            senal = nota,
            saco = saco,
        ),
) {
    /** Las piezas de la [orden] en la cocina del usuario; se calcula una vez por vista. */
    val pantalla: OrdenPaso.Pantalla by lazy { orden.pantalla(cocina) }

    val momento: MomentoPaso
        get() =
            when (accion) {
                AccionCarril.EMPEZAR, AccionCarril.BLOQUEADO -> MomentoPaso.POR_EMPEZAR
                AccionCarril.PAUSAR, AccionCarril.SEGUIR -> MomentoPaso.EN_MARCHA
                AccionCarril.HECHO -> MomentoPaso.VENCIDO
                AccionCarril.TERMINADO -> MomentoPaso.TERMINADO
            }

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
    val cola = datos.cola(carril.id)
    val siguiente = cola.getOrNull(ec.i + 1)
    return VistaCarril(
        id = carril.id,
        nombre = carril.nombre,
        colorHex = carril.color,
        indiceCarril = indice,
        paso = ec.i + 1,
        pasos = cola.size,
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
        despues = siguiente?.let { despuesDe(it, esFuego, datos.cocina) },
        despuesS = siguiente?.segundos,
        despuesF = siguiente?.f?.takeIf { esFuego },
        orden = ordenDe(cola, ec.i, esFuego),
        accion = accion,
        bloqueadoPor =
            pendientes.firstOrNull()?.takeIf { accion == AccionCarril.BLOQUEADO }?.let {
                "${it.t} (${datos.nombre(it.carril)})"
            },
        clase = vista.clase,
        sugeridoMs = vista.sugeridoMs,
        finAtMs = ec.finAtMs,
        tienePorque =
            !paso.p.isNullOrBlank() ||
                datos.receta.tags.tecnica
                    .isNotEmpty(),
        porque = paso.p?.takeIf { it.isNotBlank() },
        proximos = cola.drop(ec.i + 1).take(MAX_PROXIMOS).map { PasoProximo(it.t, it.segundos) },
        faltanMin = vista.sugeridoMs?.let { Math.round((it - datos.ahoraMs) / MS_POR_MIN.toDouble()).toInt() },
        bloqueadoPorN =
            pendientes
                .firstOrNull()
                ?.takeIf { accion == AccionCarril.BLOQUEADO }
                ?.let { datos.numeroCarril(it.carril) },
        juntoN = paso.junto?.let { datos.recetaUi.paso(it) }?.let { datos.numeroCarril(it.carril) },
    )
}

/** La orden del paso [indice] de la cola; en un carril que no es de fuego no lleva potencia (ni «4 → 5»). */
private fun ordenDe(
    cola: List<Paso>,
    indice: Int,
    esFuego: Boolean,
): OrdenPaso {
    val orden = OrdenPaso.delCarril(cola, indice)
    return if (esFuego) orden else orden.copy(fuego = null, fuegoAnterior = null)
}

/** Número del carril (1-based, el de la insignia y el de la voz); null si no existe. */
private fun DatosCrono.numeroCarril(id: String): Int? =
    carriles.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.plus(1)

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
 * Carril en foco (el de las órdenes por voz y la notificación). Prioridad: 1) HECHO vencido con el fin más antiguo
 * (gana incluso a un [fijado]); 2) [fijado] si sigue vivo, salvo que esté en ESPERA o BLOQ y haya un carril que toca
 * ya; 3) el que toca ya (YA) de hora más temprana; 4) en marcha o en pausa (el que acaba antes); 5) el de hora más
 * temprana. Nunca un carril que espera pasa por delante de uno que toca ya.
 */
fun elegirFoco(
    vistas: List<VistaCarril>,
    fijado: String?,
): String? {
    val vivas = vistas.filter { !it.terminado }
    val fijadoVivo = vivas.firstOrNull { it.id == fijado }
    val hayYa = vivas.any { it.clase == ClaseEstado.YA }
    return vivas
        .filter { it.accion == AccionCarril.HECHO }
        .minByOrNull { it.finAtMs ?: Long.MAX_VALUE }
        ?.id
        ?: fijadoVivo?.takeUnless { hayYa && it.esperando }?.id
        ?: masUrgente(vivas)
}

/** Sin vencido ni fijado: el que toca ya, si no el que acaba antes en marcha o pausa, si no el de hora más temprana. */
private fun masUrgente(vivas: List<VistaCarril>): String? =
    vivas.filter { it.clase == ClaseEstado.YA }.minByOrNull { it.sugeridoMs ?: Long.MAX_VALUE }?.id
        ?: vivas
            .filter { it.accion == AccionCarril.PAUSAR || it.accion == AccionCarril.SEGUIR }
            .minByOrNull { it.finAtMs ?: Long.MAX_VALUE }
            ?.id
        ?: vivas.minByOrNull { it.sugeridoMs ?: Long.MAX_VALUE }?.id

private val VistaCarril.esperando: Boolean get() = clase == ClaseEstado.ESPERA || clase == ClaseEstado.BLOQ
