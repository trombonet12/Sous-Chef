package app.souschef.ui.ficha

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.souschef.domain.CompraRepo
import app.souschef.domain.PreferenciasRepo
import app.souschef.domain.RecetasRepo
import app.souschef.domain.TecnicasRepo
import app.souschef.receta.Escalado
import app.souschef.ui.Rutas
import dagger.hilt.android.lifecycle.HiltViewModel
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
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        recetas: RecetasRepo,
        tecnicas: TecnicasRepo,
        private val preferencias: PreferenciasRepo,
        private val compra: CompraRepo,
    ) : ViewModel() {
        private val slug: String = checkNotNull(savedStateHandle[Rutas.ARG_SLUG])
        private val local = MutableStateFlow(Local())
        private var jobGuardar: Job? = null
        private var jobMensaje: Job? = null

        val estado: StateFlow<FichaUiState> =
            combine(
                recetas.observar(slug),
                preferencias.observar(slug),
                tecnicas.observarTodas(),
                local,
            ) { receta, pref, existentes, l ->
                FichaUiState(
                    cargando = false,
                    receta = receta,
                    raciones = l.raciones ?: pref?.raciones ?: receta?.raciones ?: 2,
                    servirMin = l.servirMin,
                    tecnicas = receta?.let { enlacesTecnicas(it, existentes) }.orEmpty(),
                    mensaje = l.mensaje,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FichaUiState())

        /** Cambia las raciones al instante y las guarda 600 ms después del último toque. */
        fun cambiarRaciones(nuevas: Int) {
            val valor = nuevas.coerceIn(Escalado.RACIONES_MIN, Escalado.RACIONES_MAX)
            local.update { it.copy(raciones = valor) }
            jobGuardar?.cancel()
            jobGuardar =
                viewModelScope.launch {
                    delay(DEBOUNCE_RACIONES_MS)
                    preferencias.fijarRaciones(slug, valor)
                }
        }

        fun ajustarServir(delta: Int) {
            local.update { it.copy(servirMin = Servir.ajustar(it.servirMin, delta)) }
        }

        fun anadirALaCompra() {
            val actual = estado.value
            val receta = actual.receta ?: return
            viewModelScope.launch {
                compra.agregarVarios(lineasCompra(receta, actual.raciones), receta.slug)
                avisar(MensajeFicha.ANADIDA)
            }
        }

        /** Texto plano de "Copiar la lista", o null si la receta aún no ha cargado. */
        fun textoCompra(): String? = estado.value.let { s -> s.receta?.let { Escalado.textoCompra(it, s.raciones) } }

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
