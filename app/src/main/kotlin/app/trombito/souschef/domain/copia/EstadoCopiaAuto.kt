package app.trombito.souschef.domain.copia

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Por qué la última copia automática no salió (ADR-0015). Se guarda en `meta` por su nombre. */
enum class FalloCopiaAuto {
    /** La carpeta ya no vale: permiso retirado, carpeta borrada o proveedor desinstalado. Hay que elegir otra. */
    CARPETA_NO_VALE,

    /** La carpeta vale pero la copia no se pudo escribir o releer (espacio, proveedor, archivo grande...). */
    NO_SE_PUDO,
}

/**
 * Estado de la copia automática, tal como se guarda en `meta`: la carpeta elegida (URI del árbol SAF), el instante
 * de la última copia automática releída bien, el de la última manual y el último fallo.
 *
 * La URI de la carpeta es privada (ADR-0015): [toString] no la enseña, para que no llegue a un log por error.
 */
data class EstadoCopiaAuto(
    val carpeta: String? = null,
    val ultimaAutoMs: Long? = null,
    val ultimaManualMs: Long? = null,
    val fallo: FalloCopiaAuto? = null,
) {
    /** Hay una carpeta elegida. */
    val activa: Boolean get() = carpeta != null

    /** La copia más reciente, automática o manual; null si nunca hubo ninguna. */
    val ultimaCopiaMs: Long? get() = listOfNotNull(ultimaAutoMs, ultimaManualMs).maxOrNull()

    override fun toString(): String =
        "EstadoCopiaAuto(activa=$activa, ultimaAutoMs=$ultimaAutoMs, ultimaManualMs=$ultimaManualMs, fallo=$fallo)"

    companion object {
        const val CLAVE_CARPETA = "copia.auto.carpeta"
        const val CLAVE_ULTIMA_AUTO = "copia.auto.ultima"
        const val CLAVE_ULTIMA_MANUAL = "copia.manual.ultima"
        const val CLAVE_FALLO = "copia.auto.fallo"

        val CLAVES = listOf(CLAVE_CARPETA, CLAVE_ULTIMA_AUTO, CLAVE_ULTIMA_MANUAL, CLAVE_FALLO)

        /** Lee el estado de las filas de `meta`; lo ausente, vacío o ilegible cuenta como «sin valor». */
        fun decodificar(valores: Map<String, String>): EstadoCopiaAuto =
            EstadoCopiaAuto(
                carpeta = valores[CLAVE_CARPETA]?.takeIf { it.isNotBlank() },
                ultimaAutoMs = valores[CLAVE_ULTIMA_AUTO]?.toLongOrNull()?.takeIf { it > 0 },
                ultimaManualMs = valores[CLAVE_ULTIMA_MANUAL]?.toLongOrNull()?.takeIf { it > 0 },
                fallo = FalloCopiaAuto.entries.firstOrNull { it.name == valores[CLAVE_FALLO] },
            )
    }
}

/** Dónde se guarda el [EstadoCopiaAuto] (la implementación usa la tabla `meta`). */
interface CopiaAutoRepo {
    fun observar(): Flow<EstadoCopiaAuto>

    suspend fun leer(): EstadoCopiaAuto

    /** Fija (o quita, con null) la carpeta; el fallo anterior deja de valer. */
    suspend fun fijarCarpeta(carpeta: String?)

    /** Una copia automática releída bien en [ms]: borra el fallo. */
    suspend fun registrarCopia(ms: Long)

    suspend fun registrarFallo(fallo: FalloCopiaAuto)

    /** Una copia manual guardada en [ms] (solo cuenta para «Última copia»). */
    suspend fun registrarManual(ms: Long)
}

/** Cuándo avisar al usuario de que la copia automática no funciona (ADR-0015). */
object AvisoCopiaAuto {
    /** Un fallo pasajero de escritura solo avisa si la última copia automática buena tiene más de esto. */
    const val TOLERANCIA_DIAS = 7L

    private const val MS_POR_DIA = 24 * 60 * 60 * 1000L

    /**
     * La carpeta que ya no vale avisa siempre: no se arregla sola. Un fallo al escribir puede ser pasajero (sin
     * espacio un rato, proveedor ocupado), así que solo avisa si nunca ha habido una copia automática o la última
     * buena es de hace [TOLERANCIA_DIAS] días o más.
     */
    fun debeAvisar(
        estado: EstadoCopiaAuto,
        ahoraMs: Long,
    ): Boolean =
        when {
            !estado.activa -> false
            estado.fallo == null -> false
            estado.fallo == FalloCopiaAuto.CARPETA_NO_VALE -> true
            else -> estado.ultimaAutoMs?.let { ahoraMs - it >= TOLERANCIA_DIAS * MS_POR_DIA } ?: true
        }
}

/** Para el «Última copia: hace N días» de Ajustes. */
object UltimaCopia {
    /** Días de calendario (en [zona]) entre [ultimaMs] y [ahoraMs]; null si nunca hubo copia. Nunca negativo. */
    fun dias(
        ultimaMs: Long?,
        ahoraMs: Long,
        zona: ZoneId = ZoneId.systemDefault(),
    ): Long? =
        ultimaMs?.let {
            val ultima = Instant.ofEpochMilli(it).atZone(zona).toLocalDate()
            val hoy = Instant.ofEpochMilli(ahoraMs).atZone(zona).toLocalDate()
            maxOf(0L, ChronoUnit.DAYS.between(ultima, hoy))
        }
}
