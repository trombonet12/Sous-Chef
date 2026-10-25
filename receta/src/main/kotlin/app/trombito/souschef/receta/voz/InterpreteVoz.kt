package app.trombito.souschef.receta.voz

import app.trombito.souschef.receta.Texto

/** Un carril tal como se nombra en voz: [id] estable, [nombre] visible («SARTÉN GRANDE») y [numero] 1-based. */
data class CarrilVoz(
    val id: String,
    val nombre: String,
    val numero: Int,
)

/** Orden de voz ya interpretada (ADR-0008). `carril == null`: la orden afecta al carril activo o más cercano. */
sealed interface OrdenVoz {
    /** «hecho», «listo», «siguiente», «ya está», «pasa», «avanza». */
    data class Hecho(
        val carril: String?,
    ) : OrdenVoz

    /** «pausa», «para el tiempo», «espera», «para el 1». */
    data class Pausa(
        val carril: String?,
    ) : OrdenVoz

    /** «sigue», «continúa», «reanuda», «dale», «quita la pausa». */
    data class Seguir(
        val carril: String?,
    ) : OrdenVoz

    /** «empieza», «arranca», «vamos». */
    data class Empezar(
        val carril: String?,
    ) : OrdenVoz

    /** «30 segundos más», «dos minutos más», «medio minuto», «un poco más» (tope 1800 s). */
    data class Prorrogar(
        val segundos: Int,
        val carril: String?,
    ) : OrdenVoz

    /** «cuánto queda», «cuánto falta». */
    data class CuantoQueda(
        val carril: String?,
    ) : OrdenVoz

    /** «por qué», «explica». */
    data class PorQue(
        val carril: String?,
    ) : OrdenVoz

    /** «info», «detalle», «cómo va». */
    data class Info(
        val carril: String?,
    ) : OrdenVoz

    /** «qué toca», «qué hago», «qué tengo que hacer». */
    data class QueToca(
        val carril: String?,
    ) : OrdenVoz

    /** «qué viene», «y luego», «qué sigue». */
    data object QueViene : OrdenVoz

    /** «próximos pasos», «siguientes pasos», «qué viene después» (+ carril: los pasos que le quedan a ese carril). */
    data class ProximosPasos(
        val carril: String?,
    ) : OrdenVoz

    /** «cuánto de sal», «cuánta harina», «cantidad de aceite»: la cantidad escalada del ingrediente [consulta]. */
    data class Cantidad(
        val consulta: String,
    ) : OrdenVoz

    /** «ingredientes de este paso», «qué lleva este paso» (+ carril): lo que nombra el paso actual del carril. */
    data class IngredientesPaso(
        val carril: String?,
    ) : OrdenVoz

    /** «para», «calla», «silencio», «apaga la alarma». */
    data object Para : OrdenVoz

    /** «repite», «otra vez», «qué has dicho». */
    data object Repite : OrdenVoz

    /** «deshaz», «atrás», «vuelve al paso anterior», «me he equivocado»: deshace el último Hecho. */
    data class Deshacer(
        val carril: String?,
    ) : OrdenVoz
}

/** Carril nombrado en la orden, si la orden lo lleva. */
val OrdenVoz.carril: String?
    get() =
        when (this) {
            is OrdenVoz.Hecho -> carril
            is OrdenVoz.Pausa -> carril
            is OrdenVoz.Seguir -> carril
            is OrdenVoz.Empezar -> carril
            is OrdenVoz.Prorrogar -> carril
            is OrdenVoz.CuantoQueda -> carril
            is OrdenVoz.PorQue -> carril
            is OrdenVoz.Info -> carril
            is OrdenVoz.QueToca -> carril
            is OrdenVoz.ProximosPasos -> carril
            is OrdenVoz.Deshacer -> carril
            is OrdenVoz.IngredientesPaso -> carril
            is OrdenVoz.Cantidad -> null
            OrdenVoz.QueViene, OrdenVoz.Para, OrdenVoz.Repite -> null
        }

/**
 * Intérprete puro de órdenes de voz del cronómetro: sin Android, sin reloj, sin estado. Solo reconoce frases
 * dichas tras la palabra de activación «chef» (y sus errores típicos de reconocimiento).
 */
