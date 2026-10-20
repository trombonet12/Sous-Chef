package app.trombito.souschef.ui.tecnicas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.TecnicasRepo
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tecnica
import app.trombito.souschef.ui.Rutas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TecnicasViewModel
    @Inject
    constructor(
        tecnicas: TecnicasRepo,
    ) : ViewModel() {
        private val consulta = MutableStateFlow("")
        private val categoria = MutableStateFlow<String?>(null)

        val estado: StateFlow<TecnicasUiState> =
            combine(tecnicas.observarTodas(), consulta, categoria) { t, q, c ->
                construirEstadoTecnicas(t, q, c)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TecnicasUiState())

        fun buscar(texto: String) {
            consulta.value = texto
        }

        fun elegirCategoria(nueva: String?) {
            categoria.value = nueva
        }
    }

data class TecnicaUiState(
    val cargando: Boolean = true,
    val tecnica: Tecnica? = null,
    val recetas: List<Receta> = emptyList(),
)

@HiltViewModel
class TecnicaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        tecnicas: TecnicasRepo,
        recetas: RecetasRepo,
    ) : ViewModel() {
        private val slug: String = checkNotNull(savedStateHandle[Rutas.ARG_SLUG])

        val estado: StateFlow<TecnicaUiState> =
            combine(tecnicas.observar(slug), recetas.observarTodas()) { t, r ->
                TecnicaUiState(cargando = false, tecnica = t, recetas = recetasConTecnica(slug, r))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TecnicaUiState())
    }
