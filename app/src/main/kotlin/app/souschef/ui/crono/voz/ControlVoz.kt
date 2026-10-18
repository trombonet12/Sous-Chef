package app.souschef.ui.crono.voz

import app.souschef.domain.voz.Escucha
import app.souschef.domain.voz.Habla
import app.souschef.receta.Texto
import app.souschef.receta.voz.CarrilVoz
import app.souschef.receta.voz.InterpreteVoz
import app.souschef.receta.voz.OrdenVoz
import app.souschef.ui.crono.AccionCarril
import app.souschef.ui.crono.EstadoVoz
import app.souschef.ui.crono.VistaCarril
import app.souschef.ui.crono.elegirFoco

/** Los mandos del cronómetro que la voz puede accionar; los pone el ViewModel. */
class MandosVoz(
    val hecho: (String) -> Unit,
    /** Pausa o sigue según el estado del carril (igual que el botón de la pantalla). */
    val alternar: (String) -> Unit,
    /** Suma segundos al carril (id, segundos). */
    val prorrogar: (String, Int) -> Unit,
    val empezarIgual: (String) -> Unit,
    /** Lo mismo que pulsar Parar en la alarma. */
    val silenciarAlarma: () -> Unit,
)

/**
 * Control por voz del cronómetro (ADR-0008): interpreta lo que oye la [Escucha], acciona los [mandos] y contesta con
 * el [Habla]. Sin Android: se prueba en JVM con falsos. No guarda ni registra audio ni transcripciones; al [registro]
 * solo llega el tipo de orden. Tiene una función por tipo de orden y de respuesta (es un despachador).
 */