object InterpreteVoz {
    /** Palabras de activación (con los errores típicos del reconocedor). */
    val ACTIVADORES: Set<String> = setOf("chef", "jef", "shef", "jefe", "chefs", "sheff")

    private const val MIN_PALABRA_CARRIL = 4
    private const val SEG_MINUTO = 60
    private const val SEG_MEDIO_MINUTO = 30
    private const val SEG_MINUTO_Y_MEDIO = 90
    private const val TOPE_S = 1800
    private const val MIN_N_MAS = 10
    private const val DECENA = 10
    private const val PRIMERA_DECENA = 3
    private const val UNO = 1
    private const val NUEVE = 9
    private val noPalabra = Regex("[^a-z0-9]+")
    private val digitos = Regex("[0-9]+")

    /** Palabras de 4+ letras que no identifican un carril. */
    private val vacias = setOf("para", "sobre", "entre", "desde", "hasta", "como", "esta", "este")

    // El índice es el valor.
    private val unidades =
        (
            "cero uno dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince dieciseis " +
                "diecisiete dieciocho diecinueve veinte veintiuno veintidos veintitres veinticuatro veinticinco " +
                "veintiseis veintisiete veintiocho veintinueve"
        ).split(' ')
    private val decenas = listOf("treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa")
    private val alias = mapOf("un" to 1, "una" to 1, "veintiun" to 21, "veintiuna" to 21)

    private val palabrasReferencia = setOf("del", "de", "en", "al", "el", "la", "carril", "numero", "fuego")

    // «dos» tras «de/del» al final de la frase suele llegar del reconocedor como «los».
    private val antesDeLos = setOf("del", "de")
    private const val DOS = 2
    private val palabrasTiempo = setOf("segundo", "segundos", "seg", "segs", "minuto", "minutos", "min", "mins")

    private class Ctx(
        val frase: String,
        val carril: String?,
        val cantidad: Int?,
        val conNumero: Boolean,
    )

    private class Regla(
        patron: String,
        val crear: (Ctx) -> OrdenVoz?,
    ) {
        val regex = Regex(patron)
    }

    // El texto se compara como « palabra palabra » (con espacio a cada lado); el orden de las reglas es la prioridad.
    private val reglas =
        listOf(
            Regla(" (por que|porque|para que|explica|explicame) ") { OrdenVoz.PorQue(it.carril) },
            Regla(
                " (proximos pasos|siguientes pasos|proximo paso|que viene despues|que hay despues|que pasos quedan) ",
            ) { OrdenVoz.ProximosPasos(it.carril) },
            Regla(" (que viene|y luego|que sigue) ") { OrdenVoz.QueViene },
            Regla(
                " (que toca|que hago|que tengo que hacer|que hay que hacer|que me toca|y ahora que|que debo hacer" +
                    "|ahora que) ",
            ) { OrdenVoz.QueToca(it.carril) },
            Regla(" cuanto (tiempo )?(queda|falta) ") { OrdenVoz.CuantoQueda(it.carril) },
            // Antes que «cuánto de X»: «ingredientes de este paso» y «qué lleva…» nunca piden una cantidad.
            Regla(" (ingredientes?|que lleva|que llevan) ") { OrdenVoz.IngredientesPaso(it.carril) },
            Regla(" (cuanto|cuanta|cuantos|cuantas|cantidad) ") { c ->
                consulta(c.frase)?.let { OrdenVoz.Cantidad(it) }
            },
            Regla(" (info|informacion|detalle|detalles|como va) ") { OrdenVoz.Info(it.carril) },
            // Detrás de las preguntas: «por qué el paso anterior…» pregunta, no deshace.
            Regla(
                " (deshaz|deshazlo|deshacer|atras|paso anterior|me he equivocado|me equivoque|no era eso) ",
            ) { OrdenVoz.Deshacer(it.carril) },
            Regla(" (repite|repetir|otra vez|que has dicho|que dijiste) ") { OrdenVoz.Repite },
            Regla(" ") { c -> c.cantidad?.let { OrdenVoz.Prorrogar(it, c.carril) } },
            Regla(" quita (la )?pausa ") { OrdenVoz.Seguir(it.carril) },
            Regla("^ para el tiempo | (pausa|pausar|espera|detente) ") { OrdenVoz.Pausa(it.carril) },
            Regla("^ para ") { c -> if (c.conNumero) OrdenVoz.Pausa(c.carril) else null },
            Regla("^ para | (calla|callate|silencio|apaga la alarma) ") { c ->
                if (c.frase.startsWith(" para ") && c.carril != null) OrdenVoz.Pausa(c.carril) else OrdenVoz.Para
            },
            Regla(" (sigue|continua|continuar|reanuda|reanudar|seguir|dale) ") { OrdenVoz.Seguir(it.carril) },
            Regla(" (empieza|empezar|arranca|arrancar|comienza|vamos) ") { OrdenVoz.Empezar(it.carril) },
            Regla(
                " (hecho|hecha|listo|lista|siguiente|ya esta|terminado|acabado|termine|pasa|avanza) ",
            ) { OrdenVoz.Hecho(it.carril) },
        )

