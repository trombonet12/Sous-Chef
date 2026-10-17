package app.souschef.receta.voz

import app.souschef.receta.Texto

/** Un carril tal como se nombra en voz: [id] estable y [nombre] visible («SARTÉN GRANDE»). */
data class CarrilVoz(
    val id: String,
    val nombre: String,
)

/** Orden de voz ya interpretada (ADR-0008). `carril == null`: la orden afecta al carril activo o más cercano. */
sealed interface OrdenVoz {
    /** «hecho», «listo», «siguiente», «ya está». */
    data class Hecho(
        val carril: String?,
    ) : OrdenVoz

    /** «pausa», «para el tiempo», «espera». */
    data class Pausa(
        val carril: String?,
    ) : OrdenVoz

    /** «sigue», «continúa», «reanuda». */
    data class Seguir(
        val carril: String?,
    ) : OrdenVoz

    /** «empieza», «arranca», «vamos». */
    data class Empezar(
        val carril: String?,
    ) : OrdenVoz

    /** «treinta segundos más», «30 más», «un poco más». */
    data class MasTreinta(
        val carril: String?,
    ) : OrdenVoz

    /** «cuánto queda», «cuánto falta». */
    data class CuantoQueda(
        val carril: String?,
    ) : OrdenVoz

    /** «qué toca», «qué hago». */
    data object QueToca : OrdenVoz

    /** «qué viene», «y luego», «qué sigue». */
    data object QueViene : OrdenVoz

    /** «para», «calla», «silencio», «apaga la alarma». */
    data object Para : OrdenVoz

    /** «repite», «otra vez», «qué has dicho». */
    data object Repite : OrdenVoz
}

/**
 * Intérprete puro de órdenes de voz del cronómetro: sin Android, sin reloj, sin estado. Solo reconoce frases
 * dichas tras la palabra de activación «chef» (y sus errores típicos de reconocimiento).
 */
object InterpreteVoz {
    private val activadores = setOf("chef", "jef", "shef")
    private const val MIN_PALABRA_CARRIL = 4
    private val noPalabra = Regex("[^a-z0-9]+")

    /** Palabras de 4+ letras que no identifican un carril. */
    private val vacias = setOf("para", "sobre", "entre", "desde", "hasta", "como", "esta", "este")

    private class Regla(
        patron: String,
        val crear: (String?) -> OrdenVoz,
    ) {
        val regex = Regex(patron)
    }

    // El texto se compara como « palabra palabra » (con espacio a cada lado); el orden de las reglas es la prioridad.
    private val reglas =
        listOf(
            Regla(" (que viene|y luego|que sigue) ") { OrdenVoz.QueViene },
            Regla(" (que toca|que hago) ") { OrdenVoz.QueToca },
            Regla(" cuanto (tiempo )?(queda|falta) ") { OrdenVoz.CuantoQueda(it) },
            Regla(" (repite|repetir|otra vez|que has dicho|que dijiste) ") { OrdenVoz.Repite },
            Regla(" (30 (segundos )?mas|mas 30 segundos|un poco mas|medio minuto mas) ") { OrdenVoz.MasTreinta(it) },
            Regla("^ para el tiempo | (pausa|pausar|espera|detente) ") { OrdenVoz.Pausa(it) },
            Regla("^ para | (calla|callate|silencio|apaga la alarma) ") { OrdenVoz.Para },
            Regla(" (sigue|continua|continuar|reanuda|reanudar|seguir) ") { OrdenVoz.Seguir(it) },
            Regla(" (empieza|empezar|arranca|arrancar|comienza|vamos) ") { OrdenVoz.Empezar(it) },
            Regla(" (hecho|listo|lista|siguiente|ya esta|terminado) ") { OrdenVoz.Hecho(it) },
        )

    /** null si no hay «chef» o lo que sigue no es una orden. Analiza solo el texto tras la ÚLTIMA «chef». */
    fun interpretar(
        texto: String,
        carriles: List<CarrilVoz>,
    ): OrdenVoz? {
        val tokens =
            Texto
                .normalizar(texto)
                .split(noPalabra)
                .filter { it.isNotEmpty() }
                .map(::numero)
        val inicio = tokens.indexOfLast { it in activadores }
        val cola = tokens.drop(inicio + 1)
        val frase = cola.joinToString(" ", prefix = " ", postfix = " ")
        return if (inicio < 0) {
            null
        } else {
            reglas.firstOrNull { it.regex.containsMatchIn(frase) }?.crear(carrilMencionado(cola, carriles))
        }
    }

    /** Primera alternativa del reconocedor que da orden. */
    fun interpretar(
        alternativas: List<String>,
        carriles: List<CarrilVoz>,
    ): OrdenVoz? = alternativas.firstNotNullOfOrNull { interpretar(it, carriles) }

    private fun numero(token: String): String = if (token == "treinta") "30" else token

    private fun palabrasDe(nombre: String): Set<String> =
        Texto
            .normalizar(nombre)
            .split(noPalabra)
            .filter { it.length >= MIN_PALABRA_CARRIL && it !in vacias }
            .map(Texto::singular)
            .toSet()

    /** El carril con más palabras de su nombre mencionadas; null si ninguno o si hay empate. */
    private fun carrilMencionado(
        cola: List<String>,
        carriles: List<CarrilVoz>,
    ): String? {
        val dichas = cola.map(Texto::singular).toSet()
        val puntuados = carriles.map { it to palabrasDe(it.nombre).count { p -> p in dichas } }
        val mejor = puntuados.maxOfOrNull { it.second } ?: 0
        return if (mejor == 0) null else puntuados.singleOrNull { it.second == mejor }?.first?.id
    }
}
