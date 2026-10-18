package app.souschef.ui.crono.voz

import app.souschef.receta.voz.OrdenVoz
import app.souschef.ui.crono.AccionCarril
import app.souschef.ui.crono.VistaCarril
import app.souschef.ui.crono.elegirFoco

/**
 * Frases cortas que el cronómetro dice en voz alta (TTS es-ES) a partir del modelo de vista. Puro: sin Android,
 * sin Compose, sin reloj. Cada frase cabe en [MAX_FRASE] caracteres y no lleva emojis ni símbolos de pantalla.
 */
object RespuestasVoz {
    const val NO_ENTENDIDO = "No te he entendido."
    const val SIN_PORQUE = "Este paso no tiene explicación."
    const val NADA_QUE_DESHACER = "No hay nada que deshacer."

    /** Tiempo que queda de un carril, o el estado en que está si no corre. */
    fun cuantoQueda(v: VistaCarril): String {
        val nombre = nombreHablado(v.nombre)
        val frase =
            when (v.accion) {
                AccionCarril.TERMINADO -> "$nombre: terminado."
                AccionCarril.HECHO -> "$nombre: ya está, di hecho."
                AccionCarril.EMPEZAR, AccionCarril.BLOQUEADO -> "$nombre: sin empezar."
                AccionCarril.SEGUIR -> "$nombre: en pausa, quedan ${tiempo(v.quedanS)}."
                AccionCarril.PAUSAR -> "$nombre: ${verboQuedar(v.quedanS)} ${tiempo(v.quedanS)}."
            }
        return limitar(frase)
    }

    /** Lo más urgente ahora mismo (mismo criterio que el foco de la pantalla). */
    fun queToca(
        vistas: List<VistaCarril>,
        foco: String?,
    ): String {
        val id = elegirFoco(vistas, foco)
        val v = vistas.firstOrNull { it.id == id } ?: return "Todo terminado."
        val nombre = nombreHablado(v.nombre)
        val titulo = v.titulo.orEmpty()
        val frase =
            when (v.accion) {
                AccionCarril.EMPEZAR -> "Toca empezar $titulo en $nombre."
                AccionCarril.BLOQUEADO -> "$nombre: espera a ${v.bloqueadoPor ?: "otro carril"}."
                AccionCarril.HECHO -> "$nombre: $titulo. Ya está, di hecho."
                else -> "$nombre: $titulo.${detalles(v)}"
            }
        return limitar(frase)
    }

    /** El porqué (`p`) del paso actual, sin marcas; recortado por palabras a [MAX_PORQUE] caracteres. */
    fun porQue(v: VistaCarril): String {
        val texto = v.porque?.let(::sinMarcas)?.takeIf { it.isNotEmpty() } ?: return SIN_PORQUE
        return limitar(cerrar(texto), MAX_PORQUE)
    }

    /** Ficha hablada del paso: título, nota, fuego y tapa, y el tiempo (hasta [MAX_INFO] caracteres). */
    fun info(v: VistaCarril): String {
        val nombre = nombreHablado(v.nombre)
        if (v.terminado) return "$nombre: terminado."
        val cola = detalles(v) + " " + estadoHablado(v)
        val cabeza =
            listOfNotNull(
                "$nombre: ${v.titulo.orEmpty()}",
                v.nota?.let(::sinMarcas)?.takeIf { it.isNotEmpty() },
            ).joinToString(" ") { cerrar(it) }
        return limitar(cabeza, MAX_INFO - cola.length) + cola
    }

    /** Qué toca en un carril concreto (aunque otro esté en foco); si ya terminó, cómo está. */
    fun queTocaCarril(v: VistaCarril): String = if (v.terminado) cuantoQueda(v) else queToca(listOf(v), v.id)

    /** El siguiente fin entre los carriles que corren. */
    fun queViene(vistas: List<VistaCarril>): String {
        val v =
            vistas
                .filter { !it.terminado && it.finAtMs != null && it.accion != AccionCarril.SEGUIR }
                .minByOrNull { it.quedanS }
                ?: return "Nada en marcha."
        val cuando = if (v.quedanS <= 0) "Ahora" else "En ${tiempo(v.quedanS)}"
        val que =
            v.despues?.let { "sigue ${hablar(it)}" } ?: "fin de ${v.titulo.orEmpty()}"
        return limitar("$cuando, ${nombreHablado(v.nombre)}: $que.")
    }

    /** Une avisos de la alarma («SARTÉN GRANDE: texto») en una sola locución. */
    fun anuncio(lineas: List<String>): String =
        lineas
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ") { limitar(cerrar(separadorAviso.replace(normalizarNombre(it), ". "))) }

