package app.trombito.souschef.receta.crono

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder

/**
 * Un temporizador libre (ADR-0016): un tiempo que cuenta el usuario durante la cocinada y que no es de la receta.
 * Vive en [EstadoCrono.libres], fuera del `Plano`, de los fuegos, de `pasosReales` y del deshacer. Los instantes son
 * reloj de pared (epoch ms), como en [EstadoCarril].
 */
@Serializable
data class TemporizadorLibre(
    val id: String,
    /** Nombre opcional («huevos»); hoy ni la cocina ni la voz lo piden. */
    val etiqueta: String? = null,
    val minutos: Int,
    val inicioMs: Long,
    /** Ya sonó: deja de mantener viva la cocinada y de armar alarmas. */
    val avisado: Boolean = false,
) {
    /** Instante en que llega a cero. */
    val finMs: Long get() = inicioMs + minutos * MS_MINUTO

    /** Segundos que quedan (hacia arriba); 0 o negativo si ya llegó a cero. */
    fun quedanS(ahoraMs: Long): Int = Math.floorDiv(finMs - ahoraMs + MS_SEGUNDO - 1, MS_SEGUNDO).toInt()

    /** «Temporizador: 10 min» (o «Temporizador huevos: 10 min» con etiqueta): lo que se oye y se lee al vencer. */
    val texto: String
        get() {
            val nombre = listOfNotNull(TemporizadoresLibres.NOMBRE, etiqueta?.takeIf { it.isNotBlank() })
            return "${nombre.joinToString(" ")}: $minutos min"
        }
}

private const val MS_MINUTO = 60_000L
private const val MS_SEGUNDO = 1_000L

/**
 * Lee `libres` con tolerancia: si el campo no es una lista de temporizadores bien formados, se pierden los libres y
 * la cocinada se conserva (ADR-0016). Escribir es el formato normal.
 */
internal object LibresTolerantes : KSerializer<List<TemporizadorLibre>> {
    private val lista = ListSerializer(TemporizadorLibre.serializer())

    override val descriptor = lista.descriptor

    override fun serialize(
        encoder: Encoder,
        value: List<TemporizadorLibre>,
    ) = lista.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): List<TemporizadorLibre> {
        if (decoder !is JsonDecoder) return lista.deserialize(decoder)
        val elemento = decoder.decodeJsonElement()
        return runCatching { decoder.json.decodeFromJsonElement(lista, elemento) }.getOrDefault(emptyList())
    }
}

/** Funciones puras sobre los temporizadores libres de un [EstadoCrono]: sin reloj propio, `ahoraMs` por parámetro. */
object TemporizadoresLibres {
    /** Cómo se llama en avisos y notificaciones. */
    const val NOMBRE = "Temporizador"

    const val MIN_MINUTOS = 1
    const val MAX_MINUTOS = 180

    /** Como mucho este número de libres sin avisar a la vez. */
    const val MAX_A_LA_VEZ = 3

    /** Prefijo del «carril» de un aviso de libre: lo distingue de un carril de la receta en alarmas y avisos. */
    const val PREFIJO_CARRIL = "libre:"

    fun carrilDe(id: String): String = PREFIJO_CARRIL + id

    fun esCarrilLibre(carril: String): Boolean = carril.startsWith(PREFIJO_CARRIL)

    /** Los que aún no han avisado. */
    fun pendientes(e: EstadoCrono): List<TemporizadorLibre> = e.libres.filterNot { it.avisado }

    /** Solo existen en una cocinada abierta y sin caducar, y con sitio (como mucho [MAX_A_LA_VEZ] pendientes). */
    fun puedeAnadir(
        e: EstadoCrono,
        ahoraMs: Long,
    ): Boolean = e.inicioMs != null && !SesionCrono.caducado(e, ahoraMs) && pendientes(e).size < MAX_A_LA_VEZ

    /**
     * Añade un libre de [minutos] (1 a 180) que empieza ahora. Si no se puede (duración fuera de rango, sin cocina
     * abierta, caducada o ya hay [MAX_A_LA_VEZ]) devuelve [e] tal cual. Los ya avisados se retiran de la lista.
     */
    fun anadir(
        e: EstadoCrono,
        minutos: Int,
        ahoraMs: Long,
        etiqueta: String? = null,
    ): EstadoCrono {
        if (minutos !in MIN_MINUTOS..MAX_MINUTOS || !puedeAnadir(e, ahoraMs)) return e
        val vigentes = pendientes(e)
        val usados = e.libres.mapTo(HashSet()) { it.id }
        var n = ahoraMs
        while ("l$n" in usados) n++
        val nuevo = TemporizadorLibre("l$n", etiqueta?.takeIf { it.isNotBlank() }, minutos, ahoraMs)
        return e.copy(libres = vigentes + nuevo, actualizadoMs = ahoraMs)
    }

    /** Quita el libre [id] (a mano, o ya avisado); [e] tal cual si no existe. */
    fun quitar(
        e: EstadoCrono,
        id: String,
        ahoraMs: Long,
    ): EstadoCrono =
        if (e.libres.any { it.id == id }) {
            e.copy(libres = e.libres.filterNot { it.id == id }, actualizadoMs = ahoraMs)
        } else {
            e
        }

    /** Los libres que ya han llegado a cero y aún no han avisado. */
    fun vencidos(
        e: EstadoCrono,
        ahoraMs: Long,
    ): List<TemporizadorLibre> = e.libres.filter { !it.avisado && it.finMs <= ahoraMs }

    /** Marca el libre [id] como avisado; [e] tal cual si no existe o ya lo estaba. */
    fun marcarAvisado(
        e: EstadoCrono,
        id: String,
        ahoraMs: Long,
    ): EstadoCrono =
        if (e.libres.any { it.id == id && !it.avisado }) {
            e.copy(
                libres = e.libres.map { if (it.id == id) it.copy(avisado = true) else it },
                actualizadoMs = ahoraMs,
            )
        } else {
            e
        }

    /** Un evento por libre sin avisar: no cuelga de ningún carril de la receta. */
    internal fun eventos(e: EstadoCrono): List<Evento> =
        pendientes(e).map { Evento(it.finMs, TipoAviso.TEMPORIZADOR, carrilDe(it.id), "${it.minutos} min") }

    /** El aviso de un libre que ya llegó a cero; `silencioso` si lleva más de 90 s vencido. */
    internal fun aviso(
        l: TemporizadorLibre,
        ahoraMs: Long,
    ): Aviso =
        Aviso(
            tipo = TipoAviso.TEMPORIZADOR,
            carril = carrilDe(l.id),
            paso = "${l.minutos} min",
            texto = l.texto,
            instanteMs = l.finMs,
            silencioso = ahoraMs - l.finMs > AvisosCrono.SILENCIO_MS,
        )
}
