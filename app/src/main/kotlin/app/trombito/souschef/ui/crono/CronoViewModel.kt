package app.trombito.souschef.ui.crono

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.NotaCocinada
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.TecnicasRepo
import app.trombito.souschef.domain.crono.Avisador
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.domain.entradaDe
import app.trombito.souschef.domain.notasDe
import app.trombito.souschef.domain.voz.Escucha
import app.trombito.souschef.domain.voz.Habla
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Potencia
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.conCocina
import app.trombito.souschef.receta.crono.Aviso
import app.trombito.souschef.receta.crono.AvisosCrono
import app.trombito.souschef.receta.crono.ConsultasCrono
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.MotorCrono
import app.trombito.souschef.receta.crono.ResumenCrono
import app.trombito.souschef.receta.crono.SesionCrono
import app.trombito.souschef.receta.crono.TipoAviso
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.crono.voz.ControlVoz
import app.trombito.souschef.ui.crono.voz.MandosVoz
import app.trombito.souschef.ui.crono.voz.SesionVoz
import app.trombito.souschef.ui.crono.voz.SituacionVoz
import app.trombito.souschef.ui.ficha.Servir
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import javax.inject.Inject

/** Estado del control por voz para la cabecera (ADR-0008). */
enum class EstadoVoz { APAGADA, ESCUCHANDO, SIN_PERMISO }

/** Una línea de la banda "mientras no mirabas". */
data class LineaBanda(
    val carril: String,
    val texto: String,
    val haceMin: Int,
)

data class CronoUi(
    val cargando: Boolean = true,
    /** Receta canónica (vitro 0-9): con ella trabajan el motor, las alarmas, el estado guardado y las casillas. */
    val receta: Receta? = null,
    /** La misma receta con la potencia de los textos traducida a la [cocina]: solo para pintarla (ADR-0014). */
    val recetaUi: Receta? = null,
    /** Cocina del usuario (Ajustes). */
    val cocina: Cocina = Cocina.VITRO_9,
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
    /** Título de otra cocinada viva que habría que abandonar para abrir esta cocina (solo se cocina una a la vez). */
    val conflicto: String? = null,
    /** Cómo se reparte la cocina (Ajustes): un carril en foco con el resto compacto, o todos apilados. */
    val disposicion: DisposicionCrono = DisposicionCrono.APILADA,
    /** Escuchando, sin permiso de micrófono, o apagada (ajuste, silenciada, sin reconocedor o fuera de pantalla). */
    val voz: EstadoVoz = EstadoVoz.APAGADA,
    /** El usuario ha silenciado la voz en esta cocinada (no se guarda). */
    val vozSilenciada: Boolean = false,
    /** El ajuste de voz está activo (con él, el botón de silenciar al Chef se ve siempre en la cocina). */
    val vozActiva: Boolean = false,
    /**
     * La guía de la primera cocinada ya se vio (Ajustes). Parte de true para que no parpadee antes de leer el ajuste.
     */
    val guiaVista: Boolean = true,
    /** Notas de las cocinadas anteriores de esta receta, de la más reciente a la más antigua. */
    val notas: List<NotaCocinada> = emptyList(),
)

