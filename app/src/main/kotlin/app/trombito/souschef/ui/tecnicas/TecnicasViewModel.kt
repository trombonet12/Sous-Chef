package app.trombito.souschef.ui.tecnicas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.TecnicasRepo
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tecnica
import app.trombito.souschef.receta.conCocina
import app.trombito.souschef.ui.Rutas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TecnicasViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        tecnicas: TecnicasRepo,
        ajustes: AjustesRepo,
    ) : ViewModel() {
        private val categoriaFija: CategoriaTecnica? =
            CategoriaTecnica.deClave(savedStateHandle.get<String>(Rutas.ARG_CATEGORIA))
        private val consulta = MutableStateFlow("")

        val estado: StateFlow<TecnicasUiState> =
            combine(
                tecnicas.observarTodas(),
                consulta,
                ajustes.observar().map { it.lista }.distinctUntilChanged(),
            ) { t, q, lista ->
                construirEstadoTecnicas(t, q, categoriaFija, lista)
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TecnicasUiState(categoriaFija = categoriaFija),
            )

        fun buscar(texto: String) {
            consulta.value = texto
        }
    }

data class TecnicaUiState(
    val cargando: Boolean = true,
    /** Técnica ya traducida a [cocina] (`Tecnica.conCocina`). */
    val tecnica: Tecnica? = null,
    val cocina: Cocina = Cocina.VITRO_9,
    val recetas: List<Receta> = emptyList(),
)

@HiltViewModel
class TecnicaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        tecnicas: TecnicasRepo,
        recetas: RecetasRepo,
        ajustes: AjustesRepo,
    ) : ViewModel() {
        private val slug: String = checkNotNull(savedStateHandle[Rutas.ARG_SLUG])

        val estado: StateFlow<TecnicaUiState> =
            combine(
                tecnicas.observar(slug),
                recetas.observarTodas(),
                ajustes.observar().map { it.cocina }.distinctUntilChanged(),
            ) { t, r, cocina ->
                TecnicaUiState(
                    cargando = false,
                    tecnica = t?.conCocina(cocina),
                    cocina = cocina,
                    recetas = recetasConTecnica(slug, r),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TecnicaUiState())
    }
