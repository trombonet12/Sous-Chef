package app.trombito.souschef.ui.crono.voz

import app.trombito.souschef.domain.voz.Escucha
import app.trombito.souschef.domain.voz.Habla
import app.trombito.souschef.ui.crono.EstadoVoz

/** Lo que la voz necesita saber de la cocina para decidir si habla y oye. */
data class SituacionVoz(
    /** La cocina está abierta y en marcha: ni portada, ni cierre, ni cierre pendiente de deshacer. */
    val cocinando: Boolean,
    /** El usuario ha silenciado la voz en esta cocinada. */
    val silenciada: Boolean,
)

/**
 * Estado de la voz de una cocinada (ADR-0008): la pantalla avisa de si se ve y de si hay permiso; el resto lo decide
 * [reevaluar] con el [planVoz]. Solo se escucha con la cocina abierta y a la vista. Sin Android: se prueba en JVM.
 *
 * [situacion] da lo que sabe la cocina en cada momento y [alCambiar] recibe el estado a mostrar en la cabecera.
 */
class SesionVoz(
    private val escucha: Escucha,
    private val habla: Habla,
    private val control: ControlVoz,
    private val situacion: () -> SituacionVoz,
    private val alCambiar: (EstadoVoz) -> Unit,
) {
    /** El ajuste de voz está activo (Ajustes). Tras cambiarlo hay que llamar a [reevaluar]. */
    var ajusteActivo = false

    private var enPantalla = false
    private var permisoMicrofono = false
    private var escuchando = false
    private var vozCaida = false
    private val hayReconocedor: Boolean by lazy { escucha.disponible }

    /** La cocina se ve: escucha si el ajuste está activo, hay reconocedor y [permiso] de micrófono. */
    fun iniciar(permiso: Boolean) {
        enPantalla = true
        permisoMicrofono = permiso
        vozCaida = false
        reevaluar()
    }

    fun parar() {
        enPantalla = false
        reevaluar()
    }

    /** El usuario cambia el silencio: la escucha caída se da por reintentable en el siguiente [reevaluar]. */
    fun olvidarCaida() {
        vozCaida = false
    }

    /** Anuncia en voz los avisos que ya han sonado como pitido. */
    fun anunciar(lineas: List<String>) = control.anunciar(lineas)

    fun liberar() = control.liberar()

    /** Solo se escucha con la cocina abierta y a la vista; la voz del cierre o de la portada no hace falta. */
    fun reevaluar() {
        val s = situacion()
        val oirPosible = hayReconocedor && !vozCaida
        val plan = planVoz(enPantalla && s.cocinando, ajusteActivo, s.silenciada, permisoMicrofono, oirPosible)
        val hablabaAntes = control.puedeHablar
        control.puedeHablar = plan.habla
        when {
            plan.oye && !escuchando -> control.escuchar(::alDetenerseLaEscucha)
            !plan.oye && escuchando -> control.parar()
            hablabaAntes && !plan.habla -> habla.callar()
        }
        escuchando = plan.oye
        alCambiar(plan.estado)
    }

    /**
     * El bucle de escucha se paró solo (sin permiso o idioma no disponible): la cabecera deja de decir escuchando.
     * Sin permiso se muestra SIN_PERMISO; si no, la voz queda apagada hasta que la pantalla vuelva a verse o se
     * reactive, para no reintentar en bucle.
     */
    private fun alDetenerseLaEscucha(porPermiso: Boolean) {
        escuchando = false
        if (porPermiso) permisoMicrofono = false else vozCaida = true
        reevaluar()
    }
}