    private val medioMinuto = Regex(" medio minuto ")
    private val minutosYMedio = Regex(" ([0-9]+) minutos? y medio ")
    private val minutoYMedio = Regex(" minuto y medio ")
    private val enSegundos = Regex(" ([0-9]+) (?:segundos?|segs?) ")
    private val enMinutos = Regex(" ([0-9]+) (?:minutos?|mins?) ")
    private val nMas = Regex(" ([0-9]+) mas ")
    private val unPoco = Regex(" 1 poco ")

    // «cuánta sal lleva», «cantidad de aceite de oliva»: lo que sigue al cuantificador, sin la cola de cortesía.
    private val cantidadDe =
        Regex(
            " (?:cuanto|cuanta|cuantos|cuantas|cantidad) (.+?)" +
                "(?: (?:lleva|llevan|echo|pongo|necesito|tiene|se echa|va|hay que echar|hay que poner|por favor))? $",
        )
    private val articulos = setOf("de", "del", "la", "el", "los", "las", "un", "una", "unos", "unas")

    // «cuánto tiempo lleva», «cuánto lleva»: preguntas por el tiempo, no por un ingrediente.
    private val noIngrediente = setOf("tiempo", "rato", "falta", "queda", "lleva", "llevan", "paso")

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
        val inicio = tokens.indexOfLast { it in ACTIVADORES }
        if (inicio < 0) return null
        val cola = numeros(tokens.drop(inicio + 1))
        val frase = cola.joinToString(" ", prefix = " ", postfix = " ")
        val numero = numeroMencionado(cola, carriles.size)
        val ctx = Ctx(frase, carrilMencionado(cola, numero, carriles), cantidad(frase), numero != null)
        return reglas.firstNotNullOfOrNull { if (it.regex.containsMatchIn(frase)) it.crear(ctx) else null }
    }

    /**
     * Primera alternativa del reconocedor que da orden; si otra da la MISMA orden y además nombra carril, gana esa
     * (el reconocedor a veces pierde el número en la primera). Nunca se cambia de orden por nombrar carril.
     */
    fun interpretar(
        alternativas: List<String>,
        carriles: List<CarrilVoz>,
    ): OrdenVoz? {
        val ordenes = alternativas.mapNotNull { interpretar(it, carriles) }
        val primera = ordenes.firstOrNull() ?: return null
        return ordenes.firstOrNull { it::class == primera::class && it.carril != null } ?: primera
    }

    /** Ingrediente por el que se pregunta («cuánto de la sal» -> «sal»), sin artículos ni cifras; null si no hay. */
    private fun consulta(frase: String): String? {
        val palabras =
            cantidadDe
                .find(frase)
                ?.groupValues
                ?.get(1)
                ?.split(' ')
                ?.filter { it.isNotEmpty() && !digitos.matches(it) }
                ?.dropWhile { it in articulos }
                .orEmpty()
        return palabras.takeIf { it.isNotEmpty() && it.first() !in noIngrediente }?.joinToString(" ")
    }

    /** Segundos pedidos en la frase (1..[TOPE_S]) o null. */
    private fun cantidad(frase: String): Int? {
        val s: Int? =
            if (medioMinuto.containsMatchIn(frase)) {
                SEG_MEDIO_MINUTO
            } else if (minutoYMedio.containsMatchIn(frase)) {
                SEG_MINUTO_Y_MEDIO
            } else {
                minutosYMedio.find(frase)?.let { it.valor() * SEG_MINUTO + SEG_MEDIO_MINUTO }
                    ?: enSegundos.find(frase)?.valor()
                    ?: enMinutos.find(frase)?.let { it.valor() * SEG_MINUTO }
                    ?: nMas.find(frase)?.valor()?.takeIf { it >= MIN_N_MAS }
                    ?: SEG_MEDIO_MINUTO.takeIf { unPoco.containsMatchIn(frase) }
            }
        return s?.takeIf { it > 0 }?.coerceAtMost(TOPE_S)
    }

    private fun MatchResult.valor(): Int = groupValues[1].toIntOrNull()?.coerceAtMost(TOPE_S) ?: TOPE_S

    /** Pasa las palabras numéricas (0-99) a cifras: «treinta y cinco» -> «35», «un» -> «1». */
    private fun numeros(tokens: List<String>): List<String> {
        val salida = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val decena = decenas.indexOf(t).takeIf { it >= 0 }?.let { (it + PRIMERA_DECENA) * DECENA }
            val suelta = unidad(tokens.getOrNull(i + 2))?.takeIf { it in UNO..NUEVE }
            if (decena != null && tokens.getOrNull(i + 1) == "y" && suelta != null) {
                salida += (decena + suelta).toString()
                i += 3
            } else {
                salida += (decena ?: unidad(t))?.toString() ?: t
                i++
            }
        }
        return salida
    }

    private fun unidad(token: String?): Int? =
        when {
            token == null -> null
            token in alias -> alias[token]
            else -> unidades.indexOf(token).takeIf { it >= 0 }
        }

    /**
     * Número de carril dicho como referencia («del 2», «de 2», «en el 2», «carril 3») si no va seguido de una unidad de
     * tiempo; si no, un número suelto al final de la frase («info 2»), que nunca es una prórroga (esa lleva unidad o
     * «más» detrás). «de los» al final cuenta como «de dos».
     */
    private fun numeroMencionado(
        cola: List<String>,
        totalCarriles: Int,
    ): Int? {
        for (i in 0 until cola.size - 1) {
            val n = cola[i + 1]
            if (cola[i] in palabrasReferencia && digitos.matches(n) && cola.getOrNull(i + 2) !in palabrasTiempo) {
                return n.toIntOrNull() ?: 0
            }
        }
        val ultimo = cola.lastOrNull()
        return when {
            cola.size >= 2 && ultimo == "los" && cola[cola.size - 2] in antesDeLos -> {
                DOS
            }

            // Suelto al final solo si es un carril que existe: «para 30» no es «para el carril 30».
            cola.size >= 2 && ultimo != null && digitos.matches(ultimo) -> {
                ultimo.toIntOrNull()?.takeIf { it in 1..totalCarriles }
            }

            else -> {
                null
            }
        }
    }

    private fun palabrasDe(nombre: String): Set<String> =
        Texto
            .normalizar(nombre)
            .split(noPalabra)
            .filter { it.length >= MIN_PALABRA_CARRIL && it !in vacias }
            .map(Texto::singular)
            .toSet()

    /** Por número si se dijo uno (inexistente: null); si no, por palabras del nombre; null si hay empate. */
    private fun carrilMencionado(
        cola: List<String>,
        numero: Int?,
        carriles: List<CarrilVoz>,
    ): String? {
        if (numero != null) return carriles.firstOrNull { it.numero == numero }?.id
        val dichas = cola.map(Texto::singular).toSet()
        val puntuados = carriles.map { it to palabrasDe(it.nombre).count { p -> p in dichas } }
        val mejor = puntuados.maxOfOrNull { it.second } ?: 0
        return if (mejor == 0) null else puntuados.singleOrNull { it.second == mejor }?.first?.id
    }
}