    /**
     * Confirmación de una orden ya ejecutada. [v] es la vista del carril afectado ANTES de ejecutarla (su `despues`
     * es el paso al que se pasa), o null si no hay carril.
     */
    fun confirmacion(
        orden: OrdenVoz,
        v: VistaCarril?,
    ): String =
        when (orden) {
            is OrdenVoz.Hecho -> confirmarHecho(v)
            is OrdenVoz.Pausa -> "En pausa."
            is OrdenVoz.Seguir -> "Sigo."
            is OrdenVoz.Empezar -> "Vamos."
            is OrdenVoz.Prorrogar -> "${duracionHablada(orden.segundos)} más."
            is OrdenVoz.Para -> "Silencio."
            is OrdenVoz.CuantoQueda -> v?.let(::cuantoQueda) ?: NO_ENTENDIDO
            is OrdenVoz.QueToca, is OrdenVoz.PorQue, is OrdenVoz.Info -> "Vale."
            is OrdenVoz.QueViene, is OrdenVoz.Repite, is OrdenVoz.ProximosPasos, is OrdenVoz.Deshacer -> "Vale."
        }

    /** Lo que falta del paso según la acción del carril, ya como frase con inicial mayúscula. */
    private fun estadoHablado(v: VistaCarril): String =
        when (v.accion) {
            AccionCarril.HECHO -> "Ya está, di hecho."
            AccionCarril.EMPEZAR -> "Sin empezar."
            AccionCarril.BLOQUEADO -> "Espera a ${v.bloqueadoPor ?: "otro carril"}."
            AccionCarril.SEGUIR -> "En pausa, quedan ${tiempo(v.quedanS)}."
            else -> "${verboQuedar(v.quedanS).replaceFirstChar { it.titlecase() }} ${tiempo(v.quedanS)}."
        }

    private fun confirmarHecho(v: VistaCarril?): String =
        when {
            v == null -> "Hecho."
            v.despues != null -> limitar("Hecho. Ahora, ${hablar(v.despues)}.")
            else -> limitar("Hecho. ${nombreHablado(v.nombre)} terminado.")
        }
}

private const val MAX_FRASE = 120
private const val MAX_PORQUE = 300
internal const val MAX_INFO = 200
private const val SEG_MINUTO = 60
private const val SEG_DIEZ_MINUTOS = 600
private const val MEDIO_MINUTO = 30
private const val SEPARADOR_PANTALLA = " · "
private val separadorAviso = Regex("""[.!?]?\s+·\s+""")
private val cierres = setOf('.', '!', '?')

private fun detalles(v: VistaCarril): String {
    val partes = listOfNotNull(v.fuego?.let { "fuego $it" }, textoTapa(v.tapa))
    if (partes.isEmpty()) return ""
    return " " + partes.joinToString(", ").replaceFirstChar { it.titlecase() } + "."
}

/** Mismo criterio que `recursoTapa`, pero con el texto en castellano. */
private fun textoTapa(tapa: String?): String? {
    val t = tapa?.uppercase().orEmpty()
    return when {
        t.isBlank() -> null
        "MEDIAS" in t -> "a medias"
        "SIN" in t || "FUERA" in t -> "destapada"
        "PUESTA" in t -> "tapada"
        else -> null
    }
}

private fun verboQuedar(segundos: Int): String = if (segundos == 1 || segundos == SEG_MINUTO) "queda" else "quedan"

/** Por debajo de 60 s solo segundos; hasta 10 min minutos y segundos; por encima, minutos redondeados. */
internal fun tiempo(segundos: Int): String =
    when {
        segundos < SEG_MINUTO -> unidad(segundos, "segundo")
        segundos > SEG_DIEZ_MINUTOS -> unidad((segundos + MEDIO_MINUTO) / SEG_MINUTO, "minuto")
        segundos % SEG_MINUTO == 0 -> unidad(segundos / SEG_MINUTO, "minuto")
        else -> "${unidad(segundos / SEG_MINUTO, "minuto")} y ${unidad(segundos % SEG_MINUTO, "segundo")}"
    }

private fun unidad(
    n: Int,
    nombre: String,
): String = if (n == 1) "1 $nombre" else "$n ${nombre}s"

/** «SARTÉN GRANDE» -> «Sartén grande». */
internal fun nombreHablado(nombre: String): String = nombre.trim().lowercase().replaceFirstChar { it.titlecase() }

/** El texto de pantalla con « · » se dice con comas. */
internal fun hablar(texto: String): String = texto.replace(SEPARADOR_PANTALLA, ", ")

/** Si la línea empieza por un nombre de carril en mayúsculas («MESA: ...») lo pasa a «Mesa: ...». */
private fun normalizarNombre(linea: String): String {
    val corte = linea.indexOf(':')
    if (corte <= 0) return linea
    val prefijo = linea.substring(0, corte)
    val esNombreEnMayusculas = prefijo.any { it.isLetter() } && prefijo == prefijo.uppercase()
    return if (esNombreEnMayusculas) nombreHablado(prefijo) + linea.substring(corte) else linea
}

private fun cerrar(frase: String): String = if (frase.last() in cierres) frase else "$frase."

/** Recorta por palabras hasta [max] caracteres y cierra con punto. */
internal fun limitar(
    frase: String,
    max: Int = MAX_FRASE,
): String {
    if (frase.length <= max) return frase
    val corte = frase.take(max - 1).substringBeforeLast(' ')
    return corte.trimEnd(',', '.', ':', ';', ' ') + "."
}
