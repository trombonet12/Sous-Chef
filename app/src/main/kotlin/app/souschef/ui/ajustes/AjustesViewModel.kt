package app.souschef.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.souschef.domain.Ajustes
import app.souschef.domain.AjustesRepo
import app.souschef.domain.DisposicionCrono
import app.souschef.domain.copia.GestorCopia
import app.souschef.domain.copia.ResultadoCopia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AjustesViewModel
    @Inject
    constructor(
        private val ajustes: AjustesRepo,
        private val copias: GestorCopia,
    ) : ViewModel() {
        val estado: StateFlow<Ajustes> =
            ajustes.observar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ajustes())

        private val _copia = MutableStateFlow<ResultadoCopia?>(null)

        /** Resultado de la última copia guardada o cargada (para el mensaje bajo los botones). */
        val copia: StateFlow<ResultadoCopia?> = _copia

        /** [destino]: URI elegida con el selector del sistema. */
        fun guardarCopia(destino: String) {
            viewModelScope.launch { _copia.value = copias.exportar(destino) }
        }

        fun cargarCopia(origen: String) {
            viewModelScope.launch { _copia.value = copias.importar(origen) }
        }

        fun elegirDisposicion(d: DisposicionCrono) {
            viewModelScope.launch { ajustes.fijarDisposicion(d) }
        }

        fun activarVoz(activa: Boolean) {
            viewModelScope.launch { ajustes.fijarVoz(activa) }
        }
    }
