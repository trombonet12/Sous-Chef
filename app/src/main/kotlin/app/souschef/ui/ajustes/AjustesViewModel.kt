package app.souschef.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.souschef.domain.Ajustes
import app.souschef.domain.AjustesRepo
import app.souschef.domain.DisposicionCrono
import dagger.hilt.android.lifecycle.HiltViewModel
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
    ) : ViewModel() {
        val estado: StateFlow<Ajustes> =
            ajustes.observar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ajustes())

        fun elegirDisposicion(d: DisposicionCrono) {
            viewModelScope.launch { ajustes.fijarDisposicion(d) }
        }

        fun activarVoz(activa: Boolean) {
            viewModelScope.launch { ajustes.fijarVoz(activa) }
        }
    }
