package app.trombito.souschef.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.RecetasRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Ventana para confirmar el borrado con un segundo toque. */
const val VENTANA_BORRAR_MS = 3_000L

@HiltViewModel
class HistorialViewModel
    @Inject
    constructor(
        private val historial: HistorialRepo,
        recetas: RecetasRepo,
        ajustes: AjustesRepo,
    ) : ViewModel() {
        private val consulta = MutableStateFlow("")
        private val pendiente = MutableStateFlow<String?>(null)
        private var jobPendiente: Job? = null

        val estado: StateFlow<HistorialUiState> =
            combine(
                historial.observarTodo(),
                recetas.observarTodas().map { r -> r.map { it.slug }.toSet() },
                consulta,
                pendiente,
                ajustes.observar().map { it.lista }.distinctUntilChanged(),
            ) { entradas, slugs, q, p, d ->
                construirEstadoHistorial(entradas, slugs, q, p, d)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

        fun buscar(texto: String) {
            consulta.value = texto
        }

        /** Primer toque: pide confirmación durante 3 s. Segundo toque sobre la misma entrada: borra. */
        fun tocarBorrar(id: String) {
            jobPendiente?.cancel()
            if (pendiente.value == id) {
                pendiente.value = null
                viewModelScope.launch { historial.borrar(id) }
            } else {
                pendiente.value = id
                jobPendiente =
                    viewModelScope.launch {
                        delay(VENTANA_BORRAR_MS)
                        pendiente.value = null
                    }
            }
        }
    }
