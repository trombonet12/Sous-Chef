package app.souschef.ui.crono

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.souschef.domain.EntradaHistorial
import app.souschef.domain.HistorialRepo
import app.souschef.domain.RecetasRepo
import app.souschef.domain.Reloj
import app.souschef.domain.TecnicasRepo
import app.souschef.domain.crono.CadenaAlarmas
import app.souschef.receta.Receta
import app.souschef.receta.crono.Aviso
import app.souschef.receta.crono.AvisosCrono
import app.souschef.receta.crono.ConsultasCrono
import app.souschef.receta.crono.EstadoCrono
import app.souschef.receta.crono.MotorCrono
import app.souschef.receta.crono.ResumenCrono
import app.souschef.receta.crono.SesionCrono
import app.souschef.receta.crono.TipoAviso
import app.souschef.ui.Rutas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import app.souschef.domain.PasoReal as PasoRealHistorial

/** Una línea de la banda "mientras no mirabas". */
data class LineaBanda(
    val carril: String,
    val texto: String,
    val haceMin: Int,
)

data class CronoUi(
    val cargando: Boolean = true,
    val receta: Receta? = null,
    val estado: EstadoCrono? = null,
    val ahoraMs: Long = 0L,
    val banda: List<LineaBanda> = emptyList(),
    val reiniciarArmado: Boolean = false,
    /** Presente cuando todos los recipientes han terminado: pantalla de cierre. */
    val resumen: ResumenCrono? = null,
    /** El cierre está hecho pero sin confirmar: ventana de [VENTANA_DESHACER_MS] para deshacer. */
    val cierrePendiente: Boolean = false,
    /** La pantalla debe cerrarse (cierre guardado). */
    val salida: Boolean = false,
    /** Técnicas de la receta que existen: slug a título (enlaces de "Ver el porqué"). */
    val tecnicas: Map<String, String> = emptyMap(),
)

private const val PERIODO_TIC_MS = 1_000L
const val VENTANA_REINICIAR_MS = 4_000L
const val VENTANA_DESHACER_MS = 8_000L
const val MAX_NOTA = 160
private const val MAX_LINEAS_BANDA = 4
private const val MS_MINUTO = 60_000L