@Suppress("TooManyFunctions")
class ControlVoz(
    private val escucha: Escucha,
    private val habla: Habla,
    private val vistas: () -> List<VistaCarril>,
    private val foco: () -> String?,
    private val mandos: MandosVoz,
    private val registro: (String) -> Unit = {},
) {
    /** Si es `false` no se dice nada (voz apagada, silenciada o pantalla fuera de vista). */
    var puedeHablar: Boolean = false

    private var ultimaFrase: String? = null

    fun escuchar(alDetenerse: (porPermiso: Boolean) -> Unit = {}) = escucha.empezar(::alOir, alDetenerse)

    /** Deja de oír y calla lo que suene. */
    fun parar() {
        escucha.parar()
        habla.callar()
    }

    fun liberar() {
        escucha.parar()
        habla.liberar()
    }

    /** Anuncia en voz los avisos que ya han sonado como pitido («SARTÉN GRANDE: texto»). */
    fun anunciar(lineas: List<String>) {
        val frase = RespuestasVoz.anuncio(lineas)
        if (frase.isNotEmpty()) decir(frase)
    }

    private fun alOir(alternativas: List<String>) {
        val actuales = vistas()
        val carriles = actuales.map { CarrilVoz(it.id, it.nombre, it.indiceCarril + 1) }
        val orden = InterpreteVoz.interpretar(alternativas, carriles)
        when {
            orden != null -> {
                registro(orden::class.simpleName.orEmpty())
                responder(orden, actuales)
            }

            alternativas.any(::nombraAlChef) -> {
                registro("NoEntendido")
                decir(RespuestasVoz.NO_ENTENDIDO)
            }
        }
    }

    private fun responder(
        orden: OrdenVoz,
        actuales: List<VistaCarril>,
    ) {
        val frase =
            when (orden) {
                is OrdenVoz.Hecho -> {
                    sobreCarril(orden, orden.carril, actuales) { it.accion in HECHABLES }
                }

                is OrdenVoz.Pausa -> {
                    sobreCarril(orden, orden.carril, actuales) { it.accion == AccionCarril.PAUSAR }
                }

                is OrdenVoz.Seguir -> {
                    sobreCarril(orden, orden.carril, actuales) { it.accion == AccionCarril.SEGUIR }
                }

                is OrdenVoz.Empezar -> {
                    sobreCarril(orden, orden.carril, actuales) { it.accion in EMPEZABLES }
                }

                is OrdenVoz.Prorrogar -> {
                    sobreCarril(orden, orden.carril, actuales) { it.accion in PRORROGABLES }
                }

                is OrdenVoz.CuantoQueda, is OrdenVoz.QueToca, is OrdenVoz.PorQue, is OrdenVoz.Info,
                is OrdenVoz.QueViene, is OrdenVoz.ProximosPasos,
                -> {
                    consulta(orden, actuales)
                }

                is OrdenVoz.Para -> {
                    mandos.silenciarAlarma()
                    RespuestasVoz.confirmacion(orden, null)
                }

                is OrdenVoz.Repite -> {
                    ultimaFrase
                }
            }
        if (frase != null) decir(frase)
    }

    /** Preguntas: no tocan nada, solo responden. */
    private fun consulta(
        orden: OrdenVoz,
        actuales: List<VistaCarril>,
    ): String? =
        when (orden) {
            is OrdenVoz.CuantoQueda -> {
                deCarril(orden.carril, actuales, RespuestasVoz::cuantoQueda)
            }

            is OrdenVoz.QueToca -> {
                queTocaDe(orden.carril, actuales)
            }

            is OrdenVoz.PorQue -> {
                deCarril(orden.carril, actuales, RespuestasVoz::porQue)
            }

            is OrdenVoz.Info -> {
                deCarril(orden.carril, actuales, RespuestasVoz::info)
            }

            is OrdenVoz.QueViene -> {
                RespuestasVoz.queViene(actuales)
            }

            is OrdenVoz.ProximosPasos -> {
                actuales.firstOrNull { it.id == orden.carril }?.let(ProximosPasosVoz::deCarril)
                    ?: ProximosPasosVoz.general(actuales)
            }

            else -> {
                null
            }
        }

    /**
     * Ejecuta [orden] sobre el carril nombrado o, si no hay, el que está en foco. Si el carril no admite la orden en su
     * estado actual no se toca nada y se dice cómo está.
     */
    private fun sobreCarril(
        orden: OrdenVoz,
        carril: String?,
        actuales: List<VistaCarril>,
        admite: (VistaCarril) -> Boolean,
    ): String {
        val v = elegir(carril, actuales)
        return when {
            v == null -> {
                RespuestasVoz.queToca(actuales, null)
            }

            !admite(v) -> {
                RespuestasVoz.cuantoQueda(v)
            }

            else -> {
                accionar(orden, v)
                RespuestasVoz.confirmacion(orden, v)
            }
        }
    }

    private fun accionar(
        orden: OrdenVoz,
        v: VistaCarril,
    ) {
        when (orden) {
            is OrdenVoz.Hecho -> {
                mandos.hecho(v.id)
            }

            is OrdenVoz.Prorrogar -> {
                mandos.prorrogar(v.id, orden.segundos)
            }

            is OrdenVoz.Empezar -> {
                if (v.accion == AccionCarril.BLOQUEADO) mandos.empezarIgual(v.id) else mandos.alternar(v.id)
            }

            else -> {
                mandos.alternar(v.id)
            }
        }
    }

    /** Qué toca en el carril nombrado; sin carril (o inexistente), lo más urgente según el foco. */
    private fun queTocaDe(
        carril: String?,
        actuales: List<VistaCarril>,
    ): String =
        actuales.firstOrNull { it.id == carril }?.let(RespuestasVoz::queTocaCarril)
            ?: RespuestasVoz.queToca(actuales, foco())

    /** Respuesta sobre el carril nombrado o el del foco; sin ninguno, lo que toca ahora. */
    private fun deCarril(
        carril: String?,
        actuales: List<VistaCarril>,
        respuesta: (VistaCarril) -> String,
    ): String = elegir(carril, actuales)?.let(respuesta) ?: RespuestasVoz.queToca(actuales, null)

    private fun elegir(
        carril: String?,
        actuales: List<VistaCarril>,
    ): VistaCarril? {
        val id = carril?.takeIf { c -> actuales.any { it.id == c } } ?: elegirFoco(actuales, foco())
        return actuales.firstOrNull { it.id == id }
    }

    private fun decir(frase: String) {
        if (!puedeHablar) return
        ultimaFrase = frase
        escucha.pausar()
        habla.decir(frase) { escucha.reanudar() }
    }

    private companion object {
        fun nombraAlChef(texto: String): Boolean =
            Texto.normalizar(texto).split(NO_PALABRA).any { it in InterpreteVoz.ACTIVADORES }

        val NO_PALABRA = Regex("[^a-z0-9]+")
        val HECHABLES = setOf(AccionCarril.PAUSAR, AccionCarril.HECHO, AccionCarril.SEGUIR, AccionCarril.EMPEZAR)
        val EMPEZABLES = setOf(AccionCarril.EMPEZAR, AccionCarril.BLOQUEADO)
        val PRORROGABLES = setOf(AccionCarril.PAUSAR, AccionCarril.HECHO, AccionCarril.SEGUIR)
    }
}

/** Qué debe hacer la voz en un momento dado: hablar, oír y qué mostrar en la cabecera. */
data class PlanVoz(
    val habla: Boolean,
    val oye: Boolean,
    val estado: EstadoVoz,
)

/**
 * Decide el plan de voz. Solo se habla con la cocina abierta y a la vista ([aLaVista]), con el ajuste activo y sin
 * silenciar; solo se oye además con permiso de micrófono y reconocedor. Sin permiso (pero con reconocedor) se avisa
 * en la cabecera.
 */
fun planVoz(
    aLaVista: Boolean,
    ajusteActivo: Boolean,
    silenciada: Boolean,
    permiso: Boolean,
    hayReconocedor: Boolean,
): PlanVoz {
    val habla = aLaVista && ajusteActivo && !silenciada
    val oye = habla && permiso && hayReconocedor
    val estado =
        when {
            oye -> EstadoVoz.ESCUCHANDO
            habla && hayReconocedor -> EstadoVoz.SIN_PERMISO
            else -> EstadoVoz.APAGADA
        }
    return PlanVoz(habla, oye, estado)
}
