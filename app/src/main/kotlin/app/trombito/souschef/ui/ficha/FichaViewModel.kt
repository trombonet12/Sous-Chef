package app.trombito.souschef.ui.ficha

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.di.ApplicationScope
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.CualHora
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.TecnicasRepo
import app.trombito.souschef.domain.ahoraMin
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.domain.crono.GestorRecordatorio
import app.trombito.souschef.domain.crono.Recordatorio
import app.trombito.souschef.domain.crono.RecordatorioEmpezar
import app.trombito.souschef.domain.desvioHabitualMin
import app.trombito.souschef.domain.entradaAnotada
import app.trombito.souschef.domain.horaServir
import app.trombito.souschef.domain.notasDe
import app.trombito.souschef.domain.resumenDeReceta
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.conCocina
import app.trombito.souschef.ui.Rutas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ceil

/** Retardo de guardado de las raciones tras el último toque (docs/spec/app-legado.md §2). */
const val DEBOUNCE_RACIONES_MS = 600L

/** Lo que el usuario ha tocado en esta ficha y aún no vive en la base de datos. */
private data class Local(
    val raciones: Int? = null,
    /** Hora de servir solo para esta vez; null = la de Ajustes que toque (comer o cenar). */
    val servirMin: Int? = null,
)

/** Las fuentes que cambian por su cuenta, agrupadas para no pasar de cinco flujos en el `combine` principal. */
private data class Fuentes(
    val local: Local,
    val ajustes: Ajustes,
    val historial: List<EntradaHistorial>,
    val recordatorio: Recordatorio?,
)

