package app.trombito.souschef.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Cuánto se puede deshacer un borrado: lo que dura el aviso de «Cocinada borrada». */
const val VENTANA_DESHACER_MS = 6_000L

@HiltViewModel
class HistorialViewModel
    @Inject
    constructor(
        private val historial: HistorialRepo,
        recetas: RecetasRepo,
        ajustes: AjustesRepo,
        private val reloj: Reloj,
    ) : ViewModel() {
        private val consulta = MutableStateFlow("")
        private val borrada = MutableStateFlow<EntradaHistorial?>(null)

        val estado: StateFlow<HistorialUiState> =
            combine(
                historial.observarTodo(),
                recetas.observarTodas().map { r -> r.map { it.slug }.toSet() },
                consulta,
                borrada,
                ajustes.observar().map { it.lista }.distinctUntilChanged(),
            ) { entradas, slugs, q, b, d ->
                val hoy = Instant.ofEpochMilli(reloj.ahoraMs()).atZone(ZoneId.systemDefault()).toLocalDate()
                construirEstadoHistorial(entradas, slugs, q, b, hoy, d)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

        fun buscar(texto: String) {
            consulta.value = texto
        }

        /** Borra de un toque y guarda la entrada para poder [deshacerBorrado]; solo se recuerda la última. */
        fun borrar(entrada: EntradaHistorial) {
            viewModelScope.launch {
                historial.borrar(entrada.id)
                borrada.value = entrada
            }
        }

        /** Vuelve a guardar la última borrada, tal cual (con su valoración y su nota). */
        fun deshacerBorrado() {
            val entrada = borrada.value ?: return
            borrada.value = null
            viewModelScope.launch { historial.agregar(entrada) }
        }

        /** Se acabó el tiempo de deshacer: ya no se puede recuperar. */
        fun descartarBorrado() {
            borrada.value = null
        }

        /** Cambia solo la nota de la cocinada [id] (un UPDATE puntual); vacía la quita. */
        fun editarNota(
            id: String,
            nota: String,
        ) {
            viewModelScope.launch { historial.editarNota(id, nota) }
        }
    }
