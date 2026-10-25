package app.trombito.souschef.ui.ficha

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.di.ApplicationScope
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.CualHora
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.TecnicasRepo
import app.trombito.souschef.domain.ahoraMin
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.domain.desvioHabitualMin
import app.trombito.souschef.domain.horaServir
import app.trombito.souschef.domain.notasDe
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
        historial: HistorialRepo,
        private val reloj: Reloj,
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
                    historial.observarTodo(),
                ) { l, a, h -> Triple(l, a, h) },
            ) { receta, pref, existentes, viva, (l, a, h) ->
                val permitidas = receta?.let { Escalado.racionesPermitidas(it) } ?: FichaUiState().racionesPermitidas
                val guardada = l.raciones ?: pref?.raciones
                val raciones =
                    l.raciones?.takeIf { it in permitidas }
                        ?: receta?.let { Escalado.racionesIniciales(it, guardada, a.racionesDefecto) }
                        ?: guardada
                        ?: a.racionesDefecto
                val usada = horaServir(receta?.tags?.momento, reloj.ahoraMin(), a)
                FichaUiState(
                    cargando = false,
                    receta = receta?.conCocina(a.cocina),
                    cocina = a.cocina,
                    raciones = raciones,
                    racionesPermitidas = permitidas,
                    ajustadoAlRango =
                        receta != null && guardada !in permitidas && a.racionesDefecto !in permitidas,
                    servirMin = l.servirMin ?: usada.minutos,
                    paraCenar = l.servirMin == null && usada.cual == CualHora.CENAR,
                    favorita = pref?.favorito == true,
                    tecnicas = receta?.let { enlacesTecnicas(it, existentes) }.orEmpty(),
                    cocinando = viva?.let { e -> e.carriles.values.count { !it.fin } },
                    notas = notasDe(h, slug),
                    ritmoMin = receta?.let { desvioHabitualMin(h, slug, ceil(it.totalEfectivo).toInt()) },
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

        fun ajustarServir(delta: Int) {
            local.update { it.copy(servirMin = Servir.ajustar(it.servirMin ?: estado.value.servirMin, delta)) }
        }
    }