private const val PERIODO_TIC_MS = 1_000L
const val VENTANA_REINICIAR_MS = 4_000L
const val VENTANA_DESHACER_MS = 12_000L
private const val MAX_LINEAS_BANDA = 4
private const val MS_MINUTO = 60_000L
private const val ETIQUETA_VOZ = "Voz"

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
        private val ajustes: AjustesRepo,
        private val escucha: Escucha,
        private val habla: Habla,
    ) : ViewModel() {
        private val slug: String = checkNotNull(savedStateHandle.get<String>(Rutas.ARG_SLUG))
        private val racionesRuta: Int = savedStateHandle.get<Int>(Rutas.ARG_RACIONES) ?: 0
        private val servirRuta: String = savedStateHandle.get<String>(Rutas.ARG_SERVIR).orEmpty()
        private val zona: ZoneId = ZoneId.systemDefault()

        private val _ui = MutableStateFlow(CronoUi())
        val ui: StateFlow<CronoUi> = _ui

        /** Cerrojos "mando:carril" contra el doble toque; se sueltan en el siguiente tic. */
        private val cerrojos = mutableSetOf<String>()
        private var jobReiniciar: Job? = null
        private var jobCierre: Job? = null
        private var cierre: (suspend () -> Unit)? = null
        private var otraSlug: String? = null

        private val control =
            ControlVoz(
                escucha = escucha,
                habla = habla,
                vistas = ::vistasAhora,
                foco = { _ui.value.estado?.foco },
                mandos =
                    MandosVoz(
                        hecho = ::hecho,
                        alternar = ::alternar,
                        prorrogar = ::prorrogar,
                        empezarIgual = ::empezarIgual,
                        deshacer = ::deshacer,
                        silenciarAlarma = { viewModelScope.launch { cadena.silenciar(slug) } },
                    ),
                registro = { Log.d(ETIQUETA_VOZ, "orden: $it") },
            )

        // Voz (ADR-0008): la pantalla avisa de si se ve y de si hay permiso; el resto lo decide SesionVoz.
        private val voz =
            SesionVoz(
                escucha = escucha,
                habla = habla,
                control = control,
                situacion = ::situacionVoz,
                alCambiar = { estado -> _ui.update { if (it.voz == estado) it else it.copy(voz = estado) } },
            )

        init {
            viewModelScope.launch {
                ajustes.observar().collect { a ->
                    voz.ajusteActivo = a.vozActiva
                    _ui.update {
                        conCocinaDe(it, a.cocina)
                            .copy(disposicion = a.disposicion, guiaVista = a.guiaCronoVista, vozActiva = a.vozActiva)
                    }
                    voz.reevaluar()
                }
            }
            viewModelScope.launch {
                // Viva mientras exista el VM; la nota de esta cocinada puede entrar al cerrar sin consecuencias.
                historial.observarTodo().collect { entradas ->
                    _ui.update { it.copy(notas = notasDe(entradas, slug)) }
                }
            }
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
            val cocina = ajustes.observar().first().cocina
            if (receta == null || receta.pasos.isEmpty()) {
                _ui.update { conCocinaDe(it.copy(cargando = false, receta = receta), cocina) }
                return
            }
            val ahora = reloj.ahoraMs()
            val guardado = cadena.almacen.leer(slug)
            val raciones = if (racionesRuta > 0) racionesRuta else receta.raciones
            // Sin hora en la ruta (p. ej. desde la notificación), la de Ajustes.
            val servir = servirRuta.ifBlank { Servir.formato(ajustes.observar().first().servirMin) }
            val estado = SesionCrono.reanudar(guardado, receta, raciones, servirDeHoy(servir, ahora, zona), ahora)
            cadena.almacen.fijar(estado)
            val enlaces =
                receta.tags.tecnica
                    .distinct()
                    .mapNotNull { s -> tecnicas.obtener(s)?.let { s to it.titulo } }
                    .toMap()
            _ui.update { conCocinaDe(it.copy(cargando = false, receta = receta, tecnicas = enlaces), cocina) }
            aplicar(estado, ahora)
            cadena.reprogramar()
        }

        /** Fija la [cocina] y deja `recetaUi` al día; solo traduce de nuevo si cambió la receta o la cocina. */
        private fun conCocinaDe(
            u: CronoUi,
            cocina: Cocina,
        ): CronoUi {
            val alDia = u.cocina == cocina && (u.receta == null) == (u.recetaUi == null)
            return if (alDia) u else u.copy(cocina = cocina, recetaUi = u.receta?.conCocina(cocina))
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
                        // Solo se consumen los avisos con la cocina a la vista: fuera de ella los emite la cadena de
                        // alarmas (alarma del sistema) y, si el tic se los llevara, solo sonaría un pitido.
                        if (cadena.presencia.visible == slug) {
                            val v = AvisosCrono.vencidos(e, receta, ahora)
                            avisos = v.avisos
                            v.estado
                        } else {
                            e
                        }
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
            val nombres = ConsultasCrono.carriles(receta).associate { it.id to it.nombre }
            val cocina = _ui.value.cocina
            if (suenan.isNotEmpty()) {
                // El texto del aviso sale de la receta canónica: se traduce a la cocina antes de oírlo.
                voz.anunciar(
                    suenan.map { "${nombres[it.carril] ?: it.carril}: ${Potencia.reescribir(it.texto, cocina)}" },
                )
            }
            val perdidos = avisos.filter { it.silencioso }
            if (perdidos.isEmpty()) return
            avisador.resumen()
            val lineas =
                perdidos.map {
                    LineaBanda(
                        nombres[it.carril] ?: it.carril,
                        Potencia.reescribir(it.texto, cocina),
                        ((ahora - it.instanteMs) / MS_MINUTO).toInt(),
                    )
                }
            _ui.update { it.copy(banda = (it.banda + lineas).takeLast(MAX_LINEAS_BANDA)) }
        }

        private fun prioridad(tipo: TipoAviso): Int =
            when (tipo) {
                TipoAviso.FIN -> 0
                TipoAviso.HITO -> 1
                else -> 2
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
            voz.reevaluar()
        }

        // --- mandos ---------------------------------------------------------------------------------------------

        /** "Abrir la cocina": fija el inicio y arma la cadena de alarmas. */
        fun abrirCocina() {
            viewModelScope.launch {
                val otra = cadena.otraViva(slug)
                if (otra == null) {
                    ejecutar { e, r, t -> SesionCrono.abrir(e, r, t) }
                } else {
                    otraSlug = otra.slug
                    val titulo = recetas.obtener(otra.slug)?.titulo ?: otra.slug
                    _ui.update { it.copy(conflicto = titulo) }
                }
            }
        }

        /** El usuario acepta abandonar la otra cocinada para abrir esta. */
        fun abandonarOtraYAbrir() {
            val otra = otraSlug
            otraSlug = null
            _ui.update { it.copy(conflicto = null) }
            viewModelScope.launch {
                if (otra != null) cadena.abandonar(otra)
                ejecutar { e, r, t -> SesionCrono.abrir(e, r, t) }
            }
        }

        fun descartarConflicto() {
            otraSlug = null
            _ui.update { it.copy(conflicto = null) }
        }

        /** Abandona esta cocinada: se borra el estado, sin historial, y se sale. */
        fun abandonar() {
            jobReiniciar?.cancel()
            viewModelScope.launch {
                cadena.abandonar(slug)
                _ui.update { it.copy(salida = true) }
            }
        }

        /**
         * La pantalla se ve (o vuelve a verse): con la cocina en pantalla no hay notificaciones, así que se marca la
         * presencia, se calla la alarma y se quita la fija (silenciar repinta).
         */
        fun alVer() {
            cadena.presencia.visible = slug
            viewModelScope.launch { cadena.silenciar(slug) }
        }

        fun marcar(
            idCasilla: String,
            marcada: Boolean,
        ) = mutar { e, _, t -> SesionCrono.marcar(e, idCasilla, marcada, t) }

        /** «Marcar todo» de un bloque: un único cambio de sesión para todas sus casillas. */
        fun marcarVarias(
            idsCasilla: List<String>,
            marcada: Boolean,
        ) = mutar { e, _, t -> idsCasilla.fold(e) { acc, id -> SesionCrono.marcar(acc, id, marcada, t) } }

        fun enfocar(carril: String) = mutar { e, _, t -> SesionCrono.enfocar(e, carril, t) }

        /** La misma preferencia que en Ajustes: la cocina se repinta con la nueva disposición sin reiniciarse. */
        fun elegirDisposicion(d: DisposicionCrono) {
            viewModelScope.launch { ajustes.fijarDisposicion(d) }
        }

        /** La guía de la cocina se acabó o se saltó: no vuelve a salir hasta que Ajustes la repita. */
        fun marcarGuiaVista() {
            _ui.update { it.copy(guiaVista = true) }
            viewModelScope.launch { ajustes.marcarGuiaCronoVista(true) }
        }

        fun cambiarServir(deltaMin: Int) =
            mutar { e, _, t -> SesionCrono.servir(e, e.servirMs + deltaMin * MS_MINUTO, t) }

        /** Si el carril era el fijado en el foco, se suelta: el foco salta solo al siguiente más urgente. */
        fun hecho(carril: String) =
            mando("hecho:$carril") { e, r, t ->
                val nuevo = MotorCrono.hecho(e, r, carril, t)
                if (e.foco == carril) soltarFoco(nuevo, r, t) else nuevo
            }

        private fun soltarFoco(
            e: EstadoCrono,
            receta: Receta,
            ahoraMs: Long,
        ): EstadoCrono {
            val urgente = elegirFoco(vistasCarriles(DatosCrono(receta, e, ahoraMs)), null)
            return if (urgente != null) SesionCrono.enfocar(e, urgente, ahoraMs) else e
        }

        // Acumulable: sin cerrojo (cada toque suma 30 s); `mutar` ya serializa vía `almacen.modificar`.
        fun masTreinta(carril: String) = mutar { e, r, t -> MotorCrono.masTreinta(e, r, carril, t) }

        /** +[segundos] en el carril (voz: «chef dos minutos más al 2»); acumulable como [masTreinta]. */
        fun prorrogar(
            carril: String,
            segundos: Int,
        ) = mutar { e, r, t -> MotorCrono.prorrogar(e, r, carril, segundos, t) }

        /** Deshace el último Hecho de [carril] o, sin carril, el último de la cocina. Un nivel por carril. */
        fun deshacer(carril: String?) = mando("deshacer:$carril") { e, r, t -> MotorCrono.deshacer(e, r, carril, t) }

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

        /** La pantalla deja de verse: reaparece la fija y la cadena queda armada con el estado persistido. */
        fun enSegundoPlano() {
            cadena.presencia.visible = null
            confirmarCierre()
            viewModelScope.launch { cadena.reprogramar() }
        }

        /**
         * Cierre con 12 s para deshacer: se quitan avisos y ongoing, pero el historial y el borrado del estado solo se
         * confirman al vencer la ventana o al salir ([confirmarCierre]).
         */
        fun cerrar(nota: String) {
            val actual = _ui.value
            val receta = actual.receta
            val estado = actual.estado
            val resumen = actual.resumen
            if (actual.cierrePendiente) return
            if (receta == null || estado == null || resumen == null) return
            val entrada = entradaDe(receta, estado, resumen, nota, zona)
            cierre = {
                historial.agregar(entrada)
                cadena.almacen.borrar(slug)
                cadena.reprogramar()
                _ui.update { it.copy(salida = true) }
            }
            _ui.update { it.copy(cierrePendiente = true) }
            voz.reevaluar()
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
            voz.reevaluar()
        }

        /** Confirma ya un cierre pendiente (vence la ventana o se sale de la pantalla). */
        fun confirmarCierre() {
            val pendiente = cierre ?: return
            cierre = null
            jobCierre?.cancel()
            jobCierre = null
            viewModelScope.launch { withContext(NonCancellable) { pendiente() } }
        }

        // --- voz ------------------------------------------------------------------------------------------------

        /** La cocina se ve: escucha si el ajuste está activo, hay reconocedor y [permiso] de micrófono. */
        fun vozIniciar(permiso: Boolean) = voz.iniciar(permiso)

        fun vozParar() = voz.parar()

        /** Silencia o reactiva la voz de esta cocinada; no se guarda. */
        fun silenciarVoz() {
            voz.olvidarCaida()
            _ui.update { it.copy(vozSilenciada = !it.vozSilenciada) }
            voz.reevaluar()
        }

        private fun situacionVoz(): SituacionVoz {
            val u = _ui.value
            return SituacionVoz(u.estado?.inicioMs != null && u.resumen == null && !u.cierrePendiente, u.vozSilenciada)
        }

        private fun vistasAhora(): List<VistaCarril> {
            val u = _ui.value
            val receta = u.receta
            val estado = u.estado
            return if (receta == null || estado == null) {
                emptyList()
            } else {
                vistasCarriles(DatosCrono(receta, estado, reloj.ahoraMs(), u.cocina, u.recetaUi ?: receta))
            }
        }

        override fun onCleared() {
            voz.liberar()
            super.onCleared()
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
