package app.trombito.souschef.ui.crono.voz

import app.trombito.souschef.receta.crono.OrdenPaso
import app.trombito.souschef.receta.voz.OrdenVoz
import app.trombito.souschef.ui.crono.AccionCarril
import app.trombito.souschef.ui.crono.VistaCarril
import app.trombito.souschef.ui.crono.elegirFoco

/**
 * Frases cortas que el cronómetro dice en voz alta (TTS es-ES) a partir del modelo de vista. Puro: sin Android,
 * sin Compose, sin reloj. Las frases cortas caben en [MAX_FRASE] caracteres y no llevan emojis ni símbolos de
 * pantalla; la orden compuesta del paso ([OrdenPaso], ADR-0017) que dicen «qué toca» e «info» no se corta.
 */
object RespuestasVoz {
    const val NO_ENTENDIDO = "No te he entendido."
    const val SIN_PORQUE = "Este paso no tiene explicación."
    const val NADA_QUE_DESHACER = "No hay nada que deshacer."
    const val SIN_TEMPORIZADOR = "Ya hay tres temporizadores en marcha."

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
        val orden = ordenDe(v)
        return when (v.accion) {
            AccionCarril.EMPEZAR -> {
                unir("Toca empezar $titulo en $nombre.", orden.detalleHablado(v.cocina, ::duracionHablada))
            }

            AccionCarril.BLOQUEADO -> {
                limitar("$nombre: espera a ${v.bloqueadoPor ?: "otro carril"}.")
            }

            AccionCarril.HECHO -> {
                unir(
                    "${orden.tituloHablado(nombre, v.cocina) ?: "$nombre."} Ya está, di hecho.",
                    orden.sacaHablado(v.cocina),
                )
            }

            else -> {
                orden.voz(nombre, v.cocina, ::duracionHablada, conDuracion = false)
            }
        }
    }

    /**
     * El porqué (`p`) del paso actual: su primer párrafo (hasta la primera línea en blanco), sin marcas; recortado por
     * palabras a [MAX_PORQUE] caracteres. El resto de `p` (listas, más párrafos) es para la ficha.
     */
    fun porQue(v: VistaCarril): String {
        val texto =
            v.porque
                ?.let { parrafoNuevo.split(it.trim(), 2).first() }
                ?.let(::sinMarcas)
                ?.takeIf { it.isNotEmpty() }
                ?: return SIN_PORQUE
        return limitar(cerrar(texto), MAX_PORQUE)
    }

    /**
     * Ficha hablada del paso, sin cortarse: la orden compuesta (título, lo que se mete, fuego y tapa, y la duración si
     * aún no corre), la señal, cómo está el carril y, si ya venció, lo que se saca.
     */
    fun info(v: VistaCarril): String {
        val nombre = nombreHablado(v.nombre)
        if (v.terminado) return "$nombre: terminado."
        val orden = ordenDe(v)
        val enMarcha = v.accion in setOf(AccionCarril.PAUSAR, AccionCarril.SEGUIR, AccionCarril.HECHO)
        return listOfNotNull(
            orden.voz(nombre, v.cocina, ::duracionHablada, conDuracion = !enMarcha),
            orden.senalHablada(v.cocina),
            estadoHablado(v),
            orden.sacaHablado(v.cocina).takeIf { v.accion == AccionCarril.HECHO },
        ).joinToString(" ")
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
            .joinToString(" ") { limitar(cerrar(separadorAviso.replace(normalizarNombre(it), ". ")), MAX_AVISO) }

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
            is OrdenVoz.Temporizador -> "Temporizador de ${minutosHablados(orden.minutos)} en marcha."
            is OrdenVoz.Para -> "Silencio."
            is OrdenVoz.CuantoQueda -> v?.let(::cuantoQueda) ?: NO_ENTENDIDO
            is OrdenVoz.QueToca, is OrdenVoz.PorQue, is OrdenVoz.Info -> "Vale."
            is OrdenVoz.Cantidad, is OrdenVoz.IngredientesPaso -> "Vale."
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

/** Un carril puede avisar de lo que acaba (y lo que saca) y de lo que toca empezar (y lo que mete) en una línea. */
private const val MAX_AVISO = 300
internal const val MAX_INFO = 200
private const val SEG_MINUTO = 60
private const val SEG_DIEZ_MINUTOS = 600
private const val MEDIO_MINUTO = 30
private const val SEPARADOR_PANTALLA = " · "
private val separadorAviso = Regex("""[.!?]?\s+·\s+""")
private val cierres = setOf('.', '!', '?')
private val parrafoNuevo = Regex("""\R\s*\R""")

/** La orden compuesta del paso que enseña la vista (la voz no sabe la potencia del paso anterior: sin «4 → 5»). */
private fun ordenDe(v: VistaCarril): OrdenPaso =
    OrdenPaso(
        titulo = v.titulo.orEmpty(),
        meto = v.meto,
        fuego = v.fuego,
        tapa = v.tapa,
        segundos = v.duracionS,
        senal = v.nota,
        saco = v.saco,
    )

private fun unir(
    frase: String,
    resto: String?,
): String = if (resto == null) frase else "$frase $resto"

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
