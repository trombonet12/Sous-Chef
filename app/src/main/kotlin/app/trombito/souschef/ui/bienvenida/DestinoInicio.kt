package app.trombito.souschef.ui.bienvenida

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.shell.Pestana
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Primera pantalla: la intro si aún no se vio; si no, Recetas. */
fun destinoInicial(ajustes: Ajustes): String = if (ajustes.introVista) Pestana.Recetas.ruta else Rutas.BIENVENIDA

/**
 * Decide el destino de inicio **una sola vez**, con los ajustes ya cargados (una lectura, no un flujo): guardar una
 * elección en mitad de la intro no puede cambiar la pantalla de inicio. `null` hasta tenerlo.
 */
@HiltViewModel
class DestinoInicioViewModel
    @Inject
    constructor(
        ajustes: AjustesRepo,
    ) : ViewModel() {
        private val _destino = MutableStateFlow<String?>(null)
        val destino: StateFlow<String?> = _destino

        init {
            viewModelScope.launch { _destino.value = destinoInicial(ajustes.observar().first()) }
        }
    }
