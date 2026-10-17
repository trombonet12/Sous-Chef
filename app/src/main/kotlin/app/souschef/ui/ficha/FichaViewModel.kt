package app.souschef.ui.ficha

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.souschef.di.ApplicationScope
import app.souschef.domain.CompraRepo
import app.souschef.domain.DespensaRepo
import app.souschef.domain.PreferenciasRepo
import app.souschef.domain.RecetasRepo
import app.souschef.domain.TecnicasRepo
import app.souschef.receta.Escalado
import app.souschef.ui.Rutas
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

/** Retardo de guardado de las raciones tras el último toque (docs/spec/app-legado.md §2). */
const val DEBOUNCE_RACIONES_MS = 600L
private const val DURACION_MENSAJE_MS = 3_000L

/** Lo que el usuario ha tocado en esta ficha y aún no vive en la base de datos. */
private data class Local(
    val raciones: Int? = null,
    val servirMin: Int = Servir.DEFECTO,
    val mensaje: MensajeFicha? = null,
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
        private val compra: CompraRepo,
        despensa: DespensaRepo,
        @ApplicationScope private val ambito: CoroutineScope,
    ) : ViewModel() {
        private var pendiente: Int? = null
        private val slug: String = checkNotNull(savedStateHandle[Rutas.ARG_SLUG])
        private val local = MutableStateFlow(Local())
        private var jobGuardar: Job? = null
        private var jobMensaje: Job? = null

        val estado: StateFlow<FichaUiState> =
            combine(
                recetas.observar(slug),
                preferencias.observar(slug),
                tecnicas.observarTodas(),
                despensa.observar(),
                local,
            ) { receta, pref, existentes, items, l ->
                val raciones = l.raciones ?: pref?.raciones ?: receta?.raciones ?: 2
                FichaUiState(
                    cargando = false,
                    receta = receta,
                    raciones = raciones,
                    faltas = receta?.let { faltas(it, raciones, items) }.orEmpty(),
                    servirMin = l.servirMin,
                    tecnicas = receta?.let { enlacesTecnicas(it, existentes) }.orEmpty(),
                    mensaje = l.mensaje,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FichaUiState())

        /** Cambia las raciones al instante y las guarda 600 ms después del último toque. */
        fun cambiarRaciones(nuevas: Int) {
            val valor = nuevas.coerceIn(Escalado.RACIONES_MIN, Escalado.RACIONES_MAX)
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

        fun ajustarServir(delta: Int) {
            local.update { it.copy(servirMin = Servir.ajustar(it.servirMin, delta)) }
        }

        fun anadirALaCompra() {
            val actual = estado.value
            val receta = actual.receta ?: return
            if (actual.faltas.isEmpty()) return
            viewModelScope.launch {
                compra.agregarVarios(actual.faltas.map { it.linea }, receta.slug)
                avisar(MensajeFicha.ANADIDA)
            }
        }

        /** Texto plano de "Copiar": una línea por ingrediente que falta, o null si no falta nada. */
        fun textoCompra(): String? =
            estado.value.faltas
                .takeIf { it.isNotEmpty() }
                ?.joinToString("\n") { it.linea }

        fun avisar(mensaje: MensajeFicha) {
            local.update { it.copy(mensaje = mensaje) }
            jobMensaje?.cancel()
            jobMensaje =
                viewModelScope.launch {
                    delay(DURACION_MENSAJE_MS)
                    local.update { it.copy(mensaje = null) }
                }
        }
    }