// Una función por mando de la pantalla: la API de intenciones de la UI es ancha a propósito.
@Suppress("TooManyFunctions")
@HiltViewModel
class CronoViewModel
    @Suppress("LongParameterList")
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val recetas: RecetasRepo,
        private val tecnicas: TecnicasRepo,
        private val historial: HistorialRepo,
        private val cadena: CadenaAlarmas,
        private val reloj: Reloj,
        private val avisador: Avisador,
    ) : ViewModel() {
        private val slug: String = checkNotNull(savedStateHandle.get<String>(Rutas.ARG_SLUG))
        private val racionesRuta: Int = savedStateHandle.get<Int>(Rutas.ARG_RACIONES) ?: 0
        private val servirRuta: String = savedStateHandle.get<String>(Rutas.ARG_SERVIR) ?: Rutas.SERVIR_DEFECTO
        private val zona: ZoneId = ZoneId.systemDefault()

        private val _ui = MutableStateFlow(CronoUi())
        val ui: StateFlow<CronoUi> = _ui

        /** Cerrojos "mando:carril" contra el doble toque; se sueltan en el siguiente tic. */
        private val cerrojos = mutableSetOf<String>()
        private var jobReiniciar: Job? = null
        private var jobCierre: Job? = null
        private var cierre: (suspend () -> Unit)? = null

        init {
            viewModelScope.launch {
                cargar()
                while (isActive) {
                    delay(PERIODO_TIC_MS)
                    tic()
                }
            }
        }

        private suspend fun cargar() {
            val receta = recetas.obtener(slug)
            if (receta == null || receta.pasos.isEmpty()) {
                _ui.update { it.copy(cargando = false, receta = receta) }
                return
            }
            val ahora = reloj.ahoraMs()
            val guardado = SesionCrono.vigente(cadena.almacen.leer(slug), receta, ahora)
            val raciones = if (racionesRuta > 0) racionesRuta else receta.raciones
            val estado =
                when {
                    guardado == null -> {
                        SesionCrono.inicial(receta, servirDeHoy(servirRuta, ahora, zona), ahora, raciones)
                    }

                    // Cocinada en marcha: se reanuda tal cual, sin pisarla con la hora de la ficha.
                    guardado.inicioMs != null -> {
                        guardado
                    }

                    else -> {
                        guardado.copy(
                            raciones = raciones,
                            servirMs = servirDeHoy(servirRuta, ahora, zona),
                            actualizadoMs = ahora,
                        )
                    }
                }
            cadena.almacen.fijar(estado)
            val enlaces =
                receta.tags.tecnica
                    .distinct()
                    .mapNotNull { s -> tecnicas.obtener(s)?.let { s to it.titulo } }
                    .toMap()
            _ui.update { it.copy(cargando = false, receta = receta, tecnicas = enlaces) }
            aplicar(estado, ahora)
            cadena.reprogramar()
        }

        // --- tic ------------------------------------------------------------------------------------------------

        private suspend fun tic() {
            cerrojos.clear()
            val receta = _ui.value.receta ?: return
            val ahora = reloj.ahoraMs()
            var avisos = emptyList<Aviso>()
            val abierta = _ui.value.estado?.inicioMs != null
            val nuevo =
                if (abierta) {
                    cadena.almacen.modificar(slug) { e ->
                        val v = AvisosCrono.vencidos(e, receta, ahora)
                        avisos = v.avisos
                        v.estado
                    }
                } else {
                    null
                }
            if (nuevo == null) {
                _ui.update { it.copy(ahoraMs = ahora) }
            } else {
                darAvisos(receta, avisos, ahora)
                aplicar(nuevo, ahora)
                if (avisos.isNotEmpty()) cadena.reprogramar()
            }
        }

        private fun darAvisos(
            receta: Receta,
            avisos: List<Aviso>,
            ahora: Long,
        ) {
            val suenan = avisos.filterNot { it.silencioso }
            val sonido = suenan.minByOrNull { prioridad(it.tipo) }
            if (sonido != null) avisador.sonar(sonido.tipo)
            val perdidos = avisos.filter { it.silencioso }
            if (perdidos.isEmpty()) return
            avisador.resumen()
            val nombres = ConsultasCrono.carriles(receta).associate { it.id to it.nombre }
            val lineas =
                perdidos.map {
                    LineaBanda(nombres[it.carril] ?: it.carril, it.texto, ((ahora - it.instanteMs) / MS_MINUTO).toInt())
                }
            _ui.update { it.copy(banda = (it.banda + lineas).takeLast(MAX_LINEAS_BANDA)) }
        }

        private fun prioridad(tipo: TipoAviso): Int =
            when (tipo) {
                TipoAviso.FIN -> 0
                TipoAviso.HITO -> 1
                TipoAviso.TOCA_EMPEZAR -> 2
                TipoAviso.DIEZ_S -> 3
            }

        private fun aplicar(
            estado: EstadoCrono,
            ahora: Long,
        ) {
            val receta = _ui.value.receta
            _ui.update { it.copy(estado = estado, ahoraMs = ahora) }
            val cerrada = estado.inicioMs != null && ConsultasCrono.todoTerminado(estado)
            if (receta != null && cerrada && _ui.value.resumen == null) {
                avisador.cierre()
                val resumen = SesionCrono.resumen(estado, receta, ahora)
                _ui.update { it.copy(resumen = resumen) }
            }
        }

        // --- mandos ---------------------------------------------------------------------------------------------

        /** "Abrir la cocina": fija el inicio y arma la cadena de alarmas. */
        fun abrirCocina() = mutar { e, r, t -> SesionCrono.abrir(e, r, t) }

        fun marcar(
            idCasilla: String,
            marcada: Boolean,
        ) = mutar { e, _, t -> SesionCrono.marcar(e, idCasilla, marcada, t) }

        fun enfocar(carril: String) = mutar { e, _, t -> SesionCrono.enfocar(e, carril, t) }

        fun cambiarServir(deltaMin: Int) =
            mutar { e, _, t -> SesionCrono.servir(e, e.servirMs + deltaMin * MS_MINUTO, t) }

        fun hecho(carril: String) = mando("hecho:$carril") { e, r, t -> MotorCrono.hecho(e, r, carril, t) }

        fun masTreinta(carril: String) = mando("mas30:$carril") { e, r, t -> MotorCrono.masTreinta(e, r, carril, t) }

        fun empezarIgual(carril: String) =
            mando("igual:$carril") { e, r, t -> MotorCrono.empezarIgual(e, r, carril, t) }

        /** Play / Pausa / Seguir según el estado del carril en el momento de pulsar. */
        fun alternar(carril: String) =
            mando("play:$carril") { e, r, t ->
                val ec = e.carriles[carril]
                when {
                    ec == null -> e
                    ec.corriendo -> MotorCrono.pausar(e, r, carril, t)
                    ec.pausado -> MotorCrono.reanudar(e, r, carril, t)
                    else -> MotorCrono.empezar(e, r, carril, t)
                }
            }

        fun cerrarBanda() = _ui.update { it.copy(banda = emptyList()) }

        /** Doble toque: el primero arma 4 s ("¿seguro?"), el segundo borra el progreso y vuelve a la portada. */
        fun reiniciar() {
            val armado = _ui.value.reiniciarArmado
            jobReiniciar?.cancel()
            if (!armado) {
                _ui.update { it.copy(reiniciarArmado = true) }
                jobReiniciar =
                    viewModelScope.launch {
                        delay(VENTANA_REINICIAR_MS)
                        _ui.update { it.copy(reiniciarArmado = false) }
                    }
                return
            }
            viewModelScope.launch {
                val receta = _ui.value.receta ?: return@launch
                val ahora = reloj.ahoraMs()
                val nuevo = cadena.almacen.modificar(slug) { SesionCrono.reiniciar(it, receta, ahora) } ?: return@launch
                _ui.update { it.copy(reiniciarArmado = false, banda = emptyList(), resumen = null) }
                cadena.cerrado(slug)
                aplicar(nuevo, ahora)
                cadena.reprogramar()
            }
        }

        /** La pantalla deja de verse: la cadena de alarmas queda armada con el estado persistido. */
        fun enSegundoPlano() {
            confirmarCierre()
            viewModelScope.launch { cadena.reprogramar() }
        }

        /**
         * Cierre con 8 s para deshacer: se quitan avisos y ongoing, pero el historial y el borrado del estado solo se
         * confirman al vencer la ventana o al salir ([confirmarCierre]).
         */
        fun cerrar(nota: String) {
            val actual = _ui.value
            val receta = actual.receta
            val estado = actual.estado
            val resumen = actual.resumen
            if (actual.cierrePendiente) return
            if (receta == null || estado == null || resumen == null) return
            val momento = Instant.ofEpochMilli(resumen.servidoMs).atZone(zona)
            val sello = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss", Locale.ROOT).format(momento)
            val entrada =
                EntradaHistorial(
                    id = "$slug-$sello",
                    slug = slug,
                    titulo = receta.titulo,
                    fecha = DateTimeFormatter.ISO_LOCAL_DATE.format(momento),
                    base = receta.tags.base,
                    tecnica = receta.tags.tecnica,
                    raciones = estado.raciones,
                    minutosReales = resumen.minutosReales,
                    nota = nota.trim().take(MAX_NOTA).ifBlank { null },
                    pasosReales = resumen.pasosReales.map { PasoRealHistorial(it.id, it.t, it.segundos) },
                )
            cierre = {
                historial.agregar(entrada)
                cadena.almacen.borrar(slug)
                cadena.reprogramar()
                _ui.update { it.copy(salida = true) }
            }
            _ui.update { it.copy(cierrePendiente = true) }
            cadena.cerrado(slug)
            jobCierre =
                viewModelScope.launch {
                    delay(VENTANA_DESHACER_MS)
                    confirmarCierre()
                }
        }

        fun deshacerCierre() {
            if (!_ui.value.cierrePendiente) return
            jobCierre?.cancel()
            jobCierre = null
            cierre = null
            _ui.update { it.copy(cierrePendiente = false) }
        }

        /** Confirma ya un cierre pendiente (vence la ventana o se sale de la pantalla). */
        fun confirmarCierre() {
            val pendiente = cierre ?: return
            cierre = null
            jobCierre?.cancel()
            jobCierre = null
            viewModelScope.launch { withContext(NonCancellable) { pendiente() } }
        }

        // --- infraestructura ------------------------------------------------------------------------------------

        private fun mutar(cambio: (EstadoCrono, Receta, Long) -> EstadoCrono) {
            viewModelScope.launch { ejecutar(cambio) }
        }

        private fun mando(
            clave: String,
            cambio: (EstadoCrono, Receta, Long) -> EstadoCrono,
        ) {
            if (cerrojos.add(clave)) mutar(cambio)
        }

        private suspend fun ejecutar(cambio: (EstadoCrono, Receta, Long) -> EstadoCrono) {
            val receta = _ui.value.receta ?: return
            val ahora = reloj.ahoraMs()
            var antes: EstadoCrono? = null
            val nuevo =
                cadena.almacen.modificar(slug) {
                    antes = it
                    cambio(it, receta, ahora)
                } ?: return
            cadena.descartarObsoletos(antes, nuevo)
            aplicar(nuevo, ahora)
            cadena.reprogramar()
        }
    }
