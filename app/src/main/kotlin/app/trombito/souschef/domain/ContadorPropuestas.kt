package app.trombito.souschef.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/** Cuántas veces se ha pulsado «Otra» en el día [dia]. */
data class OtraDelDia(
    val dia: LocalDate,
    val veces: Int,
)

/**
 * Los dos contadores de «¿Qué cocino hoy?», guardados en `meta` (sin analítica, solo para saber si sirve):
 * - «Otra» del día, que desplaza la ventana de propuestas y se reinicia cada día;
 * - cuántas veces se cocinó una receta que estaba entre las propuestas de ese día.
 */
interface PropuestasRepo {
    /** El contador de «Otra» guardado (con su día); null si nunca se pulsó. Quien lo lee compara el día con hoy. */
    fun observarOtra(): Flow<OtraDelDia?>

    /** Suma una pulsación de «Otra» a [hoy]; si lo guardado es de otro día, vuelve a 1. */
    suspend fun otra(hoy: LocalDate)

    /** Anota las propuestas que se enseñan en [hoy] (se suman a las que ya salieron ese día). */
    suspend fun mostrar(
        hoy: LocalDate,
        slugs: List<String>,
    )

    /** Veces que se cocinó una receta propuesta ese día, desde siempre. */
    fun observarCocinadas(): Flow<Int>
}

/** Sin contadores: lo que usan los tests que no miran las propuestas. */
object SinPropuestas : PropuestasRepo {
    override fun observarOtra(): Flow<OtraDelDia?> = flowOf(null)

    override suspend fun otra(hoy: LocalDate) = Unit

    override suspend fun mostrar(
        hoy: LocalDate,
        slugs: List<String>,
    ) = Unit

    override fun observarCocinadas(): Flow<Int> = flowOf(0)
}

/** El formato de los valores de `meta`: «yyyy-MM-dd|dato». Un valor ausente o ilegible cuenta como vacío. */
object ContadorPropuestas {
    private const val SEPARADOR = '|'
    private const val SEPARADOR_SLUGS = ","

    private fun partir(valor: String?): Pair<LocalDate, String>? {
        val i = valor?.indexOf(SEPARADOR) ?: -1
        val texto = valor?.substring(0, maxOf(i, 0))
        val dia = texto?.takeIf { i >= 0 }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return if (valor == null || dia == null) null else dia to valor.substring(i + 1)
    }

    /** El contador de «Otra» guardado en [valor], o null si no se entiende. */
    fun otraDe(valor: String?): OtraDelDia? =
        partir(valor)?.let { (dia, n) -> n.toIntOrNull()?.takeIf { it >= 0 }?.let { OtraDelDia(dia, it) } }

    /** Veces que se ha pulsado «Otra» en [hoy]: 0 si lo guardado es de otro día. */
    fun vecesHoy(
        otra: OtraDelDia?,
        hoy: LocalDate,
    ): Int = otra?.takeIf { it.dia == hoy }?.veces ?: 0

    /** El valor a guardar tras una pulsación de «Otra» en [hoy], sobre lo que había en [valor]. */
    fun siguienteOtra(
        valor: String?,
        hoy: LocalDate,
    ): String = "$hoy$SEPARADOR${vecesHoy(otraDe(valor), hoy) + 1}"

    /** Slugs propuestos en [hoy] según [valor]; vacío si lo guardado es de otro día. */
    fun mostradasDe(
        valor: String?,
        hoy: LocalDate,
    ): Set<String> =
        partir(valor)
            ?.takeIf { it.first == hoy }
            ?.second
            ?.split(SEPARADOR_SLUGS)
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

    /** El valor a guardar al enseñar [slugs] en [hoy], sumados a los que ya salieron ese día. */
    fun mostrar(
        valor: String?,
        hoy: LocalDate,
        slugs: List<String>,
    ): String = "$hoy$SEPARADOR${(mostradasDe(valor, hoy) + slugs).joinToString(SEPARADOR_SLUGS)}"

    /** Una receta [slug] cocinada en [fecha] (yyyy-MM-dd) estaba propuesta ese día según [valor] (las mostradas). */
    fun eraPropuesta(
        valor: String?,
        slug: String,
        fecha: String,
    ): Boolean {
        val dia = runCatching { LocalDate.parse(fecha.take(FECHA_LARGO)) }.getOrNull() ?: return false
        return slug in mostradasDe(valor, dia)
    }

    private const val FECHA_LARGO = 10
}