@HiltViewModel
class FichaViewModel
    @Suppress("LongParameterList")
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        recetas: RecetasRepo,
        tecnicas: TecnicasRepo,
        private val preferencias: PreferenciasRepo,
        private val cadena: CadenaAlarmas,
        ajustes: AjustesRepo,
        private val historialRepo: HistorialRepo,
        private val reloj: Reloj,
        private val recordatorio: GestorRecordatorio,
        @ApplicationScope private val ambito: CoroutineScope,
    ) : ViewModel() {
        private var pendiente: Int? = null
        private val slug: String = checkNotNull(savedStateHandle[Rutas.ARG_SLUG])
        private val local = MutableStateFlow(Local())
        private var jobGuardar: Job? = null

        val estado: StateFlow<FichaUiState> =
            combine(
                recetas.observar(slug),
                preferencias.observar(slug),
                tecnicas.observarTodas(),
                cadena.vivas.viva(slug),
                combine(
                    local,
                    ajustes.observar(),
                    historialRepo.observarTodo(),
                    recordatorio.observar(),
                ) { l, a, h, puesto -> Fuentes(l, a, h, puesto) },
            ) { receta, pref, existentes, viva, fuentes ->
                val (l, a, h) = fuentes
                val permitidas = receta?.let { Escalado.racionesPermitidas(it) } ?: FichaUiState().racionesPermitidas
                val guardada = l.raciones ?: pref?.raciones
                val raciones =
                    l.raciones?.takeIf { it in permitidas }
                        ?: receta?.let { Escalado.racionesIniciales(it, guardada, a.racionesDefecto) }
                        ?: guardada
                        ?: a.racionesDefecto
                val usada = horaServir(receta?.tags?.momento, reloj.ahoraMin(), a)
                val servirMin = l.servirMin ?: usada.minutos
                val ritmoMin = receta?.let { desvioHabitualMin(h, slug, ceil(it.totalEfectivo).toInt()) }
                FichaUiState(
                    cargando = false,
                    receta = receta?.conCocina(a.cocina),
                    cocina = a.cocina,
                    raciones = raciones,
                    racionesPermitidas = permitidas,
                    ajustadoAlRango =
                        receta != null && guardada !in permitidas && a.racionesDefecto !in permitidas,
                    servirMin = servirMin,
                    paraCenar = l.servirMin == null && usada.cual == CualHora.CENAR,
                    favorita = pref?.favorito == true,
                    quiereHacerla = pref?.quiereHacerla == true,
                    tecnicas = receta?.let { enlacesTecnicas(it, existentes) }.orEmpty(),
                    cocinando = viva?.let { e -> e.carriles.values.count { !it.fin } },
                    notas = notasDe(h, slug),
                    ritmoMin = ritmoMin,
                    equipo = a.equipo,
                    historial = receta?.let { resumenDeReceta(h, slug, it.totalEfectivo) },
                    aviso =
                        RecordatorioEmpezar.chip(
                            slug = slug,
                            empezarMin = receta?.let { Servir.inicioConRitmo(Servir.inicio(servirMin, it), ritmoMin) },
                            puesto = fuentes.recordatorio,
                            hayViva = cadena.vivas.hayViva(),
                            ahoraMs = reloj.ahoraMs(),
                            zona = ZoneId.systemDefault(),
                        ),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FichaUiState())

        /**
         * Cambia las raciones al instante (a la permitida más cercana) y las guarda 600 ms después del último
         * toque.
         */
        fun cambiarRaciones(nuevas: Int) {
            val permitidas = estado.value.racionesPermitidas
            val valor = permitidas.minWithOrNull(compareBy({ abs(it - nuevas) }, { it })) ?: return
            local.update { it.copy(raciones = valor) }
            pendiente = valor
            jobGuardar?.cancel()
            jobGuardar =
                viewModelScope.launch {
                    delay(DEBOUNCE_RACIONES_MS)
                    pendiente = null
                    preferencias.fijarRaciones(slug, valor)
                }
        }

        /** Si se sale antes del debounce, guarda el valor pendiente en un scope que sobrevive. */
        internal fun guardarPendiente() {
            val valor = pendiente ?: return
            pendiente = null
            jobGuardar?.cancel()
            ambito.launch { preferencias.fijarRaciones(slug, valor) }
        }

        override fun onCleared() {
            guardarPendiente()
        }

        /** Salir de la ficha con la cocinada viva la abandona; en un scope que sobrevive a la salida. */
        fun abandonarCocinada() {
            ambito.launch { cadena.abandonar(slug) }
        }

        fun alternarFavorita() {
            val nueva = !estado.value.favorita
            viewModelScope.launch { preferencias.fijarFavorito(slug, nueva) }
        }

        fun alternarQuiereHacerla() {
            val nueva = !estado.value.quiereHacerla
            viewModelScope.launch { preferencias.fijarQuiereHacerla(slug, nueva) }
        }

        /**
         * «Anótala sin cronómetro»: guarda una cocinada hecha sin la app en [momento] (hora local), con [raciones] y
         * una [nota] opcional. Sin pasos ni duración real: cuenta para «hace…» y «Nueva», no para «Tu ritmo». Va en
         * el scope de la aplicación para no perderse si se sale de la ficha justo después.
         */
        fun anotarCocinada(
            momento: LocalDateTime,
            raciones: Int,
            nota: String,
        ) {
            val receta = estado.value.receta ?: return
            ambito.launch { historialRepo.agregar(entradaAnotada(receta, raciones, momento, nota)) }
        }

        /**
         * «Avísame a las HH:mm»: arma el recordatorio para empezar con la hora que enseña la ficha (la del ritmo). En
         * el scope de la aplicación: no debe perderse si se sale de la ficha justo después de tocar.
         */
        fun avisarParaEmpezar() {
            val e = estado.value
            val chip = e.aviso?.takeUnless { it.puesto } ?: return
            ambito.launch { recordatorio.armar(slug, chip.horaMin, e.servirMin) }
        }

        fun quitarAviso() {
            ambito.launch { recordatorio.quitar() }
        }

        fun ajustarServir(delta: Int) {
            local.update { it.copy(servirMin = Servir.ajustar(it.servirMin ?: estado.value.servirMin, delta)) }
        }
    }
