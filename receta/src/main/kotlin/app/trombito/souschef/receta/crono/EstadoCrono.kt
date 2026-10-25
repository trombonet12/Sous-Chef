package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.DesvioPaso
import kotlinx.serialization.Serializable

/**
 * Todo lo persistible del cronómetro (docs/spec/app-legado.md §4.2 y §4.14). Los instantes son reloj de pared
 * (epoch ms) para sobrevivir a reinicios del proceso. Un solo documento por cocinada; sin reloj propio.
 */
@Serializable
data class EstadoCrono(
    val slug: String,
    val raciones: Int,
    val servirMs: Long,
    val carriles: Map<String, EstadoCarril>,
    val foco: String? = null,
    /** Checklist de la portada: ids estables por contenido ([MotorCrono.idCasilla]). */
    val marcados: Set<String> = emptySet(),
    /** Id de paso a segundos reales; se persiste (bug viejo corregido). */
    val pasosReales: Map<String, Int> = emptyMap(),
    /** "Abrir la cocina"; null mientras se está en la portada. */
    val inicioMs: Long? = null,
    val actualizadoMs: Long,
    /** Estado de cada carril justo antes de su último Hecho, para deshacerlo (un nivel por carril). */
    val deshacer: Map<String, EstadoCarril> = emptyMap(),
    /** Carril del último Hecho: el que deshace «deshaz» sin nombrar carril. */
    val ultimoHecho: String? = null,
    /**
     * Temporizadores libres (ADR-0016): fuera del plano, de los fuegos, de `pasosReales` y del deshacer. Se leen con
     * tolerancia: si el campo viene mal formado se pierden los libres, no la cocinada.
     */
    @Serializable(with = LibresTolerantes::class)
    val libres: List<TemporizadorLibre> = emptyList(),
)

/** Estado del paso actual de un carril; al pasar al siguiente paso se reinicia. */
@Serializable
data class EstadoCarril(
    val i: Int = 0,
    val fin: Boolean = false,
    /** Corriendo: instante en que llega a cero. */
    val finAtMs: Long? = null,
    /** Parado o en pausa: segundos que quedan; null = el paso entero. */
    val restanteS: Int? = null,
    val pausado: Boolean = false,
    /** Obsoleto: el aviso de 10 s ya no existe; se conserva para leer estados guardados antes. */
    val a10: Boolean = false,
    val aFin: Boolean = false,
    val avisado: Boolean = false,
    /** Empezado a pesar de dependencias pendientes. */
    val salto: Boolean = false,
    /** Índices (en `hitosValidos`) ya avisados. */
    val hitosAvisados: Set<Int> = emptySet(),
    val inicioPasoMs: Long? = null,
) {
    val corriendo: Boolean get() = finAtMs != null
}

enum class TipoAviso {
    HITO,

    @Deprecated("Ya no hay aviso de 10 s: el motor no lo genera. Se conserva para no romper código previo.")
    DIEZ_S,
    FIN,
    TOCA_EMPEZAR,

    /** Un temporizador libre ([TemporizadorLibre]) llegó a cero. */
    TEMPORIZADOR,
}

/** Un aviso que toca dar. `silencioso`: lleva más de 90 s vencido, solo informa (banda "mientras no mirabas"). */
data class Aviso(
    val tipo: TipoAviso,
    val carril: String,
    val paso: String,
    val texto: String,
    val instanteMs: Long,
    val silencioso: Boolean = false,
)

/** Próximo evento para la cadena de alarmas (ADR-0005). */
data class Evento(
    val ms: Long,
    val tipo: TipoAviso,
    val carril: String,
    val paso: String,
)

data class Vencidos(
    val avisos: List<Aviso>,
    val estado: EstadoCrono,
)

enum class ClaseEstado(
    val texto: String,
    val orden: Int,
    val rango: Int,
) {
    FIN("terminado", 9, 9),
    ATENDER("¡listo! pulsa Hecho", 0, 0),
    PAUSA("en pausa", 5, 3),
    MARCHA("en marcha", 4, 2),
    BLOQ("espera", 6, 5),
    ESPERA("a la hora", 3, 4),
    YA("empieza ya", 1, 1),
}

/** Resultado de `estado(c)`: la UI compone "a las HH:MM", "espera a <paso (CARRIL)>"... con estos datos. */
data class VistaEstado(
    val clase: ClaseEstado,
    val sugeridoMs: Long?,
    val bloqueadoPor: List<Pair<String, String>> = emptyList(),
)

enum class EtiquetaParalelo { VENCIDO, PAUSA, AHORA, EN_MIN }

data class Paralelo(
    val carril: String,
    val paso: String,
    val minutos: Int,
    val etiqueta: EtiquetaParalelo,
)

data class PlanCrono(
    val retrasoMin: Int,
    val difFinMin: Int,
    val tarde: Boolean,
    val llegadaMs: Long,
    /** Carril con el cero pasado esperando Hecho. */
    val vencido: String?,
    val aguantan: List<String>,
    val noEsperan: List<String>,
) {
    /** Minutos de retraso que se anuncian ("N min por detrás"). */
    val minutosPorDetras: Int get() = maxOf(retrasoMin, difFinMin)
}

data class PasoReal(
    val id: String,
    val t: String,
    val segundos: Int,
)

data class ResumenCarril(
    val carril: String,
    val pasos: Int,
    val minutos: Int,
    val deFuego: Boolean,
)

data class ResumenCrono(
    val duracionRealS: Long?,
    /** Para el historial: null si no cumple 0 < m <= 720. */
    val minutosReales: Int?,
    val servidoMs: Long,
    val previstoMs: Long,
    val pasosReales: List<PasoReal>,
    val desvios: List<DesvioPaso>,
    val carriles: List<ResumenCarril>,
)
