package app.trombito.souschef.ui.buzon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.AjustesRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Estado del formulario del Buzón (vive mientras la pantalla está en la pila) y los ajustes para el pie del correo. */
@HiltViewModel
class BuzonViewModel
    @Inject
    constructor(
        ajustes: AjustesRepo,
    ) : ViewModel() {
        private val _formulario = MutableStateFlow(FormularioBuzon())
        val formulario: StateFlow<FormularioBuzon> = _formulario.asStateFlow()

        val ajustes: StateFlow<Ajustes> =
            ajustes.observar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ajustes())

        fun editar(cambio: (FormularioBuzon) -> FormularioBuzon) {
            _formulario.update(cambio)
        }
    }
